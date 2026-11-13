package com.internmatch.app

import android.app.Application
import com.internmatch.app.core.di.AppContainer
import com.internmatch.app.core.di.DefaultAppContainer

/**
 * Custom Application class initializing application-wide dependencies.
 */
class InternMatchApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
