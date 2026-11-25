/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The JNI calls to the Kotlin object `com.denariidolor.VaultKeystore`
//! (`android/app/src/main/kotlin/com/denariidolor/VaultKeystore.kt`): the only hand-written
//! `unsafe` code in the workspace (ADR-0022). Compiled for Android only.
//!
//! How a call reaches Kotlin, as M3.1's spike found (ADR-0007):
//!
//! 1. `MainActivity` loads `libdenarii_bridge.so` with `System.loadLibrary`, so the runtime calls
//!    [`JNI_OnLoad`] on that thread, where `FindClass` sees the app's classes. It keeps the
//!    `JavaVM`, and global references to `VaultKeystore` and `KeystoreException` with the IDs of
//!    their members, because a thread attached from native code sees only the system's classes.
//! 2. Each call attaches its thread to the runtime unless the thread is attached already
//!    ([`Attached`]), and detaches it again when the call ends.
//! 3. It copies each argument into a new Java object, calls the static method, takes any exception
//!    the method threw, and copies the `byte[]` it returned. Before it returns, every `byte[]` it
//!    passed or received is overwritten with zeros, and every local reference is deleted.
//!
//! Who owns what: the runtime owns the `JavaVM` and each thread's `JNIEnv`. The two global class
//! references live until the process ends, and nothing deletes them. Each local reference is a
//! [`Local`], whose `Drop` deletes it, and each `byte[]` is a [`ByteArray`], whose `Drop` wipes it
//! first.
//!
//! The declarations are `jni-sys` 0.4.1's, of the JNI specification's `jni.h`. Everything here
//! asks for JNI 1.6, which Android's runtime implements on API 26 and later, so the 1.6 view of
//! each function table holds every function this module calls.
//!
//! The safety argument rests on one more rule, which every method of [`Env`] keeps: no Java
//! exception is pending between them. JNI allows only a few functions while one is pending, so
//! each method that can raise one takes it before it returns.

use std::ffi::{CStr, CString, c_void};
use std::marker::PhantomData;
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::ptr::{self, NonNull};
use std::sync::OnceLock;

use jni_sys::{
    _jobject, JNI_EDETACHED, JNI_OK, JNI_VERSION_1_6, JNIEnv, JNIInvokeInterface_,
    JNIInvokeInterface__1_4, JNINativeInterface_, JNINativeInterface__1_6, JavaVM,
    JavaVMAttachArgs, jbyte, jfieldID, jint, jmethodID, jobject, jsize, jvalue,
};

use crate::KeystoreError;

/// The codes of `KeystoreFailure` in `VaultKeystore.kt`, which `KeystoreException.code` carries.
/// Keep the two lists the same.
const KEY_MISSING: jint = 1;
const UNREADABLE: jint = 2;
const FAILED: jint = 3;

/// What [`JNI_OnLoad`] found: the runtime, the two classes, and the IDs of the members this
/// module uses. R8 keeps each of these names in the release build
/// (`android/app/proguard-rules.pro`).
struct Loaded {
    vm: NonNull<JavaVM>,
    /// `com.denariidolor.VaultKeystore`.
    keystore: Class,
    /// `com.denariidolor.KeystoreException`.
    exception: Class,
    /// `int KeystoreException.code`.
    code: jfieldID,
    /// `static byte[] deviceHmac(String aliasPrefix, byte[] message)`.
    device_hmac: jmethodID,
    /// `static void ensureDeviceKey(String aliasPrefix)`.
    ensure_device_key: jmethodID,
    /// `static byte[] readProfile(String aliasPrefix, String prefsName)`, null without an entry.
    read_profile: jmethodID,
    /// `static void writeProfile(String aliasPrefix, String prefsName, byte[] plaintext)`.
    write_profile: jmethodID,
    /// `static void deleteProfile(String prefsName)`.
    delete_profile: jmethodID,
    /// `static void deleteKeys(String aliasPrefix)`.
    delete_keys: jmethodID,
}

// SAFETY: the JNI specification makes a `JavaVM` pointer, a global reference, and a method or
// field ID valid on every thread of the process until it is deleted or its class is unloaded.
// Nothing deletes them, and the app's class loader, which loaded both classes, lives as long as
// the process.
unsafe impl Send for Loaded {}
// SAFETY: as for `Send`; nothing in `Loaded` changes after `JNI_OnLoad` writes it.
unsafe impl Sync for Loaded {}

