package com.example.myapplication.entity

import com.example.myapplication.exceptions.ApiException


sealed class DataResult<out T> {
    data class Success<T>(val response: T) : DataResult<T>()
    data class Error(val exception: ApiException) : DataResult<Nothing>()
}