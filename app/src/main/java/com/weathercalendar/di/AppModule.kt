package com.weathercalendar.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.weathercalendar.data.local.AppDatabase
import com.weathercalendar.data.local.EventDao
import com.weathercalendar.data.local.PoetryFavoriteDao
import com.weathercalendar.data.local.WeatherDao
import com.weathercalendar.data.remote.QWeatherApi
import com.weathercalendar.data.remote.WeatherApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)

        // 只在 debug 版本打印网络日志
        if (com.weathercalendar.BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @Named("weather")
    fun provideWeatherRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(WeatherApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApi(@Named("weather") retrofit: Retrofit): WeatherApi {
        return retrofit.create(WeatherApi::class.java)
    }

    @Provides
    @Singleton
    @Named("qweather")
    fun provideQWeatherRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(QWeatherApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideQWeatherApi(@Named("qweather") retrofit: Retrofit): QWeatherApi {
        return retrofit.create(QWeatherApi::class.java)
    }

    // ── Room ──

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "weather_calendar.db",
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherDao(db: AppDatabase): WeatherDao {
        return db.weatherDao()
    }

    @Provides
    @Singleton
    fun provideEventDao(db: AppDatabase): EventDao {
        return db.eventDao()
    }

    @Provides
    @Singleton
    fun providePoetryFavoriteDao(db: AppDatabase): PoetryFavoriteDao {
        return db.poetryFavoriteDao()
    }
}
