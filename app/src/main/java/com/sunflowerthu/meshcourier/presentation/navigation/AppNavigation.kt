
package com.sunflowerthu.meshcourier.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sunflowerthu.meshcourier.R
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sunflowerthu.meshcourier.presentation.screens.chat.ChatScreen
import com.sunflowerthu.meshcourier.presentation.screens.contacts.ContactsScreen
import com.sunflowerthu.meshcourier.presentation.screens.conversations.ConversationsScreen
import com.sunflowerthu.meshcourier.presentation.screens.nearby.NearbyNodesScreen
import com.sunflowerthu.meshcourier.presentation.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Conversations : Screen("conversations")
    object Chat : Screen("chat/{contactNodeId}") {
        fun createRoute(contactNodeId: String) = "chat/$contactNodeId"
    }
    object Nearby : Screen("nearby")
    object Settings : Screen("settings")
    object Contacts : Screen("contacts")
}

val bottomNavItems = listOf(
    Triple(Screen.Conversations, Icons.Default.Forum, R.string.nav_chats),
    Triple(Screen.Nearby, Icons.Default.NetworkWifi, R.string.nav_nearby),
    Triple(Screen.Settings, Icons.Default.Settings, R.string.nav_settings),
)

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Conversations.route, modifier = modifier) {
        composable(Screen.Conversations.route) {
            ConversationsScreen(onOpenChat = { contactNodeId ->
                navController.navigate(Screen.Chat.createRoute(contactNodeId))
            })
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("contactNodeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactNodeId = backStackEntry.arguments?.getString("contactNodeId") ?: return@composable
            ChatScreen(
                contactNodeId = contactNodeId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Nearby.route) {
            NearbyNodesScreen(
                onOpenChat = { contactNodeId ->
                    navController.navigate(Screen.Chat.createRoute(contactNodeId))
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onOpenContacts = { navController.navigate(Screen.Contacts.route) })
        }
        composable(Screen.Contacts.route) {
            ContactsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun BottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { (screen, icon, labelRes) ->
            val label = stringResource(labelRes)
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Conversations.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
