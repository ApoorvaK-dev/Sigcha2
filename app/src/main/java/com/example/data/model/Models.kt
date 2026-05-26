package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val statusMessage: String,
    val avatarUrl: String,
    val isCurrentUser: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val avatarUrl: String = ""
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val status: Int = 3 // 0 = Sending, 1 = Sent, 2 = Delivered, 3 = Read
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val timestamp: Long,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false
)
