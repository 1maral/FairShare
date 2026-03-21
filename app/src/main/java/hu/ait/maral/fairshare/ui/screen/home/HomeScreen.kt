package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.style.TextAlign
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

// ── Palette ───────────────────────────────────────────────────────────────────
private val Rose300    = Color(0xFFF48FB1)
private val Rose500    = Color(0xFFE76F8E)
private val Mint300    = Color(0xFFA8D8B0)
private val OffWhite   = Color(0xFFFFFAFC)
private val Stone      = Color(0xFF9E8E95)
private val FieldFocus = Color(0xFFF9E4EC)
private val FieldBg    = Color(0xFFFFF8FA)
private val ArchivedBg = Color(0xFFF5F0F2)

data class GroupUi(
    val groupId         : String,
    val name            : String,
    val memberNames     : List<String>,
    val memberBalances  : List<Double>,
    val memberAvatarUrls: List<String?>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel           : HomeScreenViewModel = viewModel(),
    ratesViewModel      : RatesViewModel = hiltViewModel(),
    onNotificationsClick: () -> Unit = {},
    onProfileClick      : () -> Unit = {},
    onRoomClick         : (String) -> Unit
) {
    val groups         = viewModel.groups.value
    val archivedGroups = viewModel.archivedGroups.value
    val isLoading      = viewModel.isLoading.value
    val errorMessage   = viewModel.errorMessage.value
    val userCurrency   = viewModel.preferredCurrency.value
    val fxRates        = ratesViewModel.fxRates.value
    val pendingCount   = viewModel.pendingNotificationCount.value

    // Track previous group count so we can auto-close the dialog when a new
    // group successfully appears in the list (snapshot listener fires).
    val prevGroupCount = remember { mutableStateOf(groups.size) }

    var isAddGroupDialogOpen   by remember { mutableStateOf(false) }
    var groupName              by remember { mutableStateOf("") }
    var memberEmails           by remember { mutableStateOf(listOf("")) }

    var isAddMembersDialogOpen by remember { mutableStateOf(false) }
    var selectedGroupId        by remember { mutableStateOf<String?>(null) }
    var addMemberEmails        by remember { mutableStateOf(listOf("")) }

    var archivedExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Auto-close "Add Group" dialog when a new group appears ───────────────
    LaunchedEffect(groups.size) {
        if (groups.size > prevGroupCount.value && isAddGroupDialogOpen) {
            isAddGroupDialogOpen = false
            groupName    = ""
            memberEmails = listOf("")
        }
        prevGroupCount.value = groups.size
    }

    // ── Show errors in snackbar, then clear so they don't re-fire ─────────────
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message           = msg,
                withDismissAction = true,
                duration          = SnackbarDuration.Long
            )
            viewModel.errorMessage.value = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserPreferredCurrency()
        viewModel.loadGroupsForUser()
        viewModel.loadPendingCount()
        viewModel.loadArchivedGroupsForUser()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Rose500,
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
                        listOf(Color(0xFFFAECF2), Color(0xFFFFF8FB), Color(0xFFEDF7F0))
                    )
                )
        ) {
            // Ambient orbs
            Box(Modifier.size(300.dp).offset((-120).dp, (-120).dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0x18E76F8E), Color.Transparent))))
            Box(Modifier.size(220.dp).align(Alignment.BottomEnd).offset(80.dp, 80.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0x14A8D8B0), Color.Transparent))))

            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ───────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp,
                            RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                            ambientColor = Rose300.copy(alpha = 0.3f),
                            spotColor    = Rose500.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(Brush.horizontalGradient(listOf(LogoGreen, Color(0xFF5DB88A))))
                ) {
                    Column {
                        Spacer(Modifier.fillMaxWidth().statusBarsPadding())
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "FairShare", fontSize = 22.sp, fontWeight = FontWeight.Black,
                                color = Color.White, letterSpacing = (-0.5).sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onProfileClick, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Person, "Profile",
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            BadgedBox(badge = {
                                if (pendingCount > 0) Badge(
                                    containerColor = Rose500, contentColor = Color.White
                                ) {
                                    Text(
                                        if (pendingCount > 9) "9+" else "$pendingCount",
                                        fontSize = 9.sp
                                    )
                                }
                            }) {
                                IconButton(
                                    onClick  = onNotificationsClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Notifications, "Notifications",
                                        tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Rose500.copy(alpha = 0.85f))
                                    .clickable(enabled = !isLoading) {
                                        isAddGroupDialogOpen = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            color       = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier    = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(Icons.Filled.AddCircle, "Add Group",
                                            tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Text("Group", color = Color.White,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // ── Body ──────────────────────────────────────────────────────
                when {
                    isLoading && groups.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LogoGreen, strokeWidth = 3.dp)
                        }
                    }

                    groups.isEmpty() && archivedGroups.isEmpty() -> {
                        Box(
                            Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(96.dp).clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(FieldFocus, OffWhite)))
                                        .border(1.5.dp, Rose300.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AddCircle, null,
                                        tint = Rose300, modifier = Modifier.size(40.dp))
                                }
                                Spacer(Modifier.height(20.dp))
                                Text("No groups yet",
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Rose500)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Tap the Group button above to create your first shared space.",
                                    fontSize = 13.sp, color = Stone, textAlign = TextAlign.Center
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
                            if (groups.isNotEmpty()) {
                                item {
                                    HomeSectionHeader(
                                        title    = "Active Rooms",
                                        subtitle = "${groups.size} room${if (groups.size != 1) "s" else ""}"
                                    )
                                }
                                items(groups) { group ->
                                    ActiveGroupItem(
                                        group            = group,
                                        userCurrency     = userCurrency,
                                        fxRates          = fxRates,
                                        viewModel        = viewModel,
                                        onRoomClick      = onRoomClick,
                                        onAddMemberClick = {
                                            selectedGroupId        = group.groupId
                                            isAddMembersDialogOpen = true
                                            addMemberEmails        = listOf("")
                                        }
                                    )
                                }
                            }

                            if (archivedGroups.isNotEmpty()) {
                                item { Spacer(Modifier.height(4.dp)) }
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(ArchivedBg)
                                            .clickable { archivedExpanded = !archivedExpanded }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Archive, null,
                                            tint = Stone, modifier = Modifier.size(18.dp))
                                        Text("Archived Rooms", fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold, color = Stone,
                                            modifier = Modifier.weight(1f))
                                        Text(
                                            "${archivedGroups.size} room${if (archivedGroups.size != 1) "s" else ""}",
                                            fontSize = 12.sp, color = Stone.copy(alpha = 0.7f)
                                        )
                                        Icon(
                                            if (archivedExpanded) Icons.Default.KeyboardArrowUp
                                            else Icons.Default.KeyboardArrowDown,
                                            null, tint = Stone, modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (archivedExpanded) {
                                    items(archivedGroups) { group ->
                                        ArchivedGroupItem(
                                            group        = group,
                                            userCurrency = userCurrency,
                                            fxRates      = fxRates,
                                            viewModel    = viewModel,
                                            onRoomClick  = onRoomClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Add Group Dialog ──────────────────────────────────────────────────
        if (isAddGroupDialogOpen) {
            StrawberryDialog(
                title        = stringResource(R.string.create_new_group),
                onDismiss    = {
                    if (!isLoading) {
                        isAddGroupDialogOpen = false
                        groupName    = ""
                        memberEmails = listOf("")
                    }
                },
                onConfirm    = {
                    viewModel.createGroup(groupName, memberEmails)
                },
                confirmLabel = stringResource(R.string.create),
                isLoading    = isLoading
            ) {
                OutlinedTextField(
                    value         = groupName,
                    onValueChange = { groupName = it },
                    label         = { Text(stringResource(R.string.group_name)) },
                    shape         = RoundedCornerShape(14.dp),
                    colors        = dialogFieldColors(),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.member_emails), color = Stone, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Members must already have a FairShare account.",
                    color = Stone.copy(alpha = 0.65f), fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    memberEmails.forEachIndexed { index, email ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value         = email,
                                onValueChange = { new ->
                                    memberEmails = memberEmails.toMutableList().also { it[index] = new }
                                },
                                placeholder = {
                                    Text(stringResource(R.string.email_fairshare_com),
                                        color = Rose300.copy(alpha = 0.5f))
                                },
                                shape      = RoundedCornerShape(14.dp),
                                colors     = dialogFieldColors(),
                                modifier   = Modifier.weight(1f),
                                singleLine = true
                            )
                            if (memberEmails.size > 1) {
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick  = {
                                        memberEmails = memberEmails.toMutableList()
                                            .also { it.removeAt(index) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Close,
                                        contentDescription = "Remove email",
                                        tint               = Rose500,
                                        modifier           = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { memberEmails = memberEmails + "" }) {
                    Text(stringResource(R.string.add_another_email),
                        color = LogoGreen, fontSize = 13.sp)
                }
            }
        }

        // ── Add Members Dialog ────────────────────────────────────────────────
        // FIX #4: Close dialog immediately and unconditionally — don't check
        // isLoading here since it's still false at the moment onConfirm fires.
        // Errors surface via the snackbar from the VM; success is visible
        // when the Firestore snapshot updates the group's member list.
        if (isAddMembersDialogOpen && selectedGroupId != null) {
            StrawberryDialog(
                title        = stringResource(R.string.add_members_to_group),
                onDismiss    = {
                    if (!isLoading) {
                        isAddMembersDialogOpen = false
                        selectedGroupId  = null
                        addMemberEmails  = listOf("")
                    }
                },
                onConfirm    = {
                    selectedGroupId?.let { id ->
                        viewModel.addMembersToGroup(id, addMemberEmails)
                    }
                    // Close unconditionally — VM emits errors via snackbar
                    isAddMembersDialogOpen = false
                    selectedGroupId  = null
                    addMemberEmails  = listOf("")
                },
                confirmLabel = "Add",
                isLoading    = isLoading
            ) {
                Text("Member emails:", color = Stone, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Members must already have a FairShare account.",
                    color = Stone.copy(alpha = 0.65f), fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    addMemberEmails.forEachIndexed { index, email ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value         = email,
                                onValueChange = { new ->
                                    addMemberEmails = addMemberEmails.toMutableList()
                                        .also { it[index] = new }
                                },
                                placeholder = {
                                    Text("email@fairshare.com",
                                        color = Rose300.copy(alpha = 0.5f))
                                },
                                shape      = RoundedCornerShape(14.dp),
                                colors     = dialogFieldColors(),
                                modifier   = Modifier.weight(1f),
                                singleLine = true
                            )
                            if (addMemberEmails.size > 1) {
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick  = {
                                        addMemberEmails = addMemberEmails.toMutableList()
                                            .also { it.removeAt(index) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Close,
                                        contentDescription = "Remove email",
                                        tint               = Rose500,
                                        modifier           = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { addMemberEmails = addMemberEmails + "" }) {
                    Text("Add another email", color = LogoGreen, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Active group item ─────────────────────────────────────────────────────────
@Composable
private fun ActiveGroupItem(
    group           : hu.ait.maral.fairshare.data.Group,
    userCurrency    : String,
    fxRates         : FxRates?,
    viewModel       : HomeScreenViewModel,
    onRoomClick     : (String) -> Unit,
    onAddMemberClick: () -> Unit
) {
    val memberIds  = group.memberIds
    val namesList  = group.members
    val balanceMap = group.balances

    val memberNames    = mutableListOf<String>()
    val memberBalances = mutableListOf<Double>()

    // FIX #1: Use the full memberIds list as the key, not just its size.
    // This ensures avatarUrls is properly rebuilt when the member list changes
    // (e.g. after inviting an existing user to the room).
    val avatarUrls = remember(group.groupId, memberIds) {
        mutableStateListOf<String?>().apply { repeat(memberIds.size) { add(null) } }
    }

    for (i in memberIds.indices) {
        memberNames.add(namesList.getOrNull(i) ?: "Member")
        memberBalances.add(convertAmount(balanceMap[memberIds[i]] ?: 0.0, userCurrency, fxRates))
    }

    // FIX #2: Also key the avatar-fetch effect on memberIds.size so it
    // re-runs when a new member is added, not just on first composition.
    LaunchedEffect(group.groupId, memberIds.size) {
        memberIds.forEachIndexed { index, memberId ->
            viewModel.fetchUserAvatar(memberId) { url ->
                if (index < avatarUrls.size) avatarUrls[index] = url
            }
        }
    }

    GroupCard(
        modifier         = Modifier.fillMaxWidth(),
        onClick          = { onRoomClick(group.groupId) },
        onAddMemberClick = onAddMemberClick,
        isArchived       = false,
        group            = GroupUi(group.groupId, group.name, memberNames, memberBalances, avatarUrls),
        currencyCode     = userCurrency
    )
}

// ── Archived group item ───────────────────────────────────────────────────────
@Composable
private fun ArchivedGroupItem(
    group       : hu.ait.maral.fairshare.data.Group,
    userCurrency: String,
    fxRates     : FxRates?,
    viewModel   : HomeScreenViewModel,
    onRoomClick : (String) -> Unit
) {
    val memberIds  = group.memberIds
    val namesList  = group.members
    val balanceMap = group.balances

    val memberNames    = mutableListOf<String>()
    val memberBalances = mutableListOf<Double>()

    // FIX #1 (archived): Same fix — use full list as remember key.
    val avatarUrls = remember(group.groupId, memberIds) {
        mutableStateListOf<String?>().apply { repeat(memberIds.size) { add(null) } }
    }

    for (i in memberIds.indices) {
        memberNames.add(namesList.getOrNull(i) ?: "Member")
        memberBalances.add(convertAmount(balanceMap[memberIds[i]] ?: 0.0, userCurrency, fxRates))
    }

    // FIX #2 (archived): Re-fetch avatars when member count changes.
    LaunchedEffect(group.groupId, memberIds.size) {
        memberIds.forEachIndexed { index, memberId ->
            viewModel.fetchUserAvatar(memberId) { url ->
                if (index < avatarUrls.size) avatarUrls[index] = url
            }
        }
    }

    GroupCard(
        modifier         = Modifier.fillMaxWidth(),
        onClick          = { onRoomClick(group.groupId) },
        onAddMemberClick = {},
        isArchived       = true,
        group            = GroupUi(group.groupId, group.name, memberNames, memberBalances, avatarUrls),
        currencyCode     = userCurrency
    )
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
private fun HomeSectionHeader(title: String, subtitle: String = "") {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Rose500, Mint300)))
        )
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Rose500,
            letterSpacing = 0.3.sp, modifier = Modifier.weight(1f))
        if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Stone)
    }
}

// ── Reusable dialog ───────────────────────────────────────────────────────────
@Composable
private fun StrawberryDialog(
    title       : String,
    onDismiss   : () -> Unit,
    onConfirm   : () -> Unit,
    confirmLabel: String,
    isLoading   : Boolean = false,
    content     : @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(24.dp),
        containerColor   = OffWhite,
        title = {
            Column {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300))))
                Spacer(Modifier.height(12.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Rose500, fontSize = 16.sp)
            }
        },
        text          = { Column { content() } },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = !isLoading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = LogoGreen, contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(16.dp)
                    )
                } else {
                    Text(confirmLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
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
    focusedContainerColor   = FieldFocus,
    unfocusedContainerColor = FieldBg
)

// ── Group Card ────────────────────────────────────────────────────────────────
@Composable
fun GroupCard(
    modifier        : Modifier = Modifier,
    onClick         : () -> Unit,
    onAddMemberClick: () -> Unit,
    isArchived      : Boolean = false,
    group           : GroupUi,
    currencyCode    : String
) {
    val memberCount = min(
        min(group.memberNames.size, group.memberBalances.size),
        group.memberAvatarUrls.size
    )
    val topStripBrush = if (isArchived)
        Brush.horizontalGradient(listOf(Stone.copy(alpha = 0.3f), Stone.copy(alpha = 0.5f)))
    else
        Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300))

    Card(
        onClick   = onClick,
        modifier  = modifier,
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isArchived) ArchivedBg else OffWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isArchived) 1.dp else 4.dp
        )
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(topStripBrush))
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isArchived) {
                        Icon(Icons.Default.Archive, "Archived",
                            tint = Stone, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text       = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = if (isArchived) Stone else LogoGreen,
                        modifier   = Modifier.weight(1f)
                    )
                    if (!isArchived) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Mint300.copy(alpha = 0.25f))
                                .clickable { onAddMemberClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.Add, "Add member",
                                    tint = LogoGreen, modifier = Modifier.size(13.dp))
                                Text("Member", color = LogoGreen,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Stone.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Archived", color = Stone,
                                fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(
                    if (isArchived) Stone.copy(alpha = 0.1f) else Rose300.copy(alpha = 0.2f)
                ))
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    items(memberCount) { i ->
                        val balance  = group.memberBalances[i]
                        val isOwed   = balance >= 0
                        val balColor = when {
                            isArchived -> Stone
                            isOwed     -> LogoGreen
                            else       -> Rose500
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(
                                        if (isArchived) Color(0xFFEEEEEE) else Color(0xFFFCE4EC),
                                        OffWhite
                                    )))
                                    .border(1.5.dp,
                                        if (isArchived) Stone.copy(alpha = 0.3f)
                                        else if (isOwed) Mint300 else Rose300,
                                        CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // FIX #3: Use getOrNull() to safely handle any
                                // transient size mismatch between lists.
                                val avatarUrl = group.memberAvatarUrls.getOrNull(i)
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model              = avatarUrl,
                                        contentDescription = "avatar",
                                        modifier           = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale       = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Filled.Person, null,
                                        tint = if (isArchived) Stone.copy(alpha = 0.5f) else Rose300,
                                        modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(group.memberNames[i], fontSize = 11.sp,
                                fontWeight = FontWeight.Medium, color = Stone)
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isArchived) Stone.copy(alpha = 0.08f)
                                        else if (isOwed) Mint300.copy(alpha = 0.2f)
                                        else FieldFocus
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${if (isOwed && !isArchived) "+" else ""}${
                                        String.format("%.2f", balance)} $currencyCode",
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
    return amountEur * (fxRates.rates[userCurrency] ?: 1.0)
}