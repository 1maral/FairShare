package hu.ait.maral.fairshare.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val profilePictureUrl: String? = null,
    val preferredCurrency: String = "EUR",
    val paymentMethods: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)