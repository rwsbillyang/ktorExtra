package com.github.rwsbillyang.ktorExt


import com.github.jershell.kbson.Configuration

val MyMongoSerializerConfig = Configuration(
    encodeDefaults = true,
// classDiscriminator = "_class",
    nonEncodeNull = true
)