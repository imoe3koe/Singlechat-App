package com.singlechat.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("SingleChatPrefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    // Session State
    private val _currentGroupToken = MutableStateFlow(prefs.getString("GROUP_TOKEN", null))
    val currentGroupToken: StateFlow<String?> = _currentGroupToken.asStateFlow()

    private val _currentUserEmail = MutableStateFlow(prefs.getString("USER_EMAIL", null))
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _currentUserId = MutableStateFlow(prefs.getString("USER_ID", null))
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _currentUserName = MutableStateFlow(prefs.getString("USER_NAME", null))
    val currentUserName: StateFlow<String?> = _currentUserName.asStateFlow()

    private val _currentUserRole = MutableStateFlow<String>(prefs.getString("USER_ROLE", "Anggota") ?: "Anggota")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("IS_DARK_THEME", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Data lists
    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private val _members = MutableStateFlow<List<MemberModel>>(emptyList())
    val members: StateFlow<List<MemberModel>> = _members.asStateFlow()

    init {
        val token = _currentGroupToken.value
        if (!token.isNullOrEmpty()) {
            loadGroupData(token)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return !_currentGroupToken.value.isNullOrEmpty() && !_currentUserEmail.value.isNullOrEmpty()
    }

    fun login(email: String, groupToken: String, name: String? = null): Boolean {
        val token = groupToken.trim().uppercase()
        val userEmail = email.trim()
        val userId = prefs.getString("USER_ID", null) ?: UUID.randomUUID().toString()
        val userName = name ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

        prefs.edit()
            .putString("GROUP_TOKEN", token)
            .putString("USER_EMAIL", userEmail)
            .putString("USER_ID", userId)
            .putString("USER_NAME", userName)
            .putString("USER_ROLE", "Anggota")
            .apply()

        _currentGroupToken.value = token
        _currentUserEmail.value = userEmail
        _currentUserId.value = userId
        _currentUserName.value = userName
        _currentUserRole.value = "Anggota"

        loadGroupData(token)
        ensureMemberExists(userId, userEmail, userName, "Anggota", token)
        return true
    }

    fun register(email: String, providedToken: String?, name: String? = null): String {
        val userEmail = email.trim()
        val userId = UUID.randomUUID().toString()
        val isNewGroup = providedToken.isNullOrBlank()
        val token = if (isNewGroup) {
            "SC-" + UUID.randomUUID().toString().take(6).uppercase()
        } else {
            providedToken.trim().uppercase()
        }
        val role = if (isNewGroup) "Ketua" else "Anggota"
        val userName = name ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

        prefs.edit()
            .putString("GROUP_TOKEN", token)
            .putString("USER_EMAIL", userEmail)
            .putString("USER_ID", userId)
            .putString("USER_NAME", userName)
            .putString("USER_ROLE", role)
            .apply()

        _currentGroupToken.value = token
        _currentUserEmail.value = userEmail
        _currentUserId.value = userId
        _currentUserName.value = userName
        _currentUserRole.value = role

        loadGroupData(token)
        ensureMemberExists(userId, userEmail, userName, role, token)
        return token
    }

    fun logout() {
        prefs.edit()
            .remove("GROUP_TOKEN")
            .remove("USER_EMAIL")
            .remove("USER_ID")
            .remove("USER_NAME")
            .remove("USER_ROLE")
            .apply()

        _currentGroupToken.value = null
        _currentUserEmail.value = null
        _currentUserId.value = null
        _currentUserName.value = null
        _currentUserRole.value = "Anggota"
        _messages.value = emptyList()
        _members.value = emptyList()
    }

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
        prefs.edit().putBoolean("IS_DARK_THEME", newTheme).apply()
    }

    fun updateDisplayName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        _currentUserName.value = trimmed
        prefs.edit().putString("USER_NAME", trimmed).apply()

        // Update in members list as well
        val userId = _currentUserId.value ?: return
        val updated = _members.value.map {
            if (it.id == userId) it.copy(name = trimmed, avatarInitial = trimmed.take(1).uppercase()) else it
        }
        _members.value = updated
        persistMembers(_currentGroupToken.value ?: "", updated)
    }

    fun sendMessage(content: String, messageType: String = "text") {
        val token = _currentGroupToken.value ?: return
        val senderId = _currentUserId.value ?: UUID.randomUUID().toString()
        val senderEmail = _currentUserEmail.value ?: "user@singlechat.io"
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val newMessage = MessageModel(
            id = UUID.randomUUID().toString(),
            group_token = token,
            sender_id = senderId,
            sender_email = senderEmail,
            message_type = messageType,
            content = content,
            created_at = timeNow
        )

        val updated = _messages.value + newMessage
        _messages.value = updated
        persistMessages(token, updated)
    }

    private fun loadGroupData(token: String) {
        // Load messages
        val savedMessagesJson = prefs.getString("MESSAGES_$token", null)
        val loadedMessages = if (savedMessagesJson != null) {
            try {
                json.decodeFromString<List<MessageModel>>(savedMessagesJson)
            } catch (e: Exception) {
                getSeedMessages(token)
            }
        } else {
            getSeedMessages(token)
        }
        _messages.value = loadedMessages
        if (savedMessagesJson == null) {
            persistMessages(token, loadedMessages)
        }

        // Load members
        val savedMembersJson = prefs.getString("MEMBERS_$token", null)
        val loadedMembers = if (savedMembersJson != null) {
            try {
                json.decodeFromString<List<MemberModel>>(savedMembersJson)
            } catch (e: Exception) {
                getSeedMembers(token)
            }
        } else {
            getSeedMembers(token)
        }
        _members.value = loadedMembers
        if (savedMembersJson == null) {
            persistMembers(token, loadedMembers)
        }
    }

    private fun ensureMemberExists(userId: String, email: String, name: String, role: String, token: String) {
        val currentList = _members.value
        if (currentList.none { it.id == userId || it.email == email }) {
            val newMember = MemberModel(
                id = userId,
                email = email,
                name = name,
                role = role,
                group_token = token,
                isOnline = true,
                avatarInitial = name.take(1).uppercase()
            )
            val updated = listOf(newMember) + currentList
            _members.value = updated
            persistMembers(token, updated)
        }
    }

    private fun persistMessages(token: String, list: List<MessageModel>) {
        try {
            val encoded = json.encodeToString(list)
            prefs.edit().putString("MESSAGES_$token", encoded).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistMembers(token: String, list: List<MemberModel>) {
        try {
            val encoded = json.encodeToString(list)
            prefs.edit().putString("MEMBERS_$token", encoded).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSeedMessages(token: String): List<MessageModel> {
        return listOf(
            MessageModel(
                id = "seed-1",
                group_token = token,
                sender_id = "bot-leader",
                sender_email = "ketua@singlechat.io",
                message_type = "text",
                content = "Selamat datang di ruang obrolan kelompok $token! Satu kelompok, satu obrolan.",
                created_at = "09:00"
            ),
            MessageModel(
                id = "seed-2",
                group_token = token,
                sender_id = "bot-member-1",
                sender_email = "sarah@singlechat.io",
                message_type = "text",
                content = "Halo semua! Siap untuk koordinasi tugas hari ini.",
                created_at = "09:05"
            ),
            MessageModel(
                id = "seed-3",
                group_token = token,
                sender_id = "bot-member-2",
                sender_email = "andi@singlechat.io",
                message_type = "pdf",
                content = "Lampiran: Dokumen_Rencana_Proyek_2026.pdf",
                created_at = "09:12"
            )
        )
    }

    private fun getSeedMembers(token: String): List<MemberModel> {
        return listOf(
            MemberModel(
                id = "bot-leader",
                email = "ketua@singlechat.io",
                name = "Ahmad Ketua",
                role = "Ketua",
                group_token = token,
                isOnline = true,
                avatarInitial = "A"
            ),
            MemberModel(
                id = "bot-member-1",
                email = "sarah@singlechat.io",
                name = "Sarah Miller",
                role = "Anggota",
                group_token = token,
                isOnline = true,
                avatarInitial = "S"
            ),
            MemberModel(
                id = "bot-member-2",
                email = "andi@singlechat.io",
                name = "Andi Pratama",
                role = "Anggota",
                group_token = token,
                isOnline = false,
                avatarInitial = "P"
            )
        )
    }
}
