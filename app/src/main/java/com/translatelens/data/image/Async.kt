package com.translatelens.data.image

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

internal suspend fun <T> Task<T>.awaitFinished(): T = await()

internal suspend fun <T> Task<T>.awaitBounded(timeoutMs: Long, timeoutMessage: String): T {
    try {
        return withTimeout(timeoutMs) { await() }
    } catch (e: TimeoutCancellationException) {
        throw IllegalStateException(timeoutMessage)
    }
}

internal suspend fun <T> imageResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
