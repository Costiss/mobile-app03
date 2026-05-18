package br.ufpr.carteiravirtual.network

import br.ufpr.carteiravirtual.model.CurrencyData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface AwesomeApiService {
    @GET("last/{pair}")
    fun getRate(@Path("pair") pair: String): Call<Map<String, CurrencyData>>
}
