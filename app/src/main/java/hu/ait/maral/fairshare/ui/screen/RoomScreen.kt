package hu.ait.maral.fairshare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.data.FxRates
import hu.ait.maral.fairshare.ui.screen.rate.RatesViewModel
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    groupId: String,
    viewModel: RoomViewModel = viewModel(),
    ratesViewModel: RatesViewModel = hiltViewModel(),
    onAddBillClick: () -> Unit
) {
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
        viewModel.loadUserPreferredCurrency()
    }

    val groupState = viewModel.group.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    val preferredCurrency = viewModel.preferredCurrency.value
    val fxRates = ratesViewModel.fxRates.value

    Scaffold(
        containerColor = BackgroundPink,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = groupState?.name ?: "Room",
                        color = ButtonGreen
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ButtonGreen.copy(alpha = 0.1f)
                )
            )

        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBillClick,
                containerColor = ButtonGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Bill",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ButtonGreen
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                groupState == null -> {
                    Text(
                        text = "No group data.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Members:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn {
                            itemsIndexed(groupState.members) { index, memberName ->

                                val memberId = groupState.memberIds.getOrNull(index)

                                // amounts in EUR stored in Firestore
                                val balanceEur = memberId?.let { id ->
                                    groupState.balances[id]
                                } ?: 0.0

                                // ✅ convert using same logic as HomeScreen
                                val balanceConverted = convertAmount(
                                    amountEur = balanceEur,
                                    userCurrency = preferredCurrency,
                                    fxRates = fxRates
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = memberName)
                                    Text(
                                        text = "${preferredCurrency} ${formatAmount(balanceConverted)}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Same helper as in HomeScreen
 */
private fun convertAmount(
    amountEur: Double,
    userCurrency: String,
    fxRates: FxRates?
): Double {
    if (fxRates == null) return amountEur
    val rate = fxRates.rates[userCurrency] ?: 1.0
    return amountEur * rate
}

private fun formatAmount(amount: Double): String {
    return String.format("%.2f", amount)
}
