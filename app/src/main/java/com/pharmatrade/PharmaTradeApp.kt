package com.pharmatrade

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.di.AppContainer
import com.russhwolf.settings.SharedPreferencesSettings
import okhttp3.OkHttpClient

class PharmaTradeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("pharmatrade_session", MODE_PRIVATE)
        SessionManager.init(SharedPreferencesSettings(prefs))
        container = AppContainer(this)
        setupCoil()
    }

    private fun setupCoil() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .apply {
                        SessionManager.authToken?.let {
                            addHeader("Authorization", "Bearer $it")
                        }
                    }
                    .build()
                chain.proceed(req)
            }
            .build()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { client }))
                }
                .build()
        }
    }
}
