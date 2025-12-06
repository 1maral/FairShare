package hu.ait.maral.fairshare.data

import java.util.Date
import java.util.UUID

data class Bill(
    var billId: String = "",
    var groupId: String = "",
    var authorId: String = "",
    var billTitle: String = "",
    var billDate: Date,
    var billItems: List<Item> = listOf(),
    var itemAssignments: Map<String, List<String>> = mapOf(),
    var splitMethod: SplitMethod = SplitMethod.BY_ITEM,
    var imgUrl: String = ""
)
data class Item(
    var itemName: String = "",
    var itemPrice: Double = 0.0,
    val itemId: String = UUID.randomUUID().toString(),
)
enum class SplitMethod {
    EQUAL,        // split equally among users
    BY_ITEM,      // each user pays for specific items
}
