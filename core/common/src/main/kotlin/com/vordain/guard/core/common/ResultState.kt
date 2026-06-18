package com.vordain.guard.core.common

sealed interface ResultState<out T> {
    data class Success<T>(val value: T) : ResultState<T>
    data class Failure(val message: String, val cause: Throwable? = null) : ResultState<Nothing>
}
