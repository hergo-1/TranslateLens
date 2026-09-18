package com.translatelens.data.image

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal suspend fun <T> Task<T>.awaitFinished(): T = await()

internal suspend fun <T> imageResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
