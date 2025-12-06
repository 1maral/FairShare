package hu.ait.maral.fairshare.data

data class Group(
    val groupId: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val pendingMemberIds: List<String> = emptyList(),
    val balances: Map<String, Double> = emptyMap<String, Double>()
)