/// Set once, by [`JNI_OnLoad`]: `None` when it couldn't find what this module needs.
static LOADED: OnceLock<Option<Loaded>> = OnceLock::new();

/// A global reference to a class, which lives until the process ends.
#[derive(Clone, Copy)]
struct Class(NonNull<_jobject>);

impl Class {
    fn raw(self) -> jobject {
        self.0.as_ptr()
    }
}

/// Called by the Java runtime when `MainActivity` loads this library with `System.loadLibrary`,
/// on a thread attached to the runtime, whose class loader is the app's.
///
/// It returns `JNI_VERSION_1_6` even when it can't find the Kotlin code, so that loading the
/// library still succeeds and the app still starts; every call then returns
/// [`KeystoreError::Unavailable`].
#[unsafe(no_mangle)]
pub(crate) extern "system" fn JNI_OnLoad(vm: *mut JavaVM, _reserved: *mut c_void) -> jint {
    // A panic must not unwind into the Java runtime.
    let loaded = catch_unwind(AssertUnwindSafe(|| {
        NonNull::new(vm).and_then(|vm| {
            // SAFETY: the runtime passes its own `JavaVM`, on a thread it has attached.
            unsafe { load(vm) }
        })
    }))
    .ok()
    .flatten();
    // The runtime loads a library once per process, so this is the only `set`. If it were
    // called again, the first result would stay, which is as good as the second.
    let _ = LOADED.set(loaded);
    JNI_VERSION_1_6
}

/// Finds the classes and members this module uses, on the thread that runs `JNI_OnLoad`.
///
/// # Safety
///
/// `vm` is the runtime's `JavaVM`, and the calling thread is attached to it, with the app's class
/// loader.
unsafe fn load(vm: NonNull<JavaVM>) -> Option<Loaded> {
    // SAFETY: the caller passes the runtime's `JavaVM`.
    let functions = unsafe { invoke_functions(vm) };
    let mut env: *mut c_void = ptr::null_mut();
    // SAFETY: the runtime's `JavaVM`, and a valid place for the environment pointer.
    let code = unsafe { (functions.GetEnv)(vm.as_ptr(), &raw mut env, JNI_VERSION_1_6) };
    if code != JNI_OK {
        return None;
    }
    let env = NonNull::new(env.cast::<JNIEnv>())?;
    // SAFETY: `GetEnv` succeeded for JNI 1.6, so `env` is this thread's environment, and the
    // runtime keeps the thread attached until `JNI_OnLoad` returns, after `env` is gone.
    let env = unsafe { Env::new(env) };
    env.clear_exception();
    let keystore = env.global_class(c"com/denariidolor/VaultKeystore")?;
    let exception = env.global_class(c"com/denariidolor/KeystoreException")?;
    Some(Loaded {
        vm,
        code: env.field_id(exception, c"code", c"I")?,
        device_hmac: env.static_method_id(keystore, c"deviceHmac", c"(Ljava/lang/String;[B)[B")?,
        ensure_device_key: env.static_method_id(
            keystore,
            c"ensureDeviceKey",
            c"(Ljava/lang/String;)V",
        )?,
        read_profile: env.static_method_id(
            keystore,
            c"readProfile",
            c"(Ljava/lang/String;Ljava/lang/String;)[B",
        )?,
        write_profile: env.static_method_id(
            keystore,
            c"writeProfile",
            c"(Ljava/lang/String;Ljava/lang/String;[B)V",
        )?,
        delete_profile: env.static_method_id(
            keystore,
            c"deleteProfile",
            c"(Ljava/lang/String;)V",
        )?,
        delete_keys: env.static_method_id(keystore, c"deleteKeys", c"(Ljava/lang/String;)V")?,
        keystore,
        exception,
    })
}

/// The runtime's invocation functions, in the JNI 1.4 view, which holds every one of them.
///
/// # Safety
///
/// `vm` is the runtime's `JavaVM`.
unsafe fn invoke_functions(vm: NonNull<JavaVM>) -> &'static JNIInvokeInterface__1_4 {
    // SAFETY: a `JavaVM` points to the runtime's table of invocation functions (the caller's
    // guarantee).
    let table: *const JNIInvokeInterface_ = unsafe { *vm.as_ptr() };
    // SAFETY: the runtime's table lives as long as the runtime, which outlives this library.
    let table: &'static JNIInvokeInterface_ = unsafe { &*table };
    // SAFETY: Android's runtime implements JNI 1.6, so its table has every function up to 1.4's.
    unsafe { &table.v1_4 }
}

