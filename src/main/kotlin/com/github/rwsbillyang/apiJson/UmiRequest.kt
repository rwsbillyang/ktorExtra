package com.github.rwsbillyang.apiJson

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URLDecoder


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
open class UmiBox(
    var code: String,
    var msg: String? = null,
    var type: Int? = null,
    var tId: String? = null,
    var host: String? = null
)
{
    constructor(): this(Code.OK)

    companion object{
        const val SILENT = 0 // 不提示错误
        const val WARN_MESSAGE = 1 // 警告信息提示
        const val ERROR_MESSAGE = 2 // 错误信息提示
        const val NOTIFICATION = 4 // 通知提示
        const val REDIRECT = 9 // 页面跳转
    }
}



/**
 * umi-request版本的databox
 * 除了列表外，可直接使用，列表使用方法如下：
 * data class ProjectListDataBox(val list: List<Project>? = null, val total: Int = 0): UmiBox()
 * @param data response payload
 * */
@Serializable
data class UmiDataBox(@Contextual val data: Any?)
    : UmiBox(Code.OK)
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
 * @param sKey 排序的字段 "sorter":{"updatedAt":"ascend"} 中的"sorter"
 * @param sort 1用于升序，而-1用于降序 与MongoDB一致
 * @param fKey filter key "someKey"
 * @param filters  "filter":{"someKey":["value1",123,"value3"]} 中的列表
 * */
@Serializable
class UmiPagination(
     val pageSize: Int = 20,
     val current: Int = 1,
     val sKey: String? = null, //sortKey
     val sort: Int = Sort.DESC, //1用于升序，而-1用于降序
     val fKey: String? = null, //filter key
     val filters: List<String>? = null
)

/**
 * 用于对umi request请求的支持
 *
 * 前端对应着按照UmiListParams的结果将参数扁平化后再encodeURIComponent，作为一个umi参数传递给后端
 * 后端的各种ListParams需实现此接口，对umi参数的解析并反序列化
 * */
interface IUmiListParams{
    val umi: String
    val pagination: UmiPagination
        get() = Json.decodeFromString(URLDecoder.decode(umi,"UTF-8"))
}
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
