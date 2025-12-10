package hu.ait.maral.fairshare.ui.screen.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
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
import hu.ait.maral.fairshare.ui.theme.CardPink
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

    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    var selectedPersonName by remember { mutableStateOf<String?>(null) }

    var selectedAmountOption by remember { mutableStateOf("owed") }
    var customAmount by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BackgroundPink,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = groupState?.name ?: "Room",
                        color = LogoGreen
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ButtonGreen
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // ---------------- GROUP BALANCES ----------------
                        item {
                            Text(
                                text = "Group Balances",
                                style = MaterialTheme.typography.titleMedium,
                                color = LogoGreen,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        item {
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

                                    var avatarUrl by remember(memberId) {
                                        mutableStateOf<String?>(
                                            null
                                        )
                                    }

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
                                        statusColor = LogoGreen
                                    } else {
                                        statusText = "Settled"
                                        statusColor = Color.Gray
                                    }

                                    Box(
                                        modifier = Modifier.clickable {
                                            selectedPersonId = memberId
                                            selectedPersonName = memberName
                                            showPaymentDialog = true
                                        }
                                    ) {
                                        MemberBalanceCard(
                                            name = memberName,
                                            statusText = statusText,
                                            statusColor = statusColor,
                                            avatarUrl = avatarUrl
                                        )
                                    }
                                }
                            }
                        }

                        // ---------------- BILL RECEIPTS ----------------
                        item {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Bill Receipts",
                                style = MaterialTheme.typography.titleMedium,
                                color = LogoGreen,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        item {
                            // Pager for bills
                            val pagerState = rememberPagerState(pageCount = { bills.size })

                            if (bills.isNotEmpty()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(360.dp) // slightly taller to fit image + items comfortably
                                ) { page ->
                                    val bill = bills[page]

                                    // Call our custom BillCard composable
                                    BillCard(
                                        bill = bill,
                                        groupState = groupState,
                                        preferredCurrency = preferredCurrency,
                                        fxRates = fxRates,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)
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
                    if (showPaymentDialog && selectedPersonId != null) {

                        val owedEur = owedPerPerson[selectedPersonId] ?: 0.0
                        val convertedOwed = convertAmount(owedEur, preferredCurrency, fxRates)

                        AlertDialog(
                            onDismissRequest = { showPaymentDialog = false },
                            title = { Text("Settle with $selectedPersonName", color = LogoGreen) },
                            text = {
                                Column {

                                    // ----- Amount Selection -----
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedAmountOption == "owed",
                                            onClick = {
                                                selectedAmountOption = "owed"
                                                customAmount = formatAmount(convertedOwed)
                                            }
                                        )
                                        Text("Owed: $preferredCurrency ${formatAmount(convertedOwed)}", color = LogoGreen)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedAmountOption == "custom",
                                            onClick = { selectedAmountOption = "custom" }
                                        )
                                        OutlinedTextField(
                                            value = customAmount,
                                            onValueChange = {
                                                if (it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                                    customAmount = it
                                                }
                                            },
                                            label = { Text("Custom amount", color = LogoGreen) },
                                            singleLine = true,
                                            modifier = Modifier.width(160.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // ---- Payment Method ----
                                    Text("Select Payment Method:", color = LogoGreen)

                                    val methods = listOf("Cash", "Venmo", "Zelle", "CashApp", "PayPal", "Bank Transfer")

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(methods.size) { i ->
                                            val m = methods[i]
                                            Card(
                                                colors = if (selectedPaymentMethod == m)
                                                    CardDefaults.cardColors(containerColor = ButtonGreen)
                                                else CardDefaults.cardColors(containerColor = Color.LightGray),
                                                modifier = Modifier
                                                    .height(50.dp)
                                                    .width(100.dp)
                                                    .clickable { selectedPaymentMethod = m }
                                            ) {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(m, color = LogoGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            },

                            confirmButton = {
                                Button(
                                    onClick = {
                                        val amountToPay = when (selectedAmountOption) {
                                            "owed" -> convertedOwed
                                            "custom" -> customAmount.toDoubleOrNull() ?: 0.0
                                            else -> 0.0
                                        }

                                        viewModel.settleDebtWithMember(
                                            groupId = groupId,
                                            creditorId = selectedPersonId!!,
                                            amountPaid = amountToPay / (fxRates?.rates?.get(preferredCurrency) ?: 1.0),
                                            // converted back to EUR
                                            onSuccess = { showPaymentDialog = false },
                                            onError = { viewModel.errorMessage.value = it }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LogoGreen)
                                ) {
                                    Text("Confirm Payment")
                                }
                            },

                            dismissButton = {
                                TextButton(onClick = { showPaymentDialog = false }) {
                                    Text("Cancel", color = Color.Black)
                                }
                            }
                        )
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
    fxRates: FxRates?,
    modifier: Modifier = Modifier
)
 {


    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardPink.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = bill.billTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = LogoGreen
            )

            Spacer(Modifier.height(4.dp))

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

            Spacer(Modifier.height(4.dp))

            // -------------------
            // IMAGE SECTION
            // -------------------
            var showFullImage by remember { mutableStateOf(false) }

            if (bill.imgUrl.isNotEmpty()) {
                AsyncImage(
                    model = bill.imgUrl,
                    contentDescription = "bill image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullImage = true },
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
                // Fullscreen image dialog
                if (showFullImage) {
                    Dialog(onDismissRequest = { showFullImage = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CardPink.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = bill.imgUrl,
                                contentDescription = "full bill image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable { showFullImage = false },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }


            // -------------------
            // ITEMS LIST
            // -------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.itemName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CardPink
                        )
                        Text(
                            text = "$assignedUserName: ${convertAmount(item.itemPrice, preferredCurrency,fxRates)} $preferredCurrency",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(4.dp))

            // -------------------
            // TOTAL ROW
            // -------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color.Red
                )
                Text(
                    text = "${convertAmount((bill.billItems.sumOf { it.itemPrice }), preferredCurrency, fxRates)} $preferredCurrency",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = LogoGreen
                )
                Spacer(Modifier.height(4.dp))
            }
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
        colors = cardColors(containerColor = CardPink.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    color = LogoGreen.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.firstOrNull()?.uppercase() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = LogoGreen
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
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 8.dp)
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
    return (amountEur * rate).round2()
}

fun Double.round2(): Double = kotlin.math.round(this * 100) / 100

private fun formatAmount(amount: Double): String {
    return String.format("%.2f", amount)
}