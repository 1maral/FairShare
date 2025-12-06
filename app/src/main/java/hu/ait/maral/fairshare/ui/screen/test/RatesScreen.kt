package hu.ait.maral.fairshare.ui.screen.test

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RatesScreen(
    viewModel: RatesViewModel = hiltViewModel()
) {
    androidx.compose.material3.Text("Testing rates... Check Logcat!")
}
