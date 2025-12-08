package hu.ait.maral.fairshare.ui.screen.bill.ai


import hu.ait.maral.fairshare.data.Item
import org.json.JSONObject

object ReceiptParser {
    data class ParsedReceipt(
        val items: List<Item>,
        val total: Double
    )

    fun parseItemsJson(json: String): ParsedReceipt {
        val obj = JSONObject(json)
        val itemsArray = obj.getJSONArray("items")
        val total = obj.getDouble("totalOnReceipt")

        val items = mutableListOf<Item>()
        for (i in 0 until itemsArray.length()) {
            val it = itemsArray.getJSONObject(i)
            items.add(
                Item(
                    itemName = it.getString("itemName"),
                    itemPrice = it.getDouble("itemPrice")
                )
            )
        }

        return ParsedReceipt(items, total)
    }
}