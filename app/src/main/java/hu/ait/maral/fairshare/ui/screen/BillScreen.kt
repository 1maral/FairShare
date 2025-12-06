package hu.ait.maral.fairshare.ui.screen

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BillScreen(
    viewModel: BillViewModel = viewModel()
) {
    val context = LocalContext.current

    var billTitle by remember { mutableStateOf("") }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }

    var billItems = remember { mutableStateListOf<Item>() }
    var splitMethod by remember { mutableStateOf(SplitMethod.EQUAL) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var hasImage by remember { mutableStateOf(false) }

    // Camera launcher & permission
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> hasImage = success }
    )
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Column(modifier = Modifier.padding(20.dp)) {
        // Bill title
        OutlinedTextField(
            value = billTitle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bill Title") },
            onValueChange = { billTitle = it }
        )

        // Add new items
        OutlinedTextField(
            value = newItemName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Item Name") },
            onValueChange = { newItemName = it }
        )
        OutlinedTextField(
            value = newItemPrice,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Item Price") },
            onValueChange = { newItemPrice = it }
        )
        Button(onClick = {
            val price = newItemPrice.toDoubleOrNull()
            if (newItemName.isNotBlank() && price != null) {
                billItems.add(Item(newItemName, price))
                newItemName = ""
                newItemPrice = ""
            }
        }) {
            Text("Add Item")
        }

        // List of items
        for (item in billItems) {
            Text("${item.itemName} - $${item.itemPrice}")
        }

        // Split method selector
        OutlinedTextField(
            value = splitMethod.name,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Split Method (EQUAL/BY_ITEM/BY_PERCENTAGE)") },
            onValueChange = { method ->
                splitMethod = SplitMethod.values().find { it.name == method.uppercase() } ?: SplitMethod.EQUAL
            }
        )

        // Camera button
        if (cameraPermissionState.status.isGranted) {
            Button(onClick = {
                val uri = ComposeFileProvider.getImageUri(context)
                imageUri = uri
                cameraLauncher.launch(uri)
            }) {
                Text("Take Photo")
            }
        } else {
            Column {
                val permissionText = if (cameraPermissionState.status.shouldShowRationale) {
                    "Please allow camera permission to take a photo for the bill."
                } else {
                    "Camera permission required to take a bill photo."
                }
                Text(permissionText)
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Request Permission")
                }
            }
        }

        if (hasImage && imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Bill Image",
                modifier = Modifier.size(200.dp)
            )
        }

        // Send button
        OutlinedButton(onClick = {
            if (imageUri == null) {
                viewModel.uploadBill(
                    title = billTitle,
                    billItems = billItems,
                    splitMethod = splitMethod
                )
            } else {
                viewModel.uploadBillImage(
                    contentResolver = context.contentResolver,
                    imageUri = imageUri!!,
                    title = billTitle,
                    billItems = billItems,
                    splitMethod = splitMethod
                )
            }
        }) {
            Text("Send")
        }

        // Upload UI state feedback
        when (val state = viewModel.billUploadUiState) {
            is BillUploadUiState.LoadingBillUpload,
            is BillUploadUiState.LoadingImageUpload -> CircularProgressIndicator()
            is BillUploadUiState.BillUploadSuccess,
            is BillUploadUiState.ImageUploadSuccess -> Text("Bill uploaded successfully!")
            is BillUploadUiState.ErrorDuringBillUpload -> Text("Error: ${state.error}")
            is BillUploadUiState.ErrorDuringImageUpload -> Text("Image upload error: ${state.error}")
            else -> {}
        }
    }
}
