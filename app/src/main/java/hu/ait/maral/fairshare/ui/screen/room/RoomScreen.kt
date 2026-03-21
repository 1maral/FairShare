package hu.ait.maral.fairshare.ui.screen.room

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.data.Bill
import hu.ait.maral.fairshare.data.FxRates
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.ui.screen.RoomViewModel
import hu.ait.maral.fairshare.ui.screen.rate.RatesViewModel
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Palette ───────────────────────────────────────────────────────────────────
private val Rose300    = Color(0xFFF48FB1)
private val Rose500    = Color(0xFFE76F8E)
private val Mint300    = Color(0xFFA8D8B0)
private val OffWhite   = Color(0xFFFFFAFC)
private val Stone      = Color(0xFF9E8E95)
private val FieldFocus = Color(0xFFF9E4EC)
private val FieldBg    = Color(0xFFFFF8FA)

private val dateFormatter     = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private val dateTimeFormatter = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    groupId: String,
    viewModel: RoomViewModel = viewModel(),
    ratesViewModel: RatesViewModel = hiltViewModel(),
    onAddBillClick: () -> Unit,
    onBack: () -> Unit = {}
) {
    LaunchedEffect(groupId) {
        viewModel.observeGroup(groupId)
        viewModel.startBillsListener(groupId)
        viewModel.loadUserPreferredCurrency()
    }

    val groupState        = viewModel.group.value
    val isLoading         = viewModel.isLoading.value
    val errorMessage      = viewModel.errorMessage.value
    val preferredCurrency = viewModel.preferredCurrency.value
    val fxRates           = ratesViewModel.fxRates.value
    val currentUserId     = viewModel.currentUserId
    val owedPerPerson     = viewModel.owedPerPerson.value
    val bills             = viewModel.bills.value

    // ── Archived check ────────────────────────────────────────────────────────
    // The user is "archived" (has left) when their uid is no longer in the
    // group's memberIds list. In that state we hide Add Bill and Leave Buttons.
    val isArchived = groupState != null && !groupState.memberIds.contains(currentUserId)

    // ── Settle dialog state ───────────────────────────────────────────────────
    var showPaymentDialog     by remember { mutableStateOf(false) }
    var selectedPersonId      by remember { mutableStateOf<String?>(null) }
    var selectedPersonName    by remember { mutableStateOf<String?>(null) }
    var selectedAmountOption  by remember { mutableStateOf("owed") }
    var customAmount          by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }

    // ── Leave dialog state ────────────────────────────────────────────────────
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showLeaveBlockedDialog by remember { mutableStateOf(false) }
    var leaveBlockedReason     by remember { mutableStateOf("") }

    // ── Summary stats ─────────────────────────────────────────────────────────
    val totalOwed = owedPerPerson.entries
        .filter { (memberId, amountEur) -> memberId != currentUserId && amountEur > 0.0 }
        .sumOf { (_, amountEur) -> convertAmount(amountEur, preferredCurrency, fxRates) }

    val totalOwedToYou = run {
        val userBal = groupState?.balances?.get(currentUserId) ?: 0.0
        if (userBal <= 0.0) 0.0
        else groupState?.memberIds
            ?.filter { it != currentUserId }
            ?.sumOf { memberId ->
                val otherBal = groupState.balances[memberId] ?: 0.0
                if (otherBal < 0.0) convertAmount(minOf(userBal, -otherBal), preferredCurrency, fxRates)
                else 0.0
            } ?: 0.0
    }

    val settledCount = groupState?.memberIds
        ?.filter { it != currentUserId }
        ?.count { memberId ->
            val owedEur  = owedPerPerson[memberId] ?: 0.0
            val userBal  = groupState.balances[currentUserId] ?: 0.0
            val otherBal = groupState.balances[memberId] ?: 0.0
            owedEur <= 0.0 && !(userBal > 0.0 && otherBal < 0.0)
        } ?: 0

    fun onLeavePressed() {
        viewModel.leaveGroup(
            groupId   = groupId,
            onSuccess = { onBack() },
            onError   = { reason ->
                leaveBlockedReason     = reason
                showLeaveBlockedDialog = true
            }
        )
    }

    fun tryLeave() { showLeaveConfirmDialog = true }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            // Hide the Add Bill FAB entirely when the room is archived
            if (!isArchived) {
                ExtendedFloatingActionButton(
                    onClick        = onAddBillClick,
                    containerColor = Rose500,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(18.dp),
                    modifier       = Modifier.shadow(
                        elevation = 10.dp, shape = RoundedCornerShape(18.dp),
                        spotColor = Rose500.copy(alpha = 0.4f)
                    ),
                    icon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) },
                    text = {
                        Text(
                            "Bill", fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, letterSpacing = 0.3.sp
                        )
                    }
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFEDF7F0), Color(0xFFF5FBF7), Color(0xFFFFFAFC))
                    )
                )
        ) {
            // Ambient background orbs
            Box(
                Modifier.size(300.dp).offset((-120).dp, (-120).dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0x20A8D8B0), Color.Transparent)))
            )
            Box(
                Modifier.size(220.dp).align(Alignment.BottomEnd).offset(80.dp, 80.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0x18E76F8E), Color.Transparent)))
            )

            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = LogoGreen, strokeWidth = 3.dp
                )
                errorMessage != null -> Text(
                    errorMessage, color = Rose500,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    textAlign = TextAlign.Center
                )
                groupState == null || currentUserId.isBlank() -> Text(
                    stringResource(R.string.no_group_data),
                    color = Stone, modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 108.dp, start = 20.dp, end = 20.dp, bottom = 110.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {

                        // Archived banner — shown at the top when the user has left
                        if (isArchived) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Stone.copy(alpha = 0.12f))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "You have left this room. It's now read-only.",
                                        fontSize = 12.sp,
                                        color = Stone,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Room created date
                        item {
                            groupState.createdAt?.let { ts ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(6.dp).clip(CircleShape)
                                            .background(Rose300.copy(alpha = 0.5f))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Room created ${dateFormatter.format(ts)}",
                                        fontSize = 11.sp,
                                        color = Stone.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Summary cards
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BigSummaryCard(
                                    label = "You Owe", amount = totalOwed,
                                    currency = preferredCurrency, isEmpty = totalOwed <= 0.0,
                                    emptyLabel = "Yourself a treat!", isPink = true,
                                    modifier = Modifier.weight(1f)
                                )
                                BigSummaryCard(
                                    label = "Owed to You", amount = totalOwedToYou,
                                    currency = preferredCurrency, isEmpty = totalOwedToYou <= 0.0,
                                    emptyLabel = "A spiritual hug!", isPink = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Members
                        item { SectionHeader("Members", "${groupState.members.size} people") }

                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(groupState.members) { index, memberName ->
                                    val memberId = groupState.memberIds.getOrNull(index)
                                    if (memberId == null || memberId == currentUserId) return@itemsIndexed

                                    val userBalance  = groupState.balances[currentUserId] ?: 0.0
                                    val otherBalance = groupState.balances[memberId] ?: 0.0
                                    val owedEur      = owedPerPerson[memberId] ?: 0.0

                                    var avatarUrl by remember(memberId) { mutableStateOf<String?>(null) }
                                    LaunchedEffect(memberId) {
                                        viewModel.fetchUserAvatar(memberId) { avatarUrl = it }
                                    }

                                    val (statusText, isOwing) = when {
                                        owedEur > 0.0 -> {
                                            val c = convertAmount(owedEur, preferredCurrency, fxRates)
                                            "You owe\n${formatAmount(c)} $preferredCurrency" to true
                                        }
                                        userBalance > 0 && otherBalance < 0 -> {
                                            val c = convertAmount(
                                                minOf(userBalance, -otherBalance),
                                                preferredCurrency, fxRates
                                            )
                                            "Owes you\n${formatAmount(c)} $preferredCurrency" to false
                                        }
                                        else -> "Settled ✓" to null
                                    }

                                    MemberBalanceCard(
                                        name = memberName, statusText = statusText,
                                        isOwing = isOwing, avatarUrl = avatarUrl,
                                        onClick = {
                                            selectedPersonId     = memberId
                                            selectedPersonName   = memberName
                                            selectedAmountOption = "owed"
                                            customAmount = formatAmount(
                                                convertAmount(owedEur, preferredCurrency, fxRates)
                                            )
                                            showPaymentDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        // Bills
                        item {
                            SectionHeader(
                                "Bill Receipts",
                                if (bills.isEmpty()) "none yet" else "${bills.size} bills"
                            )
                        }

                        item {
                            if (bills.isEmpty()) {
                                EmptyBillsCard()
                            } else {
                                val pagerState = rememberPagerState(pageCount = { bills.size })
                                Column {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { page ->
                                        BillCard(
                                            bill = bills[page], groupState = groupState,
                                            preferredCurrency = preferredCurrency, fxRates = fxRates,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (bills.size > 1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            repeat(bills.size) { i ->
                                                val selected = pagerState.currentPage == i
                                                val dotColor by animateColorAsState(
                                                    if (selected) Rose500 else Rose300.copy(alpha = 0.35f),
                                                    tween(200), label = "dot"
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 3.dp)
                                                        .size(if (selected) 8.dp else 5.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Settle payment dialog ─────────────────────────────────
                    if (showPaymentDialog && selectedPersonId != null) {
                        val owedEur       = owedPerPerson[selectedPersonId] ?: 0.0
                        val convertedOwed = convertAmount(owedEur, preferredCurrency, fxRates)

                        AlertDialog(
                            onDismissRequest = { showPaymentDialog = false },
                            shape            = RoundedCornerShape(28.dp),
                            containerColor   = OffWhite,
                            title = {
                                Column {
                                    Box(
                                        Modifier.fillMaxWidth().height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Rose300, Rose500, Mint300)
                                                )
                                            )
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        "Settle with $selectedPersonName",
                                        fontWeight = FontWeight.Black,
                                        color = Rose500, fontSize = 17.sp
                                    )
                                    Text(
                                        "Choose how much and how you'll pay",
                                        fontSize = 12.sp, color = Stone,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Amount", fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Stone, letterSpacing = 0.5.sp
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (selectedAmountOption == "owed") FieldFocus else FieldBg
                                            )
                                            .border(
                                                1.dp,
                                                if (selectedAmountOption == "owed") Rose300
                                                else Color.Transparent,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                selectedAmountOption = "owed"
                                                customAmount = formatAmount(convertedOwed)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedAmountOption == "owed",
                                            onClick  = {
                                                selectedAmountOption = "owed"
                                                customAmount = formatAmount(convertedOwed)
                                            },
                                            colors   = RadioButtonDefaults.colors(selectedColor = Rose500),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                "Full amount owed", fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold, color = Rose500
                                            )
                                            Text(
                                                "${formatAmount(convertedOwed)} $preferredCurrency",
                                                fontSize = 12.sp, color = Stone
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (selectedAmountOption == "custom") FieldFocus else FieldBg
                                            )
                                            .border(
                                                1.dp,
                                                if (selectedAmountOption == "custom") Rose300
                                                else Color.Transparent,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedAmountOption = "custom" }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selectedAmountOption == "custom",
                                                onClick  = { selectedAmountOption = "custom" },
                                                colors   = RadioButtonDefaults.colors(selectedColor = Rose500),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "Custom amount", fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold, color = Stone
                                            )
                                        }
                                        if (selectedAmountOption == "custom") {
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value         = customAmount,
                                                onValueChange = {
                                                    if (it.matches(Regex("^\\d*(\\.\\d{0,2})?\$")))
                                                        customAmount = it
                                                },
                                                label      = { Text("Amount in $preferredCurrency") },
                                                singleLine = true,
                                                shape      = RoundedCornerShape(12.dp),
                                                colors     = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor      = Rose500,
                                                    unfocusedBorderColor    = Rose300.copy(alpha = 0.5f),
                                                    focusedLabelColor       = Rose500,
                                                    cursorColor             = Rose500,
                                                    focusedContainerColor   = OffWhite,
                                                    unfocusedContainerColor = OffWhite
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Text(
                                        "Pay via", fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Stone, letterSpacing = 0.5.sp
                                    )
                                    val methods = listOf(
                                        "Cash", "Venmo", "Zelle",
                                        "Cash App", "PayPal", "Bank Transfer"
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        methods.chunked(3).forEach { rowMethods ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                rowMethods.forEach { m ->
                                                    val isSelected = selectedPaymentMethod == m
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                if (isSelected) LogoGreen else FieldBg
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isSelected) LogoGreen
                                                                else Rose300.copy(alpha = 0.3f),
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable { selectedPaymentMethod = m }
                                                            .padding(vertical = 9.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            m, fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold
                                                            else FontWeight.Normal,
                                                            color      = if (isSelected) Color.White else Stone,
                                                            textAlign  = TextAlign.Center
                                                        )
                                                    }
                                                }
                                                repeat(3 - rowMethods.size) {
                                                    Spacer(Modifier.weight(1f))
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
                                            "owed"   -> convertedOwed
                                            "custom" -> customAmount.toDoubleOrNull() ?: 0.0
                                            else     -> 0.0
                                        }
                                        viewModel.settleDebtWithMember(
                                            groupId    = groupId,
                                            creditorId = selectedPersonId!!,
                                            amountPaid = amountToPay /
                                                    (fxRates?.rates?.get(preferredCurrency) ?: 1.0),
                                            onSuccess  = { showPaymentDialog = false },
                                            onError    = { viewModel.errorMessage.value = it }
                                        )
                                    },
                                    shape    = RoundedCornerShape(14.dp),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = LogoGreen, contentColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Confirm Payment",
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPaymentDialog = false }) {
                                    Text("Cancel", color = Stone)
                                }
                            }
                        )
                    }

                    // ── Leave: confirm dialog ─────────────────────────────────
                    if (showLeaveConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showLeaveConfirmDialog = false },
                            shape            = RoundedCornerShape(24.dp),
                            containerColor   = OffWhite,
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp).clip(CircleShape)
                                        .background(Mint300.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ExitToApp, null,
                                        tint = LogoGreen, modifier = Modifier.size(26.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    "Leave \"${groupState?.name}\"?",
                                    fontWeight = FontWeight.Black, color = LogoGreen,
                                    fontSize = 17.sp, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            text = {
                                Text(
                                    "This will remove you from the group. You can only leave if all balances are settled.",
                                    fontSize = 13.sp, color = Stone,
                                    textAlign = TextAlign.Center, lineHeight = 20.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showLeaveConfirmDialog = false
                                        onLeavePressed()
                                    },
                                    shape  = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LogoGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Yes, leave", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                                    Text("Stay", color = Stone, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }

                    // ── Leave: blocked dialog ─────────────────────────────────
                    if (showLeaveBlockedDialog) {
                        AlertDialog(
                            onDismissRequest = { showLeaveBlockedDialog = false },
                            shape            = RoundedCornerShape(24.dp),
                            containerColor   = OffWhite,
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp).clip(CircleShape)
                                        .background(FieldFocus),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Warning, null,
                                        tint = Rose500, modifier = Modifier.size(26.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    "Can't Leave Yet",
                                    fontWeight = FontWeight.Black, color = Rose500,
                                    fontSize = 17.sp, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            text = {
                                Text(
                                    leaveBlockedReason,
                                    fontSize = 13.sp, color = Stone,
                                    textAlign = TextAlign.Center, lineHeight = 20.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick  = { showLeaveBlockedDialog = false },
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Rose500),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Got it", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }

            // ── Floating top bar ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .shadow(
                        elevation    = 8.dp,
                        shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                        ambientColor = Rose300.copy(alpha = 0.3f),
                        spotColor    = Rose500.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.horizontalGradient(listOf(Rose500, Color(0xFFF06292))))
            ) {
                Column {
                    Spacer(Modifier.fillMaxWidth().statusBarsPadding())
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.ArrowBack, null,
                                tint = Color.White, modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text          = groupState?.name ?: stringResource(R.string.room),
                                fontSize      = 20.sp,
                                fontWeight    = FontWeight.Black,
                                color         = Color.White,
                                letterSpacing = (-0.5).sp,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis
                            )
                            if (groupState != null) {
                                Text(
                                    "${groupState.members.size} members · ${bills.size} bills · $settledCount settled",
                                    fontSize = 11.sp,
                                    color    = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Leave button — hidden once the user has left (isArchived)
                        if (!isArchived) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .clickable { tryLeave() }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.ExitToApp,
                                        contentDescription = "Leave group",
                                        tint               = Color.White,
                                        modifier           = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Leave",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

// ── Big summary card ──────────────────────────────────────────────────────────
@Composable
private fun BigSummaryCard(
    label      : String,
    amount     : Double,
    currency   : String,
    isEmpty    : Boolean,
    emptyLabel : String,
    isPink     : Boolean,
    modifier   : Modifier = Modifier
) {
    val gradientColors = if (isPink)
        listOf(Color(0xFFFF8FAD), Rose500)
    else
        listOf(Color(0xFF5DB88A), LogoGreen)

    val formattedAmount = if (isEmpty) emptyLabel else formatAmountShort(amount)
    val needsSmallFont  = !isEmpty && amount >= 10_000

    Card(
        modifier  = modifier.aspectRatio(0.9f),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        ) {
            Box(
                Modifier.size(120.dp).align(Alignment.TopEnd).offset(30.dp, (-30).dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                Modifier.size(70.dp).align(Alignment.BottomStart).offset((-20).dp, 20.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.06f))
            )
            Column(
                modifier            = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.2.sp
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text          = formattedAmount,
                        fontSize      = if (isEmpty) 18.sp else if (needsSmallFont) 28.sp else 36.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color.White,
                        letterSpacing = (-1).sp,
                        maxLines      = 1,
                        lineHeight    = 38.sp
                    )
                    if (!isEmpty) {
                        Text(
                            currency, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

private fun formatAmountShort(amount: Double): String = when {
    amount >= 1_000_000 -> String.format("%.2fM", amount / 1_000_000)
    amount >= 10_000    -> String.format("%.1fK", amount / 1_000)
    amount >= 1_000     -> String.format("%.2fK", amount / 1_000)
    else                -> String.format("%.2f", amount)
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, subtitle: String = "") {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Rose500, Mint300)))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = Rose500, letterSpacing = 0.4.sp, modifier = Modifier.weight(1f)
        )
        if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Stone)
    }
}

// ── Empty bills card ──────────────────────────────────────────────────────────
@Composable
private fun EmptyBillsCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(FieldFocus, OffWhite)))
                    .border(1.5.dp, Rose300.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, null, tint = Rose300, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("No bills yet", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Rose500)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap \"Add Bill\" to split your first expense",
                fontSize = 12.sp, color = Stone, textAlign = TextAlign.Center
            )
        }
    }
}

// ── Member balance card ───────────────────────────────────────────────────────
@Composable
private fun MemberBalanceCard(
    name      : String,
    statusText: String,
    isOwing   : Boolean?,
    avatarUrl : String?,
    onClick   : () -> Unit
) {
    val chipBg    = when (isOwing) { true -> FieldFocus; false -> Mint300.copy(0.22f); null -> FieldBg }
    val chipColor = when (isOwing) { true -> Rose500;    false -> LogoGreen;           null -> Stone   }
    val ringColor = when (isOwing) { true -> Rose300;    false -> Mint300;             null -> Color(0xFFDDDDDD) }

    Card(
        modifier  = Modifier.width(140.dp).clickable { onClick() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300)))
            )
            Column(
                modifier            = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFFFCE4EC), OffWhite)))
                        .border(2.dp, ringColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl, contentDescription = null,
                            modifier     = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            name.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 19.sp, fontWeight = FontWeight.Black, color = Rose500
                        )
                    }
                }
                Text(
                    name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = Stone, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(chipBg)
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        statusText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = chipColor, textAlign = TextAlign.Center, lineHeight = 14.sp
                    )
                }
                if (isOwing != null) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Tap to settle", fontSize = 9.sp, color = Stone.copy(alpha = 0.7f))
                        Icon(
                            Icons.Default.KeyboardArrowRight, null,
                            tint = Stone.copy(alpha = 0.7f), modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Bill card ─────────────────────────────────────────────────────────────────
@Composable
fun BillCard(
    bill             : Bill,
    groupState       : Group,
    preferredCurrency: String,
    fxRates          : FxRates?,
    modifier         : Modifier = Modifier
) {
    Card(
        shape     = RoundedCornerShape(24.dp),
        modifier  = modifier.padding(vertical = 4.dp),
        colors    = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300)))
            )
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {

                val authorName = remember(bill.authorId, groupState.memberIds, groupState.members) {
                    val idx = groupState.memberIds.indexOf(bill.authorId)
                    if (idx in groupState.members.indices) groupState.members[idx] else "Unknown"
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            bill.billTitle,
                            fontWeight = FontWeight.Black, fontSize = 20.sp,
                            color = LogoGreen, letterSpacing = (-0.3).sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(top = 3.dp)
                        ) {
                            Text("by $authorName", fontSize = 12.sp, color = Stone)
                            bill.billDate?.let { ts ->
                                Text(
                                    "  ·  ${dateTimeFormatter.format(ts)}",
                                    fontSize = 11.sp, color = Stone.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Mint300.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            convertAndFormatAmount(
                                bill.billItems.sumOf { it.itemPrice }, preferredCurrency, fxRates
                            ),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LogoGreen
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                var showFullImage by remember { mutableStateOf(false) }
                if (bill.imgUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(70.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showFullImage = true }
                    ) {
                        AsyncImage(
                            model            = bill.imgUrl,
                            contentDescription = null,
                            modifier         = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale     = ContentScale.Crop
                        )
                        Box(
                            Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x88000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("view receipt", color = Color.White, fontSize = 9.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    if (showFullImage) {
                        Dialog(onDismissRequest = { showFullImage = false }) {
                            Box(
                                Modifier.fillMaxSize().background(Color(0xCC000000))
                                    .clickable { showFullImage = false },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model              = bill.imgUrl,
                                    contentDescription = null,
                                    modifier           = Modifier.fillMaxWidth().padding(24.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale       = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(Rose300.copy(alpha = 0.2f)))
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FieldBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bill.billItems.forEach { item ->
                        val assignedUserId   = bill.itemAssignments[item.itemId]
                        val assignedUserName = assignedUserId?.let { uid ->
                            val idx = groupState.memberIds.indexOf(uid)
                            if (idx in groupState.members.indices) groupState.members[idx]
                            else "Unassigned"
                        } ?: "Unassigned"

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.weight(1f)
                            ) {
                                Box(
                                    Modifier.size(7.dp).clip(CircleShape)
                                        .background(Rose300.copy(alpha = 0.7f))
                                )
                                Spacer(Modifier.width(7.dp))
                                Column {
                                    Text(
                                        item.itemName, fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium, color = Rose500,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Text("→ $assignedUserName", fontSize = 10.sp, color = Stone)
                                }
                            }
                            Text(
                                convertAndFormatAmount(item.itemPrice, preferredCurrency, fxRates),
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LogoGreen
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Mint300.copy(alpha = 0.18f))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Total", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LogoGreen)
                    Text(
                        convertAndFormatAmount(
                            bill.billItems.sumOf { it.itemPrice }, preferredCurrency, fxRates
                        ),
                        fontWeight = FontWeight.Black, fontSize = 15.sp, color = LogoGreen
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun convertAmount(amountEur: Double, userCurrency: String, fxRates: FxRates?): Double {
    if (fxRates == null) return amountEur
    return ((amountEur * (fxRates.rates[userCurrency] ?: 1.0)) * 100).toLong() / 100.0
}

fun Double.round2(): Double = kotlin.math.round(this * 100) / 100

private fun formatAmount(amount: Double): String = String.format("%.2f", amount)

private fun convertAndFormatAmount(
    amount: Double, preferredCurrency: String, fxRates: FxRates?
): String = String.format(
    "%.2f %s",
    convertAmount(amount, preferredCurrency, fxRates),
    preferredCurrency
)