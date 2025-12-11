package hu.ait.maral.fairshare.ui.screen.bill.ai

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.ImagePart
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.TextPart
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.BuildConfig.GEMINI_API_KEY
import hu.ait.maral.fairshare.data.Bill
import hu.ait.maral.fairshare.data.Item
import hu.ait.maral.fairshare.data.SplitMethod
import hu.ait.maral.fairshare.data.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

sealed interface AiBillUiState {
    object Init : AiBillUiState
    object LoadingAI : AiBillUiState
    data class AIError(val message: String?) : AiBillUiState
    object AIResultReady : AiBillUiState
    object Uploading : AiBillUiState
    object UploadSuccess : AiBillUiState
    data class UploadError(val message: String?) : AiBillUiState
}

class AiBillReaderViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _aiItems = MutableStateFlow<List<Item>>(emptyList())
    val aiItems = _aiItems.asStateFlow()

    private val _uiState = MutableStateFlow<AiBillUiState>(AiBillUiState.Init)
    val uiState = _uiState.asStateFlow()

    private val genModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = GEMINI_API_KEY,
        safetySettings = listOf(
            SafetySetting(
                HarmCategory.HARASSMENT,
                BlockThreshold.LOW_AND_ABOVE
            ),
            SafetySetting(
                HarmCategory.HATE_SPEECH,
                BlockThreshold.LOW_AND_ABOVE
            ),
        )
    )

    fun scanReceiptWithAI(bitmap: Bitmap) {
        _uiState.value = AiBillUiState.LoadingAI

        viewModelScope.launch {
            try {
                val prompt =
                    "Check if this image is a receipt. If it is a receipt, extract all items. " +
                            "Return ONLY JSON in this exact format if it is a receipt:\n" +
                            "{ \"items\": [ { \"name\": \"Item name\", \"price\": 0.00 } ] }\n" +
                            "If this is NOT a receipt, return: { \"items\": [], \"notReceipt\": true }.\n" +
                            "Do not add commentary."


                val inputContent = Content(
                    role = "user",
                    parts = listOf(
                        ImagePart(bitmap),
                        TextPart(text = prompt)
                    )
                )

                var fullResponse = ""
                genModel.generateContentStream(inputContent).collect { chunk ->
                    fullResponse += chunk.text
                }

                // Parse JSON
                val root = JSONObject(fullResponse.trim().removePrefix("```json").removeSuffix("```").trim())
                val notReceipt = root.optBoolean("notReceipt", false)

                if (notReceipt) {
                    _aiItems.value = emptyList()
                    _uiState.value = AiBillUiState.AIError("This is not a receipt")
                } else {
                    _aiItems.value = parseItemsJson(fullResponse)
                    _uiState.value = AiBillUiState.AIResultReady
                }

            } catch (e: Exception) {
                _uiState.value = AiBillUiState.AIError(e.message)
            }
        }
    }

    private fun parseItemsJson(jsonString: String): List<Item> {
        val cleaned = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val root = JSONObject(cleaned)
        val arr: JSONArray = root.getJSONArray("items")

        val result = mutableListOf<Item>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.optString("name", "")
            val price = obj.optDouble("price", 0.0)

            result.add(
                Item(
                    itemName = name,
                    itemPrice = price,
                    itemCurrency = "EUR"
                )
            )
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun uploadAIBill(
        groupId: String,
        contentResolver: ContentResolver,
        bitmap: Bitmap?,
        billTitle: String,
        billDate: Date,
        billItems: List<Item>,
        splitMethod: SplitMethod,
        supabase: SupabaseClient = SupabaseProvider.supabase,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = AiBillUiState.Uploading

                var imgUrl = ""

                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val imageBytes = baos.toByteArray()

                    val bucket = supabase.storage.from("bills")
                    val fileName = "uploads/${System.currentTimeMillis()}.jpg"

                    // Upload image
                    bucket.upload(fileName, imageBytes)

                    // Use the same method as your working code
                    imgUrl = bucket.publicUrl(fileName)
                }

                val myBill = Bill(
                    billId = UUID.randomUUID().toString(),
                    groupId = groupId,
                    authorId = auth.currentUser?.uid ?: "",
                    billTitle = billTitle,
                    billDate = billDate,
                    billItems = billItems,
                    itemAssignments = emptyMap(),
                    splitMethod = splitMethod,
                    imgUrl = imgUrl
                )

                val billsCollection = FirebaseFirestore.getInstance().collection("bills")
                billsCollection.add(myBill)
                    .addOnSuccessListener {
                        _uiState.value = AiBillUiState.UploadSuccess
                        onSuccess()
                    }
                    .addOnFailureListener {
                        _uiState.value = AiBillUiState.UploadError(it.message)
                    }

            } catch (e: Exception) {
                _uiState.value = AiBillUiState.UploadError(e.message)
            }
        }
    }
}