/// The calling thread's attachment to the runtime, for one call. It attaches the thread unless
/// the thread is attached already, and detaches it on drop only if it attached it. `NonNull`
/// makes it neither `Send` nor `Sync`, so it stays on its thread.
struct Attached {
    vm: NonNull<JavaVM>,
    env: NonNull<JNIEnv>,
    detach_on_drop: bool,
}

impl Attached {
    fn current(loaded: &Loaded) -> Result<Self, KeystoreError> {
        let vm = loaded.vm;
        // SAFETY: `loaded.vm` is the runtime's `JavaVM`, which `JNI_OnLoad` received.
        let functions = unsafe { invoke_functions(vm) };
        let mut env: *mut c_void = ptr::null_mut();
        // SAFETY: the runtime's `JavaVM`, and a valid place for the environment pointer.
        let code = unsafe { (functions.GetEnv)(vm.as_ptr(), &raw mut env, JNI_VERSION_1_6) };
        let detach_on_drop = match code {
            JNI_OK => false,
            JNI_EDETACHED => {
                let mut arguments = JavaVMAttachArgs {
                    version: JNI_VERSION_1_6,
                    name: ptr::null_mut(),
                    group: ptr::null_mut(),
                };
                // SAFETY: the runtime's `JavaVM`, a valid place for the environment pointer, and
                // arguments that ask for JNI 1.6 with the default thread name and group.
                let code = unsafe {
                    (functions.AttachCurrentThread)(
                        vm.as_ptr(),
                        &raw mut env,
                        (&raw mut arguments).cast::<c_void>(),
                    )
                };
                if code != JNI_OK {
                    return Err(KeystoreError::Unexpected);
                }
                true
            }
            _ => return Err(KeystoreError::Unexpected),
        };
        let Some(env) = NonNull::new(env.cast::<JNIEnv>()) else {
            if detach_on_drop {
                // SAFETY: this thread attached itself above, and made no reference since.
                let _ = unsafe { (functions.DetachCurrentThread)(vm.as_ptr()) };
            }
            return Err(KeystoreError::Unexpected);
        };
        let attached = Self {
            vm,
            env,
            detach_on_drop,
        };
        // A thread that something else attached could have an exception pending, and the
        // module's rule needs none.
        attached.env().clear_exception();
        Ok(attached)
    }

    fn env(&self) -> Env<'_> {
        // SAFETY: `env` is this thread's environment for JNI 1.6, valid while `self` keeps the
        // thread attached, and `self` can't leave the thread.
        unsafe { Env::new(self.env) }
    }
}

impl Drop for Attached {
    fn drop(&mut self) {
        if self.detach_on_drop {
            // SAFETY: the runtime's `JavaVM`, which `JNI_OnLoad` received.
            let functions = unsafe { invoke_functions(self.vm) };
            // SAFETY: this thread attached itself in `current`, and every reference it made is
            // gone, because each one borrowed `self`. If detaching fails, the thread stays
            // attached, which holds a little memory until the thread ends; nothing else can be
            // done here.
            let _ = unsafe { (functions.DetachCurrentThread)(self.vm.as_ptr()) };
        }
    }
}

/// The calling thread's JNI environment. It is valid only on that thread, and only while the
/// thread stays attached: the lifetime `'thread` ties it to the [`Attached`] guard, or to the
/// `JNI_OnLoad` call. `NonNull` makes it neither `Send` nor `Sync`.
#[derive(Clone, Copy)]
struct Env<'thread> {
    raw: NonNull<JNIEnv>,
    thread: PhantomData<&'thread ()>,
}

impl<'thread> Env<'thread> {
    /// # Safety
    ///
    /// `raw` is the calling thread's environment for JNI 1.6, and stays valid for `'thread`.
    unsafe fn new(raw: NonNull<JNIEnv>) -> Self {
        Self {
            raw,
            thread: PhantomData,
        }
    }

    fn raw(self) -> *mut JNIEnv {
        self.raw.as_ptr()
    }

