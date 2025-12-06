package hu.ait.maral.fairshare.ui.screen.test

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import hu.ait.maral.fairshare.network.MoneyAPI
import kotlinx.coroutines.launch


@HiltViewModel
class RatesViewModel @Inject constructor(
    private val moneyAPI: MoneyAPI
) : ViewModel() {

    // TEMPORARY hardcoded API key
    private val API_KEY = "969c37b5a73f8cb2d12c081dcbdc35e6"

    init {
        loadRates()
    }

    private fun loadRates() {
        viewModelScope.launch {
            try {
                val result = moneyAPI.getRates(API_KEY)

                // Print entire object
                Log.d("RATES_TEST", "FULL RESULT: $result")

                // Print base currency
                Log.d("RATES_TEST", "Base: ${result.base}")

                // Print supported currencies only
                val r = result.rates
                Log.d("RATES_TEST", "USD = ${r?.uSD}")
                Log.d("RATES_TEST", "EUR = ${r?.eUR}")
                Log.d("RATES_TEST", "GBP = ${r?.gBP}")
                Log.d("RATES_TEST", "HUF = ${r?.hUF}")
                Log.d("RATES_TEST", "JPY = ${r?.jPY}")
                Log.d("RATES_TEST", "CAD = ${r?.cAD}")
                Log.d("RATES_TEST", "AUD = ${r?.aUD}")
                Log.d("RATES_TEST", "CHF = ${r?.cHF}")
                Log.d("RATES_TEST", "INR = ${r?.iNR}")
                Log.d("RATES_TEST", "CNY = ${r?.cNY}")
                Log.d("RATES_TEST", "SEK = ${r?.sEK}")
                Log.d("RATES_TEST", "NOK = ${r?.nOK}")
                Log.d("RATES_TEST", "NZD = ${r?.nZD}")
                Log.d("RATES_TEST", "MXN = ${r?.mXN}")
                Log.d("RATES_TEST", "BRL = ${r?.bRL}")

            } catch (e: Exception) {
                Log.e("RATES_TEST", "Failed: ${e.message}", e)
            }
        }
    }
}
