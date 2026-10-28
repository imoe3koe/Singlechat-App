package com.singlechat.data

import kotlinx.serialization.Serializable

@Serializable
data class MessageModel(
    val id: String? = null,
    val group_token: String,
    val sender_id: String,
    val sender_email: String,
    val message_type: String, // 'text', 'image', 'pdf'
    val content: String,
    val created_at: String? = null
)