    /// The environment's functions, in the JNI 1.6 view.
    fn functions(self) -> &'thread JNINativeInterface__1_6 {
        // SAFETY: `raw` is a valid environment (the type's invariant), which points to the
        // runtime's function table.
        let table: *const JNINativeInterface_ = unsafe { *self.raw.as_ptr() };
        // SAFETY: the runtime's function table outlives every thread's environment.
        let table: &'thread JNINativeInterface_ = unsafe { &*table };
        // SAFETY: the environment is for JNI 1.6 (the type's invariant), so its table has every
        // function up to 1.6's.
        unsafe { &table.v1_6 }
    }

    /// Takes and clears the pending exception, if there is one.
    fn take_exception(self) -> Option<Local<'thread>> {
        let functions = self.functions();
        // SAFETY: a valid environment; JNI allows `ExceptionCheck` while an exception is pending.
        if !unsafe { (functions.ExceptionCheck)(self.raw()) } {
            return None;
        }
        // SAFETY: as above, for `ExceptionOccurred`, which returns a new local reference to the
        // exception.
        let thrown = unsafe { (functions.ExceptionOccurred)(self.raw()) };
        // SAFETY: as above, for `ExceptionClear`.
        unsafe { (functions.ExceptionClear)(self.raw()) };
        // SAFETY: `thrown` is null, or a new local reference that nothing else deletes.
        unsafe { Local::new(self, thrown) }
    }

    /// Clears the pending exception, if there is one, and says whether there was.
    fn clear_exception(self) -> bool {
        self.take_exception().is_some()
    }

    /// Fails with the [`KeystoreError`] that the pending exception stands for, after clearing it.
    fn check(self, loaded: &Loaded) -> Result<(), KeystoreError> {
        match self.take_exception() {
            None => Ok(()),
            Some(thrown) => Err(self.failure(loaded, &thrown)),
        }
    }

    /// The [`KeystoreError`] for a thrown object: its code for a `KeystoreException`, and
    /// [`KeystoreError::Unexpected`] for anything else.
    fn failure(self, loaded: &Loaded, thrown: &Local<'thread>) -> KeystoreError {
        let functions = self.functions();
        // SAFETY: a valid environment with no exception pending, because `take_exception`
        // cleared it, a live local reference, and a live global class reference.
        let is_keystore_exception =
            unsafe { (functions.IsInstanceOf)(self.raw(), thrown.raw(), loaded.exception.raw()) };
        if !is_keystore_exception {
            return KeystoreError::Unexpected;
        }
        // SAFETY: `thrown` is a `KeystoreException`, whose field `code` has the ID `loaded.code`
        // and the type `int`; `GetIntField` throws nothing.
        let code = unsafe { (functions.GetIntField)(self.raw(), thrown.raw(), loaded.code) };
        match code {
            KEY_MISSING => KeystoreError::KeyMissing,
            UNREADABLE => KeystoreError::Unreadable,
            FAILED => KeystoreError::Failed,
            _ => KeystoreError::Unexpected,
        }
    }

    /// A global reference to the class `name`, found with this thread's class loader.
    fn global_class(self, name: &CStr) -> Option<Class> {
        let functions = self.functions();
        // SAFETY: a valid environment, no exception pending, and a NUL-terminated name.
        let local = unsafe { (functions.FindClass)(self.raw(), name.as_ptr()) };
        // SAFETY: `local` is null, or a new local reference that nothing else deletes.
        let local = unsafe { Local::new(self, local) };
        // `FindClass` throws `NoClassDefFoundError` when there's no such class.
        if self.clear_exception() {
            return None;
        }
        let local = local?;
        // SAFETY: a valid environment, no exception pending, and a live local reference.
        let global = unsafe { (functions.NewGlobalRef)(self.raw(), local.raw()) };
        // `NewGlobalRef` returns null when memory runs out, and may throw then.
        self.clear_exception();
        NonNull::new(global).map(Class)
    }

    /// The ID of the static method `name`, whose JNI type is `signature`.
    fn static_method_id(self, class: Class, name: &CStr, signature: &CStr) -> Option<jmethodID> {
        // SAFETY: a valid environment, no exception pending, a live global class reference, and
        // NUL-terminated strings.
        let id = unsafe {
            (self.functions().GetStaticMethodID)(
                self.raw(),
                class.raw(),
                name.as_ptr(),
                signature.as_ptr(),
            )
        };
        // It throws `NoSuchMethodError` when there's no such method.
        let thrown = self.clear_exception();
        (!thrown && !id.is_null()).then_some(id)
    }

    /// The ID of the instance field `name`, whose JNI type is `signature`.
    fn field_id(self, class: Class, name: &CStr, signature: &CStr) -> Option<jfieldID> {
        // SAFETY: a valid environment, no exception pending, a live global class reference, and
        // NUL-terminated strings.
        let id = unsafe {
            (self.functions().GetFieldID)(
                self.raw(),
                class.raw(),
                name.as_ptr(),
                signature.as_ptr(),
            )
        };
        // It throws `NoSuchFieldError` when there's no such field.
        let thrown = self.clear_exception();
        (!thrown && !id.is_null()).then_some(id)
    }

    /// A new `String` holding `text`, which must be ASCII: `NewStringUTF` reads Java's modified
    /// UTF-8, which is the same as ASCII for ASCII text.
    fn new_string(self, text: &CStr) -> Result<Local<'thread>, KeystoreError> {
        if !text.to_bytes().is_ascii() {
            return Err(KeystoreError::InvalidName);
        }
        // SAFETY: a valid environment, no exception pending, and NUL-terminated ASCII text.
        let raw = unsafe { (self.functions().NewStringUTF)(self.raw(), text.as_ptr()) };
        // It throws `OutOfMemoryError` when it fails.
        self.clear_exception();
        // SAFETY: `raw` is null, or a new local reference that nothing else deletes.
        unsafe { Local::new(self, raw) }.ok_or(KeystoreError::Unexpected)
    }

    /// A new `byte[]` holding a copy of `bytes`.
    fn new_byte_array(self, bytes: &[u8]) -> Result<ByteArray<'thread>, KeystoreError> {
        let length = jsize::try_from(bytes.len()).map_err(|_| KeystoreError::Unexpected)?;
        // SAFETY: a valid environment and no exception pending.
        let raw = unsafe { (self.functions().NewByteArray)(self.raw(), length) };
        // It throws `OutOfMemoryError` when it fails.
        self.clear_exception();
        // SAFETY: `raw` is null, or a new local reference to a `byte[]` of `length` bytes that
        // nothing else deletes.
        let local = unsafe { Local::new(self, raw) }.ok_or(KeystoreError::Unexpected)?;
        // SAFETY: as above.
        let array = unsafe { ByteArray::new(local, length) }?;
        // SAFETY: a valid environment, no exception pending, a live `byte[]` of `length` bytes,
        // and `bytes`, which is as long; `jbyte` and `u8` have the same size.
        unsafe {
            (self.functions().SetByteArrayRegion)(
                self.raw(),
                array.raw(),
                0,
                length,
                bytes.as_ptr().cast::<jbyte>(),
            );
        };
        Ok(array)
    }

    /// Calls a static method of `VaultKeystore`, and returns a copy of the `byte[]` it returned,
    /// or `None` when it returned null or `void`.
    ///
    /// # Safety
    ///
    /// `method` is a static method of `VaultKeystore` whose parameters are, in order, a `String`
    /// for each [`Arg::Text`] and a `byte[]` for each [`Arg::Bytes`] in `arguments`, and which
    /// returns `byte[]` for [`Returns::Bytes`] and `void` for [`Returns::Nothing`].
    unsafe fn call(
        self,
        loaded: &Loaded,
        method: jmethodID,
        arguments: &[Arg<'_>],
        returns: Returns,
    ) -> Result<Option<Vec<u8>>, KeystoreError> {
        let mut objects = Vec::with_capacity(arguments.len());
        for argument in arguments {
            objects.push(match *argument {
                Arg::Text(text) => Argument::Text(self.new_string(text)?),
                Arg::Bytes(bytes) => Argument::Bytes(self.new_byte_array(bytes)?),
            });
        }
        let values: Vec<jvalue> = objects.iter().map(Argument::value).collect();
        let functions = self.functions();
        let class = loaded.keystore.raw();
        match returns {
            Returns::Nothing => {
                // SAFETY: a valid environment, no exception pending, a live class, and a method
                // whose signature takes one live reference in `values` for each parameter, of the
                // parameter's type (the caller's guarantee).
                unsafe {
                    (functions.CallStaticVoidMethodA)(self.raw(), class, method, values.as_ptr());
                }
                self.check(loaded)?;
                Ok(None)
            }
            Returns::Bytes => {
                // SAFETY: as for `CallStaticVoidMethodA` above.
                let result = unsafe {
                    (functions.CallStaticObjectMethodA)(self.raw(), class, method, values.as_ptr())
                };
                // Owned before the exception check, so that it is deleted even if the method
                // returned something and threw.
                // SAFETY: `result` is null, or a new local reference that nothing else deletes.
                let result = unsafe { Local::new(self, result) };
                self.check(loaded)?;
                let Some(result) = result else {
                    return Ok(None);
                };
                // SAFETY: the method returns `byte[]` (the caller's guarantee).
                let array = unsafe { ByteArray::returned(result) }?;
                Ok(Some(array.to_vec()))
            }
        }
    }
}

