/**
 * @Authors: Tahir Raza
 * @Date: 31/12/2023
 */
package com.tahir.fileencrypter.exceptionHandler

import timber.log.Timber
import java.lang.Thread.UncaughtExceptionHandler

class UnCaughtExceptionHandler : UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, e: Throwable) {
        // we will log it for better readability
        Timber.e("Uncaught Exception occurred in thread: ${thread.name}, exception: , $e")
        // pass it to the system
        defaultHandler?.uncaughtException(thread, e)
    }
}