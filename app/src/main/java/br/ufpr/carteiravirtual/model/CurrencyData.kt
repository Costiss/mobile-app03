package br.ufpr.carteiravirtual.model

import com.google.gson.annotations.SerializedName

data class CurrencyData(
    @SerializedName("bid") val bid: String,
    @SerializedName("ask") val ask: String
)
