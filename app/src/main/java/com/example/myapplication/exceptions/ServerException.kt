package com.example.myapplication.exceptions

class ServerException(val code: Int, val msg: String) : RuntimeException()