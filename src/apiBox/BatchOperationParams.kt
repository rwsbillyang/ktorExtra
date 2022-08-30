/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-28 18:03
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

package com.github.rwsbillyang.ktorKit.apiBox

import kotlinx.serialization.Serializable

/**
 * @param ids split by ","
 * @param action del, assign, updateStatus
 * @param arg1 parameter for action
 * @param arg1 parameter for action
 * */
@Serializable
class BatchOperationParams(
    val ids: String, //以 ","分隔的_id
    val action: String, //操作命令如： del, assign, updateStatus
    val arg1: String? = null, //提交的参数
    val arg2: String? = null //提交的参数
)