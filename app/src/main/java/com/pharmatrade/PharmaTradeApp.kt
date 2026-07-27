package com.pharmatrade

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.di.AppContainer
import okhttp3.OkHttpClient

class PharmaTradeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
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

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(client)
                .build()
        )
    }
}
