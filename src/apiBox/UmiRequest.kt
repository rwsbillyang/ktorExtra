package com.github.rwsbillyang.ktorKit.apiBox

import com.github.rwsbillyang.ktorKit.toObjectId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bson.conversions.Bson
import org.litote.kmongo.bson
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
data class UmiAnyBox(@Contextual val data: Any?)
    : UmiBox(Code.OK)
{
    constructor(): this(null)

    companion object{
        fun ok(data: Any?) = UmiAnyBox(data)
        fun ko(msg: String, type: Int = WARN_MESSAGE) = UmiAnyBox(null).apply { code = Code.KO }
        fun newUser(msg: String, type: Int = REDIRECT) = UmiAnyBox(null).apply { code = Code.NewUser }
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

//当指定的sortKey不是ObjectId类型时，前端分页传递过来的lastId对应着其他某种类型，
// 如Number(Long，Float, Int)，String,多数情况下还是按默认的_id，也就是ObjectId类型来处理
enum class SortKeyType{
 TypeNumber, TypeString, TypeObjectId
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
 *
 * 使用var字段，目的在于应用层可以直接对这些值进行修改（比如出于权限目的）
 * */
@Serializable
class UmiPagination(
     var pageSize: Int = 10,
     var current: Int = 1,
     var sKey: String = "_id", //sortKey
     var sort: Int = Sort.DESC, //1用于升序，而-1用于降序
     val sKeyType: SortKeyType = SortKeyType.TypeObjectId,
     var fKey: String? = null, //filter key
     var filters: List<String>? = null
){
    val sortJson = "{'${sKey}':${sort}}"

    /**
     * 加载分页数据时，若传递过来了lastId信息，根据不同的排序键，以及排序键数据类型，构建不同的截取范围bson
     * */
    fun lastIdFilter(lastId: String?): Bson? {
        if(lastId == null) return null
        val s = if(sort == Sort.DESC) "\$lt" else "\$gt"
        return when(sKeyType){
            SortKeyType.TypeNumber -> "{ '${sKey}': { $s: $lastId } }"
            SortKeyType.TypeString -> "{ '${sKey}': { $s: `$lastId` } }"
            SortKeyType.TypeObjectId -> "{ '${sKey}': { $s: ObjectId(\"${lastId.toObjectId().toHexString()}\") } }"
        }.bson
    }
}

/**
 * 用于对umi request请求的支持
 * umi可以为空，用于对非antd前端的支持，即参数中无需有UmiPagination所对应参数，将使用UmiPagination的默认值
 * 前端对应着按照UmiListParams的结果将参数扁平化后再encodeURIComponent，作为一个umi参数传递给后端
 * 后端的各种ListParams需实现此接口，对umi参数的解析并反序列化
 * */
interface IUmiListParams{
    val umi: String?
    val pagination: UmiPagination
        get() = umi?.let { Json.decodeFromString(URLDecoder.decode(it,"UTF-8")) }?:UmiPagination()
}
/**
 * 删除数据时，传递过来的字符型id列表
 * 示例：{"key":[96,95,94]}
 * */
@Serializable
class IntIds(val key: List<Int>)

/**
 * 删除数据时，传递过来的字符型id列表
 * 示例：{"key":["96","95","94"]}
 * */
@Serializable
 class StrIds(val key: List<String>)

@Serializable
class BatchOperationParams(
    val ids: String, //以 ","分隔的_id
    val action: String, //操作命令如： del, assign, updateStatus
    val arg1: String? = null, //提交的参数
    val arg2: String? = null //提交的参数
)


