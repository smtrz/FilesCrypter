package com.tahir.fileencrypter

import android.app.Application
import com.tahir.fileencrypter.exceptionHandler.UnCaughtExceptionHandler
import com.tahir.fileencrypter.koin.modules.AppModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import timber.log.Timber


/**
 * @Authors: Tahir Raza
 * @Date: 28/12/2023
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin()
        setUpTimber()
        setUpExceptionHandler()

    }

    override fun onTerminate() {
        super.onTerminate()
        stopKoin()

    }

    private fun startKoin() {
        // Start Koin with the application.
        startKoin {
            androidContext(this@App)
            modules(AppModules().module)
        }
    }

    private fun setUpTimber() {
        // for v1.0.0 -> Disabling BuildConfig check deliberately
        Timber.plant(Timber.DebugTree())
    }

    private fun setUpExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(UnCaughtExceptionHandler())

    }
}