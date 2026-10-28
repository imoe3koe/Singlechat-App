package com.singlechat.data

import kotlinx.serialization.Serializable

@Serializable
data class MemberModel(
    val id: String,
    val email: String,
    val name: String,
    val role: String, // "Ketua" or "Anggota"
    val group_token: String,
    val isOnline: Boolean = true,
    val avatarInitial: String = name.take(1).uppercase()
)
