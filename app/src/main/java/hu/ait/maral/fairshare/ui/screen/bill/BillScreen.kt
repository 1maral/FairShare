package hu.ait.maral.fairshare.ui.screen.bill

import android.graphics.BitmapFactory
import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.auth.FirebaseAuth
import hu.ait.maral.fairshare.BuildConfig
import hu.ait.maral.fairshare.BuildConfig.GEMINI_API_KEY
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.data.Item
import hu.ait.maral.fairshare.data.SplitMethod
import hu.ait.maral.fairshare.ui.screen.bill.ai.AiBillReaderViewModel
import hu.ait.maral.fairshare.ui.screen.bill.ai.AiBillUiState
import hu.ait.maral.fairshare.ui.screen.rate.RatesViewModel
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.CardPink
import hu.ait.maral.fairshare.ui.theme.LogoGreen

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun BillScreen(
    groupId: String,
    ratesViewModel: RatesViewModel = hiltViewModel(),
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
    var allItems = remember { mutableStateListOf<Item>() }

    LaunchedEffect(aiItems) {
        // editableAiItems = aiItems.toMutableList()
        allItems.clear()
        allItems.addAll(aiItems)
    }

    // ------------------------------
    //  STATE
    // ------------------------------
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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

    val fxRatesState by ratesViewModel.fxRates
    var errorMessage by remember { mutableStateOf<String?>(null) }


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

    fun Double.round2(): Double = kotlin.math.round(this * 100) / 100



    fun convertToEur(amount: Double): Double {
        val fx = fxRatesState
        val ratesMap = fx?.rates

        // If we don't have rates yet or EUR is selected, don't convert
        if (fx == null || ratesMap == null || selectedCurrency == "EUR") {
            return amount
        }

        val rateForSelected = ratesMap[selectedCurrency] ?: return amount

        // Assuming rates are "1 EUR = rateForSelected <selectedCurrency>".
        // So: amount_in_EUR = amount_in_selected / rateForSelected.
        return (amount / rateForSelected).round2()
    }



    // ------------------------------
    // LAYOUT
    // ------------------------------
    Scaffold(
        containerColor = BackgroundPink,
        topBar = {
            TopAppBar(
                title = { Text(
                    text = "Create Bill",
                    color = LogoGreen
                ) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ButtonGreen
                )
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
                    label = { Text(text = "Bill Title", color = LogoGreen) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            // Currency Dropdown
            item {
                Text("Currency", color = Color(0xFFE76F8E))
                Box {
                    OutlinedButton(
                        onClick = { currencyExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors( containerColor = Color(0xFFE76F8E), contentColor = Color(0xFFFFFFFF))
                    ) { Text(selectedCurrency) }
                    DropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencyOptions.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency, color = Color(0xFFE76F8E)) },
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
                Text("Bill Photo", color = Color(0xFFE76F8E))
                if (cameraPermissionState.status.isGranted) {
                    Button(onClick = { takePhoto() }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(Color(0xFFE76F8E))) { Text("Take Photo") }
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
                Text("Split Method", color = Color(0xFFE76F8E))
                var splitExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { splitExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (splitMethod == SplitMethod.BY_ITEM) "By items" else "Equally",
                            color = Color(0xFFE76F8E))
                    }
                    DropdownMenu(
                        expanded = splitExpanded,
                        onDismissRequest = { splitExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Equal", color = Color(0xFFE76F8E)) },
                            onClick = {
                                splitMethod = SplitMethod.EQUAL
                                splitExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Item", color = Color(0xFFE76F8E)) },
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
                        label = { Text("Total Price ($selectedCurrency)", color = LogoGreen) },
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
                                        Text(assignedName, color = Color(0xFFE76F8E))
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        members
                                            .filter { it.first != currentUserId }
                                            .forEach { (uid, name) ->
                                                DropdownMenuItem(
                                                    text = { Text(name, color = Color(0xFFE76F8E)) },
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
                                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(Color(
                                    0xFFFF3030
                                )
                                )) {
                                    Text("Delete", color = Color(0xFFFFFFFF))
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
                    var selectedUserName by remember { mutableStateOf("Assign to") }
                    var selectedUserId by remember { mutableStateOf<String?>(null) }

                    Box {
                        OutlinedButton(onClick = { memberDropdownExpanded = true }) {
                            Text(selectedUserName, color = Color(0xFFE76F8E))
                        }
                        DropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false }
                        ) {
                            members
                                .filter { it.first != currentUserId }
                                .forEach { (uid, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name, color = Color(0xFFE76F8E)) },
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
                    }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(Color(0xFFE76F8E))) {
                        Text("Add Item")
                    }
                    Spacer(Modifier.height(16.dp))
                }

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
                Button(
                    onClick = {
                        if (splitMethod == SplitMethod.BY_ITEM) {
                            val unassignedItems = allItems.filter { itemAssignments[it.itemId] == null }
                            if (unassignedItems.isNotEmpty()) {
                                errorMessage = "Please assign all items to a member before saving."
                                return@Button
                            }
                        }

                        errorMessage = null

                        val finalItems =
                            if (splitMethod == SplitMethod.EQUAL) {
                                val total = totalPrice.toDoubleOrNull() ?: 0.0
                                listOf(Item("Total", total))
                            } else {
                                allItems
                            }

                        val finalItemsInEur = finalItems.map { item ->
                            item.copy(itemPrice = convertToEur(item.itemPrice))
                        }

                        if (imageUri == null) {
                            viewModel.uploadBill(
                                groupId, billTitle, finalItemsInEur, itemAssignments, splitMethod
                            ) {
                                viewModel.updateBalance(
                                    groupId, finalItemsInEur, itemAssignments, splitMethod
                                )
                            }
                        } else {
                            viewModel.uploadBillImage(
                                groupId,
                                context.contentResolver,
                                imageUri!!,
                                billTitle,
                                finalItemsInEur,
                                itemAssignments,
                                splitMethod
                            ) {
                                viewModel.updateBalance(
                                    groupId, finalItemsInEur, itemAssignments, splitMethod
                                )
                            }
                        }
                    },
                     modifier = Modifier.fillMaxWidth()
                , colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LogoGreen)
                ) {
                    Text("Save Bill")
                }

                Spacer(Modifier.height(16.dp))

                when (val state = viewModel.billUploadUiState) {
                    is BillUploadUiState.LoadingBillUpload,
                    is BillUploadUiState.LoadingImageUpload ->
                        CircularProgressIndicator()

                    is BillUploadUiState.BillUploadSuccess -> {
                        Text("Bill saved successfully!")
                        onUploadSuccess()
                    }
                    is BillUploadUiState.ImageUploadSuccess -> {
                        Text("Bill Image saved successfully!")
                    }

                    is BillUploadUiState.ErrorDuringBillUpload ->
                        Text("Error: ${state.error}")

                    is BillUploadUiState.ErrorDuringImageUpload ->
                        Text("Image error: ${state.error}")

                    else -> {}
                }

                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color.Red)
                }


            }
        }
    }
}






