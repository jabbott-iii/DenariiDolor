package com.denariidolor

import android.app.Application
import com.denariidolor.data.local.db.DefaultDataInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var defaultDataInitializer: DefaultDataInitializer

    override fun onCreate() {
        super.onCreate()
        runBlocking(Dispatchers.IO) {
            defaultDataInitializer.seedDefaults()
        }
    }
}
