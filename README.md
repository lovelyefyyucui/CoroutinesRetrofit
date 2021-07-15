[TOC]

# MVVM+协程+Retrofit

## 添加依赖库

```
//Kotlin
implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"

//协程
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.3"

//ktx
implementation "androidx.activity:activity-ktx:1.2.2"
implementation "androidx.fragment:fragment-ktx:1.3.3"

//OkHttp
implementation "com.squareup.okhttp3:okhttp:4.9.0"
implementation 'com.squareup.okhttp3:logging-interceptor:4.9.0'

// retrofit
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-scalars:2.9.0"
implementation "com.squareup.retrofit2:converter-gson:2.9.0"

//jetpack
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.3.1"
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.3.1"
```



## 基本实现

### 定义结果返回

使用密封类定义返回状态

```kotlin
sealed class DataResult<out T> {
    data class Success<T>(val response: T) : DataResult<T>()
    data class Error(val exception: ApiException) : DataResult<Nothing>()
}
```



### 定义接口

注：协程+Retrofit让代码变得更加简洁，suspend挂起函数无需再使用withContext切换线程

```kotlin
interface Api {
    @GET("article/list/{page}/json")
    suspend fun search(
        @Path("page") page: Int,
        @Query("author") author: String
    ): BaseResponse<Search>

    @GET("banner/json")
    suspend fun getBanners(): BaseResponse<List<Banner>>
}
```



### View层设计

```kotlin
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var loadingDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = ProgressDialog(this)

        binding.btnRequest.setOnClickListener {
            showLoading()
            viewModel.search1(1, "鸿洋").observe(this) {
                when (it) {
                    is DataResult.Success -> {
                        hideLoading()
                        binding.tvContent.text = it.response.toString()
                    }
                    is DataResult.Error -> {
                        hideLoading()
                        toast(it.exception.msg)
                    }
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading() {
        loadingDialog.show()
    }

    private fun hideLoading() {
        loadingDialog.dismiss()
    }
}
```



### ViewModel层设计

```kotlin
class MainViewModel : ViewModel() {

    private val searchLiveData = SingleLiveData<DataResult<BaseResponse<Search>>>()

    fun search1(page: Int, keywords: String): SingleLiveData<DataResult<BaseResponse<Search>>> {
        requestMain {
            val mainModel = MainModel()
            try {
                val response = mainModel.search(page, keywords)
                if (response.errorCode == 0) {
                    searchLiveData.value = DataResult.Success(response)
                } else {
                    searchLiveData.value = DataResult.Error(
                        ExceptionHandler.handleException(ServerException(response.errorCode,
                                                                         response.errorMsg)))
                }
            } catch (e: Exception) {
                searchLiveData.value = DataResult.Error(ExceptionHandler.handleException(e))
            }
        }
        return searchLiveData
    }
}
```



### Model层设计

```kotlin
class MainModel : BaseModel() {

    suspend fun search(page: Int, keywords: String): BaseResponse<Search> {
        return HttpManager.create(Api::class.java).search(page, keywords)
    }
}
```





## 优化实现

### 定义BaseModel

BaseModel用于数据转换，简化代码

```kotlin
open class BaseModel {
    suspend inline fun <T> launchRequestForResult(noinline block: suspend () -> T): DataResult<T> {
        return try {
            val response = block.invoke()
            if ((response as BaseResponse<*>).isSuccessful()) {
                DataResult.Success(response)
            } else {
                DataResult.Error(
                    ExceptionHandler.handleException(
                        ServerException(
                            response.errorCode,
                            response.errorMsg
                        )
                    )
                )
            }
        } catch (e: Exception) {
            return DataResult.Error(ExceptionHandler.handleException(e))
        }
    }
}
```



### ViewModel层调用

```kotlin
fun search2(page: Int, keywords: String): SingleLiveData<DataResult<BaseResponse<Search>>> {
    requestMain {
        val mainModel = MainModel()
        val dataResult = mainModel.search2(page, keywords)
        when (dataResult) {
            is DataResult.Success -> {
                searchLiveData.value = dataResult
            }
            is DataResult.Error -> {
                searchLiveData.value = dataResult
            }
        }
    }
    return searchLiveData
}
```







