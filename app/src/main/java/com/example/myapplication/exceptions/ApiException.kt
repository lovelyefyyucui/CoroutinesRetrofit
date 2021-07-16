package com.example.myapplication.exceptions

/**
 * 异常封装
 */
class ApiException(val throwable: Throwable, val code: Int, var msg: String) :
    Exception(throwable) {
    companion object {
        //未知错误
        const val UNKNOWN = 9000

        //解析错误
        const val PARSE_ERROR = 9001

        //网络错误
        const val NETWORK_ERROR = 9002

        //协议错误
        const val HTTP_ERROR = 9003

        //证书错误
        const val SSL_ERROR = 9004

        //连接超时
        const val TIMEOUT_ERRO = 9006
    }
}