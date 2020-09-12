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
abstract class Box(
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

//@Serializable
//data class ProjectListDataBox(val list: List<Project>? = null, val total: Int = 0): Box()

/**
 * 抽象基类，在Box（包含code和msg）基础上额外添加了调试信息
 *
 *  Box的umi request版本， 额外添加了type，traceId， host等Troubleshooting信息
 * @param type showType: error display type： 0 silent; 1 message.warn; 2 message.error; 4 notification; 9 page
 * @param tId traceId: Convenient for back-end Troubleshooting: unique request ID
 * @param host Convenient for backend Troubleshooting: host of current access server
 * 参照 umi request需要的返回结果
 * https://umijs.org/plugins/plugin-request
 * */
@Serializable
abstract class UmiBox(
    var type: Int?,
    var tId: String?,
    var host: String?
): Box(Code.OK)
{
    constructor(): this(null,null, null)
    companion object{
        const val SILENT = 0 // 不提示错误
        const val WARN_MESSAGE = 1 // 警告信息提示
        const val ERROR_MESSAGE = 2 // 错误信息提示
        const val NOTIFICATION = 4 // 通知提示
        const val REDIRECT = 9 // 页面跳转
    }
}

//@Serializable
//class ProjectListDataBox(val data: List<Project>? = null, val total: Int = 0): UmiBox()


/**
 * umi-request版本的databox
 * 除了列表外，可直接使用，列表使用方法如下：
 * data class ProjectListDataBox(val list: List<Project>? = null, val total: Int = 0): UmiBox()
 * @param data response payload
 * */
@Serializable
data class UmiDataBox(@Contextual val data: Any?)
    : UmiBox( null,null, null)
{
    constructor(): this(null)

    companion object{
        fun ok(data: Any?) = UmiDataBox(data)
        fun ko(msg: String, type: Int = WARN_MESSAGE) = UmiDataBox(null).apply { code = Code.KO }
        fun needLogin(msg: String, type: Int = REDIRECT) = UmiDataBox(null).apply { code = Code.NeedLogin }
    }
}





/**
 * 1用于升序，而-1用于降序
 * */

class Sort {
    companion object {
        const val ASC = 1
        const val DESC = -1
    }
}


/**
 * umi request列表数据请求参数公共部分基类
 * antd pro  protable通过umi-request提交的请求参数
 * @param pageSize 默认20条
 * @param current 当前页，自1开始
 * @param sKey 排序的字段
 * @param sort 1用于升序，而-1用于降序 与MongoDB一致
 * @param fKey filter key
 * @param filters filter values
 * */
@Serializable
abstract class UmiListParams(
    open val pageSize: Int = 20,
    open val current: Int = 1,
    open val sKey: String? = null, //sortKey
    open val sort: Int = Sort.DESC, //1用于升序，而-1用于降序
    open val fKey: String? = null, //filter key
    open val filters: List<String>? = null
)


/**
 * 删除数据时，传递过来的字符型id列表
 * 示例：{"key":[96,95,94]}
 * */
@Serializable
data class IntIds(val key: List<Int>)

/**
 * 删除数据时，传递过来的字符型id列表
 * 示例：{"key":["96","95","94"]}
 * */
@Serializable
data class StrIds(val key: List<String>)



