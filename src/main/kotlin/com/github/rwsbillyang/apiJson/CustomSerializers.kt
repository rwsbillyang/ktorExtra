package com.github.rwsbillyang.apiJson

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


val apiJson = Json {
    apiJsonBuilder()
}

fun JsonBuilder.apiJsonBuilder(){
    encodeDefaults = false
    ignoreUnknownKeys = true
    //isLenient = true
    allowSpecialFloatingPointValues = true
    useArrayPolymorphism = false
//    serialModule = serializersModuleOf(mapOf(
//        ObjectId::class to ObjectIdStringSerializer,
//        LocalDateTime::class to LocalDateTimeStringSerializer
//    ))
}

@Deprecated("use ObjectIdStringSerializer instead")
@Serializer(forClass = ObjectId::class)
object ObjectIdHexStringSerializer: KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ObjectIdHexStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(value.toHexString())
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return ObjectId(decoder.decodeString())
    }
}

/**
 * based on Base64 URL Encoder and Decoder
 * */
@Serializer(forClass = ObjectId::class)
object ObjectIdStringSerializer: KSerializer<ObjectId> {
    private val base64Decoder = Base64.getUrlDecoder()
    private val base64Encoder = Base64.getUrlEncoder()
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ObjectIdStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        base64Encoder.encodeToString(value.toByteArray())
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return ObjectId(base64Decoder.decode(decoder.decodeString()))
    }
}

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeStringSerializer: KSerializer<LocalDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTimeStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(),
            formatter
        )
    }
}

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}
