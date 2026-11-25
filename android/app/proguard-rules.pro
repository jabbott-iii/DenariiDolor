# R8 rules for the release build, which Flutter's Gradle plugin adds after its own and Android's defaults.

# Rust looks these up by name through JNI (rust/crates/keystore/src/jni.rs, ADR-0022), and no Java or Kotlin code calls
# them, so R8 would rename or remove them. Keep the two class names and only the members Rust uses.
-keep class com.denariidolor.VaultKeystore {
    public static byte[] deviceHmac(java.lang.String, byte[]);
    public static void ensureDeviceKey(java.lang.String);
    public static byte[] readProfile(java.lang.String, java.lang.String);
    public static void writeProfile(java.lang.String, java.lang.String, byte[]);
    public static void deleteProfile(java.lang.String);
    public static void deleteKeys(java.lang.String);
}
-keep class com.denariidolor.KeystoreException {
    int code;
}
