package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.data.FxRates
import hu.ait.maral.fairshare.ui.screen.rate.RatesViewModel
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlin.math.min

// UI model used just for display
data class GroupUi(
    val groupId: String,
    val name: String,
    val memberNames: List<String>,      // confirmed member names (no pending)
    val memberBalances: List<Double>    // already converted to user's currency
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel(),
    ratesViewModel: RatesViewModel = hiltViewModel(),
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRoomClick: (String) -> Unit
) {
    val groups = viewModel.groups.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    val userCurrency = viewModel.preferredCurrency.value
    val fxRates = ratesViewModel.fxRates.value

    var isAddGroupDialogOpen by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var memberEmails by remember { mutableStateOf(listOf("")) }

    LaunchedEffect(Unit) {
        viewModel.loadUserPreferredCurrency()
        viewModel.loadGroupsForUser()
    }

    Scaffold(
        containerColor = BackgroundPink,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FairShare",
                        color = LogoGreen,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ButtonGreen,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { onProfileClick() }) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { onNotificationsClick() }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { isAddGroupDialogOpen = true }) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Add Group",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ButtonGreen)
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = "Error: $errorMessage",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                groups.isEmpty() -> {
                    Text(
                        "You are not in any groups yet.",
                        color = ButtonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                else -> {
                    LazyColumn {
                        items(groups) { group ->

                            // Build aligned names + balances using the new Map<memberId, Double>
                            // Only confirmed members (memberIds) are considered here; pending members
                            // are kept separate in the Group data and not displayed.
                            val memberNames = mutableListOf<String>()
                            val memberBalances = mutableListOf<Double>()

                            val memberIds = group.memberIds   // confirmed member IDs
                            val namesList = group.members      // parallel list of names
                            val balanceMap = group.balances    // Map<memberId, Double> in EUR

                            for (i in memberIds.indices) {
                                val memberId = memberIds[i]
                                val name = namesList.getOrNull(i) ?: "Member"
                                val balanceEur = balanceMap[memberId] ?: 0.0
                                val balanceConverted = convertAmount(
                                    amountEur = balanceEur,
                                    userCurrency = userCurrency,
                                    fxRates = fxRates
                                )

                                memberNames.add(name)
                                memberBalances.add(balanceConverted)
                            }

                            GroupCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onRoomClick(group.groupId) },
                                group = GroupUi(
                                    groupId = group.groupId,
                                    name = group.name,
                                    memberNames = memberNames,
                                    memberBalances = memberBalances
                                ),
                                currencyCode = userCurrency
                            )
                        }
                    }
                }
            }
        }

        if (isAddGroupDialogOpen) {
            AlertDialog(
                onDismissRequest = { isAddGroupDialogOpen = false },
                title = {
                    Text(
                        "Create new group",
                        fontWeight = FontWeight.Bold,
                        color = ButtonGreen
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Group name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Member emails:")

                        Spacer(modifier = Modifier.height(8.dp))

                        memberEmails.forEachIndexed { index, email ->
                            OutlinedTextField(
                                value = email,
                                onValueChange = { new ->
                                    memberEmails = memberEmails.toMutableList().also {
                                        it[index] = new
                                    }
                                },
                                placeholder = { Text("email@fairshare.com") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                singleLine = true
                            )
                        }

                        TextButton(
                            onClick = { memberEmails = memberEmails + "" },
                        ) {
                            Text("Add another email", color = ButtonGreen)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonGreen
                        ),
                        onClick = {
                            viewModel.createGroup(groupName, memberEmails)
                            isAddGroupDialogOpen = false
                            groupName = ""
                            memberEmails = listOf("")
                        }
                    ) {
                        Text("Create", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isAddGroupDialogOpen = false
                            groupName = ""
                            memberEmails = listOf("")
                        }
                    ) {
                        Text("Cancel", color = Color.Red)
                    }
                }
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

@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    group: GroupUi,
    currencyCode: String
) {
    val memberCount = min(group.memberNames.size, group.memberBalances.size)

    Card(
        onClick = onClick,
        modifier = modifier
            .padding(vertical = 8.dp),
        elevation = cardElevation(6.dp),
        colors = cardColors(containerColor = Color(0xFFFFF0F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // GROUP NAME
            Text(
                text = group.name,
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ButtonGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            // MEMBER ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until memberCount) {
                    Text(
                        text = group.memberNames[i],
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // BALANCES ROW (converted + labeled)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until memberCount) {
                    Text(
                        text = "${currencyCode} ${formatAmount(group.memberBalances[i])}",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFAA4A44)
                    )
                }
            }
        }
    }
}
