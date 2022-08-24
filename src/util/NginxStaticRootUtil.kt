/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-23 21:13
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.ktorKit.util

/**
 * nginx配置中，临时文件需要展示或下载，将这些文件放在某个目录下，前面加前缀如"/static/"，
 * nginx通过识别该路径将其解析到不同的静态资源root下面，如下面的配置：
 * location ^~ /static/ {
 *    root   /home/www/wxAdmin/cacheRoot;
 * }
 * 当前路径为：/home/www/wxAdmin/，用 . 代替
 * Root的路径值为："/home/www/wxAdmin/cacheRoot" 或 "./cacheRoot"
 * path为： "static"
 *
 * */
object NginxStaticRootUtil {

    /**
     * 用于配置和获取ROOT的相对路径
     * */
    const val RootKey = "Root"
    const val DefaultRootValue = "./cacheRoot"
    /**
     * 用于nginx进行路径识别
     * */
    const val StaticKey = "Path"
    const val DefaultStaticValue = "static"

    /**
     * 用于设置nginx配置中的root和static
     * */
    fun setRootAndStatic(root: String, path: String){
        System.setProperty(RootKey, root)
        System.setProperty(StaticKey, path)
    }
    /**
     * such as return ./cacheRoot when default
     * */
    fun nginxRoot() = System.getProperty(RootKey, DefaultRootValue)
    /**
     * such as return static when default
     * */
    fun nginxPath() = System.getProperty(StaticKey, DefaultStaticValue)
    /**
     * 生成文件时，用于生成完整的路径，不包含文件名
     * @param myPath 自己指定的路径的后半部分, 前后都无"/"
     * @return 返回完整的路径即 "$root/$static/$myPath"
     * */
    fun getTotalPath(myPath: String):String {
        val root = System.getProperty(RootKey, DefaultRootValue)
        val static = System.getProperty(StaticKey, DefaultStaticValue)

        return "$root/$static/$myPath"
    }

    /**
     * 用于返回网页可访问的路径，不包含文件名
     * @param myPath 自己指定的路径的后半部分, 前后都无"/"
     * @return 返回完整的路径即 "$root/$static/$myPath"
     * */
    fun getUrlPrefix(myPath: String):String {
        val static = System.getProperty(StaticKey, DefaultStaticValue)

        return "/$static/$myPath"
    }
}