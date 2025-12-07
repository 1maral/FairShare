package hu.ait.maral.fairshare.ui.screen

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.data.Item
import hu.ait.maral.fairshare.data.SplitMethod

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun BillScreen(
    groupId: String,
    viewModel: BillViewModel = viewModel()
) {
    val context = LocalContext.current

    var billTitle by remember { mutableStateOf("") }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }

    val billItems = remember { mutableStateListOf<Item>() }
    val itemAssignments = remember { mutableStateMapOf<String, String>() }

    var splitMethod by remember { mutableStateOf(SplitMethod.EQUAL) }

    // Members (uid -> name)
    val members = remember { mutableStateListOf<Pair<String, String>>() }

    LaunchedEffect(groupId) {
        viewModel.loadMembersForGroup(groupId) { list ->
            members.clear()
            members.addAll(list)
        }
    }

    // Camera image state (if you hook this up later)
    var hasImage by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        hasImage = success
    }

    Column(Modifier.padding(16.dp)) {

        OutlinedTextField(
            value = billTitle,
            onValueChange = { billTitle = it },
            label = { Text("Bill Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ---- ADD NEW ITEM ROW ----
        Row(verticalAlignment = Alignment.CenterVertically) {

            OutlinedTextField(
                value = newItemName,
                onValueChange = { newItemName = it },
                label = { Text("Item") },
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(
                value = newItemPrice,
                onValueChange = { newItemPrice = it },
                label = { Text("Price") },
                modifier = Modifier.width(90.dp)
            )
        }

        // Dropdown for assignment
        var expanded by remember { mutableStateOf(false) }
        var selectedUserName by remember { mutableStateOf("Assign to...") }
        var selectedUserId by remember { mutableStateOf<String?>(null) }

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedUserName)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                members.forEach { (uid, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedUserId = uid
                            selectedUserName = name
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                val price = newItemPrice.toDoubleOrNull()
                if (newItemName.isNotBlank() && price != null && selectedUserId != null) {

                    val item = Item(
                        itemName = newItemName,
                        itemPrice = price
                    )
                    billItems.add(item)
                    itemAssignments[item.itemId] = selectedUserId!!

                    newItemName = ""
                    newItemPrice = ""
                    selectedUserId = null
                    selectedUserName = "Assign to..."
                }
            }
        ) {
            Text("Add Item")
        }

        Spacer(Modifier.height(24.dp))

        // Show items
        LazyColumn {
            items(billItems) { item ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.itemName} - $${item.itemPrice}")
                    Text(
                        members.find { it.first == itemAssignments[item.itemId] }?.second
                            ?: "Unassigned"
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ---- Split Method Dropdown ----
        var splitExpanded by remember { mutableStateOf(false) }

        Box {
            OutlinedButton(onClick = { splitExpanded = true }) {
                Text(
                    "Split method: ${
                        splitMethod.name.lowercase().replaceFirstChar { it.uppercase() }
                    }"
                )
            }
            DropdownMenu(
                expanded = splitExpanded,
                onDismissRequest = { splitExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Equal") },
                    onClick = {
                        splitMethod = SplitMethod.EQUAL
                        splitExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("By item") },
                    onClick = {
                        splitMethod = SplitMethod.BY_ITEM
                        splitExpanded = false
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Finish & upload bill
        Button(onClick = {
            if (imageUri == null) {
                viewModel.uploadBill(
                    groupId = groupId,
                    title = billTitle,
                    billItems = billItems,
                    itemAssignments = itemAssignments,
                    splitMethod = splitMethod
                ) {
                    viewModel.updateBalance(
                        groupId = groupId,
                        billItems = billItems,
                        itemAssignments = itemAssignments,
                        splitMethod = splitMethod
                    ) { success, error ->
                        println("updateBalance result: success=$success, error=$error")
                    }
                }
            } else {
                viewModel.uploadBillImage(
                    groupId = groupId,
                    contentResolver = context.contentResolver,
                    imageUri = imageUri!!,
                    title = billTitle,
                    billItems = billItems,
                    itemAssignments = itemAssignments,
                    splitMethod = splitMethod
                ) {
                    viewModel.updateBalance(
                        groupId = groupId,
                        billItems = billItems,
                        itemAssignments = itemAssignments,
                        splitMethod = splitMethod
                    ) { success, error ->
                        println("updateBalance result (with image): success=$success, error=$error")
                    }
                }
            }
        }) {
            Text("Save Bill")
        }
    }
}
