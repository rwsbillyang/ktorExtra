package com.github.rwsbillyang.ktorKit.apiJson


import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

object Code{
    const val OK = "OK"
    const val KO = "KO"
    //const val NeedLogin = "NeedLogin"
    const val NewUser = "NewUser"
}
/**
 * Custom serializers for a generic type
 * https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#custom-serializers-for-a-generic-type
 * */
@Serializable
data class DataBox<T>(
    val code: String,
    val msg: String? = null,
    val data: T? = null)
{
    companion object{
        fun <T> ok(data: T?) = DataBox(Code.OK, data = data)
        fun <T> ko(msg: String) = DataBox<T>(Code.KO, msg)
        fun <T> newUser(msg: String) = DataBox<T>(Code.NewUser, msg)
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
data class AnyBox(@Contextual val data: Any?) : Box(Code.OK)
{
    constructor(): this(null)

    companion object{
        fun ok(data: Any?) = AnyBox(data)
        fun ko(msg: String) = AnyBox(null).apply { code = Code.KO }
        fun newUser(msg: String) = AnyBox(null).apply { code = Code.NewUser }
    }
}

