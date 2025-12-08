package hu.ait.maral.fairshare

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import hu.ait.maral.fairshare.ui.navigation.AiBillReaderScreenKey
import hu.ait.maral.fairshare.ui.navigation.BillScreenKey
import hu.ait.maral.fairshare.ui.navigation.HomeScreenKey
import hu.ait.maral.fairshare.ui.navigation.LoginScreenKey
import hu.ait.maral.fairshare.ui.navigation.NotificationScreenKey
import hu.ait.maral.fairshare.ui.navigation.ProfileScreenKey
import hu.ait.maral.fairshare.ui.navigation.RoomScreenKey
import hu.ait.maral.fairshare.ui.navigation.SignUpScreenKey
import hu.ait.maral.fairshare.ui.navigation.SplashScreenKey
import hu.ait.maral.fairshare.ui.screen.BillScreen
import hu.ait.maral.fairshare.ui.screen.RoomScreen
import hu.ait.maral.fairshare.ui.screen.home.HomeScreen
import hu.ait.maral.fairshare.ui.screen.notifications.NotificationsScreen
import hu.ait.maral.fairshare.ui.screen.profile.ProfileScreen
import hu.ait.maral.fairshare.ui.screen.start.AiBillReaderScreen
import hu.ait.maral.fairshare.ui.screen.start.LoginScreen
import hu.ait.maral.fairshare.ui.screen.start.SignUpScreen
import hu.ait.maral.fairshare.ui.screen.start.SplashScreen
import hu.ait.maral.fairshare.ui.theme.FairShareTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FairShareTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun NavGraph(modifier: Modifier) {
    val backStack = rememberNavBackStack(SplashScreenKey)
    //val backStack = rememberNavBackStack(AiBillReaderScreenKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<SplashScreenKey> {
                SplashScreen(
                    onTimeout = {
                        backStack.removeLastOrNull()
                        backStack.add(LoginScreenKey)
                    }
                )
            }
            entry<LoginScreenKey> {
                LoginScreen(
                    onLoginSuccess = {
                        backStack.add(HomeScreenKey)
                    },
                    onNavigateToRegister = {email, password ->
                        backStack.add(SignUpScreenKey(email, password))
                    }
                )
            }

            entry<SignUpScreenKey> { route ->
                SignUpScreen(
                    defaultEmail = route.email,
                    defaultPassword = route.password,
                    onRegisterSuccess = { backStack.add(HomeScreenKey)},
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<HomeScreenKey> {

                HomeScreen(onNotificationsClick = {
                    backStack.add(NotificationScreenKey)
                }, onRoomClick = { groupId ->
                    backStack.add(RoomScreenKey(groupId))
                }, onProfileClick = {backStack.add(ProfileScreenKey)})
            }

            entry<NotificationScreenKey>{
                NotificationsScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<RoomScreenKey> { key ->
                RoomScreen(groupId = key.groupId,
                    onAddBillClick = { backStack.add(BillScreenKey(key.groupId)) })
            }

            entry<ProfileScreenKey> { key -> ProfileScreen(onSaveClick = {backStack.add(
                HomeScreenKey)})}


            entry<BillScreenKey> { key ->
                BillScreen(key.groupId,
                    onBack = { backStack.removeLastOrNull() },
                    onUploadSuccess = { backStack.removeLastOrNull() })
            }

            entry<AiBillReaderScreenKey> {
                AiBillReaderScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onUploadSuccess = { backStack.removeLastOrNull() }
                )
            }

        }
    )
}