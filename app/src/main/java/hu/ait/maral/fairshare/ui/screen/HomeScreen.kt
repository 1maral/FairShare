package hu.ait.maral.fairshare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class GroupUi(
    val name: String,
    val members: List<String>,
    val prices: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // fake data for now, instead of Room
    val groups = listOf(
        GroupUi(
            name = "Roommates",
            members = listOf("Alice", "Bob", "Chris"),
            prices = listOf("5", "6", "7")
        ),
        GroupUi(
            name = "Trip to Paris",
            members = listOf("Aurelia", "Sam"),
            prices = listOf("20", "15")
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FairShare") },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile"
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Add Group"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)      // from Scaffold
                .padding(16.dp)        // your own spacing
        ) {
            items(groups) { group ->
                GroupCard(
                    modifier = Modifier.fillMaxWidth(),
                    group = group
                )
            }
        }
    }
}

@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    group: GroupUi
) {
    val memberCount = minOf(group.members.size, group.prices.size)

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
                        text = group.prices[i],
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
