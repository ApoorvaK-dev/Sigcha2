package com.example.data.repository

import android.content.Context
import com.example.data.local.SigchaDao
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserEntity
import com.example.data.remote.SigchaFirestoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class SigchaRepository(
    private val dao: SigchaDao,
    val firestoreManager: SigchaFirestoreManager,
    context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val allChats: Flow<List<ChatEntity>> = dao.getAllChats()
    val allPosts: Flow<List<PostEntity>> = dao.getAllPosts()
    val currentUser: Flow<UserEntity?> = dao.getCurrentUser()

    // Map to track firestore real-time message active listeners so we don't duplicate them
    private val activeListeners = mutableSetOf<String>()

    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        // If Firestore is connected, register a real-time snapshot listener
        if (firestoreManager.isConnected.value && !activeListeners.contains(chatId)) {
            activeListeners.add(chatId)
            firestoreManager.listenForMessages(chatId) { incomingMsgs ->
                repositoryScope.launch {
                    dao.insertMessages(incomingMsgs)
                    // Update main chat's last message
                    if (incomingMsgs.isNotEmpty()) {
                        val last = incomingMsgs.last()
                        dao.updateLastMessage(chatId, last.content, last.timestamp)
                    }
                }
            }
        }
        return dao.getMessagesForChat(chatId)
    }

    fun startPostSync() {
        if (firestoreManager.isConnected.value) {
            firestoreManager.listenForPosts { incomingPosts ->
                repositoryScope.launch {
                    dao.insertPosts(incomingPosts)
                }
            }
        }
    }

    suspend fun initializeDefaultDataIfNeeded() {
        val hasUser = dao.getCurrentUserSync()
        if (hasUser == null) {
            // Create current user
            val me = UserEntity(
                id = "me",
                displayName = "Sigcha User",
                statusMessage = "Sleek and connection-focused.",
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                isCurrentUser = true
            )
            dao.insertUser(me)

            // Setup mock contacts and chats
            val dummyChats = listOf(
                ChatEntity("chat_anya", "Anya (Designer)", false, "Hey! How does the Sigcha UI look so far?", System.currentTimeMillis() - 3600000, 0),
                ChatEntity("chat_elon", "Tony (Innovator)", false, "We need to scale this app serverless.", System.currentTimeMillis() - 7200000, 1),
                ChatEntity("chat_group_main", "Sigcha Dev Group", true, "Welcome to the Sigcha Hub!", System.currentTimeMillis() - 86400000, 0)
            )
            dao.insertChats(dummyChats)

            // Setup mock users
            dao.insertUser(UserEntity("anya", "Anya (Designer)", "Designing pixel-perfect screens", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80"))
            dao.insertUser(UserEntity("tony", "Tony (Innovator)", "Iterating on rocket speed", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80"))

            // Setup inaugural messages
            val inauguralMessages = listOf(
                MessageEntity(UUID.randomUUID().toString(), "chat_anya", "anya", "Anya (Designer)", "Welcome to Sigcha! I've styled the backgrounds with gradient meshes.", System.currentTimeMillis() - 4000000),
                MessageEntity(UUID.randomUUID().toString(), "chat_anya", "anya", "Anya (Designer)", "Hey! How does the Sigcha UI look so far?", System.currentTimeMillis() - 3600000),
                MessageEntity(UUID.randomUUID().toString(), "chat_elon", "tony", "Tony (Innovator)", "First prototype of Sigcha compiles flawlessly.", System.currentTimeMillis() - 8000000),
                MessageEntity(UUID.randomUUID().toString(), "chat_elon", "tony", "Tony (Innovator)", "We need to scale this app serverless.", System.currentTimeMillis() - 7200000),
                MessageEntity(UUID.randomUUID().toString(), "chat_group_main", "anya", "Anya (Designer)", "Hey everyone, welcome to Sigcha group!", System.currentTimeMillis() - 90000000),
                MessageEntity(UUID.randomUUID().toString(), "chat_group_main", "tony", "Tony (Innovator)", "Sigcha has a high-octane Local Cache engine.", System.currentTimeMillis() - 88000000),
                MessageEntity(UUID.randomUUID().toString(), "chat_group_main", "system", "System", "Welcome to the Sigcha Hub!", System.currentTimeMillis() - 86400000)
            )
            dao.insertMessages(inauguralMessages)

            // Setup inaugural social posts
            val inauguralPosts = listOf(
                PostEntity(
                    "post_1",
                    "Anya (Designer)",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                    "Design isn't just what it looks like and feels like. Design is how it works! Super proud of the tactile ripple feedback in #Sigcha. 🎨📱",
                    System.currentTimeMillis() - 10000000,
                    12,
                    3,
                    false
                ),
                PostEntity(
                    "post_2",
                    "Tony (Innovator)",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80",
                    "Why do most social media apps consume so much battery? We engineered Sigcha's database wrapper to process transactions under 12 milliseconds. Pure speed. 🚀⚡",
                    System.currentTimeMillis() - 25000000,
                    48,
                    14,
                    false
                )
            )
            dao.insertPosts(inauguralPosts)
        }
    }

    suspend fun insertUser(user: UserEntity) {
        dao.insertUser(user)
    }

    suspend fun signUp(
        email: String,
        passwordPlain: String,
        username: String,
        profilePictureUrl: String,
        bio: String,
        displayName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val passwordHash = firestoreManager.hashPassword(passwordPlain)
        val user = UserEntity(
            id = email,
            displayName = displayName.ifEmpty { username },
            statusMessage = bio,
            avatarUrl = profilePictureUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80" },
            isCurrentUser = false,
            username = username,
            bio = bio,
            profilePictureUrl = profilePictureUrl,
            passwordHash = passwordHash
        )

        if (firestoreManager.isConnected.value) {
            firestoreManager.registerUserInFirestore(
                email = email,
                passwordPlain = passwordPlain,
                username = username,
                profilePictureUrl = user.avatarUrl,
                bio = bio,
                displayName = user.displayName
            ) { success, errMsg ->
                if (success) {
                    repositoryScope.launch {
                        dao.insertUser(user)
                    }
                }
                onResult(success, errMsg)
            }
        } else {
            val existing = dao.getUserByEmail(email)
            if (existing != null) {
                onResult(false, "User with this email already exists locally.")
                return
            }
            dao.insertUser(user)
            onResult(true, null)
        }
    }

    suspend fun login(
        email: String,
        passwordPlain: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (firestoreManager.isConnected.value) {
            firestoreManager.loginUserInFirestore(email, passwordPlain) { user, errMsg ->
                if (user != null) {
                    repositoryScope.launch {
                        dao.clearCurrentUser()
                        dao.insertUser(user)
                        onResult(true, null)
                    }
                } else {
                    onResult(false, errMsg)
                }
            }
        } else {
            val user = dao.getUserByEmail(email)
            if (user != null) {
                val passwordHash = firestoreManager.hashPassword(passwordPlain)
                if (user.passwordHash == passwordHash) {
                    dao.clearCurrentUser()
                    dao.insertUser(user.copy(isCurrentUser = true))
                    onResult(true, null)
                } else {
                    onResult(false, "Invalid secure credentials.")
                }
            } else {
                onResult(false, "Account not found locally.")
            }
        }
    }

    suspend fun logout() {
        dao.clearCurrentUser()
    }

    suspend fun updateProfile(
        username: String,
        profilePictureUrl: String,
        bio: String,
        displayName: String,
        onResult: (Boolean) -> Unit
    ) {
        val currentUser = dao.getCurrentUserSync()
        if (currentUser == null) {
            onResult(false)
            return
        }

        val updated = currentUser.copy(
            username = username,
            displayName = displayName,
            avatarUrl = profilePictureUrl,
            profilePictureUrl = profilePictureUrl,
            statusMessage = bio,
            bio = bio
        )

        dao.insertUser(updated)

        if (firestoreManager.isConnected.value) {
            firestoreManager.updateUserProfileInFirestore(
                email = currentUser.id,
                username = username,
                profilePictureUrl = profilePictureUrl,
                bio = bio,
                displayName = displayName
            ) { success ->
                onResult(success)
            }
        } else {
            onResult(true)
        }
    }

    fun getRegisteredUsers(onComplete: (List<UserEntity>) -> Unit) {
        if (firestoreManager.isConnected.value) {
            firestoreManager.fetchAllRegisteredUsers { users ->
                repositoryScope.launch {
                    val me = dao.getCurrentUserSync()
                    users.forEach {
                        if (it.id != me?.id) {
                            dao.insertUser(it.copy(isCurrentUser = false))
                        }
                    }
                }
                onComplete(users)
            }
        } else {
            repositoryScope.launch {
                val list = arrayListOf<UserEntity>()
                dao.getAllUsers().firstOrNull()?.let { all ->
                    val me = dao.getCurrentUserSync()
                    all.forEach {
                        if (it.id != me?.id) {
                            list.add(it)
                        }
                    }
                }
                onComplete(list)
            }
        }
    }

    suspend fun getOrCreateOneOnOneChat(otherUser: UserEntity): ChatEntity {
        val me = dao.getCurrentUserSync() ?: UserEntity("me", "Sigcha User", "", "")
        val sortedIds = listOf(me.id, otherUser.id).sorted()
        val chatId = "one_on_one_" + sortedIds[0].hashCode() + "_" + sortedIds[1].hashCode()

        val allC = dao.getAllChats().firstOrNull() ?: emptyList()
        val found = allC.firstOrNull { it.id == chatId }
        if (found != null) {
            return found
        }

        val chat = ChatEntity(
            id = chatId,
            title = otherUser.displayName.ifEmpty { otherUser.username.ifEmpty { "One-on-One Chat" } },
            isGroup = false,
            lastMessage = "Started conversation.",
            lastMessageTime = System.currentTimeMillis(),
            avatarUrl = otherUser.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=150&q=80" }
        )

        dao.insertChat(chat)
        return chat
    }

    suspend fun sendMessage(chatId: String, content: String) {
        val user = dao.getCurrentUser().firstOrNull() ?: UserEntity("me", "Sigcha User", "", "")
        val timestamp = System.currentTimeMillis()
        val msgId = UUID.randomUUID().toString()

        val msg = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderId = user.id,
            senderName = user.displayName,
            content = content,
            timestamp = timestamp,
            status = if (firestoreManager.isConnected.value) 1 else 3 // Sent vs Read (Local read immediately)
        )

        // Save locally
        dao.insertMessage(msg)
        dao.updateLastMessage(chatId, content, timestamp)

        // Sync with firestore
        if (firestoreManager.isConnected.value) {
            firestoreManager.sendMessage(chatId, msg) { succeeded ->
                if (succeeded) {
                    repositoryScope.launch {
                        // Mark as read/delivered in database
                        dao.insertMessage(msg.copy(status = 3))
                    }
                }
            }
        } else {
            // Trigger local simulation reply!
            triggerSimulatedReply(chatId, content)
        }
    }

    suspend fun insertNewChat(contactName: String, isGroup: Boolean) {
        val id = "chat_" + UUID.randomUUID().toString().take(6)
        val defaultAvatar = if (isGroup) {
            "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=150&q=80"
        } else {
            "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=150&q=80"
        }
        val chat = ChatEntity(
            id = id,
            title = contactName,
            isGroup = isGroup,
            lastMessage = "Chat created.",
            lastMessageTime = System.currentTimeMillis(),
            avatarUrl = defaultAvatar
        )
        dao.insertChat(chat)
    }

    suspend fun submitPost(content: String) {
        val user = dao.getCurrentUser().firstOrNull() ?: UserEntity("me", "Sigcha User", "", "")
        val postId = "post_" + UUID.randomUUID().toString().take(8)
        val post = PostEntity(
            id = postId,
            authorName = user.displayName,
            authorAvatarUrl = user.avatarUrl,
            content = content,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            commentsCount = 0,
            isLiked = false
        )

        // Save locally
        dao.insertPost(post)

        // Save to firestore
        if (firestoreManager.isConnected.value) {
            firestoreManager.submitPost(post) {}
        }
    }

    suspend fun likePost(postId: String, currentLikes: Int, isCurrentlyLiked: Boolean) {
        val newLikes = if (isCurrentlyLiked) currentLikes - 1 else currentLikes + 1
        dao.updatePostLike(postId, newLikes, !isCurrentlyLiked)
    }

    private fun triggerSimulatedReply(chatId: String, userMessage: String) {
        repositoryScope.launch {
            // Wait 1-2 seconds of typing duration simulation!
            delay(1500)

            val replyText = when {
                chatId == "chat_anya" -> {
                    val msgs = listOf(
                        "Wow, that's awesome! Let's schedule a Figma walkthrough tomorrow.",
                        "I love that concept! Let's iterate on the design details.",
                        "Perfect! Let's keep refining the color margins.",
                        "That sounds great! I'll build a few icon variants for Sigcha.",
                        "Awesome! I'll update the component styling in the morning."
                    )
                    msgs.random()
                }
                chatId == "chat_elon" -> {
                    val msgs = listOf(
                        "Brilliant! Keep optimizing the indexes. Fast queries equal happy users.",
                        "Interesting direction. How's the local serialization latency?",
                        "Let's speed run the deployment. Execution speed is everything.",
                        "We should look at Firestore's compound index design.",
                        "Excellent. Speed is the ultimate metric."
                    )
                    msgs.random()
                }
                chatId == "chat_group_main" -> {
                    val names = listOf("Anya (Designer)", "Tony (Innovator)")
                    val msgs = listOf(
                        "This chat is highly performant. Local Room caching rocks!",
                        "I'm testing the offline sync. Works perfectly.",
                        "Sigcha's fluid layouts are pristine on mobile ratios.",
                        "Let's review the final feature release candidate."
                    )
                    val randomAuthor = names.random()
                    // Insert message from this author
                    val timestamp = System.currentTimeMillis()
                    val responseMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        chatId = chatId,
                        senderId = if (randomAuthor.contains("Anya")) "anya" else "tony",
                        senderName = randomAuthor,
                        content = msgs.random(),
                        timestamp = timestamp,
                        status = 3
                    )
                    dao.insertMessage(responseMsg)
                    dao.updateLastMessage(chatId, responseMsg.content, timestamp)
                    return@launch
                }
                else -> "Got your message! I am currently away but will reply shortly. (Simulated Response)"
            }

            val timestamp = System.currentTimeMillis()
            val responderName = when (chatId) {
                "chat_anya" -> "Anya (Designer)"
                "chat_elon" -> "Tony (Innovator)"
                else -> "Contact"
            }
            val responderId = when (chatId) {
                "chat_anya" -> "anya"
                "chat_elon" -> "tony"
                else -> "contact_id"
            }

            val responseMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = responderId,
                senderName = responderName,
                content = replyText,
                timestamp = timestamp,
                status = 3 // Read
            )

            dao.insertMessage(responseMsg)
            dao.updateLastMessage(chatId, replyText, timestamp)
        }
    }
}
