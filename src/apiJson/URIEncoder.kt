/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-06-03 16:19
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

package com.github.rwsbillyang.ktorKit.apiJson

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


object URIEncoder {
    private val CHARSET: String = StandardCharsets.UTF_8.name()
    private val CHARACTERS = arrayOf(
        arrayOf("\\+", "%20"),
        arrayOf("%21", "!"),
        arrayOf("%27", "'"),
        arrayOf("%28", "("),
        arrayOf("%29", ")"),
        arrayOf("%7E", "~")
    )

    fun encodeURIComponent(text: String?): String {
        var result: String
        result = try {
            URLEncoder.encode(text, CHARSET)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
        for (entry in CHARACTERS) {
            result = result.replace(entry[0].toRegex(), entry[1])
        }
        return result
    }
}