/// A local reference that the calling thread owns. `Drop` deletes it.
struct Local<'thread> {
    env: Env<'thread>,
    raw: NonNull<_jobject>,
}

impl<'thread> Local<'thread> {
    /// # Safety
    ///
    /// `raw` is null, or a local reference made on `env`'s thread that nothing else deletes.
    unsafe fn new(env: Env<'thread>, raw: jobject) -> Option<Self> {
        NonNull::new(raw).map(|raw| Self { env, raw })
    }

    fn raw(&self) -> jobject {
        self.raw.as_ptr()
    }
}

impl Drop for Local<'_> {
    fn drop(&mut self) {
        // SAFETY: a valid environment, and a live local reference of its thread, which nothing
        // uses after this; JNI allows `DeleteLocalRef` while an exception is pending.
        unsafe { (self.env.functions().DeleteLocalRef)(self.env.raw(), self.raw()) };
    }
}

/// A local reference to a `byte[]` of `length` bytes. `Drop` overwrites it with zeros, and then
/// the [`Local`] inside deletes the reference.
struct ByteArray<'thread> {
    local: Local<'thread>,
    length: jsize,
    size: usize,
}

impl<'thread> ByteArray<'thread> {
    /// # Safety
    ///
    /// `local` refers to a `byte[]` of `length` bytes.
    unsafe fn new(local: Local<'thread>, length: jsize) -> Result<Self, KeystoreError> {
        let size = usize::try_from(length).map_err(|_| KeystoreError::Unexpected)?;
        Ok(Self {
            local,
            length,
            size,
        })
    }

