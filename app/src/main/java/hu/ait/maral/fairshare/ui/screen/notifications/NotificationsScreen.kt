package hu.ait.maral.fairshare.ui.screen.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val pendingGroups = viewModel.pendingGroups.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadPendingGroups() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications),
                        color = LogoGreen,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LogoGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPink,
                    titleContentColor = LogoGreen,
                    navigationIconContentColor = LogoGreen,
                    actionIconContentColor = LogoGreen
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = BackgroundPink
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
                        CircularProgressIndicator(color = LogoGreen)
                    }
                }

                errorMessage != null -> {
                    LaunchedEffect(errorMessage) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = errorMessage,
                                withDismissAction = true
                            )
                        }
                    }
                }

                pendingGroups.isEmpty() -> {
                    Text(stringResource(R.string.no_pending_group_invitations), color = LogoGreen)
                }

                else -> {
                    LazyColumn {
                        items(pendingGroups) { group ->
                            NotificationGroupCard(
                                group = group,
                                onAccept = {
                                    viewModel.acceptGroup(group)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Joined ${group.name}!",
                                            withDismissAction = true
                                        )
                                    }
                                },
                                onDecline = {
                                    viewModel.declineGroup(group)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Declined ${group.name}.",
                                            withDismissAction = true
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationGroupCard(
    group: Group,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0E6))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = group.name,
                fontWeight = FontWeight.ExtraBold,
                color = LogoGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            val membersText = if (group.members.isEmpty()) stringResource(R.string.no_members_yet)
            else stringResource(R.string.members, group.members.joinToString(", "))
            Text(membersText, color = LogoGreen)

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                ) { Text(stringResource(R.string.accept), color = Color.White) }

                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) { Text(stringResource(R.string.decline), color = ButtonGreen) }
            }
        }
    }
}
