package com.driverapp.repartidor.data

import android.content.Context
import android.content.SharedPreferences
import com.driverapp.repartidor.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Session {
    private const val PREFS = "driverapp_session"
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) = sp.edit().putString("token", token).apply()
    fun saveUser(user: User) = sp.edit().putString("user", GsonFactory.gson.toJson(user)).apply()
    fun token(): String? = sp.getString("token", null)
    fun user(): User? =
        sp.getString("user", null)?.let { runCatching { GsonFactory.gson.fromJson(it, User::class.java) }.getOrNull() }
    fun clear() = sp.edit().clear().apply()

    fun apiBaseUrl(): String = BuildConfig.API_BASE_URL
}

object GsonFactory {
    val gson = com.google.gson.Gson()
}

object ApiClient {
    private val okHttp: OkHttpClient by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                Session.token()?.let { req.header("Authorization", "Bearer $it") }
                chain.proceed(req.build())
            }
            .addInterceptor(interceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var cachedUrl: String? = null
    private var cachedService: ApiService? = null

    val service: ApiService
        get() {
            val url = Session.apiBaseUrl()
            if (cachedService == null || url != cachedUrl) {
                cachedUrl = url
                cachedService = Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttp)
                    .addConverterFactory(GsonConverterFactory.create(GsonFactory.gson))
                    .build()
                    .create(ApiService::class.java)
            }
            return cachedService!!
        }
}

object Auth {
    fun isFirebaseConfigured(): Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() && BuildConfig.FIREBASE_APP_ID.isNotBlank()

    suspend fun firebaseIdToken(): String? {
        return if (isFirebaseConfigured()) FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        else null
    }
}