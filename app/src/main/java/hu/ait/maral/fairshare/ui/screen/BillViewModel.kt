package hu.ait.maral.fairshare.ui.screen

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.Bill
import hu.ait.maral.fairshare.data.Item
import hu.ait.maral.fairshare.data.SplitMethod
import hu.ait.maral.fairshare.data.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date
import java.util.UUID

sealed interface BillUploadUiState {
    object Init : BillUploadUiState
    object LoadingBillUpload : BillUploadUiState
    object BillUploadSuccess : BillUploadUiState
    data class ErrorDuringBillUpload(val error: String?) : BillUploadUiState

    object LoadingImageUpload : BillUploadUiState
    data class ErrorDuringImageUpload(val error: String?) : BillUploadUiState
    object ImageUploadSuccess : BillUploadUiState
}

class BillViewModel : ViewModel() {
    companion object {
        const val COLLECTION_BILLS = "Bills"
    }

    var billUploadUiState: BillUploadUiState
            by mutableStateOf(BillUploadUiState.Init)

    private val auth: FirebaseAuth = Firebase.auth

    /**
     * Uploads a bill without an image
     */
    fun uploadBill(
        title: String,
        billItems: List<Item>,
        itemAssignments: Map<String, String> = mapOf(),
        splitMethod: SplitMethod = SplitMethod.EQUAL,
        imgUrl: String = ""
    ) {
        billUploadUiState = BillUploadUiState.LoadingBillUpload

        val myBill = Bill(
            billId = UUID.randomUUID().toString(),
            billTitle = title,
            billItems = billItems,
            itemAssignments = itemAssignments,
            splitMethod = splitMethod,
            billDate = Date(System.currentTimeMillis()),
            authorId = auth.currentUser?.uid ?: "",
            imgUrl = imgUrl
        )

        val billsCollection = FirebaseFirestore.getInstance().collection(COLLECTION_BILLS)

        billsCollection.add(myBill)
            .addOnSuccessListener {
                billUploadUiState = BillUploadUiState.BillUploadSuccess
            }
            .addOnFailureListener {
                billUploadUiState = BillUploadUiState.ErrorDuringBillUpload(it.message)
            }
    }

    /**
     * Uploads a bill image to Supabase, then creates the bill
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun uploadBillImage(
        contentResolver: ContentResolver,
        imageUri: Uri,
        title: String,
        billItems: List<Item>,
        itemAssignments: Map<String, String> = mapOf(),
        splitMethod: SplitMethod = SplitMethod.EQUAL,
        supabase: SupabaseClient = SupabaseProvider.supabase
    ) {
        viewModelScope.launch {
            try {
                billUploadUiState = BillUploadUiState.LoadingImageUpload

                // Decode image
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageInBytes = baos.toByteArray()

                // Upload image to Supabase
                val bucket = supabase.storage.from("bills")
                val fileName = "uploads/${System.currentTimeMillis()}.jpg"
                bucket.upload(fileName, imageInBytes)
                val imgUrl = bucket.publicUrl(fileName)

                billUploadUiState = BillUploadUiState.ImageUploadSuccess

                // Upload the bill including the image URL
                uploadBill(
                    title = title,
                    billItems = billItems,
                    itemAssignments = itemAssignments,
                    splitMethod = splitMethod,
                    imgUrl = imgUrl
                )
            } catch (e: Exception) {
                billUploadUiState = BillUploadUiState.ErrorDuringImageUpload(e.message)
            }
        }
    }
}


class ComposeFileProvider : FileProvider(
    hu.ait.maral.fairshare.R.xml.filepaths
) {
    companion object {
        fun getImageUri(context: Context): Uri {
            val directory = File(context.cacheDir, "images")
            directory.mkdirs()
            val file = File.createTempFile(
                "selected_image_",
                ".jpg",
                directory,
            )
            val authority = context.packageName + ".fileprovider"
            return getUriForFile(
                context,
                authority,
                file,
            )
        }
    }
}