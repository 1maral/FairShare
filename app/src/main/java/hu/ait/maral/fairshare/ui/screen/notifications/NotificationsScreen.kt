package hu.ait.maral.fairshare.ui.screen.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.data.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    onBack: () -> Unit = {}   // MainActivity can pass backStack.removeLastOrNull()
) {
    val pendingGroups = viewModel.pendingGroups.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    LaunchedEffect(Unit) {
        viewModel.loadPendingGroups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Notifications") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                errorMessage != null -> {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                pendingGroups.isEmpty() -> {
                    Text("No pending group invitations.")
                }

                else -> {
                    LazyColumn {
                        items(pendingGroups) { group ->
                            NotificationGroupCard(
                                group = group,
                                onAccept = { viewModel.acceptGroup(group) },
                                onDecline = { viewModel.declineGroup(group) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            val membersText =
                if (group.members.isEmpty()) "No members yet"
                else "Members: ${group.members.joinToString(", ")}"

            Text(text = membersText)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onAccept) {
                    Text("Accept")
                }
                OutlinedButton(onClick = onDecline) {
                    Text("Decline")
                }
            }
        }
    }
}
