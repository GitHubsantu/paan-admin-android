package com.paan.safaadmin.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.HttpException
import java.io.IOException

class ApiException(
    message: String,
    val httpCode: Int = 0,
    val errorCode: String? = null,
) : Exception(message)

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

private const val UNEXPECTED = "The server sent an unexpected response. Please try again."
private const val OFFLINE = "No internet connection. Please check your network and try again."
private const val SESSION_EXPIRED = "Your session has expired. Please log in again."
private const val TOO_MANY = "Too many attempts. Please wait a moment and try again."
private const val SERVER = "Something went wrong on our side. Please try again."

private suspend fun <T> execute(block: suspend () -> ApiResponse<T>): Result<ApiResponse<T>> =
    try {
        val response = block()
        if (response.success) Result.success(response)
        else Result.failure(ApiException(response.message ?: UNEXPECTED, errorCode = response.code))
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(e.toApiException())
    } catch (e: IOException) {
        Result.failure(ApiException(OFFLINE))
    } catch (e: SerializationException) {
        Result.failure(ApiException(UNEXPECTED))
    }

private fun HttpException.toApiException(): ApiException {
    val code = code()
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val parsed = body?.let { runCatching { errorJson.decodeFromString<ApiResponse<JsonElement>>(it) }.getOrNull() }
    val bodyMessage = parsed?.message?.takeIf { it.isNotBlank() }
    val firstFieldError = parsed?.errors?.values?.firstOrNull()?.firstOrNull()
    val message = when {
        code == 422 && firstFieldError != null -> firstFieldError
        code == 401 -> SESSION_EXPIRED
        code == 429 -> TOO_MANY
        code >= 500 -> SERVER
        bodyMessage != null -> bodyMessage
        code == 403 -> "You don't have permission to do that."
        code == 404 -> "We couldn't find that."
        else -> UNEXPECTED
    }
    return ApiException(message, httpCode = code, errorCode = parsed?.code)
}

/** Calls the API and returns just `data`. */
suspend fun <T : Any> apiCall(block: suspend () -> ApiResponse<T>): Result<T> =
    execute(block).mapCatching { it.data ?: throw ApiException(UNEXPECTED) }

/** For endpoints whose body we don't need. */
suspend fun apiCallUnit(block: suspend () -> ApiResponse<JsonElement>): Result<Unit> =
    execute(block).map { }

/** Returns `data` together with pagination `meta`. */
suspend fun <T : Any> apiCallWithMeta(block: suspend () -> ApiResponse<T>): Result<Pair<T, PageMeta?>> =
    execute(block).mapCatching { (it.data ?: throw ApiException(UNEXPECTED)) to it.meta }