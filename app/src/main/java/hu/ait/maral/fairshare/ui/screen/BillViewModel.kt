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
        const val COLLECTION_BILLS = "bills"
    }

    var billUploadUiState: BillUploadUiState by mutableStateOf(BillUploadUiState.Init)

    private val auth: FirebaseAuth = Firebase.auth

    /**
     * Uploads a bill without an image.
     */
    fun uploadBill(
        groupId: String,
        title: String,
        billItems: List<Item>,
        itemAssignments: Map<String, String> = emptyMap(),
        splitMethod: SplitMethod,
        imgUrl: String = "",
        onSuccess: () -> Unit
    ) {
        billUploadUiState = BillUploadUiState.LoadingBillUpload

        val myBill = Bill(
            billId = UUID.randomUUID().toString(),
            groupId = groupId,
            authorId = auth.currentUser?.uid ?: "",
            billTitle = title,
            billDate = Date(System.currentTimeMillis()),
            billItems = billItems,
            itemAssignments = itemAssignments,
            splitMethod = splitMethod,
            imgUrl = imgUrl
        )

        val billsCollection = FirebaseFirestore.getInstance().collection(COLLECTION_BILLS)

        billsCollection.add(myBill)
            .addOnSuccessListener {
                billUploadUiState = BillUploadUiState.BillUploadSuccess
                onSuccess()
            }
            .addOnFailureListener {
                billUploadUiState = BillUploadUiState.ErrorDuringBillUpload(it.message)
            }
    }

    /**
     * Uploads a bill image to Supabase, then creates the bill.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun uploadBillImage(
        groupId: String,
        contentResolver: ContentResolver,
        imageUri: Uri,
        title: String,
        billItems: List<Item>,
        itemAssignments: Map<String, String> = emptyMap(),
        splitMethod: SplitMethod,
        supabase: SupabaseClient = SupabaseProvider.supabase,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                billUploadUiState = BillUploadUiState.LoadingImageUpload

                val source = ImageDecoder.createSource(contentResolver, imageUri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageInBytes = baos.toByteArray()

                val bucket = supabase.storage.from("bills")
                val fileName = "uploads/${System.currentTimeMillis()}.jpg"
                bucket.upload(fileName, imageInBytes)
                val imgUrl = bucket.publicUrl(fileName)

                billUploadUiState = BillUploadUiState.ImageUploadSuccess

                uploadBill(
                    groupId = groupId,
                    title = title,
                    billItems = billItems,
                    itemAssignments = itemAssignments,
                    splitMethod = splitMethod,
                    imgUrl = imgUrl,
                    onSuccess = onSuccess
                )
            } catch (e: Exception) {
                billUploadUiState = BillUploadUiState.ErrorDuringImageUpload(e.message)
            }
        }
    }

    fun loadMembersForGroup(
        groupId: String,
        onResult: (List<Pair<String, String>>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("groups").document(groupId).get()
            .addOnSuccessListener { groupDoc ->
                val memberIds = groupDoc.get("memberIds") as? List<String> ?: emptyList()
                if (memberIds.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val usersCollection = db.collection("users")
                val resultList = mutableListOf<Pair<String, String>>()

                memberIds.forEach { uid ->
                    usersCollection.document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "Unknown"
                            resultList.add(uid to name)

                            if (resultList.size == memberIds.size) {
                                onResult(resultList)
                            }
                        }
                        .addOnFailureListener {
                            resultList.add(uid to "Unknown")
                            if (resultList.size == memberIds.size) {
                                onResult(resultList)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    /**
     * Updates group balances in Firestore after a bill is uploaded.
     * "balances" is stored as Map<String, Double> in EUR.
     */
    fun updateBalance(
        groupId: String,
        billItems: List<Item>,
        itemAssignments: Map<String, String>,
        splitMethod: SplitMethod,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("groups").document(groupId)
        val authorId = auth.currentUser?.uid ?: ""

        db.runTransaction { tx ->
            val groupSnap = tx.get(groupRef)

            val members = groupSnap.get("memberIds") as? List<String> ?: emptyList()
            val existingBalances =
                groupSnap.get("balances") as? Map<String, Double> ?: mapOf()

            val newBalances = existingBalances.toMutableMap()
            members.forEach { uid ->
                if (!newBalances.containsKey(uid)) newBalances[uid] = 0.0
            }

            val total = billItems.sumOf { it.itemPrice }

            when (splitMethod) {
                SplitMethod.EQUAL -> {
                    if (members.isNotEmpty()) {
                        val perPerson = total / members.size
                        members.forEach { uid ->
                            newBalances[uid] = (newBalances[uid] ?: 0.0) + perPerson
                        }
                    }
                }

                SplitMethod.BY_ITEM -> {
                    billItems.forEach { item ->
                        val assignedUid = itemAssignments[item.itemId] ?: return@forEach
                        newBalances[assignedUid] =
                            (newBalances[assignedUid] ?: 0.0) + item.itemPrice
                    }
                }
            }

            // The author fronted the whole bill → negative balance
            newBalances[authorId] = (newBalances[authorId] ?: 0.0) - total

            tx.update(groupRef, "balances", newBalances as Map<String, Double>)
        }
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }
}

/**
 * Separate FileProvider used for image Uris (camera/gallery).
 */
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
            return FileProvider.getUriForFile(
                context,
                authority,
                file,
            )
        }
    }
}
