package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class GroupUi(
    val name: String,
    val members: List<String>,
    val balances: List<Double>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNotificationsClick: () -> Unit = {}, // hook into NavHost
    onProfileClick: () -> Unit = {}
) {
    val groups = viewModel.groups.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    var isAddGroupDialogOpen by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var memberEmails by remember { mutableStateOf(listOf("")) }

    LaunchedEffect(Unit) {
        viewModel.loadGroupsForUser()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FairShare") },
                actions = {
                    IconButton(onClick = { onProfileClick() }) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = { onNotificationsClick() }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = {
                        // open popup
                        isAddGroupDialogOpen = true
                    }) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add Group")
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

            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (groups.isEmpty()) {
                Text("You are not in any groups yet.")
            } else {
                LazyColumn {
                    items(groups) { group ->
                        GroupCard(
                            modifier = Modifier.fillMaxWidth(),
                            group = GroupUi(
                                name = group.name,
                                members = group.members,
                                balances = group.balances
                            )
                        )
                    }
                }
            }
        }

        // ------------ Add Group Dialog ------------
        if (isAddGroupDialogOpen) {
            AlertDialog(
                onDismissRequest = { isAddGroupDialogOpen = false },
                title = { Text("Create new group") },
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
                            onClick = {
                                memberEmails = memberEmails + ""
                            }
                        ) {
                            Text("Add another email")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createGroup(groupName, memberEmails)
                            isAddGroupDialogOpen = false
                            groupName = ""
                            memberEmails = listOf("")
                        }
                    ) {
                        Text("Create")
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
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    group: GroupUi
) {
    val memberCount = minOf(group.members.size, group.balances.size)

    Card(
        modifier = modifier
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // GROUP NAME
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // MEMBER ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until memberCount) {
                    Text(
                        text = group.members[i],
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // PRICES ROW (aligned under each member)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until memberCount) {
                    Text(
                        text = group.balances[i].toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
