package hu.ait.maral.fairshare.ui.screen.bill

import android.graphics.BitmapFactory
import android.Manifest
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.data.Item
import hu.ait.maral.fairshare.data.SplitMethod
import hu.ait.maral.fairshare.ui.screen.bill.ai.AiBillReaderViewModel
import hu.ait.maral.fairshare.ui.screen.bill.ai.AiBillUiState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun BillScreen(
    groupId: String,
    viewModel: BillViewModel = viewModel(),
    onBack: () -> Unit,
    onUploadSuccess: () -> Unit
) {
    val context = LocalContext.current
    // --- AI Scanner dependencies ---
    val aiVm: AiBillReaderViewModel = viewModel()
    val aiItems by aiVm.aiItems.collectAsState()
    val aiUiState by aiVm.uiState.collectAsState()

    //var editableAiItems by remember { mutableStateOf(mutableListOf<Item>()) }
    val allItems = remember { mutableStateListOf<Item>() }

    LaunchedEffect(aiItems) {
        // editableAiItems = aiItems.toMutableList()
        allItems.clear()
        allItems.addAll(aiItems)
    }

    // ------------------------------
    //  STATE
    // ------------------------------
    var billTitle by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf("") }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }

    val itemAssignments = remember { mutableStateMapOf<String, String>() }
    var splitMethod by remember { mutableStateOf(SplitMethod.EQUAL) }

    val currencyOptions = listOf(
        "USD", "EUR", "GBP", "HUF", "JPY", "CAD",
        "AUD", "CHF", "INR", "CNY", "SEK", "NOK",
        "NZD", "MXN", "BRL"
    )
    var selectedCurrency by remember { mutableStateOf(currencyOptions[0]) }
    var currencyExpanded by remember { mutableStateOf(false) }

    val members = remember { mutableStateListOf<Pair<String, String>>() }
    LaunchedEffect(groupId) {
        viewModel.loadMembersForGroup(groupId) { list ->
            members.clear()
            members.addAll(list)
        }
    }


    // ------------------------------
    // CAMERA LOGIC
    // ------------------------------
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var hasImage by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> hasImage = success }
    val cameraPermissionState =
        rememberPermissionState(Manifest.permission.CAMERA)
    fun takePhoto() {
        val uri = ComposeFileProvider.Companion.getImageUri(context)
        imageUri = uri
        cameraLauncher.launch(uri)
    }

    // ------------------------------
    // LAYOUT
    // ------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Bill") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = billTitle,
                    onValueChange = { billTitle = it },
                    label = { Text("Bill Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            // Currency Dropdown
            item {
                Text("Currency")
                Box {
                    OutlinedButton(
                        onClick = { currencyExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(selectedCurrency) }
                    DropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencyOptions.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    selectedCurrency = currency
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Camera Section
            item {
                Text("Bill Photo")
                if (cameraPermissionState.status.isGranted) {
                    Button(onClick = { takePhoto() }) { Text("Take Photo") }
                } else {
                    Column {
                        val text = if (cameraPermissionState.status.shouldShowRationale)
                            "Camera permission is needed to take a bill photo."
                        else
                            "Please grant camera permission."
                        Text(text)
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Allow Camera")
                        }
                    }
                }
                if (hasImage && imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(top = 12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Split Method Dropdown
            item {
                Text("Split Method")
                var splitExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { splitExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(splitMethod.name.lowercase().replaceFirstChar { it.uppercase() }) }
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
                            text = { Text("By Item") },
                            onClick = {
                                splitMethod = SplitMethod.BY_ITEM
                                splitExpanded = false

                                // 🔍 Auto-run AI if image exists
                                if (imageUri != null) {
                                    //changed val to var for bitmap for testing purposes  -Jacky
                                    var bitmap = BitmapFactory.decodeStream(
                                        context.contentResolver.openInputStream(imageUri!!)
                                    )
                                    if (bitmap != null) {
                                        bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.receite )
                                        aiVm.scanReceiptWithAI(bitmap)
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Equal split: total price
            if (splitMethod == SplitMethod.EQUAL) {
                item {
                    OutlinedTextField(
                        value = totalPrice,
                        onValueChange = { totalPrice = it },
                        label = { Text("Total Price ($selectedCurrency)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // By item split
            if (splitMethod == SplitMethod.BY_ITEM) {
                item {
                    // --- AI STATUS UI ---
                    when (aiUiState) {
                        is AiBillUiState.LoadingAI -> {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                        }
                        is AiBillUiState.AIError -> {
                            Text(
                                "AI Error: ${(aiUiState as AiBillUiState.AIError).message}"
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        is AiBillUiState.AIResultReady -> {
                            Text("AI Extracted Items:")
                            Spacer(Modifier.height(8.dp))
                        }
                        else -> {}
                    }
                }

                if (aiUiState is AiBillUiState.AIResultReady) {
                    itemsIndexed(allItems) { index, item ->
                        Column(Modifier.padding(8.dp)) {

                            OutlinedTextField(
                                value = item.itemName,
                                onValueChange = { newName ->
                                    allItems[index] = item.copy(itemName = newName)
                                },
                                label = { Text("Item Name") }
                            )

                            OutlinedTextField(
                                value = item.itemPrice.toString(),
                                onValueChange = { newPrice ->
                                    val price = newPrice.toDoubleOrNull() ?: 0.0
                                    allItems[index] = item.copy(itemPrice = price)
                                },
                                label = { Text("Price") }
                            )

                            var expanded by remember { mutableStateOf(false) }
                            var assignedId = itemAssignments[item.itemId]
                            var assignedName =
                                members.find { it.first == assignedId }?.second ?: "Assign to..."

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Assign dropdown button
                                Box {
                                    OutlinedButton(onClick = { expanded = true }) {
                                        Text(assignedName)
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        members.forEach { (uid, name) ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    itemAssignments[item.itemId] = uid
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Delete button
                                Button(onClick = {
                                    allItems.removeAt(index)
                                    itemAssignments.remove(item.itemId)
                                }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }

                item {
                    Text("Add More Bill Items")
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("Item Name") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { newItemPrice = it },
                            label = { Text("Price") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    var memberDropdownExpanded by remember { mutableStateOf(false) }
                    var selectedUserName by remember { mutableStateOf("Assign to...") }
                    var selectedUserId by remember { mutableStateOf<String?>(null) }

                    Box {
                        OutlinedButton(onClick = { memberDropdownExpanded = true }) {
                            Text(selectedUserName)
                        }
                        DropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false }
                        ) {
                            members.forEach { (uid, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedUserId = uid
                                        selectedUserName = name
                                        memberDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(onClick = {
                        val price = newItemPrice.toDoubleOrNull()
                        if (newItemName.isNotBlank() &&
                            price != null &&
                            selectedUserId != null
                        ) {
                            val item = Item(newItemName, price)
                            //billItems.add(item)
                            allItems.add(item)
                            itemAssignments[item.itemId] = selectedUserId!!

                            newItemName = ""
                            newItemPrice = ""
                            selectedUserId = null
                            selectedUserName = "Assign to..."
                        }
                    }) {
                        Text("Add Item")
                    }
                    Spacer(Modifier.height(16.dp))
                }
                //changed billItems to allItems
                items(allItems) { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.itemName} — ${item.itemPrice} $selectedCurrency")
                        Text(
                            members.find { it.first == itemAssignments[item.itemId] }?.second
                                ?: "Unassigned"
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            // Save Bill button + progress messages
            item {
                Button(onClick = {
                    val finalItems =
                        if (splitMethod == SplitMethod.EQUAL) {
                            val total = totalPrice.toDoubleOrNull() ?: 0.0
                            listOf(Item("Total", total))
                        } else {
                            allItems
                        }

                    if (imageUri == null) {
                        viewModel.uploadBill(
                            groupId, billTitle, finalItems, itemAssignments, splitMethod
                        ) {
                            viewModel.updateBalance(
                                groupId, finalItems, itemAssignments, splitMethod
                            )
                            onUploadSuccess()
                        }
                    } else {
                        viewModel.uploadBillImage(
                            groupId,
                            context.contentResolver,
                            imageUri!!,
                            billTitle,
                            finalItems,
                            itemAssignments,
                            splitMethod
                        ) {
                            viewModel.updateBalance(
                                groupId, finalItems, itemAssignments, splitMethod
                            )
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save Bill")
                }

                Spacer(Modifier.height(16.dp))

                when (val state = viewModel.billUploadUiState) {
                    is BillUploadUiState.LoadingBillUpload,
                    is BillUploadUiState.LoadingImageUpload ->
                        CircularProgressIndicator()

                    is BillUploadUiState.BillUploadSuccess,
                    is BillUploadUiState.ImageUploadSuccess -> {
                        // Only show a message here instead of navigating
                        Text("Bill saved successfully!")
                    }

                    is BillUploadUiState.ErrorDuringBillUpload ->
                        Text("Error: ${state.error}")

                    is BillUploadUiState.ErrorDuringImageUpload ->
                        Text("Image error: ${state.error}")

                    else -> {}
                }

            }
        }
    }
}




