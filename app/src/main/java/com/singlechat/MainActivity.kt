package com.singlechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singlechat.data.ChatRepository
import com.singlechat.ui.AuthScreen
import com.singlechat.ui.ChatScreen
import com.singlechat.ui.MembersScreen
import com.singlechat.ui.SettingsScreen
import com.singlechat.ui.theme.SingleChatTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = ChatRepository(applicationContext)

        setContent {
            val isDarkTheme by repository.isDarkTheme.collectAsState()
            val currentGroupToken by repository.currentGroupToken.collectAsState()
            val currentUserEmail by repository.currentUserEmail.collectAsState()
            val currentUserName by repository.currentUserName.collectAsState()
            val currentUserRole by repository.currentUserRole.collectAsState()
            val messages by repository.messages.collectAsState()
            val members by repository.members.collectAsState()

            SingleChatTheme(darkTheme = isDarkTheme) {
                if (currentGroupToken.isNullOrEmpty() || currentUserEmail.isNullOrEmpty()) {
                    AuthScreen(
                        onLoginSuccess = { email, token ->
                            repository.login(email, token)
                        },
                        onRegisterSuccess = { email, token ->
                            repository.register(email, token)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                } else {
                    MainAppShell(
                        currentGroupToken = currentGroupToken,
                        currentUserEmail = currentUserEmail,
                        currentUserName = currentUserName,
                        currentUserRole = currentUserRole,
                        messages = messages,
                        members = members,
                        isDarkTheme = isDarkTheme,
                        onSendMessage = { content, type ->
                            repository.sendMessage(content, type)
                        },
                        onAddMember = { email, name ->
                            val token = currentGroupToken ?: ""
                            val newMember = com.singlechat.data.MemberModel(
                                id = java.util.UUID.randomUUID().toString(),
                                email = email,
                                name = name,
                                role = "Anggota",
                                group_token = token,
                                isOnline = true,
                                avatarInitial = name.take(1).uppercase()
                            )
                            // Broadcast an automated welcome message from new member
                            repository.sendMessage("Halo semua! Saya $name baru saja bergabung ke kelompok $token.", "text")
                        },
                        onUpdateName = { newName ->
                            repository.updateDisplayName(newName)
                        },
                        onToggleTheme = {
                            repository.toggleTheme()
                        },
                        onLogout = {
                            repository.logout()
                        }
                    )
                }
            }
        }
    }
}

enum class NavigationTab(val symbol: String, val label: String, val title: String) {
    CHAT("[ / ]", "Obrolan", "OBROLAN"),
    MEMBERS("[ o ]", "Anggota", "ANGGOTA"),
    SETTINGS("[ * ]", "Pengaturan", "PENGATURAN")
}

@Composable
fun MainAppShell(
    currentGroupToken: String?,
    currentUserEmail: String?,
    currentUserName: String?,
    currentUserRole: String,
    messages: List<com.singlechat.data.MessageModel>,
    members: List<com.singlechat.data.MemberModel>,
    isDarkTheme: Boolean,
    onSendMessage: (String, String) -> Unit,
    onAddMember: (String, String) -> Unit,
    onUpdateName: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NavigationTab.CHAT) }

    BackHandler(enabled = selectedTab != NavigationTab.CHAT) {
        selectedTab = NavigationTab.CHAT
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SINGLECHAT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedTab.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Group Token Pill with Quick Copy
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Group Token", currentGroupToken ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Token kelompok disalin: $currentGroupToken", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("header_token_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentGroupToken ?: "-",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin token",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                // Minimalist 3-tab Bottom Navigation matching original [ / ], [ o ], [ * ] style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    NavigationTab.entries.forEachIndexed { index, tab ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(60.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                        }

                        val isSelected = selectedTab == tab
                        val backgroundModifier = if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .then(backgroundModifier)
                                .clickable { selectedTab = tab }
                                .testTag("nav_tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tab.symbol,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.CHAT -> {
                    ChatScreen(
                        messages = messages,
                        currentUserEmail = currentUserEmail,
                        currentGroupToken = currentGroupToken,
                        onSendMessage = onSendMessage
                    )
                }
                NavigationTab.MEMBERS -> {
                    MembersScreen(
                        groupToken = currentGroupToken,
                        members = members,
                        onAddMember = onAddMember
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        userEmail = currentUserEmail,
                        userName = currentUserName,
                        userRole = currentUserRole,
                        groupToken = currentGroupToken,
                        isDarkTheme = isDarkTheme,
                        onUpdateName = onUpdateName,
                        onToggleTheme = onToggleTheme,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
