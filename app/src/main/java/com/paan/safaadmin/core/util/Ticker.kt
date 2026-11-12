package com.paan.safaadmin.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits once, then runs [action] every [intervalMs] while someone collects (stops when the app is in the background). */
fun ticker(intervalMs: Long, action: suspend () -> Unit): Flow<Unit> = flow {
    emit(Unit)
    while (true) {
        action()
        delay(intervalMs)
    }
}