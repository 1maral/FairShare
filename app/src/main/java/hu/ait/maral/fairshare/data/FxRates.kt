package hu.ait.maral.fairshare.data


data class FxRates(
    val base: String = "EUR",
    val rates: Map<String, Double> = emptyMap()
)
