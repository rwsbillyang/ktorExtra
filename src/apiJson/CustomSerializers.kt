package com.github.rwsbillyang.apiJson

import com.github.rwsbillyang.util.toUtc
import com.github.rwsbillyang.util.utcToLocalDateTime
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object ApiJson {
    val json = Json {
        apiJsonBuilder()
    }
    val json2 = Json {
        apiJsonBuilder()
        serializersModule = SerializersModule {
            contextual(ObjectIdBase64Serializer)
            //contextual(LocalDateTimeAsStringSerializer)
            contextual(LocalDateTimeAsLongSerializer)

        }
    }
}

fun JsonBuilder.apiJsonBuilder() {
    encodeDefaults = false
    ignoreUnknownKeys = true
    classDiscriminator = "_class" //某些payload中拥有type字段，会冲突
    //isLenient = true
    allowSpecialFloatingPointValues = true
    useArrayPolymorphism = false

}

@Deprecated("use ObjectIdBase64Serializer instead")
@Serializer(forClass = ObjectId::class)
object ObjectIdHexStringSerializer : KSerializer<ObjectId> {
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
object ObjectIdBase64Serializer : KSerializer<ObjectId> {
    private val base64Decoder = Base64.getUrlDecoder()
    private val base64Encoder = Base64.getUrlEncoder()
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ObjectIdBase64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(base64Encoder.encodeToString(value.toByteArray()))
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return ObjectId(base64Decoder.decode(decoder.decodeString()))
    }
}

fun ObjectId.to64String() = Base64.getUrlEncoder().encodeToString(toByteArray())
fun String.toObjectId() = ObjectId(Base64.getUrlDecoder().decode(this))


@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTimeAsStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(
            decoder.decodeString(),
            formatter
        )
    }
}

object LocalDateTimeAsLongSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTimeAsLongSerializer", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeLong(value.toUtc())
    override fun deserialize(decoder: Decoder): LocalDateTime = decoder.decodeLong().utcToLocalDateTime()
}
