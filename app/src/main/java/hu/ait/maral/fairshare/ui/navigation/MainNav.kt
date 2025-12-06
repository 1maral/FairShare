package hu.ait.maral.fairshare.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable


@Serializable
data object LoginScreenKey: NavKey

@Serializable
data class SignUpScreenKey(val email: String, val password: String): NavKey

@Serializable
data object HomeScreenKey : NavKey

@Serializable
data object NotificationScreenKey : NavKey

@Serializable
data object SplashScreenKey : NavKey

@Serializable
data class RoomScreenKey(val groupId: String): NavKey

@Serializable
data object ProfileScreenKey: NavKey


@Serializable
data object BillScreenKey: NavKey
