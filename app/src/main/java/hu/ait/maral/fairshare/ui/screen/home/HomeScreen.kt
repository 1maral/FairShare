package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.R
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
    val memberNames: List<String>,
    val memberBalances: List<Double>,
    val memberAvatarUrls: List<String?>
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

    var isAddMembersDialogOpen by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var addMemberEmails by remember { mutableStateOf(listOf("")) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )

        }
    }

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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // ⭐ here
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
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

                groups.isEmpty() -> {
                    Text(
                        stringResource(R.string.you_are_not_in_any_groups_yet),
                        color = ButtonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                else -> {
                    LazyColumn {
                        items(groups) { group ->

                            val memberNames = mutableListOf<String>()
                            val memberBalances = mutableListOf<Double>()

                            val memberIds = group.memberIds
                            val namesList = group.members
                            val balanceMap = group.balances

                            val avatarUrls = remember(group.groupId, memberIds.size) {
                                mutableStateListOf<String?>().apply {
                                    repeat(memberIds.size) { add(null) }
                                }
                            }


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


                            LaunchedEffect(group.groupId) {
                                memberIds.forEachIndexed { index, memberId ->
                                    viewModel.fetchUserAvatar(memberId) { url ->
                                        if (index < avatarUrls.size) {
                                            avatarUrls[index] = url
                                        }
                                    }
                                }
                            }

                            GroupCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onRoomClick(group.groupId) },
                                onAddMemberClick = {
                                    selectedGroupId = group.groupId
                                    isAddMembersDialogOpen = true
                                    addMemberEmails = listOf("")
                                },
                                group = GroupUi(
                                    groupId = group.groupId,
                                    name = group.name,
                                    memberNames = memberNames,
                                    memberBalances = memberBalances,
                                    memberAvatarUrls = avatarUrls
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
                        stringResource(R.string.create_new_group),
                        fontWeight = FontWeight.Bold,
                        color = ButtonGreen
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text(stringResource(R.string.group_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = stringResource(R.string.member_emails))

                        Spacer(modifier = Modifier.height(8.dp))

                        memberEmails.forEachIndexed { index, email ->
                            OutlinedTextField(
                                value = email,
                                onValueChange = { new ->
                                    memberEmails = memberEmails.toMutableList().also {
                                        it[index] = new
                                    }
                                },
                                placeholder = { Text(stringResource(R.string.email_fairshare_com)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                singleLine = true
                            )
                        }

                        TextButton(
                            onClick = { memberEmails = memberEmails + "" },
                        ) {
                            Text(stringResource(R.string.add_another_email), color = ButtonGreen)
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
                        Text(stringResource(R.string.create), color = Color.White)
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
                        Text(stringResource(R.string.cancel), color = Color.Red)
                    }
                }
            )
        }


        if (isAddMembersDialogOpen && selectedGroupId != null) {
            AlertDialog(
                onDismissRequest = {
                    isAddMembersDialogOpen = false
                    selectedGroupId = null
                    addMemberEmails = listOf("")
                },
                title = {
                    Text(
                        stringResource(R.string.add_members_to_group),
                        fontWeight = FontWeight.Bold,
                        color = ButtonGreen
                    )
                },
                text = {
                    Column {
                        Text(text = "Member emails:")

                        Spacer(modifier = Modifier.height(8.dp))

                        addMemberEmails.forEachIndexed { index, email ->
                            OutlinedTextField(
                                value = email,
                                onValueChange = { new ->
                                    addMemberEmails = addMemberEmails.toMutableList().also {
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
                            onClick = { addMemberEmails = addMemberEmails + "" }
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
                            val gid = selectedGroupId
                            if (gid != null) {
                                viewModel.addMembersToGroup(gid, addMemberEmails)
                            }
                            isAddMembersDialogOpen = false
                            selectedGroupId = null
                            addMemberEmails = listOf("")
                        }
                    ) {
                        Text("Add", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isAddMembersDialogOpen = false
                            selectedGroupId = null
                            addMemberEmails = listOf("")
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
    onAddMemberClick: () -> Unit,
    group: GroupUi,
    currencyCode: String
) {
    val memberCount = min(
        min(group.memberNames.size, group.memberBalances.size),
        group.memberAvatarUrls.size
    )

    Card(
        onClick = onClick,
        modifier = modifier.padding(vertical = 8.dp),
        elevation = cardElevation(6.dp),
        colors = cardColors(containerColor = Color(0xFFFFF0F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = group.name,
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ButtonGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ⭐ HORIZONTAL SCROLLABLE LIST OF MEMBERS
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(memberCount) { i ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = group.memberNames[i],
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val avatarUrl = group.memberAvatarUrls[i]
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "avatar placeholder",
                                tint = LogoGreen,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${currencyCode} ${formatAmount(group.memberBalances[i])}",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFAA4A44)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onAddMemberClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add member",
                        tint = ButtonGreen
                    )
                }
            }
        }
    }
}
