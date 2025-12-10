package hu.ait.maral.fairshare.ui.screen.rate

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.maral.fairshare.data.FxRates
import hu.ait.maral.fairshare.network.MoneyAPI
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RatesViewModel @Inject constructor(
    private val moneyAPI: MoneyAPI
) : ViewModel() {

    private val API_KEY = "2df4ba14f9260afddbedbb51de05d359"

    // 🔹 PUBLIC STATE THAT HOMESCREEN WILL READ
    var fxRates = mutableStateOf<FxRates?>(null)
        private set

    init {
        loadRates()
    }

    private fun loadRates() {
        viewModelScope.launch {
            try {
                val result = moneyAPI.getRates(API_KEY)
                val r = result.rates

                if (r == null) {
                    Log.e("RATES_TEST", "Rates were null")
                    return@launch
                }

                val map = mutableMapOf<String, Double>()

                r.uSD?.let { map["USD"] = it }
                map["EUR"] = 1.0
                r.gBP?.let { map["GBP"] = it }
                r.hUF?.let { map["HUF"] = it }
                r.jPY?.let { map["JPY"] = it }
                r.cAD?.let { map["CAD"] = it }
                r.aUD?.let { map["AUD"] = it }
                r.cHF?.let { map["CHF"] = it }
                r.iNR?.let { map["INR"] = it }
                r.cNY?.let { map["CNY"] = it }
                r.sEK?.let { map["SEK"] = it }
                r.nOK?.let { map["NOK"] = it }
                r.nZD?.let { map["NZD"] = it }
                r.mXN?.let { map["MXN"] = it }
                r.bRL?.let { map["BRL"] = it }

                fxRates.value = FxRates(
                    base = "EUR",
                    rates = map
                )

                Log.d("RATES_TEST", "FxRates loaded: ${fxRates.value}")

            } catch (e: Exception) {
                Log.e("RATES_TEST", "Failed: ${e.message}", e)
            }
        }
    }
}
