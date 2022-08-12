
使用ktor经常用到的公共套件， 提取作为公共库，提高重用和开发效率

## 引入
repositories中添加：`maven { url 'https://jitpack.io' }`:
```groovy
repositories {
			maven { url 'https://jitpack.io' }
		}
```

		
```groovy
implementation("com.github.rwsbillyang:ktorKit:$ktorKitVersion")
```

## AppModule
   随着系统演进，一个app往往逐渐会变成一个巨无霸，最后变成上帝代码。解决方案是：拆分。
   
   采用组件化开发方式，将拆分成出来的业务子系统打包成一个单独的库，需要的时候，直接在app里引用。而app不再有任何业务代码，这样可以灵活地拼装各种app。
   
   
   
   一个app例子，它包含了多个模块：
   ```kotlin
   
   @Suppress("unused") // Referenced in application.conf
   @kotlin.jvm.JvmOverloads
   fun Application.module(testing: Boolean = false) {
       val app = this
       val mainModule = AppModule(
           listOf( module{
               single<JwtHelper> { MyJwtHelper() }
               single<Application> { app }
           }),null)
   
       installModule(mainModule)
       installModule(newsModule,"youke_news")
       installModule(activityModule, "youke_activity")
       installModule(questionnaireModule, "youke_questionnaire")
       installModule(userModule,"youke_user")
       installModule(weixinModule,"youke_weixin")
       installModule(saleModule,"youke_sale")
       installModule(soloModule, "youke_solo")
   
   
       defaultInstall(MyJwtHelper())
   }
   ```
   其中`installModule`用于安装一个module，后面的字符串名称为MongoDB数据库名称。像这样，可以创建多个APP，分别`installModule`不同的module，就实现了不同的app子系统实例，灵活地实现业务系统的拆分。
   其第一个从参数Koin的modlue列表，表示需要注入的module，第二个参数时数据库名称，可以为null，意为无数据库，不传参表示使用appModule的默认名称，第三四个参数时数据库地址和端口号，使用默认值。
   
   
   
   一个appModule例子：
   ```kotlin
   val userModule = AppModule(
           listOf(module{single { UserSofaRpc(get()) }}, accountModule, statsModule, feedbackModule),
           "user")
   {
       userApi()
       feedbackApi()
   }
   

   ```
   
   其中，AppModule第一个参数时koin的一个module列表，用于列举出来子系统中需要依赖注入的对象实例
   
   第二个参数是数据库名称，为null时，表示无需使用数据库，无参时将使用module中定义的默认数据库名称；
   最后一个参数是routing，用于注册http API（endpoint）
   
   
 - defaultInstall
   
   默认install了一些feature，比如 / 和 /api/hello 用于测试后端app是否正在运行。

## JWT Auth认证与权限检查
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

权限控制
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

## LifeCycle
方便注册系统启动和关闭时的事件处理，比如微信配置
   
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
   
   ## cache
   暂只支持caffeine，未来计划支持redis
   
   基于caffeine，支持CachePut, CacheEvict等DSL
   
   非DSL用法，适合于简单的情形，无需每个函数中都指定cache，直接函数式调用。
   只需继承自CacheService，并且提供一个ICache，比如库中默认提供的CaffeinCache，多app server冗余部署时，需提供redis实现，库中暂未实现,未来计划支持redis
   
   非DSL用法使用示例1：
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
   DSL方式用法：略
   
   
   ## 列表分页查询

定义一个包括所有查询参数的data类，继承自IUmiListParams，controller中可调用checkValid检查参数合法性：
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
): IUmiListParams {
    fun toFilter(): Bson {
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
   
   ##  SOFA RPC
   
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
   
   ## apiJson
   kotlin-serialization和api响应payload的统一配置
   
   ## util
   
   ### String Extension
   用于对字符串的正则判断扩展，如是否电话号码，是否email等
   
   ### EmailSender
   需要自己搭建postfix，然后发邮件

   ### 其它
    
    时间日期工具 DatatimeUtil
    文件上传工具 UploadUtil
    压缩包工具 ZipFileUtil 

