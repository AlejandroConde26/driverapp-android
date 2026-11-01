package com.driverapp.repartidor.data.remote

import com.driverapp.repartidor.BuildConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP único de la app (S08).
 *
 * - `Authorization: Bearer` inyectado en cada request desde [TokenDataStore].
 * - Timeouts explícitos (15 s conecta / 20 s lee): el backend es una IP pública.
 * - El log de red **sólo** se activa en debug: en release no se escriben URLs ni
 *   cabeceras en logcat.
 */
object RetrofitClient {

    fun build(baseUrl: String, tokenProvider: () -> String?): Retrofit {
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    tokenProvider()?.let { header("Authorization", "Bearer $it") }
                    header("Accept", "application/json")
                }.build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }
}
