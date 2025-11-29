package hu.ait.maral.fairshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import hu.ait.maral.fairshare.ui.navigation.LoginScreenKey
import hu.ait.maral.fairshare.ui.navigation.SignUpScreenKey
import hu.ait.maral.fairshare.ui.screen.start.LoginScreen
import hu.ait.maral.fairshare.ui.screen.start.SignUpScreen
import hu.ait.maral.fairshare.ui.theme.FairShareTheme

class MainActivity : ComponentActivity() {
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
@Composable
fun NavGraph(modifier: Modifier) {
    val backStack = rememberNavBackStack(LoginScreenKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {

            // LOGIN SCREEN
            entry<LoginScreenKey> {
                LoginScreen(
                    onLoginSuccess = {
                        // TODO: navigate to home later
                    },
                    onNavigateToRegister = {
                        backStack.add(SignUpScreenKey)
                    }
                )
            }

            // SIGNUP SCREEN
            entry<SignUpScreenKey> {
                SignUpScreen(
                    // return to login
                    onRegisterSuccess = {
                        backStack.removeLastOrNull()
                    },
                    // return to login
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}
