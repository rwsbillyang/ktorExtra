
集成了相关第三方库，做到对[ktor](https://github.com/ktorio/ktor)更快地开箱即用。

# 1. 添加依赖
repositories中添加：`maven { url 'https://jitpack.io' }`:
```groovy
repositories {
			maven { url 'https://jitpack.io' }
		}
```


```groovy
implementation("com.github.rwsbillyang:ktorKit:$ktorKitVersion")
```

ktorKit集成的功能模块都为可选，即使用的是compileOnly引入依赖。如果自己的app中需要使用对应的功能时，则需在自己的应用中引入依赖。
例如：自己的应用中使用了MySQL数据库，则需在自己的app中添加如下依赖，否则提示找不到对应的库：
```groovy
    implementation "mysql:mysql-connector-java:$mysqlConnectorVersion"
    implementation "com.zaxxer:HikariCP:$HikariCPVersion"
    implementation("org.komapper:komapper-starter-jdbc:$komapperVersion")
    implementation("org.komapper:komapper-dialect-mysql-jdbc:$komapperVersion")
```

# 2. 依赖注入DI

集成了 [Koin](https://insert-koin.io/)  GitHub：https://github.com/InsertKoinIO/koin

引入依赖：
```groovy
    implementation "io.insert-koin:koin-core:$koin_version"
    testImplementation "io.insert-koin:koin-test:$koin_version"
    implementation "io.insert-koin:koin-ktor:$koin_version"
    implementation "io.insert-koin:koin-logger-slf4j:$koin_version"
```

各组件很多都是KoinComponent，直接可注入使用。

# 3. Server

添加依赖：
```groovy
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-data-conversion:$ktor_version")
    implementation("io.ktor:ktor-server-forwarded-header:$ktor_version")
    implementation("io.ktor:ktor-server-partial-content:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-call-id:$ktor_version")
    implementation("io.ktor:ktor-server-resources:$ktor_version")
```

## 3.1. installModule与组件化

随着系统演进，一个app往往越来越庞大，通过组件化，可以灵活引入应用内，或从应用内剔除。

将拆分成出来的业务子系统打包成一个单独的库，需要的时候，直接在app里引用。而app不再有任何业务代码，这样可以灵活地拼装各种app。

### 3.1.1. 示例1：多库多组件

包含了多个模块，使用多个MongoDB库（并且都是默认设置）、MySQL库，：
```kotlin
   @Suppress("unused") // Referenced in application.conf
   @kotlin.jvm.JvmOverloads
   fun Application.module(testing: Boolean = false) 
   {
   
       clientLogConfigFunc = {
        level = LogLevel.BODY
        filter { it.url.host.contains("qyapi.weixin.qq.com") }//qyapi.weixin.qq.com
    }

    val app = this
    val mainModule = AppModule(
        listOf(module(createdAtStart = true){
            single<AbstractJwtHelper> { MyJwtHelper() } //MyJwtHelper
            single<Application> { app }
        }),null)

    installModule(mainModule)
     
    installModule(wxUserAppModule, dbName) //wxUser feedback等功能 需预置产品信息

   installModule(wxOaAppModule,dbName) //公众号基础接入模块

        //MsgNotifier url设置, 改成数据库中配置
   System.setProperty("xcshop.corpId", "wwfc2fead39b1e6xxx")
   System.setProperty("xcshop.agentId", "1000005")

        //企业微信应用配置模块: 配置数据需要在应用自己的库中，否则启动时加载了别的agent的配置
  installModule(wxWorkModule, dbName) 

    //暂寄宿于此
    installModule(xcShopModule, "rws_xc_db", DbType.SQL, "rws_xc","123456")

    //defaultInstall(enableWebSocket= true, logHeaders = listOf("X-Auth-UserId"))//"X-Auth-ExternalUserId",
    defaultInstall(enableWebSocket= true)
   
   }
```
其中`installModule`用于安装一个组件模块（即第一个参数），第二个参数后面是符串，为MongoDB数据库名称，为null时，表示无需使用数据库，无参时将使用module中定义的默认数据库名称；

像这样，可以创建多个APP，分别`installModule`不同的module，就实现了不同的app子系统实例，灵活地实现业务系统的拆分。


一个appModule示例：
```kotlin

val xcShopModule = AppModule(
    listOf(
        module(createdAtStart = true){
            single { OnStartDailyCommentJob(get()) }
        },
        module {
            single { LiGoodsService(get()) }
            single { LiGoodsController() }

            single { CommentController()}
            single { CommentService(get()) }
            single { OrderService(get()) }
            single { OrderController() }
        }),
    "rws_xiucheng_db"
) {
    goodsApi()
    orderApi()
    commentApi()
}
```
AppModule第一个参数时koin的一个module列表，最后一个参数是routing，用于注册http API（endpoint）



### 3.1.2. 示例2： 只有MySQL

```kotlin
//默认情况下enableJsonApi为true，使用的是LocalDateTimeAsLongSerializer and ObjectId64Serializer
val MySerializeJson = Json {
    apiJsonBuilder()
    serializersModule = SerializersModule {
        contextual(LocalDateTimeAsStringSerializer) //默认情况下enableJsonApi为true，使用的是LocalDateTimeAsLongSerializer and ObjectId64Serializer
    }
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    // java -jar -DwithSPA=../webapp/www/ build/libs/RuleComposer-1.0.0-all.jar
    // java -jar -DwithSPA=../webapp/www/ -DdbHost=127.0.0.1 -DdbPort=3306 -DdbName=ruleEngineDb -DdbUser=root -DdbPwd=123456 -DdbHost=127 build/libs/RuleComposer-1.0.0-all.jar
    // nohup java -jar -DwithSPA=../webapp/www/ -DdbHost=127.0.0.1 -DdbPort=3306 -DdbName=ruleEngineDb -DdbUser=root -DdbPwd=123456 -DdbHost=127 build/libs/RuleComposer-1.0.0-all.jar > log/err.txt 2>&1 &
    val dbHost = System.getProperty("dbHost") ?: "127.0.0.1"
    val dbPort = System.getProperty("dbPort")?.toInt() ?: 3306
    val dbName = System.getProperty("dbName") ?: "ruleEngineDb"
    val dbUser = System.getProperty("dbUser") ?: "root"
    val dbPwd = System.getProperty("dbPwd") ?: "123456"
    val withSPA = System.getProperty("withSPA")

    val app = this
    val mainModule = AppModule(
        listOf(module(createdAtStart = true){
            single<Application> { app }
        }),null)

    installModule(mainModule)

    
   //使用SQL数据库，非默认设置
    installModule(bizModule,dbName, DbType.SQL, dbUser, dbPwd, dbHost, dbPort)

    //非默认配置
    defaultInstall(enableJwt = false, false, enableWebSocket = false)

    //https://ktor.io/servers/features/content-negotiation/serialization-converter.html
    //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/custom_serializers.md
    install(ContentNegotiation) {
        json(
            json = MySerializeJson,
            contentType = ContentType.Application.Json
        )
    }
    if(withSPA != null){
        log.info("======withSPA=$withSPA======")
        routing {
            singlePageApplication {
                react(withSPA)
            }
        }
    }else{
        log.info("======installCORS======")
        installCORS(false)
    }
    log.info("======dbHost=$dbHost======")
    log.info("======dbPort=$dbPort======")
    log.info("======dbName=$dbName======")
    log.info("======dbUser=$dbUser======")
    log.info("======dbPort=$dbPort======")
}
```

该示例，
- 使用了非默认的MongoDB数据库
- 没有权限控制，没有websocket
- JSON默认配置关闭，重新使用MySerializeJson，只添加了LocalDateTimeAsStringSerializer
- 根据是否指定withSPA参数，确定是否使用SPA前端单页应用，否则install CORS，且server app没有配置在nginx之后。


### 3.1.3. API参考

```kotlin
/**
 * 安装appModule，实际完成功能是
 * （1）将待注入的koin module添加到一个私有全局列表，便于defaultInstall中进行 install(Koin)
 * （2）将routing配置加入私有全局列表，便于后面执行，添加endpoint
 * （3）自动注入了DataSource（以数据库名称作为qualifier）
 * @param app 待安装的module
 * @param dbType DbType.NOSQL, DbType.SQL
 * @param userName 连接数据的用户名，mysql通常需要赋值
 * @param pwd 连接数据的密码，mysql通常需要赋值
 * @param dbName 数据库名称，不指定则使用AppModule中的默认名称
 * @param host 数据库host 默认127.0.0.1
 * @param port 数据库port 对于NOSQL MongoDB，默认27017， SQL之MySQL为3306
 * */
fun Application.installModule(
    app: AppModule,
    dbName: String? = null,
    dbType: DbType = DbType.NOSQL,
    userName: String? = null,
    pwd: String? = null,
    host: String = "127.0.0.1",
    port: Int = when(dbType){
        DbType.NOSQL -> 27017
        DbType.SQL -> 3306
    }
)
```
其第一个从参数Koin的modlue列表，表示需要注入的module，第二个参数时数据库名称，可以为null，意为无数据库，不传参表示使用appModule的默认名称，第三四个参数时数据库地址和端口号，使用默认值。




## 3.2. defaultInstall

每个APP都需使用一些常态化的plugin，可集成到一起，统一默认安装。

默认install了一些feature，如：
```kotlin
    install(Koin){ ... }
    install(AutoHeadResponse)
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(PartialContent)
    install(Resources)
    install(CallLogging){
        ...
    }
```
另：还注入了以数据库名称为标识的DataSource


其它则通过参数进行控制
```kotlin
/**
 * @param enableJwt 为false时只适合于route中无authentication时的情况
 * @param enableJsonApi 是否打开api接口json序列化
 * @param jsonBuilderAction 添加额外的自定义json配置，通常用于添加自己的json contextual
 * @param enableWebSocket 是否开启websocket
 * @param logHeaders 需要输出哪些请求头，用于调试
 * @param cache 自动注入 CaffeineCache，如不需要可使用VoidCache代替
 * */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.defaultInstall(
    enableJwt: Boolean = true,
    enableJsonApi: Boolean = true,
    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null,
    enableWebSocket: Boolean = false,
    logHeaders: List<String>? = null, //"X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
    cache: ICache = CaffeineCache()
)
```
比如：如果激活了enableJsonApi，将默认使用`ApiJson.serverSerializeJson`， 如果提供了jsonBuilderAction，则还将其添加进去。

## 3.3. CORS
当前后端分离，且api不在同一域名之后，或端口不同，则是跨域调用，需配置CORS

由于Nginx也需跨域配置，ktor serverApp在nginx之后时，需不同的配置，否则可能重写了响应头，导致跨域调用失败。

```kotlin
fun Application.installCORS(backOfNginx: Boolean)
```


示例：
```kotlin
if(withSPA != null){
        log.info("======withSPA=$withSPA======")
        routing {
            singlePageApplication {
                react(withSPA)
            }
        }
    }else{
        log.info("======installCORS======")
        installCORS(false)
    }
```
需要时才install CORS配置。


## 3.4. Security
默认defaultInstall时，将开启JWT Auth认证与权限检查，需配置自己的JwtHelper


```kotlin
//openssl genrsa -out rsa_private.key 2048
open class MyJwtHelper: UserInfoJwtHelper("https://wxAddmin.rwsbillyang.github.com/",
    "-----BEGIN RSA PRIVATE KEY-----\n" +
        "......"  +
    "-----END RSA PRIVATE KEY-----\n"

)
{

    //private val accountController: AccountController by inject()

    //private val userService: UserService by inject()

    override fun isValidUser(uId: String, payload: Payload): Boolean{
        //只做格式合法性检查
        return try{
            uId.toObjectId()
            true
        }catch (e:Exception){
            log.warn("invalid ObjectId uId=$uId")
            false
        }


        //val user = userService.findById(uId)
        //return user != null && user.status != User.StatusDisabled
    }
}
```



route权限控制
```kotlin
 authenticate {
            // verify admin privileges
            intercept(ApplicationCallPipeline.Call) {
                //log.info("intercept admin save: ${call.request.uri}")
                when (jwtHelper.isAuthorized(call, listOf("admin", "user"))) { //需要admin 或 user角色才可访问
                    false -> {
                        call.respond(HttpStatusCode.Forbidden)
                        return@intercept finish()
                    }
                    else -> proceed()
                }
            }

            route("/admin") {
                get<ArticleListParams> {
                    if(it.outline == 1){
                        call.respondBox(DataBox.ok(controller.outlineList(it)))
                    }else{
                        call.respond(controller.list(it, call.uId, call.isFromAdmin()))
                    }
                }
             }              
}

```

## 3.5. 为了测试

可用性路由：
```kotlin
fun Application.simpleTestableModule() {
    routing {
        get("/ok") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }
    }
}
```
在测试代码中，调用simpleTestableModule，将安装一个"ok"的路由，可用于可用性测试




对于需要权限检查的测试，可使用testModule，注入一个任何访问都放行的JwtHelper：
```kotlin
fun Application.testModule(module: AppModule) {
    val app = this
    installModule(AppModule(
        listOf(module(createdAtStart = true) {
            single<UserInfoJwtHelper> { TestJwtHelper() }
            single<AbstractJwtHelper> { DevJwtHelper() }
            single<Application> { app }
        }), null))
    installModule(module)
    defaultInstall(true)
}
```



## 3.6. LifeCycle
方便注册系统启动和关闭时的事件处理，比如某些微信配置需在启动后立即执行

onStarted和onStopping用于注册启动后和关闭时的执行操作handler，subscribeEvent用于订阅执行，订阅后才会生效。

比如配置微信：
```kotlin
   class WxConfigInstallation(application: Application): LifeCycle(application){
       init {
           onStarted {
               val prefService: PrefService by it.inject()
               val myMsgHandler: MsgHandler by it.inject()
               val myEventHandler: EventHandler by it.inject()

               prefService.findOfficialAccount()?.let {
                   application.install(OfficialAccountFeature) {
                       appId = it._id
                       secret = it.secret
                       encodingAESKey = it.aesKey
                       token = it.token
                       wechatId = it.wechatId
                       wechatName = it.name

                       msgHandler = myMsgHandler
                       eventHandler = myEventHandler                }
               }
           }
       }
   }
```
这意味着，在启动后，将注入service，查询公众号配置，试图配置好微信。



# 4. Client侧
使用client，需引入下面的依赖：
```groovy
    implementation "io.ktor:ktor-client-core-jvm:$ktor_version"
    implementation "io.ktor:ktor-client-cio-jvm:$ktor_version"
    implementation "io.ktor:ktor-client-content-negotiation:$ktor_version"
    implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
    implementation "io.ktor:ktor-client-encoding:$ktor_version"
```

在一些SDK中，需要与远程服务器进行接口交互，需用到HttpClient，这里提供了基于Ktor的Client实现。
为了实现方便，定义了一个DefaultClient的lazy懒加载CIO实现，并使用ApiJson.clientApiJson作为序列化配置。

并对文件的上传和下载做了实现：参见`doUpload`和`doDownload`

对于日志配置，默认配置为；
```kotlin
var clientLogConfigFunc: Logging.Config.() -> Unit = {
    logger = Logger.DEFAULT
    level = LogLevel.INFO
}
```
使用中可修改clientLogConfigFunc，以提供不同的配置.


# 5. api接口

需引入依赖：
```kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version") // JVM dependency
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
```

## 5.1. 序列化
server与client通过http方式进行交互，序列化采用效率更高的kotlin-serialization[Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)

### 5.1.1. ObjectId Serializer
ktorKit为MongoDB的ObjectId定义了两种序列化器，分别是ObjectIdBase64Serializer和ObjectIdHexStringSerializer，前者使用64进制，使用[0-9a-zA-Z]等URL中支持的字符作为编码字符串，生成19个字符长度的id，后者使用HEX16进制，生成24个字符长度id


### 5.1.2. LocalDateTime Serializer
分别提供了两种：- LocalDateTimeAsStringSerializer  格式化格式为: `yyyy-MM-dd HH:mm:ss`
- LocalDateTimeAsLongSerializer 生成Long类型数据

### 5.1.3. Json默认配置ApiJson
基本的json配置如下：
```kotlin
object ApiJson {
    //sealed class的子类通过classDiscriminator判断是哪个子类， kotlinx.serialization默认使用type
    //与正常的type字段冲突，kmongo/kbson 默认是___type，修改不生效。
    // 但使用___type, 与spring不兼容，spring中序列化时默认添加_class字段
    const val myClassDiscriminator = "_class"

    @OptIn(ExperimentalSerializationApi::class)
    fun JsonBuilder.apiJsonBuilder() {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true

        //输出给前端和前端提交的类型，可以为任意字符串，与kmongo/kbson无关，
        // 二者各使用各的classDiscriminator，都会转换成Java 对象实体
        //前端 <---ApiJson.serverSerializeJson---> Java对象实体 <---kmongo/kbson---> MongoDB bson存储
        // kmongo/kbson总是使用___type
        classDiscriminator = myClassDiscriminator
        //isLenient = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = false
    }
   
   ...
  
  }
```

- ApiJson.serverSerializeJson  通常用于server侧的序列化和反序列化，其在apiJsonBuilder基础上，额外添加了ObjectIdBase64Serializer和LocalDateTimeAsLongSerializer两种自定义序列化器。

- ApiJson.clientApiJson，通常用于远程server交互时，请求数据和收到的回复数据的序列化和反序列化


若自己的app有额外的需求，则需使用自己的Json配置

## 5.2. 业务数据与DataBox

所有的业务数据都被封装在DataBox中（即data字段）。
当code为OK时，表示返回正常，否则code表示错误码，通常错误码为KO，也可以定义其它更多错误码，前端根据它进行处理，而msg字段则表示出错时的出错信息。

其它字段type、tId、host则提供了更多的错误信息，如让前端如何展示错误、错误追踪id，发生在哪个主机上。若不需要，可忽略这些字段。

```kotlin
/**
 * @param code identify the result type, generally it's OK or KO
 * @param msg error message if code is not OK
 * @param data payload of the result
 *
 * @param type showType: error display type： 0 silent; 1 message.warn; 2 message.error; 4 notification; 9 page
 * @param tId traceId: Convenient for back-end Troubleshooting: unique request ID
 * @param host Convenient for backend Troubleshooting: host of current access server
 *
 * About parameters: type, tId, host, please refer to: https://umijs.org/plugins/plugin-request
 * Custom serializers for a generic type
 * https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#custom-serializers-for-a-generic-type
 *
 * */
@Serializable
class DataBox<T>(
    val code: String,
    val msg: String? = null,
    val data: T? = null,
    var type: Int? = null,
    var tId: String? = null,
    var host: String? = null
)
```


为了较方便地实现对DataBox的序列化，定义了下列扩展
```kotlin
ApplicationCall.respondBox(box: DataBox<T>)
ApplicationCall.respondBoxOK(data: T)
ApplicationCall.respondBoxKO(msg: String)
ApplicationCall.respondBoxJsonText(json: String)
```

## 5.3. PostData和BatchOperationParams
前端只提交了一个字符串，后端可以通过PostData接收，避免简单的业务数据创建一个类
```kotlin
class PostData(val data: String)//data 任意业务数据
```

对于前端提交的批量操作，后端可通过BatchOperationParams接收处理：
```kotlin
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
```


在前端[usecache](https://github.com/rwsbillyang/usecache)库中，已提供对DataBox的解析处理，以及对请求数据的前端缓存。

# 6. Database

## 6.1. 分页与排序

```kotlin
/**
 * 1 for acs
 * -1 for desc
 * */
object Sort {
    const val ASC = 1
    const val DESC = -1
}


/**
 * the type of sort key, receives from the client(front end),
 * includes: TypeNumber, TypeString, TypeObjectId
 * */
enum class SortKeyType{
 TypeNumber, TypeString, TypeObjectId
}


/**
 * front end show list, encode search parameters and pagination info: &umi=encodeURIComponent(pagination_and_sort_parameters:UmiPagination)
 * sever side get umi value,  using pagination to decode it and get UmiPagination info
 * */
interface IUmiPaginationParams{
    val umi: String?
    val pagination: UmiPagination
        get() = umi?.let { Json.decodeFromString(URLDecoder.decode(it,"UTF-8")) }?:UmiPagination()

    /**
     * convert search params to Bson(not include UmiPagination.lastIdFilter) for mongodb/Kmongo
     * */
    fun toFilter(): Bson{
        TODO("Not Implement")
    }
    
    /**
     * convert search params to SqlPagination for mysql/komapper
     * */
    fun toSqlPagination(): SqlPagination{
        TODO("Not Implement")
    }
}
```
Sql和NoSql根据自己的集成库，给出各自不同的实现即可。


## 6.2. Sql
基于[Komapper官网](https://www.komapper.org/docs/) Github： https://github.com/komapper/komapper

### 6.2.1. CRUD


无需为每个业务类定义service和controller，以及路由，通过route中的name区分是哪种类型，然后增删查改。

```kotlin
/**
 * 一个service实现了对各个model的crud的处理，添加新的model时，需将model放到EnumMeta中
 * 若需自定义功能，可在子类中实现
 * */
open class BaseCrudService(cache: ICache): SqlGenericService(bizModule.dbName!!, cache)
```

```

class BaseCrudController : KoinComponent {
    private val log = LoggerFactory.getLogger("BaseCrudController")

    companion object {
        const val Name_domain = "domain"
        const val Name_param = "param"
        //...
    }

    private val service: BaseCrudService by inject()


    fun findPage(name: String, params: IUmiPaginationParams): String {
        return when (name) {
            Name_domain -> MySerializeJson.encodeToString(DataBox.ok(service.findAll(Meta.domain, {})))
            Name_param -> MySerializeJson.encodeToString(
                DataBox.ok(
                    service.findPage(
                        Meta.param,
                        params.toSqlPagination()
                    ).onEach { it.toBean(service) })
            )
            //...
            else -> {
                log.warn("findPage: Not support $name in findPageList")
                MySerializeJson.encodeToString(DataBox.ko<Unit>("findPage: Not support $name in findPageList"))
            }
        }
    }

    /**
     * @return 返回DataBox的json字符串
     * */
    fun findOne(name: String, id: Int): String {
        return when (name) {
            Name_domain -> MySerializeJson.encodeToString(
                DataBox.ok(service.findOne(Meta.domain,{ Meta.domain.id eq id }, "domain/$id"))
            )

            Name_param -> MySerializeJson.encodeToString(
                DataBox.ok(service.findOne(Meta.param,{ Meta.param.id eq id }, "param/$id")?.apply { toBean(service) })
            )
            //...

            else -> {
                log.warn("findOne: Not support $name in findOne")
                MySerializeJson.encodeToString(DataBox.ko<Unit>("findOne: Not support $name in findOne"))
            }
        }
    }

    /**
     * @return 返回DataBox的json字符串
     * 新增顶级节点、编辑修改时的保存，
     * 在新增和鞭酒修改时无需构建children，因1是新增时没有无需构建，2是修改时构建也是从当前节点开始的，不是从根节点开始的parentPath
     * */
    suspend fun saveOne(name: String, call: ApplicationCall) = when (name) {
        Name_domain -> {
            val e = call.receive<Domain>()
            MySerializeJson.encodeToString(
                DataBox.ok(service.save(Meta.domain,e,e.id == null, e.id?.let { "domain/${it}" })))
        }

        Name_param -> {
            val e = call.receive<Param>()
            MySerializeJson.encodeToString(
                DataBox.ok(service.save(Meta.param,e,e.id == null,e.id?.let { "param/${it}" }).apply { toBean(service) }))
        }
        //...

        else -> MySerializeJson.encodeToString(DataBox.ko<Int>("saveOne: not support $name"))
    }

    /**
     * 删除一项
     * */
    fun delOne(name: String, id: Int): DataBox<Long> {
        val count = when (name) {
            Name_domain -> service.delete(Meta.domain, { Meta.domain.id eq id }, "domain/$id")
            Name_param -> service.delete(Meta.param, { Meta.param.id eq id }, "param/$id")
            //...

            else -> {
                log.warn("delOne: Not support $name in delOne")
                0L
            }
        }
        return DataBox.ok(count)
    }

    /**
     * 批处理，暂只支持action：Del
     * */
    fun batchOperation(name: String, batchParams: BatchOperationParams): DataBox<Long> {
        val ids = batchParams.ids.split(",").map { it.toInt() }

        if (ids.isEmpty())
            return DataBox.ko("batchOperation: invalid parameter: no name or ids")

        return when (batchParams.action) {
            "del" -> DataBox.ok(batchDel(name, ids))
            else -> DataBox.ko("batchOperation: not support action: ${batchParams.action}")
        }
    }

    private fun batchDel(name: String, ids: List<Int>): Long {
        val count = when (name) {
            Name_domain -> service.delete(Meta.domain,{ Meta.domain.id inList ids.map { it } },  null, ids.map { "domain/$it" })

            Name_param -> service.delete( Meta.param, { Meta.param.id inList ids.map { it } }, null, ids.map { "param/$it" })
            //...

            else -> {
                log.warn("batchOperation: Not support $name in batchDel")
                0L
            }
        }
        return count
    }
}
```

### 6.2.2. 主键
对于分布式系统，或数据量特别大的系统，不宜使用自增主键，此时可使用SnowflakeId生成主键:

```kotlin
SnowflakeId.getId()
```

### 6.2.3. 只选取部分字段的查询

```kotlin
//只选取部分字段
    fun findSkuOutlineList(params: SkuListParams): List<SkuOutlineBean>{
        val meta = Meta.goodsSku
        return db.runQuery{
            val pagination = params.toSqlPagination()

            (pagination.where?.let { QueryDsl.from(meta).where(it) }?:QueryDsl.from(meta))
                .orderBy(pagination.sort)
                .offset(pagination.offset)
                .limit(pagination.pageSize)
                .select(meta.id, meta.goodsId, meta.goodsName, meta.storeId, meta.storeName, meta.thumbnail, meta.createdAt)
        }.map{
            SkuOutlineBean(it[meta.id].toString(),it[meta.goodsId].toString(),
                it[meta.goodsName], it[meta.storeId].toString(),it[meta.storeName],it[meta.thumbnail],it[meta.createdAt])
        }
    }
```

### 6.2.4. 分页


**注意：**
**1.若希望得到在某些查询条件下的全部数据，前端只需将pageSize设置为-1即可**
**2.lastId字段尤其需要注意，可进行大小比较，记录的值尽量具备唯一性，尽量不要选取记录里有大量相等值的字段，否则失去意义**
**3.若需全部数据（无过滤查询条件），则调用基类的findAll函数**

基于komapper（https://www.komapper.org/）

- 示例1：

```kotlin
@Serializable
@Resource("/skuList")
class SkuListParams(
    override val umi: String? = null,
    val id: String? = null,
    val goodsId:String? = null,
    val storeId: String? = null,
    val keyword: String? = null, //搜索关键字
) : IUmiPaginationParams {
    override fun toSqlPagination(): SqlPagination {
        val meta = Meta.goodsSku
        val lastId = pagination.lastId
        var lastW: WhereDeclaration? = null
        val sort: SortExpression
        
        when (pagination.sKey) {
            "createdAt" -> {
                val key = meta.createdAt
                sort = if (pagination.sort == Sort.DESC) key.desc() else key.asc()
                if (lastId != null) {
                    val lastT = lastId.toLong().utcToLocalDateTime()
                    lastW = if (pagination.sort == Sort.DESC) {
                        { key less lastT }
                    } else {
                        { key greater lastT }
                    }
                }
            }
            else -> {
                val key = meta.id
                sort = if (pagination.sort == Sort.DESC) key.desc() else key.asc()
                if (lastId != null) {
                    val lastT = lastId.toLong()
                    lastW = if (pagination.sort == Sort.DESC) {
                        { key less lastT }
                    } else {
                        { key greater lastT }
                    }
                }
            }
        }

        val w1: WhereDeclaration? = if(id != null) { { meta.id eq id.toLong() } } else null
        val w2: WhereDeclaration? = if(keyword != null) { { meta.goodsName like keyword } }else null
        val w3: WhereDeclaration? = goodsId?.let { { meta.goodsId eq it.toLong() }  }
        val w4: WhereDeclaration? = storeId?.let { { meta.storeId eq it.toLong() }  }
        
        //pageSize为-1时表示该查询条件下的全部数据
        return SqlPagination(sort, pagination.pageSize, (pagination.current - 1) * pagination.pageSize)
            .addWhere(w1, w3, w4, lastW, w2)
    }
}
```

完整调用过程：
```kotlin
//route中
get<CommentQueryParams>{
     call.respondBoxOK(commentController.findPage(it))
}

//controller中   
fun findPage(params: CommentQueryParams) = commentService.findPage(params)
        .map { it.toBean() }
        
//service中
fun findPage(params: CommentQueryParams) = findPage(Meta.comment, params.toSqlPagination())
```


- 示例2：

对于lastId固定为某个字段的排序字段的情况，无需根据pagination.sKey进行排序
```kotlin
@Resource("/list") //  /parameter/list
@Serializable
class ParameterQueryParams(
    override val umi: String? = null,
    val label: String? = null,
    val typeId: Int? = null,
    val key: String? = null
): IUmiPaginationParams {
    override fun toSqlPagination(): SqlPagination {
        val meta = Meta.parameter
        val lastId = pagination.lastId
        var lastW: WhereDeclaration? = null
        val sort: SortExpression

        //一般是根据pagination.sKey进行排序，但此处总是根据id进行排序
        val sortKey = meta.id
        sort = if (pagination.sort == Sort.DESC) sortKey.desc() else sortKey.asc()
        if (lastId != null) {
            val lastT = lastId.toInt()
            lastW = if (pagination.sort == Sort.DESC) {
                { sortKey less lastT }
            } else {
                { sortKey greater lastT }
            }
        }

        val w1: WhereDeclaration? = if(label != null) { { meta.label like label } } else null
        val w2: WhereDeclaration? = typeId?.let { { meta.typeId eq it }  }
        val w3: WhereDeclaration? = key?.let { { meta.key like it }  }

        //pageSize为-1时表示该查询条件下的全部数据
        return SqlPagination(sort, pagination.pageSize, (pagination.current - 1) * pagination.pageSize)
            .addWhere(w1, w2, w3, lastW)
    }
}
```




## 6.3. NoSQL

基于[KMongo](https://litote.org/kmongo/) Github: https://github.com/Litote/kmongo
使用时，需引入依赖：
```groovy
    implementation ("com.github.jershell:kbson:$kbson_version")
    implementation ("org.litote.kmongo:kmongo-coroutine-serialization:$kmongo_version")
```
### 6.3.1. CRUD

TODO

### 6.3.2. 分页


后端业务代码：
```kotlin

    //搜索接收的过滤参数
    @Location("/list")
    data class OrderInfoListParams(
        override val umi: String? = null,
        val _id: String? = null,
        val adder: String? = null, //添加人

        val pf: String? = null,
        val shopId: String? = null, //所属shop

        val product: String? = null,

        val name: String? = null, //收货人
        val tel: String? = null, //收货人
        val type: Int? = null,

        val appId: String? = null,

        val lastId: String? = null//必须为字符串类型，然后根据前端传递过来的类型信息，进行bson字符串的构建
    ): IUmiListParams { //实现IUmiListParams接口目的在于继承一个函数：将umi字符串转换成UmiPagination对象
        fun toFilter(): Bson {
            val idFilter = _id?.let { OrderInfo::_id eq it}

            val adderF = adder?.let { OrderInfo::adder eq it.toObjectId() }
            val pfFilter = pf?.let { OrderInfo::pf eq it }
            val shopIdF = shopId?.let { OrderInfo::shopId eq it.toObjectId() }
            val productF = product?.let { OrderInfo::product regex  (".*$it.*")}
            val nameF = name?.let { OrderInfo::name regex  (".*$it.*")}
            val telF = tel?.let { OrderInfo::tel eq it }
            val typeFilter = type?.let { OrderInfo::type eq it }
            val appIdF = appId?.let { OrderInfo::appId eq it }
            return and(idFilter, adderF, nameF, telF, shopIdF, productF, pfFilter,  typeFilter, appIdF)
        }
    }

        //后端路由 endpoint
        route("/orderInfo") {
                get<OrderInfoListParams> {
                    val list = service.findOrderInfoList(it.toFilter(), it.pagination, it.lastId)
                    call.respondBox(DataBox.ok(list))
                }
        }
        
        class Service{
         //所有的列表查询都一样，只需换掉对象示例OrderInfo，以及orderInfoCol
            fun findOrderInfoList(filter: Bson, pagination: UmiPagination, lastId: String?): List<OrderInfo> = runBlocking {
                    val sort =  pagination.sortJson.bson
                    if(lastId == null)
                        orderInfoCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
                    else{
                        orderInfoCol.find(and(filter,pagination.lastIdFilter(lastId))).limit(pagination.pageSize).sort(sort).toList()
                    }
                }
        }
    
```

前端代码：
```typescript
 const initialQuery: OrderInfoListQueryForApp = { appId, shopId, productId, pagination: {pageSize: 10,  current: 1, sKey: 'create', sKeyType: "TypeNumber", sort: -1  } }
```
此处指定了排序键 为'create'，其数据类型是"TypeNumber"， -1代表降序。若按默认ObjectId排序，则前端的initialQuery中无需指定pagination，后端为空时自动创建一个默认的UmiPagination，前端若指定了pagination则使用指定的值。





示例2：
```kotlin
定义一个包括所有查询参数的data类，继承自IUmiPaginationParams，controller中可调用checkValid检查参数合法性：
```kotlin
/**
 * 列表过滤查询
 * @param _id 直接查询某个id
 * @param scope 属主范围
 * @param state 状态
 * @param uId  属主
 * @param tag 标签查询
 * @param keyword 关键字查询
 * @param lastId 上一条记录中最后一条的id，用于分页  Limitation： lastId只有在基于_id排序时才可正确地工作，而且只支持上下一页
 * */
@Serializable
@Resource("/list")
data class ArticleListParams(
        override val umi: String? = null,
        val _id: String? = null,
        val scope: Int? = null,
        val state: Int? = null,

        val uId: String? = null,
        val corpId: String? = null,

        val tag: String? = null,
        val keyword: String? = null,
        val lastId: String? = null,
        val outline: Int = 1 //不同的api
): IUmiPaginationParams {
    override fun toFilter(): Bson {
        val idFilter = _id?.let { Article::_id eq it.toObjectId() }
        val corpIdFilter = corpId?.let { Article::corpId eq it }

        //如果未指定scope，或指定为ALL，或指定为私有且指定了某个用户，都不做限制。对于最后一种情况私有个人素材，目的在于：即使充公了，也会在其个人上传列表里显示
        val scopeFilter = if(scope == null || scope == Article.SCOPE_ALL) null else  Article::scope eq scope
        val uIdFilter = uId?.let{Article::uId eq it.toObjectId()}

        val tagFilter = tag?.let{Article::tags.all(it) }
        val keywordFilter = keyword?.let { Article::title regex  (".*$it.*")}


        return if(outline == 1){
            //...
        }else{//for admin
            val stateFilter = state?.let { Article::state eq it }?:Article::state gt Constants.STATUS_DEL
            and(idFilter, uIdFilter, corpIdFilter,scopeFilter, stateFilter,tagFilter, keywordFilter)
        }
    }

    /**
     * 若参数合法性
     * */
    fun checkValid(): Boolean{
        //...
        return false
    }
}
```

然后serverice中创建查询列表函数：

```kotlin
    fun findList(filter: Bson, pagination: UmiPagination,lastId: String?): List<Article> = runBlocking {
        val sort =  pagination.sortJson.bson
        if(lastId == null)
            articleCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
        else{
            articleCol.find(and(filter,pagination.lastIdFilter(lastId))).limit(pagination.pageSize).sort(sort).toList()
        }
    }
```



API参考：

```kotlin
/**
 * pagination info, sort info, and filter key info
 * @param pageSize default 20
 * @param current starts from 1, not 0
 * @param sKey sort key.  mongodb example: "sorter":{"updatedAt":"ascend"} , the sort key is "updatedAt"
 * @param sort 1 for asc，-1 for desc, same as MongoDB
 * @param sKeyType the type of sKey
 * @param lastId the last value of sort key in current page when load more
 * @param fKey filter key
 * @param filters  items which contains values of filters, "filter":{"someKey":["value1",123,"value3"]}
 *
 * using var instead of val，aims to modify them for permission
 * */
@Serializable
class UmiPagination(
     var pageSize: Int = 10,
     var current: Int = 1,
     var sKey: String = "_id", //sortKey
     var sort: Int = Sort.DESC, //1用于升序，而-1用于降序
     val sKeyType: SortKeyType = SortKeyType.TypeObjectId,
     val lastId: String? = null,
     var fKey: String? = null, //filter key
     var filters: List<String>? = null
){
    /**
     * for mongodb/kmongo sort
     * */
    val sortJson = "{'${sKey}':${sort}}"

    /**
     * setup mongodb bson for pagination
     * new version: ignore the parameter. legacy: pass lastId in listSearchParams,
     * new version, lastId is in UmiPagination, legacy version it's in listSearchParams
     * @return mongodb bson
     * */
    fun lastIdFilter(): Bson? {
        if(lastId == null) return null
        val s = if(sort == Sort.DESC) "\$lt" else "\$gt"
        return when(sKeyType){
            SortKeyType.TypeNumber -> "{ '${sKey}': { $s: $lastId } }"
            SortKeyType.TypeString -> "{ '${sKey}': { $s: `$lastId` } }"
            SortKeyType.TypeObjectId -> "{ '${sKey}': { $s: ObjectId(\"${lastId.toObjectId().toHexString()}\") } }"
        }.bson
    }
}
```

## 6.4. Cache

暂只支持caffeine，暂未实现redis的Cache。

基于caffeine，支持CachePut, CacheEvict等DSL

非DSL用法，适合于简单的情形，无需每个函数中都指定cache，直接函数式调用。
只需继承自CacheService，并且提供一个ICache，比如库中默认提供的CaffeinCache，多app server冗余部署时，需提供redis实现。


如不需要使用缓存，可注入VoidCache，它将不进行任何缓存操作。

默认采用的是Caffeine， 使用中需引入以下依赖：
```groovy
    testImplementation "com.github.ben-manes.caffeine:caffeine:$caffeineVersion"
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
```

若使用其它方案，则需自行实现ICache接口，并引入对应依赖。

非DSL用法使用示例：
```kotlin
class ArticleService(cache: ICache) : CacheService(cache){
    
    fun find(id: String) = cacheable("article/$id"){
        runBlocking {
            articleCol.findOneById(id.toObjectId())
        }
    }

    fun insert(doc: Article) = putable("article/${doc._id.to64String()}")
    {
        runBlocking { articleCol.insertOne(doc) }
        doc
    }
    
    fun deleteOne(id: String) = evict("article/$id"){
        runBlocking{
           articleCol.deleteOneById(id.toObjectId())
        }
    }
    fun batchDel(ids: List<String>) = batchEvict(ids.map { "article/$it" }){
        runBlocking {
           articleCol.deleteMany(Article::_id `in` ids.map { it.toObjectId() })
        }
   }
}
```
DSL方式用法：TODO



## 6.5. 非依赖注入

有时候不方便通过依赖注入方式使用Service，却又得使用它。但仍需添加Koin依赖：
```groovy
    implementation "io.insert-koin:koin-core:$koin_version"
    testImplementation "io.insert-koin:koin-test:$koin_version"
```
因为ktorKit中很多类继承自Koin中的KoinComponent，不需要再实现该接口。毕竟应用中，不需要依赖注入的场景还是少。


如下面的代码，继承自AbstractSqlService，实现了字段dbSource，而不是依赖注入：
```kotlin
class AvatarService(cache: ICache): AbstractSqlService( cache) {
    override val dbSource: SqlDataSource = SqlDataSource("xcshopDB", "127.0.0.1", 3306, "root", "123456")

    fun saveAvatars(list: List<Avatar>) = batchSave(Meta.avatar, list, true)

    fun saveNicks(list: List<Nick>) = batchSave(Meta.nick, list, true)

    fun findAllNicks(w: WhereDeclaration) = findAll(Meta.nick, w)
    fun updateNick(id: Int, name: String) = db.runQuery {
        QueryDsl.update(Meta.nick).set {
            Meta.nick.name eq name
        }.where {
            Meta.nick.id eq id
        }
    }
    fun updateNick2(id: Int, name: String) = updateValues(Meta.nick,
        {Meta.nick.name eq name}, {Meta.nick.id eq id})

}

```


使用AvatarService示例：
```kotlin
class AvatarPipeline: Pipeline {
    private val avatarService: AvatarService = AvatarService(VoidCache())

    override fun process(resultItems: ResultItems, task: Task) {
        //...
        if(list.isNotEmpty())
            avatarService.saveAvatars(list)
    }
    //...
}

```


# 7. 其它
## 7.1. utils

### 7.1.1. String Extension
用于对字符串的正则判断扩展，如是否电话号码，是否email等

```kotlin
/**
 * String的扩展函数： 是否是IP地址，只支持IPv4版本
 * */
fun String.isIp() : Boolean

/**
 * String的扩展函数： 是否是中国大陆手机号
 * */
fun String.isMobileNumber() : Boolean
/**
 *  是否是邮箱
 * */
fun String.isEmail(): Boolean

/**
 *  是否是http Url
 * */
fun String.isUrl(): Boolean

/**
 *  是否是身份证号码，不支持最后一位是字母？
 * */
fun String.IsIDCard(): Boolean


/**
 * 判断字符串中是否包含中文
 * @param str
 * 待校验字符串
 * @return 是否为中文
 * @warn 不能校验是否为中文标点符号
 *
 * find()方法是部分匹配，是查找输入串中与模式匹配的子串，如果该匹配的串有组还可以使用group()函数
 */
fun String.isContainChinese(): Boolean


/**
 * 是否含有非latin字符，即多字节字符
 * */
fun String.isContainMultiChar(): Boolean

/**
 * 校验字符是否是a-z、A-Z、_、0-9
 * @return true代表符合条件
 */
fun Char.isWord(): Boolean

/**
 * 校验一个字符是否是多字节字符
 * @return true代表是汉字
 */
fun Char.isMultiChar(): Boolean

/**
 * 判定输入的是否是: 中日韩文字及标点符号
 * @return true代表是汉字
 */
fun Char.isCJK(): Boolean


/**
 * 将127.0.0.1形式的IP地址转换成十进制数
 * */
fun String.ipv4ToLong(): Long?

/**
 *  将十进制整数形式转换成127.0.0.1形式的ip地址
 */
fun Long.toIPv4(): String

/**
 * 驼峰法转下划线
 */
fun String.camel2Underline(): String
```


### 7.1.2. DatatimeUtil

LocalDateTime与Long全局转换， 以及计算
```kotlin
LocalDateTime.toUtc()
Long.utcToLocalDateTime
Long.utcSecondsToLocalDateTime
Long.plusTime(years: Long = 0L, months: Long = 0L, days: Long = 0L) 
```

日期解析与格式化：

- DatetimeUtil.format  对Long和LocalDateTime的格式化，默认格式为：`yyyy-MM-dd HH:mm:ss`
- DatetimeUtil.parse  parse出LocalDateTime

计算某日的起始毫秒：

- DatetimeUtil.getTodayStartMilliSeconds  计算今天的起始毫秒：
- DatetimeUtil.getStartMilliSeconds 数日数月前的某天的起始毫秒

### 7.1.3. EmailSender
需要自己搭建postfix，然后发邮件

EmailSender.sendEmail


### 7.1.4. IpCheckUtil
是否本机IP，以及获取本机IP


### 7.1.5. UploadUtil
处理上传文件、multipart、base64编码的上传，文件路径等

### 7.1.6. URIEncoder
与js类似，encodeURIComponent编码





### 7.1.7. NginxStaticRootUtil

```kotlin
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
 * object NginxStaticRootUtil {

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
    fun getTotalPath(myPath: String):String 

    /**
     * 用于返回网页可访问的路径，不包含文件名
     * @param myPath 自己指定的路径的后半部分, 前后都无"/"
     * @return 返回完整的路径即 "$root/$static/$myPath"
     * */
    fun getUrlPrefix(myPath: String):String 
}
```
### 7.1.8. ZipFileUtil




## 7.2. RPC

采用SOFA RPC方案，使用时需要在在自己app中引入SOFA RPC的相关依赖

RPC接口即RPC通信实体类，需要放在公共库里定义，以便通信双方均可使用。

如需支持此功能，引入SOFA RPC库即可使用

对于server一侧，需要实现接口类，client一侧直接注入，无需写一个client子类。

如Server侧：
```kotlin
   
   class UserRpcServer : IUser, KoinComponent {
       private val service: AccountService by inject()
   
       override fun updateAccountExpiration(
           uId: String?, oId: String, edition: Int, year: Int,
           month: Int, bonus: Int
       ): Boolean {
           //... //真正实现
       }
   }
   
   
   //启动时注册RPC：
   class UserSofaRpc(application: Application): SofaRpc(application){
       private val accountRpcServer: UserRpcServer by inject()
   
       override fun publicRpcService() { // publish service
           ProviderConfig<IUser>()
               .setInterfaceId(IUser::class.java.name)
               .setRef(accountRpcServer)
               .setServer(serverConfig)
               .setRegistry(registryConfig)
               .export()
       }

       override fun injectRpcClient() {}
   }
```

client侧直接注入使用：

```kotlin
   class OrderController : KoinComponent {
       private val userClient: IUser by inject()
       //直接使用userClient
   }
   
   //启动时注册
   class OrderSofaRpc(application: Application) : SofaRpc(application) {
       override fun publicRpcService() {}
   
          override fun injectRpcClient() {
           val config = getConsumerConfig<IUser>()
           if(config != null){
               //https://start.insert-koin.io/#/getting-started/modules-definitions?id=module-amp-definitions
               loadKoinModules(module {
                   single<IUser> { config.refer() }
               })
           }
       }
   }
```
注意，需在启动时注入，可使用LifeCycle自动注入。
   







