package hu.ait.maral.fairshare.network

import hu.ait.maral.fairshare.data.MoneyResult
import retrofit2.http.GET
import retrofit2.http.Query

interface MoneyAPI {

    @GET("api/latest")
    suspend fun getRates(@Query("access_key") accessKey: String): MoneyResult
}