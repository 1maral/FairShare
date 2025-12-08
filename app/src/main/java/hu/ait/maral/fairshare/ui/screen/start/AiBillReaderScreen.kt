package hu.ait.maral.fairshare.ui.screen.start

import hu.ait.maral.fairshare.ui.screen.BillUploadUiState
import hu.ait.maral.fairshare.ui.screen.BillViewModel
import hu.ait.maral.fairshare.ui.screen.ComposeFileProvider
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import hu.ait.maral.fairshare.data.Item
import hu.ait.maral.fairshare.data.SplitMethod
import hu.ait.maral.fairshare.ui.screen.start.AiBillReaderViewModel
import hu.ait.maral.fairshare.ui.screen.start.AiBillUiState

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

    //val billItems = remember { mutableStateListOf<Item>() }
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
        rememberPermissionState(android.Manifest.permission.CAMERA)
    fun takePhoto() {
        val uri = ComposeFileProvider.getImageUri(context)
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
                                        bitmap = BitmapFactory.decodeResource(context.resources, hu.ait.maral.fairshare.R.drawable.receite )
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
//                    val finalItems =
//                        if (splitMethod == SplitMethod.EQUAL) {
//                            val total = totalPrice.toDoubleOrNull() ?: 0.0
//                            listOf(Item("Total", total))
//                        } else {
//                            if (aiUiState is AiBillUiState.AIResultReady)
//                                editableAiItems   // <- AI items
//                            else
//                                billItems         // <- fallback if no AI
//                        }
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
                    is BillUploadUiState.ImageUploadSuccess ->
                        onUploadSuccess()
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








//
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.os.Build
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import hu.ait.maral.fairshare.data.SplitMethod
//import java.util.Date
//
//@RequiresApi(Build.VERSION_CODES.P)
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AiBillReaderScreen(
//    groupId: String = "test_group",
//    onBack: () -> Unit = {},
//    onUploadSuccess: () -> Unit = {}
//) {
//    val vm: AiBillReaderViewModel = viewModel()
//
//    val context = LocalContext.current // <-- capture context once here
//
//    var billTitle by remember { mutableStateOf("") }
//    var billDate by remember { mutableStateOf(Date()) }
//    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
//
//    val aiItems by vm.aiItems.collectAsState()
//    val uiState by vm.uiState.collectAsState()
//
//    // Editable copy of AI items
//    var editableItems by remember { mutableStateOf(aiItems.toMutableList()) }
//
//    val cameraLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.TakePicturePreview()
//    ) { bmp -> bitmap = bmp }
//
//    // Update editable items whenever AI items change
//    LaunchedEffect(aiItems) {
//        editableItems = aiItems.toMutableList()
//    }
//
//    // Upload success dialog
//    if (uiState is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.UploadSuccess) {
//        AlertDialog(
//            onDismissRequest = { },
//            confirmButton = {
//                TextButton(onClick = onUploadSuccess) { Text("OK") }
//            },
//            title = { Text("Bill Uploaded") },
//            text = { Text("Your bill has been uploaded with AI support.") }
//        )
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("AI Bill Reader") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) { Text("Back") }
//                }
//            )
//        }
//    ) { padding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                item {
//                    Spacer(Modifier.height(16.dp))
//                    OutlinedTextField(
//                        value = billTitle,
//                        onValueChange = { billTitle = it },
//                        label = { Text("Bill Title") },
//                        modifier = Modifier.fillMaxWidth(0.9f)
//                    )
//
//                    Spacer(Modifier.height(16.dp))
//                    Button(onClick = {
//                        //cameraLauncher.launch(null)
//                        bitmap = BitmapFactory.decodeResource(context.resources, hu.ait.maral.fairshare.R.drawable.receite )
//                    }) {
//                        Text("Take Picture")
//                    }
//
//                    bitmap?.let {
//                        Spacer(Modifier.height(16.dp))
//                        Image(
//                            bitmap = it.asImageBitmap(),
//                            contentDescription = null,
//                            modifier = Modifier.size(220.dp)
//                        )
//                    }
//
//                    if (bitmap != null) {
//                        val vm: AiBillReaderViewModel = viewModel()
//                        Spacer(Modifier.height(16.dp))
//                        Button(onClick = { vm.scanReceiptWithAI(bitmap!!) }) {
//                            Text("Scan With AI")
//                        }
//                    }
//
//                    Spacer(Modifier.height(16.dp))
//                    when (uiState) {
//                        is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.LoadingAI,
//                        is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.Uploading -> {
//                            CircularProgressIndicator()
//                        }
//                        is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.AIError -> {
//                            Text(
//                                "AI Error: ${(uiState as hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.AIError).message}",
//                                color = MaterialTheme.colorScheme.error
//                            )
//                        }
//                        is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.UploadError -> {
//                            Text(
//                                "Upload Error: ${(uiState as hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.UploadError).message}",
//                                color = MaterialTheme.colorScheme.error
//                            )
//                        }
//                        else -> {}
//                    }
//
//                    Spacer(Modifier.height(16.dp))
//
//                    if (uiState is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.AIResultReady) {
//                        Text("AI Extracted Items:", style = MaterialTheme.typography.titleMedium)
//                        Spacer(Modifier.height(8.dp))
//                    }
//
//                    Spacer(Modifier.height(8.dp))
//                }
//
//                // Editable AI items
//                /////////////////////////////
//                if (uiState is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.AIResultReady) {
//                    itemsIndexed(editableItems) { index, item ->
//                        Card(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(8.dp)
//                        ) {
//                            Column(Modifier.padding(8.dp)) {
//                                OutlinedTextField(
//                                    value = item.itemName,
//                                    onValueChange = { editableItems[index] = item.copy(itemName = it) },
//                                    label = { Text("Item Name") }
//                                )
//                                OutlinedTextField(
//                                    value = item.itemPrice.toString(),
//                                    onValueChange = {
//                                        val price = it.toDoubleOrNull() ?: 0.0
//                                        editableItems[index] = item.copy(itemPrice = price)
//                                    },
//                                    label = { Text("Price (€)") }
//                                )
//                            }
//                        }
//                    }
//
//
//
//                }
//
//
//
//                item {
//                    if (bitmap != null) {
//                        Spacer(Modifier.height(16.dp))
//                        Button(
//                            enabled = uiState is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.AIResultReady,
//                            onClick = {
//                                vm.uploadAIBill(
//                                    groupId = groupId,
//                                    billTitle = billTitle,
//                                    billDate = billDate,
//                                    billItems = editableItems,
//                                    bitmap = bitmap,
//                                    splitMethod = SplitMethod.BY_ITEM,
//                                    contentResolver = context.contentResolver, // <-- use captured context
//                                    onSuccess = onUploadSuccess
//                                )
//                            }
//                        ) {
//                            if (uiState is hu.ait.maral.fairshare.ui.screen.start.AiBillUiState.Uploading) {
//                                CircularProgressIndicator()
//                            } else Text("Upload Bill")
//                        }
//
//                        Spacer(Modifier.height(50.dp))
//                    }
//                }
//            }
//        }
//    }
//}