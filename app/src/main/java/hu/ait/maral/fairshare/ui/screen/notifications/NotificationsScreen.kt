package hu.ait.maral.fairshare.ui.screen.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

// ── Strawberry-Matcha Palette ─────────────────────────────────────────────────
private val Rose300    = Color(0xFFF48FB1)
private val Rose500    = Color(0xFFE76F8E)
private val Mint300    = Color(0xFFA8D8B0)
private val OffWhite   = Color(0xFFFFFAFC)
private val Stone      = Color(0xFF9E8E95)
private val FieldFocus = Color(0xFFF9E4EC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val pendingGroups     = viewModel.pendingGroups.value
    val isLoading         = viewModel.isLoading.value
    val errorMessage      = viewModel.errorMessage.value
    val coroutineScope    = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadPendingGroups() }

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
            // ── Accent orbs ───────────────────────────────────────────────────
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

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.Center),
                        color       = LogoGreen,
                        strokeWidth = 3.dp
                    )
                }

                errorMessage != null -> {
                    LaunchedEffect(errorMessage) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message           = errorMessage,
                                withDismissAction = true
                            )
                        }
                    }
                }

                pendingGroups.isEmpty() -> {
                    // ── Empty state ───────────────────────────────────────────
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint     = Rose300,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text       = "All caught up!",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Rose500
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text      = stringResource(R.string.no_pending_group_invitations),
                                fontSize  = 13.sp,
                                color     = Stone,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(
                            top    = 88.dp,
                            start  = 20.dp,
                            end    = 20.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier            = Modifier.fillMaxSize()
                    ) {
                        items(pendingGroups) { group ->
                            NotificationGroupCard(
                                group     = group,
                                onAccept  = {
                                    viewModel.acceptGroup(group)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Joined ${group.name}!",
                                            withDismissAction = true
                                        )
                                    }
                                },
                                onDecline = {
                                    viewModel.declineGroup(group)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Declined ${group.name}.",
                                            withDismissAction = true
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── Floating top bar — identical to HomeScreen & ProfileScreen ────
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
                        IconButton(
                            onClick  = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint     = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text          = stringResource(R.string.notifications),
                            fontSize      = 22.sp,
                            fontWeight    = FontWeight.Black,
                            color         = Color.White,
                            letterSpacing = (-0.5).sp,
                            modifier      = Modifier.weight(1f)
                        )
                        // Notification count badge
                        if (pendingGroups.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Rose500.copy(alpha = 0.85f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = "${pendingGroups.size}",
                                    color      = Color.White,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationGroupCard(
    group    : Group,
    onAccept : () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {

            // Rose→Mint accent strip
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300))
                    )
            )

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {

                // Group name + member count pill
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
                    if (group.members.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Mint300.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text       = "${group.members.size} members",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = LogoGreen
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Thin divider
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Rose300.copy(alpha = 0.2f))
                )

                Spacer(Modifier.height(10.dp))

                // Members list
                val membersText = if (group.members.isEmpty())
                    stringResource(R.string.no_members_yet)
                else
                    stringResource(R.string.members, group.members.joinToString(", "))

                Text(
                    text     = membersText,
                    fontSize = 12.sp,
                    color    = Stone
                )

                Spacer(Modifier.height(14.dp))

                // Accept / Decline buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Accept — solid LogoGreen with check icon
                    Button(
                        onClick   = onAccept,
                        shape     = RoundedCornerShape(12.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor = LogoGreen,
                            contentColor   = Color.White
                        ),
                        modifier  = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            stringResource(R.string.accept),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        )
                    }

                    // Decline — rose outlined
                    OutlinedButton(
                        onClick  = onDecline,
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                        border   = androidx.compose.foundation.BorderStroke(1.5.dp, Rose300),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            stringResource(R.string.decline),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        )
                    }
                }
            }
        }
    }
}