package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SigchaDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserSync(): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // --- Chats ---
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :time WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    // --- Posts ---
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("UPDATE posts SET likesCount = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLike(postId: String, likes: Int, isLiked: Boolean)
}

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        PostEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SigchaDatabase : RoomDatabase() {
    abstract fun dao(): SigchaDao
}
