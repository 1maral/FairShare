package hu.ait.maral.fairshare.ui.screen.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.data.Bill
import hu.ait.maral.fairshare.data.FxRates
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.ui.screen.RoomViewModel
import hu.ait.maral.fairshare.ui.screen.rate.RatesViewModel
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
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
        viewModel.startBillsListener(groupId)
        viewModel.loadUserPreferredCurrency()
    }

    val groupState = viewModel.group.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    val preferredCurrency = viewModel.preferredCurrency.value
    val fxRates = ratesViewModel.fxRates.value

    val currentUserId = viewModel.currentUserId
    val owedPerPerson = viewModel.owedPerPerson.value
    val bills = viewModel.bills.value

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

                groupState == null || currentUserId.isBlank() -> {
                    Text(
                        text = "No group data.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // ---------------- GROUP BALANCES ----------------
                        Text(
                            text = "Group balances",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(groupState.members) { index, memberName ->
                                val memberId = groupState.memberIds.getOrNull(index)

                                // Skip if no memberId or if it's the current user
                                if (memberId == null || memberId == currentUserId) {
                                    return@itemsIndexed
                                }

                                val userBalance = groupState.balances[currentUserId] ?: 0.0
                                val otherBalance = groupState.balances[memberId] ?: 0.0
                                val owedEur = owedPerPerson[memberId] ?: 0.0

                                var avatarUrl by remember(memberId) { mutableStateOf<String?>(null) }

                                LaunchedEffect(memberId) {
                                    viewModel.fetchUserAvatar(memberId) { url ->
                                        avatarUrl = url
                                    }
                                }

                                val statusText: String
                                val statusColor: Color

                                if (owedEur > 0.0) {
                                    val converted =
                                        convertAmount(owedEur, preferredCurrency, fxRates)
                                    statusText =
                                        "You owe: $preferredCurrency ${formatAmount(converted)}"
                                    statusColor = Color.Red
                                } else if (userBalance > 0 && otherBalance < 0) {
                                    val theyOweEur = minOf(userBalance, -otherBalance)
                                    val converted =
                                        convertAmount(theyOweEur, preferredCurrency, fxRates)
                                    statusText =
                                        "They owe: $preferredCurrency ${formatAmount(converted)}"
                                    statusColor = ButtonGreen
                                } else {
                                    statusText = "Settled"
                                    statusColor = Color.Gray
                                }

                                MemberBalanceCard(
                                    name = memberName,
                                    statusText = statusText,
                                    statusColor = statusColor,
                                    avatarUrl = avatarUrl
                                )
                            }
                        }

                        // ---------------- BILL RECEIPTS ----------------
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Bill Receipts",
                            style = MaterialTheme.typography.titleMedium,
                            color = LogoGreen,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        val pagerState = rememberPagerState(pageCount = { bills.size })

                        if (bills.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                            ) { page ->
                                val bill = bills[page]

                                BillCard(
                                    bill = bill,
                                    groupState = groupState,
                                    preferredCurrency = preferredCurrency,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text(
                                text = "No bills yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LogoGreen,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillCard(
    bill: Bill,
    groupState: Group,
    preferredCurrency: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = BackgroundPink.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Title
            Text(
                text = bill.billTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ButtonGreen
            )

            Spacer(Modifier.height(4.dp))

            // Author name
            val authorName = remember(
                bill.authorId,
                groupState.memberIds,
                groupState.members
            ) {
                val idx = groupState.memberIds.indexOf(bill.authorId)
                if (idx in groupState.members.indices) groupState.members[idx] else "Unknown"
            }

            Text(
                text = "by $authorName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))

            // Receipt image if present
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

            // Items + who they're assigned to
            Column(modifier = Modifier.fillMaxWidth()) {
                bill.billItems.forEach { item ->
                    val assignedUserId = bill.itemAssignments[item.itemId]
                    val assignedUserName = if (assignedUserId != null) {
                        val idx = groupState.memberIds.indexOf(assignedUserId)
                        if (idx in groupState.members.indices)
                            groupState.members[idx]
                        else
                            "Unassigned"
                    } else {
                        "Unassigned"
                    }

                    Text(
                        text = "${item.itemName} — $assignedUserName: ${item.itemPrice} $preferredCurrency",
                        style = MaterialTheme.typography.bodySmall,
                        color = ButtonGreen
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Total
            val total = bill.billItems.sumOf { it.itemPrice }

            Text(
                text = "Total: $total $preferredCurrency",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = ButtonGreen
            )
        }
    }
}

@Composable
private fun MemberBalanceCard(
    name: String,
    statusText: String,
    statusColor: Color,
    avatarUrl: String?
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp),
        colors = cardColors(containerColor = Color(0xFFFFF0F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "$name profile picture",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = ButtonGreen.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.firstOrNull()?.uppercase() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = ButtonGreen
                        )
                    }
                }
            }

            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

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
