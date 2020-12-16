package com.github.rwsbillyang.data


import com.github.jershell.kbson.Configuration

val MyMongoSerializerConfig = Configuration(
    encodeDefaults = false,
// classDiscriminator = "_class",
    nonEncodeNull = true
)