package com.proyect.qchuago.data.servicio

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> esperar(tarea: Task<T>): T = suspendCancellableCoroutine { cont ->
    tarea.addOnCompleteListener { t ->
        if (t.isSuccessful) cont.resume(t.result)
        else cont.resumeWithException(t.exception ?: Exception("Error desconocido"))
    }
}
