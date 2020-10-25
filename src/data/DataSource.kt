package com.github.rwsbillyang.data



import com.github.rwsbillyang.ktorExt.MyMongoSerializerConfig
import org.koin.core.KoinComponent
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.serialization.configuration


//http://litote.org/kmongo/
class DataSource(dbName: String, host: String = "127.0.0.1", port: Int = 27017) : KoinComponent {
    var mongoDb: CoroutineDatabase

    init {
        //mongodb://[username:password@]host1[:port1][,...hostN[:portN]]][/[database][?options]]
        val client = KMongo.createClient("mongodb://${host}:${port}/${dbName}").coroutine
        mongoDb = client.getDatabase(dbName)


        //Null properties are taken into account during the update (they are set to null in the document
        // in MongoDb). If you prefer to ignore null properties during the update, you can use the
        // updateOnlyNotNullProperties parameter:
        //UpdateConfiguration.updateOnlyNotNullProperties = true


        //registerModule(serializersModuleOf(ObjectId::class, ObjectIdSerializer))
        //registerSerializer(MyReMsgSerializer)
        configuration = MyMongoSerializerConfig
    }
}
