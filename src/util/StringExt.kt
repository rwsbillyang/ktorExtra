package com.github.rwsbillyang.ktorKit.util

import java.util.regex.Pattern


/**
 * String的扩展函数： 是否是IP地址，只支持IPv4版本
 * */
fun String.isIp() =  Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").matcher(this).matches()

/**
 * String的扩展函数： 是否是中国大陆手机号
 * */
fun String.isMobileNumber() =  Pattern.compile("^((\\+?86)|(\\+86))?1\\d{10}$").matcher(this).matches()

/**
 *  是否是邮箱
 * */
fun String.isEmail() =  Pattern.compile("^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$").matcher(this).matches()

/**
 *  是否是http Url
 * */
fun String.isUrl() =  Pattern.compile("http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?").matcher(this).matches()

/**
 *  是否是身份证号码，不支持最后一位是字母？
 * */
fun String.IsIDCard() =  Pattern.compile("(^\\d{18}$)|(^\\d{15}$)").matcher(this).matches()


/**
 * 判断字符串中是否包含中文
 * @param str
 * 待校验字符串
 * @return 是否为中文
 * @warn 不能校验是否为中文标点符号
 *
 * find()方法是部分匹配，是查找输入串中与模式匹配的子串，如果该匹配的串有组还可以使用group()函数
 */
fun String.isContainChinese() = Pattern.compile("[\u4e00-\u9fa5]").matcher(this).find()


/**
 * 是否含有非latin字符，即多字节字符
 * */
fun String.isContainMultiChar() = Pattern.compile(".*[^\\x00-\\xff].*").matcher(this).find()


/**
 * 校验字符是否是a-z、A-Z、_、0-9
 * @return true代表符合条件
 */
fun Char.isWord() = Pattern.compile("[\\w]").matcher(this.toString()).matches()

/**
 * 校验一个字符是否是多字节字符
 * @return true代表是汉字
 */
fun Char.isMultiChar(): Boolean {
    return try {
        toString().toByteArray(charset("UTF-8")).size > 1
    } catch (e: Exception) {
        false
    }
}

/**
 * 判定输入的是否是: 中日韩文字及标点符号
 * @return true代表是汉字
 */
fun Char.isCJK() = Character.UnicodeBlock.of(this).let{
    it === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || it === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || it === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || it === Character.UnicodeBlock.GENERAL_PUNCTUATION
            || it === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || it === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
}


/**
 * 将127.0.0.1形式的IP地址转换成十进制整数
 * */
fun String.ipv4ToLong(): Long? {
    val ip = LongArray(4)
    // 先找到IP地址字符串中.的位置
    val position1 = this.indexOf(".")
    if (position1 < 0) {
        return null
    }
    val position2 = this.indexOf(".", position1 + 1)
    if (position2 < 0) {
        return null
    }
    val position3 = this.indexOf(".", position2 + 1)
    if (position3 < 0) {
        return null
    }
    return try {
        // 将每个.之间的字符串转换成整型
        ip[0] = java.lang.Long.parseLong(this.substring(0, position1).trim { it <= ' ' })
        ip[1] = java.lang.Long.parseLong(this.substring(position1 + 1, position2).trim { it <= ' ' })
        ip[2] = java.lang.Long.parseLong(this.substring(position2 + 1, position3).trim { it <= ' ' })
        ip[3] = java.lang.Long.parseLong(this.substring(position3 + 1).trim { it <= ' ' })
         (ip[0] shl 24) + (ip[1] shl 16) + (ip[2] shl 8) + ip[3]
    } catch (e: java.lang.NumberFormatException) {
         null
    }

}

/**
 *  将十进制整数形式转换成127.0.0.1形式的ip地址
 */
fun Long.toIPv4(): String {
    val sb = StringBuffer("")
    // 直接右移24位
    sb.append(this.ushr(24).toString())
    sb.append(".")
    // 将高8位置0，然后右移16位
    sb.append((this and 0x00FFFFFF).ushr(16).toString())
    sb.append(".")
    // 将高16位置0，然后右移8位
    sb.append((this and 0x0000FFFF).ushr(8).toString())
    sb.append(".")
    // 将高24位置0
    sb.append((this and 0x000000FF).toString())
    return sb.toString()
}