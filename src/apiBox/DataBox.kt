package com.github.rwsbillyang.ktorKit.apiBox



import kotlinx.serialization.Serializable



/**
 * @param code identify the result type, generally it's OK or KO
 * @param msg error message if code is not OK
 * @param data payload of the result
 *
 * @param type showType: error display type： 0 silent; 1 message.warn; 2 message.error; 4 notification; 9 page
 * @param tId traceId: Convenient for back-end Troubleshooting: unique request ID
 * @param host Convenient for backend Troubleshooting: host of current access server
 *
 * About parameters: type, tId, host, please refer to: https://umijs.org/plugins/plugin-request
 * Custom serializers for a generic type
 * https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#custom-serializers-for-a-generic-type
 *
 * */
@Serializable
class DataBox<T>(
    val code: String,
    val msg: String? = null,
    val data: T? = null,
    var type: Int? = null,
    var tId: String? = null,
    var host: String? = null
)
{
    companion object{
        fun <T> ok(data: T?) = DataBox(CODE_OK, data = data)
        fun <T> ko(msg: String, type: Int? = null, tId: String? = null, host: String? = null) = DataBox<T>(CODE_KO, msg, null, type,tId, host)
        fun <T> newUser(msg: String, type: Int? = null, tId: String? = null, host: String? = null) = DataBox<T>(CODE_NewUser, msg)


        const val CODE_OK = "OK"
        const val CODE_KO = "KO"
        const val CODE_NewUser = "NewUser"
        const val CODE_TokenExpired = "TokenExpired"

        const val TYPE_SILENT = 0 // 不提示错误
        const val TYPE_WARN_MESSAGE = 1 // 警告信息提示
        const val TYPE_ERROR_MESSAGE = 2 // 错误信息提示
        const val TYPE_NOTIFICATION = 4 // 通知提示
        const val TYPE_REDIRECT = 9 // 页面跳转
    }
}



