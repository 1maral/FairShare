package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.elevatedCardElevation
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.LogoGreen

data class GroupUi(
    val name: String,
    val members: List<String>,
    val balances: List<Double>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel(),
    onNotificationsClick: () -> Unit = {},
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
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                    }
                    IconButton(onClick = { onNotificationsClick() }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
                    }
                    IconButton(onClick = { isAddGroupDialogOpen = true }) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add Group", tint = Color.White)
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ButtonGreen)
                }
            } else if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            } else if (groups.isEmpty()) {
                Text(
                    "You are not in any groups yet.",
                    color = ButtonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
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

        if (isAddGroupDialogOpen) {
            AlertDialog(
                onDismissRequest = { isAddGroupDialogOpen = false },
                title = {
                    Text("Create new group", fontWeight = FontWeight.Bold, color = ButtonGreen)
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
                                    memberEmails = memberEmails.toMutableList().also { it[index] = new }
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
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
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


@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    group: GroupUi
) {
    val memberCount = minOf(group.members.size, group.balances.size)

    Card(
        modifier = modifier
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
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
                        text = group.members[i],
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // BALANCES ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until memberCount) {
                    Text(
                        text = "$${group.balances[i]}",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFAA4A44)
                    )
                }
            }
        }
    }
}
