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
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import hu.ait.maral.fairshare.data.Bill
import hu.ait.maral.fairshare.ui.theme.LogoGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    groupId: String,
    viewModel: RoomViewModel = viewModel(),
    ratesViewModel: RatesViewModel = hiltViewModel(),
    onAddBillClick: () -> Unit
) {
    LaunchedEffect(groupId) {
        viewModel.observeGroup(groupId)
        viewModel.loadUserPreferredCurrency()
    }

    val groupState = viewModel.group.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    val preferredCurrency = viewModel.preferredCurrency.value
    val fxRates = ratesViewModel.fxRates.value

    val currentUserId = viewModel.currentUserId
    val owedPerPerson = viewModel.owedPerPerson.value

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
                groupState == null || currentUserId == null -> {
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
                            text = "What you owe to others in this group:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn {
                            itemsIndexed(groupState.members) { index, memberName ->

                                val memberId = groupState.memberIds.getOrNull(index)

                                // Skip self or null ids
                                if (memberId == null || memberId == currentUserId) {
                                    return@itemsIndexed
                                }

                                val owedEur = owedPerPerson[memberId] ?: 0.0

                                val owedConverted = convertAmount(
                                    amountEur = owedEur,
                                    userCurrency = preferredCurrency,
                                    fxRates = fxRates
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = memberName)

                                    if (owedEur > 0.0) {
                                        Text(
                                            text = "You owe $preferredCurrency ${
                                                formatAmount(
                                                    owedConverted
                                                )
                                            }"
                                        )
                                    } else {
                                        Text(
                                            text = "Settled",
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // ------------------------------
                        // BILL RECEIPTS SECTION
                        // ------------------------------
                        val bills: List<Bill>  = remember(groupId) { viewModel.getBills(groupId) }
                        val pagerState = rememberPagerState(
                            pageCount = { bills.size }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Bill Receipts",
                            style = MaterialTheme.typography.titleMedium,
                            color = LogoGreen,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        if (bills.isNotEmpty()) {

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                            ) { page ->

                                val bill: Bill = bills[page]

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = BackgroundPink.copy(alpha = 0.2f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {

                                        Text(
                                            text = bill.billTitle,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = ButtonGreen
                                        )

                                        Spacer(Modifier.height(4.dp))
                                        val authorName = remember(bill.authorId, groupState) {
                                            val idx = groupState.memberIds.indexOf(bill.authorId)
                                            if (idx != -1) groupState.members[idx] else "Unknown"
                                        }

                                        Text(
                                            text = "by $authorName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        if (bill.imgUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = bill.imgUrl,
                                                contentDescription = "bill image",
                                                modifier = Modifier
                                                    .size(90.dp)
                                                    .align(Alignment.Start)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(Modifier.height(12.dp))
                                        }

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            bill.billItems.forEach { item ->

                                                // 1. Find which user this item is assigned to
                                                val assignedUserId = bill.itemAssignments[item.itemId]

                                                // 2. Convert userId → name
                                                val assignedUserName = remember(item.itemId, bill.itemAssignments, groupState) {
                                                    val assignedUserId = bill.itemAssignments[item.itemId]
                                                    if (assignedUserId != null) {
                                                        val idx = groupState.memberIds.indexOf(assignedUserId)
                                                        if (idx != -1) groupState.members[idx] else "Unassigned"
                                                    } else "Unassigned"
                                                }


                                                // 3. Display
                                                Text(
                                                    text = "${item.itemName} — $assignedUserName: ${item.itemPrice} $preferredCurrency",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ButtonGreen
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(14.dp))

                                        Text(
                                            text = "Total: ${bill.billItems.sumOf { it.itemPrice }} $preferredCurrency",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            ),
                                            color = ButtonGreen
                                        )
                                    }
                                }
                            }

                        } else {
                            Text(
                                text = "No bills yet.",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LogoGreen
                            )
                        }


                    }
                }
            }
        }
    }
}

/**
 * Convert amount from EUR to the user's preferred currency using FxRates.
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

/**
 * Format amounts nicely (2 decimal places).
 */
private fun formatAmount(amount: Double): String {
    return String.format("%.2f", amount)
}
