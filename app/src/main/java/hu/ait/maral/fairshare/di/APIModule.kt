package hu.ait.maral.fairshare.di


import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.ait.maral.fairshare.network.MoneyAPI
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MoneyExchangeAPIHost



@Module
@InstallIn(SingletonComponent::class)
object APIModule {

    @Provides
    @MoneyExchangeAPIHost
    @Singleton
    fun provideMoneyAPIRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .build()

        return Retrofit.Builder()
            .baseUrl("https://data.fixer.io/")
            .addConverterFactory(
                Json{ ignoreUnknownKeys = true }.asConverterFactory(
                    "application/json".toMediaType()) )
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoneyAPI(@MoneyExchangeAPIHost retrofit: Retrofit): MoneyAPI {
        return retrofit.create(MoneyAPI::class.java)
    }


}