    /// A `byte[]` that a method returned.
    ///
    /// # Safety
    ///
    /// `local` refers to a `byte[]`.
    unsafe fn returned(local: Local<'thread>) -> Result<Self, KeystoreError> {
        let env = local.env;
        // SAFETY: a valid environment, no exception pending, and a live array (the caller's
        // guarantee); `GetArrayLength` throws nothing.
        let length = unsafe { (env.functions().GetArrayLength)(env.raw(), local.raw()) };
        // SAFETY: `GetArrayLength` gave the array's length.
        unsafe { Self::new(local, length) }
    }

    fn raw(&self) -> jobject {
        self.local.raw()
    }

    /// A copy of the array's bytes.
    fn to_vec(&self) -> Vec<u8> {
        let mut bytes = vec![0_u8; self.size];
        let env = self.local.env;
        // SAFETY: a valid environment, no exception pending, a live `byte[]` of `length` bytes,
        // and a buffer of as many; `jbyte` and `u8` have the same size. Indexes within the array
        // never throw.
        unsafe {
            (env.functions().GetByteArrayRegion)(
                env.raw(),
                self.raw(),
                0,
                self.length,
                bytes.as_mut_ptr().cast::<jbyte>(),
            );
        };
        bytes
    }
}

impl Drop for ByteArray<'_> {
    fn drop(&mut self) {
        let env = self.local.env;
        // `SetByteArrayRegion` isn't allowed while an exception is pending. None is, by the
        // module's rule, but wiping matters more than an exception that nothing would read.
        env.clear_exception();
        let zeros = vec![0_u8; self.size];
        // SAFETY: a valid environment, no exception pending, a live `byte[]` of `length` bytes,
        // and `zeros`, which is as long; `jbyte` and `u8` have the same size.
        unsafe {
            (env.functions().SetByteArrayRegion)(
                env.raw(),
                self.raw(),
                0,
                self.length,
                zeros.as_ptr().cast::<jbyte>(),
            );
        };
    }
}

