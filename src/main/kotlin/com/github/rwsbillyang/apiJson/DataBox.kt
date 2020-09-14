package com.github.rwsbillyang.apiJson


import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

object Code{
    const val OK = "OK"
    const val KO = "KO"
    const val NeedLogin = "NeedLogin"
}
/**
 * Custom serializers for a generic type
 * https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#custom-serializers-for-a-generic-type
 * */
@Serializable
data class ResponseBox<T>(var code: String,
                   var msg: String? = null, val data: T? = null)
{
    companion object{
        fun <T> ok(data: T?) = ResponseBox(Code.OK, data = data)
        fun ko(msg: String) = ResponseBox<Nothing>(Code.KO, msg)
        fun needLogin(msg: String) = ResponseBox<Nothing>(Code.NeedLogin, msg)
    }
}

/**
 * 抽象基类，包含了code和msg， API返回结果数据结构
 * @param code 错误码信息，正确则为"OK"，普通错误为"KO"
 * @param msg 错误信息，
 * */
@Serializable
open class Box(
    var code: String,
    var msg: String? = null
){
    constructor(): this(Code.OK)
}

/**
 * 简化版的data class，可直接使用
 * 列表数据应该继承自Box：
 * data class ProjectListDataBox(val list: List<Project>? = null, val total: Int = 0): Box()
 *
 * @param data 返回的负载数据 response payload
 * */
@Serializable
data class DataBox(@Contextual val data: Any?) : Box(Code.OK)
{
    constructor(): this(null)

    companion object{
        fun ok(data: Any?) = DataBox(data)
        fun ko(msg: String) = DataBox(null).apply { code = Code.KO }
        fun needLogin(msg: String) = DataBox(null).apply { code = Code.NeedLogin }
    }
}
