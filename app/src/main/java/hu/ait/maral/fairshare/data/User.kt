package hu.ait.maral.fairshare.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val profilePictureUrl: String? = null,
    // "Zelle" -> "@username"
    val paymentMethods: Map<String, String> = emptyMap(),
    // expense roomIDs the user is part of
    val rooms: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)
