package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.data.FxRates
import hu.ait.maral.fairshare.ui.screen.rate.RatesViewModel
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlin.math.min

// ── Strawberry-Matcha Palette (shared with Login & Profile) ──────────────────
private val Rose300    = Color(0xFFF48FB1)
private val Rose500    = Color(0xFFE76F8E)
private val Mint300    = Color(0xFFA8D8B0)
private val OffWhite   = Color(0xFFFFFAFC)
private val Stone      = Color(0xFF9E8E95)
private val FieldFocus = Color(0xFFF9E4EC)

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
    val groups        = viewModel.groups.value
    val isLoading     = viewModel.isLoading.value
    val errorMessage  = viewModel.errorMessage.value
    val userCurrency  = viewModel.preferredCurrency.value
    val fxRates       = ratesViewModel.fxRates.value
    val pendingCount = viewModel.pendingNotificationCount.value

    var isAddGroupDialogOpen    by remember { mutableStateOf(false) }
    var groupName               by remember { mutableStateOf("") }
    var memberEmails            by remember { mutableStateOf(listOf("")) }
    var isAddMembersDialogOpen  by remember { mutableStateOf(false) }
    var selectedGroupId         by remember { mutableStateOf<String?>(null) }
    var addMemberEmails         by remember { mutableStateOf(listOf("")) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, withDismissAction = true, duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.loadUserPreferredCurrency()
        viewModel.loadGroupsForUser()
        viewModel.loadPendingCount()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Mint300,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp)
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
                        listOf(
                            Color(0xFFFAECF2),
                            Color(0xFFFFF8FB),
                            Color(0xFFEDF7F0)
                        )
                    )
                )
        ) {

            // ── Soft accent orbs (same as LoginScreen) ────────────────────────
            Box(
                Modifier
                    .size(300.dp)
                    .offset((-120).dp, (-120).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x18E76F8E), Color.Transparent))
                    )
            )
            Box(
                Modifier
                    .size(220.dp)
                    .align(Alignment.BottomEnd)
                    .offset(80.dp, 80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x14A8D8B0), Color.Transparent))
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {

                // ── Custom top bar — rounded bottom corners, matches ProfileScreen ─
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation    = 8.dp,
                            shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                            ambientColor = Rose300.copy(alpha = 0.3f),
                            spotColor    = Rose500.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(
                            Brush.horizontalGradient(listOf(LogoGreen, Color(0xFF5DB88A)))
                        )
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
                            // Title
                            Text(
                                text          = "FairShare",
                                fontSize      = 22.sp,
                                fontWeight    = FontWeight.Black,
                                color         = Color.White,
                                letterSpacing = (-0.5).sp,
                                modifier      = Modifier.weight(1f)
                            )
                            // Action icons
                            IconButton(
                                onClick  = onProfileClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Notifications
                            BadgedBox(
                                badge = {
                                    if (pendingCount > 0) {
                                        Badge(
                                            containerColor = Rose500,
                                            contentColor   = Color.White
                                        ) {
                                            Text(
                                                text     = if (pendingCount > 9) "9+" else "$pendingCount",
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick  = onNotificationsClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Notifications,
                                        contentDescription = "Notifications",
                                        tint     = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            // Add group — rose pill button
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Rose500.copy(alpha = 0.85f))
                                    .clickable { isAddGroupDialogOpen = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AddCircle,
                                        contentDescription = "Add Group",
                                        tint     = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Group",
                                        color      = Color.White,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // ── Body ──────────────────────────────────────────────────────
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LogoGreen, strokeWidth = 3.dp)
                        }
                    }
                    groups.isEmpty() -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Empty state illustration — soft rose circle with + icon
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(listOf(FieldFocus, OffWhite))
                                        )
                                        .border(1.5.dp, Rose300.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AddCircle,
                                        contentDescription = null,
                                        tint     = Rose300,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text       = "No groups yet",
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Rose500
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text     = "Tap the Group button above to create your first shared space.",
                                    fontSize = 13.sp,
                                    color    = Stone,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier            = Modifier.fillMaxSize()
                        ) {
                            items(groups) { group ->
                                val memberIds   = group.memberIds
                                val namesList   = group.members
                                val balanceMap  = group.balances

                                val memberNames    = mutableListOf<String>()
                                val memberBalances = mutableListOf<Double>()

                                val avatarUrls = remember(group.groupId, memberIds.size) {
                                    mutableStateListOf<String?>().apply {
                                        repeat(memberIds.size) { add(null) }
                                    }
                                }

                                for (i in memberIds.indices) {
                                    val memberId       = memberIds[i]
                                    val name           = namesList.getOrNull(i) ?: "Member"
                                    val balanceEur     = balanceMap[memberId] ?: 0.0
                                    val balConverted   = convertAmount(balanceEur, userCurrency, fxRates)
                                    memberNames.add(name)
                                    memberBalances.add(balConverted)
                                }

                                LaunchedEffect(group.groupId) {
                                    memberIds.forEachIndexed { index, memberId ->
                                        viewModel.fetchUserAvatar(memberId) { url ->
                                            if (index < avatarUrls.size) avatarUrls[index] = url
                                        }
                                    }
                                }

                                GroupCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick  = { onRoomClick(group.groupId) },
                                    onAddMemberClick = {
                                        selectedGroupId    = group.groupId
                                        isAddMembersDialogOpen = true
                                        addMemberEmails    = listOf("")
                                    },
                                    group = GroupUi(
                                        groupId          = group.groupId,
                                        name             = group.name,
                                        memberNames      = memberNames,
                                        memberBalances   = memberBalances,
                                        memberAvatarUrls = avatarUrls
                                    ),
                                    currencyCode = userCurrency
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Add Group Dialog ──────────────────────────────────────────────────
        if (isAddGroupDialogOpen) {
            StrawberryDialog(
                title     = stringResource(R.string.create_new_group),
                onDismiss = { isAddGroupDialogOpen = false },
                onConfirm = {
                    viewModel.createGroup(groupName, memberEmails)
                    isAddGroupDialogOpen = false
                    groupName    = ""
                    memberEmails = listOf("")
                },
                confirmLabel = stringResource(R.string.create)
            ) {
                OutlinedTextField(
                    value         = groupName,
                    onValueChange = { groupName = it },
                    label         = { Text(stringResource(R.string.group_name)) },
                    shape         = RoundedCornerShape(14.dp),
                    colors        = dialogFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.member_emails), color = Stone, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                memberEmails.forEachIndexed { index, email ->
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { new ->
                            memberEmails = memberEmails.toMutableList().also { it[index] = new }
                        },
                        placeholder = { Text(stringResource(R.string.email_fairshare_com), color = Rose300.copy(alpha = 0.5f)) },
                        shape       = RoundedCornerShape(14.dp),
                        colors      = dialogFieldColors(),
                        modifier    = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine  = true
                    )
                }
                TextButton(onClick = { memberEmails = memberEmails + "" }) {
                    Text(stringResource(R.string.add_another_email), color = LogoGreen, fontSize = 13.sp)
                }
            }
        }

        // ── Add Members Dialog ────────────────────────────────────────────────
        if (isAddMembersDialogOpen && selectedGroupId != null) {
            StrawberryDialog(
                title     = stringResource(R.string.add_members_to_group),
                onDismiss = {
                    isAddMembersDialogOpen = false
                    selectedGroupId  = null
                    addMemberEmails  = listOf("")
                },
                onConfirm = {
                    selectedGroupId?.let { viewModel.addMembersToGroup(it, addMemberEmails) }
                    isAddMembersDialogOpen = false
                    selectedGroupId  = null
                    addMemberEmails  = listOf("")
                },
                confirmLabel = "Add"
            ) {
                Text("Member emails:", color = Stone, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                addMemberEmails.forEachIndexed { index, email ->
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { new ->
                            addMemberEmails = addMemberEmails.toMutableList().also { it[index] = new }
                        },
                        placeholder = { Text("email@fairshare.com", color = Rose300.copy(alpha = 0.5f)) },
                        shape       = RoundedCornerShape(14.dp),
                        colors      = dialogFieldColors(),
                        modifier    = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine  = true
                    )
                }
                TextButton(onClick = { addMemberEmails = addMemberEmails + "" }) {
                    Text("Add another email", color = LogoGreen, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Reusable styled dialog ────────────────────────────────────────────────────
@Composable
private fun StrawberryDialog(
    title        : String,
    onDismiss    : () -> Unit,
    onConfirm    : () -> Unit,
    confirmLabel : String,
    content      : @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(24.dp),
        containerColor   = OffWhite,
        title = {
            Column {
                // Rose→Mint accent bar at top of dialog
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300))
                        )
                )
                Spacer(Modifier.height(12.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Rose500, fontSize = 16.sp)
            }
        },
        text = {
            Column { content() }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = LogoGreen,
                    contentColor   = Color.White
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Stone)
            }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Rose500,
    unfocusedBorderColor    = Rose300.copy(alpha = 0.5f),
    focusedLabelColor       = Rose500,
    unfocusedLabelColor     = Stone,
    cursorColor             = Rose500,
    focusedContainerColor   = Color(0xFFF9E4EC),
    unfocusedContainerColor = Color(0xFFFFF8FA)
)

// ── Group Card ────────────────────────────────────────────────────────────────
@Composable
fun GroupCard(
    modifier         : Modifier = Modifier,
    onClick          : () -> Unit,
    onAddMemberClick : () -> Unit,
    group            : GroupUi,
    currencyCode     : String
) {
    val memberCount = min(
        min(group.memberNames.size, group.memberBalances.size),
        group.memberAvatarUrls.size
    )

    Card(
        onClick   = onClick,
        modifier  = modifier,
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {

            // Rose→Mint top accent strip
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300))
                    )
            )

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {

                // Group name + add member button
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = LogoGreen,
                        modifier   = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Mint300.copy(alpha = 0.25f))
                            .clickable { onAddMemberClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add member",
                                tint     = LogoGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                "Member",
                                color      = LogoGreen,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Thin divider
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Rose300.copy(alpha = 0.2f))
                )

                Spacer(Modifier.height(12.dp))

                // Member list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    items(memberCount) { i ->
                        val balance  = group.memberBalances[i]
                        val isOwed   = balance >= 0
                        val balColor = if (isOwed) LogoGreen else Rose500

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            // Avatar
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFFCE4EC), OffWhite)
                                            )
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isOwed) Mint300 else Rose300,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val avatarUrl = group.memberAvatarUrls[i]
                                    if (avatarUrl != null) {
                                        AsyncImage(
                                            model              = avatarUrl,
                                            contentDescription = "avatar",
                                            modifier           = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale       = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint     = Rose300,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text       = group.memberNames[i],
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = Stone
                            )

                            Spacer(Modifier.height(2.dp))

                            // Balance chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isOwed) Mint300.copy(alpha = 0.2f)
                                        else        FieldFocus
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text       = "${if (isOwed) "+" else ""}${String.format("%.2f", balance)} $currencyCode",
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = balColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun convertAmount(amountEur: Double, userCurrency: String, fxRates: FxRates?): Double {
    if (fxRates == null) return amountEur
    val rate = fxRates.rates[userCurrency] ?: 1.0
    return amountEur * rate
}