/// An argument of a Kotlin method.
#[derive(Clone, Copy)]
enum Arg<'a> {
    /// A `String`, from ASCII text.
    Text(&'a CStr),
    /// A `byte[]`.
    Bytes(&'a [u8]),
}

/// The Java object made for an [`Arg`], which lives until the call returns.
enum Argument<'thread> {
    Text(Local<'thread>),
    Bytes(ByteArray<'thread>),
}

impl Argument<'_> {
    fn value(&self) -> jvalue {
        jvalue {
            l: match self {
                Self::Text(local) => local.raw(),
                Self::Bytes(array) => array.raw(),
            },
        }
    }
}

/// What a Kotlin method returns.
#[derive(Clone, Copy)]
enum Returns {
    Nothing,
    Bytes,
}

/// Runs `operation` with the calling thread attached to the runtime.
fn with_env<T>(
    operation: impl FnOnce(Env<'_>, &Loaded) -> Result<T, KeystoreError>,
) -> Result<T, KeystoreError> {
    let loaded = LOADED
        .get()
        .and_then(Option::as_ref)
        .ok_or(KeystoreError::Unavailable)?;
    let attached = Attached::current(loaded)?;
    operation(attached.env(), loaded)
}

/// A name as a C string. `lib.rs` has checked that it is ASCII without NUL.
fn name(text: &str) -> Result<CString, KeystoreError> {
    CString::new(text).map_err(|_| KeystoreError::InvalidName)
}

pub(crate) fn ensure_device_key(alias_prefix: &str) -> Result<(), KeystoreError> {
    let alias_prefix = name(alias_prefix)?;
    with_env(|env, loaded| {
        let arguments = [Arg::Text(&alias_prefix)];
        // SAFETY: `ensureDeviceKey` is `(Ljava/lang/String;)V`, as `load` looked it up.
        unsafe {
            env.call(
                loaded,
                loaded.ensure_device_key,
                &arguments,
                Returns::Nothing,
            )
        }
        .map(drop)
    })
}

pub(crate) fn device_hmac(alias_prefix: &str, message: &[u8]) -> Result<Vec<u8>, KeystoreError> {
    let alias_prefix = name(alias_prefix)?;
    with_env(|env, loaded| {
        let arguments = [Arg::Text(&alias_prefix), Arg::Bytes(message)];
        // SAFETY: `deviceHmac` is `(Ljava/lang/String;[B)[B`, as `load` looked it up.
        unsafe { env.call(loaded, loaded.device_hmac, &arguments, Returns::Bytes) }?
            .ok_or(KeystoreError::Unexpected)
    })
}

pub(crate) fn read_profile(
    alias_prefix: &str,
    profile_file: &str,
) -> Result<Option<Vec<u8>>, KeystoreError> {
    let alias_prefix = name(alias_prefix)?;
    let profile_file = name(profile_file)?;
    with_env(|env, loaded| {
        let arguments = [Arg::Text(&alias_prefix), Arg::Text(&profile_file)];
        // SAFETY: `readProfile` is `(Ljava/lang/String;Ljava/lang/String;)[B`, as `load` looked
        // it up.
        unsafe { env.call(loaded, loaded.read_profile, &arguments, Returns::Bytes) }
    })
}

pub(crate) fn write_profile(
    alias_prefix: &str,
    profile_file: &str,
    plaintext: &[u8],
) -> Result<(), KeystoreError> {
    let alias_prefix = name(alias_prefix)?;
    let profile_file = name(profile_file)?;
    with_env(|env, loaded| {
        let arguments = [
            Arg::Text(&alias_prefix),
            Arg::Text(&profile_file),
            Arg::Bytes(plaintext),
        ];
        // SAFETY: `writeProfile` is `(Ljava/lang/String;Ljava/lang/String;[B)V`, as `load` looked
        // it up.
        unsafe { env.call(loaded, loaded.write_profile, &arguments, Returns::Nothing) }.map(drop)
    })
}

pub(crate) fn delete_profile(profile_file: &str) -> Result<(), KeystoreError> {
    let profile_file = name(profile_file)?;
    with_env(|env, loaded| {
        let arguments = [Arg::Text(&profile_file)];
        // SAFETY: `deleteProfile` is `(Ljava/lang/String;)V`, as `load` looked it up.
        unsafe { env.call(loaded, loaded.delete_profile, &arguments, Returns::Nothing) }.map(drop)
    })
}

pub(crate) fn delete_keys(alias_prefix: &str) -> Result<(), KeystoreError> {
    let alias_prefix = name(alias_prefix)?;
    with_env(|env, loaded| {
        let arguments = [Arg::Text(&alias_prefix)];
        // SAFETY: `deleteKeys` is `(Ljava/lang/String;)V`, as `load` looked it up.
        unsafe { env.call(loaded, loaded.delete_keys, &arguments, Returns::Nothing) }.map(drop)
    })
}
