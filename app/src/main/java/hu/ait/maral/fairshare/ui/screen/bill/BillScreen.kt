package hu.ait.maral.fairshare.ui.screen.bill

import android.graphics.BitmapFactory
import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.auth.FirebaseAuth
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.data.Item
import hu.ait.maral.fairshare.data.SplitMethod
import hu.ait.maral.fairshare.ui.screen.bill.ai.AiBillReaderViewModel
import hu.ait.maral.fairshare.ui.screen.bill.ai.AiBillUiState
import hu.ait.maral.fairshare.ui.screen.rate.RatesViewModel
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

// ── Palette (mirrors LoginScreen / ProfileScreen) ─────────────────────────────
private val Rose300    = Color(0xFFF48FB1)
private val Rose500    = Color(0xFFE76F8E)
private val Mint300    = Color(0xFFA8D8B0)
private val OffWhite   = Color(0xFFFFFAFC)
private val Stone      = Color(0xFF9E8E95)
private val FieldBg    = Color(0xFFFFF8FA)
private val FieldFocus = Color(0xFFF9E4EC)
private val ErrorRed   = Color(0xFFE53935)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun BillScreen(
    groupId: String,
    ratesViewModel: RatesViewModel = hiltViewModel(),
    viewModel: BillViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val aiVm: AiBillReaderViewModel = viewModel()
    val aiItems    by aiVm.aiItems.collectAsState()
    val aiUiState  by aiVm.uiState.collectAsState()

    var allItems = remember { mutableStateListOf<Item>() }
    LaunchedEffect(aiItems) {
        allItems.clear()
        allItems.addAll(aiItems)
    }

    LaunchedEffect(viewModel.billUploadUiState) {
        if (viewModel.billUploadUiState is BillUploadUiState.BillUploadSuccess ||
            viewModel.billUploadUiState is BillUploadUiState.ImageUploadSuccess) {
            onBack()
        }
    }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var billTitle       by remember { mutableStateOf("") }
    var totalPrice      by remember { mutableStateOf("") }
    var newItemName     by remember { mutableStateOf("") }
    var newItemPrice    by remember { mutableStateOf("") }

    val itemAssignments = remember { mutableStateMapOf<String, String>() }
    var splitMethod     by remember { mutableStateOf(SplitMethod.EQUAL) }

    val currencyOptions = listOf(
        stringResource(R.string.usd), stringResource(R.string.eur),
        stringResource(R.string.gbp), stringResource(R.string.huf),
        stringResource(R.string.jpy), stringResource(R.string.cad),
        stringResource(R.string.aud), stringResource(R.string.chf),
        stringResource(R.string.inr), stringResource(R.string.cny),
        stringResource(R.string.sek), stringResource(R.string.nok),
        stringResource(R.string.nzd), stringResource(R.string.mxn),
        stringResource(R.string.brl)
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

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var hasImage by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> hasImage = success }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    fun takePhoto() {
        val uri = ComposeFileProvider.Companion.getImageUri(context)
        imageUri = uri
        cameraLauncher.launch(uri)
    }

    fun Double.round2(): Double = kotlin.math.round(this * 100) / 100

    fun convertToEur(amount: Double): Double {
        val fx       = fxRatesState
        val ratesMap = fx?.rates
        if (fx == null || ratesMap == null || selectedCurrency == "EUR") return amount
        val rateForSelected = ratesMap[selectedCurrency] ?: return amount
        return (amount / rateForSelected).round2()
    }

    // ── Ambient slow rotation for diamond accents ──────────────────────────────
    val spin = rememberInfiniteTransition(label = "spin")
    val ringRotation by spin.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing)),
        label = "ring"
    )

    // ── Entrance animation ─────────────────────────────────────────────────────
    val contentAlpha = remember { Animatable(0f) }
    val contentSlide = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(450, easing = EaseOutCubic))
        contentSlide.animateTo(0f, tween(500, easing = EaseOutCubic))
    }

    // ── Save button press scale ────────────────────────────────────────────────
    val saveBtnScale   = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFFAECF2), Color(0xFFFFF8FB)))
                    )
                    .statusBarsPadding()
            ) {
                // Animated gradient top strip
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300))
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint               = LogoGreen
                        )
                    }
                    Icon(
                        imageVector        = Icons.Default.Receipt,
                        contentDescription = null,
                        tint               = Rose500,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text          = stringResource(R.string.create_bill),
                        fontSize      = 20.sp,
                        fontWeight    = FontWeight.Black,
                        color         = LogoGreen,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(8.dp)
                            .rotate(ringRotation * 0.4f)
                            .background(Rose500.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF8FB), Color(0xFFFFFAFC), Color(0xFFEDF7F0))
                    )
                )
        ) {
            // Soft ambient orbs (static)
            Box(
                Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .offset(110.dp, (-80).dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0x18E76F8E), Color.Transparent)))
            )
            Box(
                Modifier
                    .size(220.dp)
                    .align(Alignment.BottomStart)
                    .offset((-70).dp, 60.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0x14A8D8B0), Color.Transparent)))
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha        = contentAlpha.value
                        translationY = contentSlide.value.dp.toPx()
                    },
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Bill Title ────────────────────────────────────────────────
                item {
                    BillCard {
                        Text(
                            text       = "Bill Details",
                            fontSize   = 12.sp,
                            color      = Stone,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value         = billTitle,
                            onValueChange = { billTitle = it },
                            label         = { Text(stringResource(R.string.bill_title)) },
                            singleLine    = true,
                            shape         = RoundedCornerShape(14.dp),
                            colors        = billFieldColors(),
                            modifier      = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Currency picker ───────────────────────────────────────────
                item {
                    BillCard {
                        Text(
                            text          = "Currency",
                            fontSize      = 12.sp,
                            color         = Stone,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        ExposedDropdownMenuBox(
                            expanded         = currencyExpanded,
                            onExpandedChange = { currencyExpanded = !currencyExpanded },
                            modifier         = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value         = selectedCurrency,
                                onValueChange = {},
                                readOnly      = true,
                                trailingIcon  = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                                },
                                shape    = RoundedCornerShape(14.dp),
                                colors   = billFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded         = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false },
                                modifier         = Modifier.heightIn(max = 240.dp),
                                containerColor   = OffWhite
                            ) {
                                currencyOptions.forEach { currency ->
                                    DropdownMenuItem(
                                        text    = { Text(currency, color = LogoGreen, fontSize = 14.sp) },
                                        onClick = {
                                            selectedCurrency = currency
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Camera / Photo ────────────────────────────────────────────
                item {
                    BillCard {
                        Text(
                            text          = stringResource(R.string.bill_photo),
                            fontSize      = 12.sp,
                            color         = Stone,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        if (cameraPermissionState.status.isGranted) {
                            if (hasImage && imageUri != null) {
                                // Show preview with re-take option
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color(0xFFE8D5DC), RoundedCornerShape(14.dp))
                                ) {
                                    AsyncImage(
                                        model              = imageUri,
                                        contentDescription = null,
                                        contentScale       = ContentScale.Crop,
                                        modifier           = Modifier.fillMaxSize()
                                    )
                                    // Retake button overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xBBFFFFFF))
                                            .clickable { takePhoto() }
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Retake",
                                            tint     = Rose500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                // Camera button
                                OutlinedButton(
                                    onClick = { takePhoto() },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape    = RoundedCornerShape(14.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                                    border   = androidx.compose.foundation.BorderStroke(1.5.dp, Rose500)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Take Photo",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 14.sp
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text     = if (cameraPermissionState.status.shouldShowRationale)
                                        "Camera permission is needed to take a bill photo."
                                    else "Please grant camera permission.",
                                    fontSize = 13.sp,
                                    color    = Stone
                                )
                                OutlinedButton(
                                    onClick  = { cameraPermissionState.launchPermissionRequest() },
                                    shape    = RoundedCornerShape(14.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                                    border   = androidx.compose.foundation.BorderStroke(1.5.dp, Rose500)
                                ) {
                                    Text(stringResource(R.string.allow_camera), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // ── Split method ──────────────────────────────────────────────
                item {
                    BillCard {
                        Text(
                            text          = stringResource(R.string.split_method),
                            fontSize      = 12.sp,
                            color         = Stone,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        // Toggle chips
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SplitChip(
                                label    = stringResource(R.string.equal),
                                selected = splitMethod == SplitMethod.EQUAL,
                                onClick  = { splitMethod = SplitMethod.EQUAL },
                                modifier = Modifier.weight(1f)
                            )
                            SplitChip(
                                label    = stringResource(R.string.by_item),
                                selected = splitMethod == SplitMethod.BY_ITEM,
                                onClick  = {
                                    splitMethod = SplitMethod.BY_ITEM
                                    if (imageUri != null) {
                                        val bitmap = BitmapFactory.decodeStream(
                                            context.contentResolver.openInputStream(imageUri!!)
                                        )
                                        if (bitmap != null) aiVm.scanReceiptWithAI(bitmap)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── Equal split: total price ───────────────────────────────────
                if (splitMethod == SplitMethod.EQUAL) {
                    item {
                        BillCard {
                            Text(
                                text          = "Total Amount",
                                fontSize      = 12.sp,
                                color         = Stone,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value         = totalPrice,
                                onValueChange = { totalPrice = it },
                                label         = { Text(stringResource(R.string.total_price, selectedCurrency)) },
                                singleLine    = true,
                                shape         = RoundedCornerShape(14.dp),
                                colors        = billFieldColors(),
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── By-item split ─────────────────────────────────────────────
                if (splitMethod == SplitMethod.BY_ITEM) {

                    // AI state banner
                    item {
                        when (aiUiState) {
                            is AiBillUiState.LoadingAI -> {
                                AiBanner(
                                    text    = "AI is reading your receipt…",
                                    isLoading = true
                                )
                            }
                            is AiBillUiState.AIError -> {
                                AiBanner(
                                    text    = "AI Error: ${(aiUiState as AiBillUiState.AIError).message}",
                                    isError = true
                                )
                            }
                            is AiBillUiState.AIResultReady -> {
                                AiBanner(
                                    text = stringResource(R.string.ai_extracted_items),
                                    isSuccess = true
                                )
                            }
                            else -> {}
                        }
                    }

                    // Extracted / editable items
                    if (aiUiState is AiBillUiState.AIResultReady) {
                        itemsIndexed(allItems) { index, item ->
                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn(tween(250, delayMillis = index * 50)) +
                                        slideInVertically(tween(250, delayMillis = index * 50)) { it / 2 }
                            ) {
                                ItemEditCard(
                                    item            = item,
                                    members         = members,
                                    currentUserId   = currentUserId,
                                    assignedId      = itemAssignments[item.itemId],
                                    onNameChange    = { allItems[index] = item.copy(itemName = it) },
                                    onPriceChange   = { allItems[index] = item.copy(itemPrice = it.toDoubleOrNull() ?: 0.0) },
                                    onAssign        = { itemAssignments[item.itemId] = it },
                                    onDelete        = {
                                        allItems.removeAt(index)
                                        itemAssignments.remove(item.itemId)
                                    }
                                )
                            }
                        }
                    }

                    // ── Add new item form ─────────────────────────────────────
                    item {
                        var memberDropdownExpanded by remember { mutableStateOf(false) }
                        var selectedUserName by remember { mutableStateOf("Assign to") }
                        var selectedUserId by remember { mutableStateOf<String?>(null) }

                        BillCard {
                            Text(
                                text          = stringResource(R.string.add_more_bill_items),
                                fontSize      = 12.sp,
                                color         = Stone,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value         = newItemName,
                                    onValueChange = { newItemName = it },
                                    label         = { Text("Item", fontSize = 12.sp) },
                                    singleLine    = true,
                                    shape         = RoundedCornerShape(12.dp),
                                    colors        = billFieldColors(),
                                    modifier      = Modifier.weight(1.6f)
                                )
                                OutlinedTextField(
                                    value         = newItemPrice,
                                    onValueChange = { newItemPrice = it },
                                    label         = { Text("Price", fontSize = 12.sp) },
                                    singleLine    = true,
                                    shape         = RoundedCornerShape(12.dp),
                                    colors        = billFieldColors(),
                                    modifier      = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                // Member picker
                                ExposedDropdownMenuBox(
                                    expanded         = memberDropdownExpanded,
                                    onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded },
                                    modifier         = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value         = selectedUserName,
                                        onValueChange = {},
                                        readOnly      = true,
                                        leadingIcon   = {
                                            Icon(Icons.Default.Person, null, tint = Rose500, modifier = Modifier.size(16.dp))
                                        },
                                        trailingIcon  = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded)
                                        },
                                        singleLine = true,
                                        shape      = RoundedCornerShape(12.dp),
                                        colors     = billFieldColors(),
                                        modifier   = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded         = memberDropdownExpanded,
                                        onDismissRequest = { memberDropdownExpanded = false },
                                        containerColor   = OffWhite
                                    ) {
                                        members.filter { it.first != currentUserId }.forEach { (uid, name) ->
                                            DropdownMenuItem(
                                                text    = { Text(name, color = LogoGreen, fontSize = 14.sp) },
                                                onClick = {
                                                    selectedUserId   = uid
                                                    selectedUserName = name
                                                    memberDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Add button
                                val isFormValid = newItemName.isNotBlank() &&
                                        newItemPrice.toDoubleOrNull() != null &&
                                        selectedUserId != null
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isFormValid)
                                                Brush.linearGradient(listOf(Rose500, Rose300))
                                            else
                                                Brush.linearGradient(listOf(Stone.copy(alpha = 0.3f), Stone.copy(alpha = 0.2f)))
                                        )
                                        .clickable(enabled = isFormValid) {
                                            val price = newItemPrice.toDoubleOrNull() ?: return@clickable
                                            val newItem = Item(newItemName, price)
                                            allItems.add(newItem)
                                            itemAssignments[newItem.itemId] = selectedUserId!!
                                            newItemName      = ""
                                            newItemPrice     = ""
                                            selectedUserId   = null
                                            selectedUserName = "Assign to"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text       = "+",
                                        fontSize   = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (isFormValid) Color.White else Stone.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Item summary list
                    if (allItems.isNotEmpty()) {
                        item {
                            BillCard {
                                Text(
                                    text          = "Items Summary",
                                    fontSize      = 12.sp,
                                    color         = Stone,
                                    fontWeight    = FontWeight.SemiBold,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                allItems.forEachIndexed { idx, item ->
                                    if (idx > 0) HorizontalDivider(
                                        Modifier.padding(vertical = 6.dp),
                                        color = Color(0xFFEDD8E0)
                                    )
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text     = item.itemName,
                                            color    = LogoGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text     = "${item.itemPrice} $selectedCurrency",
                                            color    = Rose500,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text     = members.find { it.first == itemAssignments[item.itemId] }?.second ?: "—",
                                            color    = Stone,
                                            fontSize = 12.sp,
                                            modifier = Modifier.widthIn(max = 80.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Save button + status ──────────────────────────────────────
                item {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                saveBtnScale.animateTo(0.95f, tween(80))
                                saveBtnScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                                if (splitMethod == SplitMethod.BY_ITEM && allItems.isEmpty()) {
                                    errorMessage = context.getString(R.string.please_add_at_least_one_item_before_saving)
                                    return@launch
                                }
                                if (splitMethod == SplitMethod.BY_ITEM) {
                                    val unassigned = allItems.filter { itemAssignments[it.itemId] == null }
                                    if (unassigned.isNotEmpty()) {
                                        errorMessage = context.getString(R.string.please_assign_all_items_to_a_member_before_saving)
                                        return@launch
                                    }
                                }
                                errorMessage = null

                                val finalItems = if (splitMethod == SplitMethod.EQUAL) {
                                    val total = totalPrice.toDoubleOrNull() ?: 0.0
                                    listOf(Item(context.getString(R.string.total), total))
                                } else { allItems }

                                val finalItemsInEur = finalItems.map { it.copy(itemPrice = convertToEur(it.itemPrice)) }

                                if (imageUri == null) {
                                    viewModel.uploadBill(groupId, billTitle, finalItemsInEur, itemAssignments, splitMethod) {
                                        viewModel.updateBalance(groupId, finalItemsInEur, itemAssignments, splitMethod)
                                    }
                                } else {
                                    viewModel.uploadBillImage(
                                        groupId, context.contentResolver, imageUri!!, billTitle,
                                        finalItemsInEur, itemAssignments, splitMethod
                                    ) {
                                        viewModel.updateBalance(groupId, finalItemsInEur, itemAssignments, splitMethod)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp).scale(saveBtnScale.value),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = LogoGreen,
                            contentColor   = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text          = stringResource(R.string.save_bill),
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Status messages
                    AnimatedVisibility(
                        visible = viewModel.billUploadUiState is BillUploadUiState.LoadingBillUpload ||
                                viewModel.billUploadUiState is BillUploadUiState.LoadingImageUpload,
                        enter   = fadeIn(),
                        exit    = fadeOut()
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color       = LogoGreen,
                                strokeWidth = 2.5.dp,
                                modifier    = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Saving…", color = Stone, fontSize = 13.sp)
                        }
                    }

                    when (val state = viewModel.billUploadUiState) {
                        is BillUploadUiState.BillUploadSuccess ->
                            StatusChip(text = stringResource(R.string.bill_saved_successfully), isSuccess = true)
                        is BillUploadUiState.ImageUploadSuccess ->
                            StatusChip(text = stringResource(R.string.bill_image_saved_successfully), isSuccess = true)
                        is BillUploadUiState.ErrorDuringBillUpload ->
                            StatusChip(text = "Error: ${state.error}", isSuccess = false)
                        is BillUploadUiState.ErrorDuringImageUpload ->
                            StatusChip(text = "Image error: ${state.error}", isSuccess = false)
                        else -> {}
                    }

                    errorMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        StatusChip(text = it, isSuccess = false)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Card wrapper ──────────────────────────────────────────────────────────────
@Composable
private fun BillCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300)))
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            content  = content
        )
    }
}

// ── Split method toggle chip ──────────────────────────────────────────────────
@Composable
private fun SplitChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected)
        Brush.linearGradient(listOf(Rose500, Rose300))
    else
        Brush.linearGradient(listOf(FieldBg, FieldBg))

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.5.dp,
                color = if (selected) Color.Transparent else Color(0xFFE8D5DC),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (selected) Color.White else Stone,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Editable item card (AI extracted or manually added) ───────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditCard(
    item: Item,
    members: List<Pair<String, String>>,
    currentUserId: String?,
    assignedId: String?,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onAssign: (String) -> Unit,
    onDelete: () -> Unit
) {
    var memberExpanded by remember { mutableStateOf(false) }
    val assignedName = members.find { it.first == assignedId }?.second ?: "Assign to"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = FieldBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value         = item.itemName,
                    onValueChange = onNameChange,
                    label         = { Text(stringResource(R.string.item_name), fontSize = 12.sp) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp),
                    colors        = billFieldColors(),
                    modifier      = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value         = item.itemPrice.toString(),
                    onValueChange = onPriceChange,
                    label         = { Text(stringResource(R.string.price), fontSize = 12.sp) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp),
                    colors        = billFieldColors(),
                    modifier      = Modifier.weight(1f)
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded         = memberExpanded,
                    onExpandedChange = { memberExpanded = !memberExpanded },
                    modifier         = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value         = assignedName,
                        onValueChange = {},
                        readOnly      = true,
                        leadingIcon   = {
                            Icon(Icons.Default.Person, null, tint = Rose500, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon  = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberExpanded)
                        },
                        singleLine = true,
                        shape      = RoundedCornerShape(10.dp),
                        colors     = billFieldColors(),
                        modifier   = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded         = memberExpanded,
                        onDismissRequest = { memberExpanded = false },
                        containerColor   = OffWhite
                    ) {
                        members.filter { it.first != currentUserId }.forEach { (uid, name) ->
                            DropdownMenuItem(
                                text    = { Text(name, color = LogoGreen, fontSize = 14.sp) },
                                onClick = { onAssign(uid); memberExpanded = false }
                            )
                        }
                    }
                }

                // Delete button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF0F3))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "delete",
                        tint     = Rose500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── AI status banner ──────────────────────────────────────────────────────────
@Composable
private fun AiBanner(
    text: String,
    isLoading: Boolean  = false,
    isError: Boolean    = false,
    isSuccess: Boolean  = false
) {
    val bg = when {
        isError   -> Color(0xFFFFF0F3)
        isSuccess -> Color(0xFFF0FAF4)
        else      -> FieldFocus
    }
    val textColor = when {
        isError   -> ErrorRed
        isSuccess -> LogoGreen
        else      -> Rose500
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color       = Rose500,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector        = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint               = textColor,
                modifier           = Modifier.size(16.dp)
            )
        }
        Text(text = text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Status chip for save result ───────────────────────────────────────────────
@Composable
private fun StatusChip(text: String, isSuccess: Boolean) {
    val bg        = if (isSuccess) Color(0xFFF0FAF4) else Color(0xFFFFF0F3)
    val textColor = if (isSuccess) LogoGreen else ErrorRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Consistent field colors ───────────────────────────────────────────────────
@Composable
private fun billFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Rose500,
    unfocusedBorderColor    = Color(0xFFE8D5DC),
    focusedLabelColor       = Rose500,
    unfocusedLabelColor     = Stone,
    cursorColor             = Rose500,
    focusedContainerColor   = FieldFocus,
    unfocusedContainerColor = FieldBg
)