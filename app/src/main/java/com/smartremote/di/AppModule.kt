package com.smartremote.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.smartremote.BuildConfig
import com.smartremote.data.local.SmartRemoteDatabase
import com.smartremote.data.local.dao.*
import com.smartremote.data.remote.api.AnthropicApiService
import com.smartremote.data.remote.api.SmartRemoteApiService
import com.smartremote.data.remote.mqtt.MqttManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Database ──────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartRemoteDatabase =
        Room.databaseBuilder(context, SmartRemoteDatabase::class.java, SmartRemoteDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideDeviceDao(db: SmartRemoteDatabase) = db.deviceDao()
    @Provides fun provideRoomDao(db: SmartRemoteDatabase) = db.roomDao()
    @Provides fun provideSceneDao(db: SmartRemoteDatabase) = db.sceneDao()
    @Provides fun provideScheduleDao(db: SmartRemoteDatabase) = db.scheduleDao()
    @Provides fun provideEnergyDao(db: SmartRemoteDatabase) = db.energyDao()
    @Provides fun provideCommandHistoryDao(db: SmartRemoteDatabase) = db.commandHistoryDao()
    @Provides fun provideAiMessageDao(db: SmartRemoteDatabase) = db.aiMessageDao()

    // ─── Networking ────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_LOGGING)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson() = Gson()

    @Provides
    @Singleton
    @Named("main")
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("anthropic")
    fun provideAnthropicRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideApiService(@Named("main") retrofit: Retrofit): SmartRemoteApiService =
        retrofit.create(SmartRemoteApiService::class.java)

    @Provides
    @Singleton
    fun provideAnthropicApiService(@Named("anthropic") retrofit: Retrofit): AnthropicApiService =
        retrofit.create(AnthropicApiService::class.java)

    // ─── Firebase ──────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // ─── MQTT ──────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMqttManager(@ApplicationContext context: Context) = MqttManager(context)

    // ─── Context ──────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
