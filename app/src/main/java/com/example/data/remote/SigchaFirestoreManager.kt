package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SigchaFirestoreManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("sigcha_settings", Context.MODE_PRIVATE)

    private var db: FirebaseFirestore? = null
    private var messageListener: ListenerRegistration? = null
    private var postListener: ListenerRegistration? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    init {
        tryAutoConnect()
    }

    private fun tryAutoConnect() {
        val projectId = sharedPrefs.getString("fb_project_id", "") ?: ""
        val apiKey = sharedPrefs.getString("fb_api_key", "") ?: ""
        val appId = sharedPrefs.getString("fb_app_id", "") ?: ""

        if (projectId.isNotEmpty() && apiKey.isNotEmpty() && appId.isNotEmpty()) {
            setupFirebase(projectId, apiKey, appId, onSuccess = {
                Log.d("SigchaFirestore", "Successfully auto-connected to Firestore")
            }, onError = { err ->
                Log.e("SigchaFirestore", "Auto-connect failed: $err")
            })
        } else {
            Log.d("SigchaFirestore", "Firestore parameters not configured. Running in local-offline mode.")
        }
    }

    fun setupFirebase(
        projectId: String,
        apiKey: String,
        appId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Check if app is already initialized
            val existingApp = FirebaseApp.getApps(context).firstOrNull { it.name == "SigchaApp" }
            val app = if (existingApp == null) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                FirebaseApp.initializeApp(context, options, "SigchaApp")
            } else {
                existingApp
            }

            db = FirebaseFirestore.getInstance(app)
            _isConnected.value = true

            // Save credentials
            sharedPrefs.edit()
                .putString("fb_project_id", projectId)
                .putString("fb_api_key", apiKey)
                .putString("fb_app_id", appId)
                .apply()

            onSuccess()
        } catch (e: Exception) {
            _isConnected.value = false
            onError(e.localizedMessage ?: "Unknown Firebase error")
        }
    }

    fun disconnect() {
        messageListener?.remove()
        postListener?.remove()
        db = null
        _isConnected.value = false
        sharedPrefs.edit()
            .remove("fb_project_id")
            .remove("fb_api_key")
            .remove("fb_app_id")
            .apply()
    }

    fun isConfigured(): Boolean {
        val projectId = sharedPrefs.getString("fb_project_id", "") ?: ""
        return projectId.isNotEmpty()
    }

    fun getSavedCredentials(): Triple<String, String, String> {
        val projectId = sharedPrefs.getString("fb_project_id", "") ?: ""
        val apiKey = sharedPrefs.getString("fb_api_key", "") ?: ""
        val appId = sharedPrefs.getString("fb_app_id", "") ?: ""
        return Triple(projectId, apiKey, appId)
    }

    // --- Secure Hashing Helper ---
    fun hashPassword(password: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            val hexString = java.lang.StringBuilder()
            for (b in hash) {
                val hex = java.lang.Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (ex: java.lang.Exception) {
            password
        }
    }

    // --- Authentication & Profiles ---
    fun registerUserInFirestore(
        email: String,
        passwordPlain: String,
        username: String,
        profilePictureUrl: String,
        bio: String,
        displayName: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val currentDb = db
        if (currentDb == null) {
            onComplete(false, "Firestore is not connected. Registering locally only.")
            return
        }

        val hashedPassword = hashPassword(passwordPlain)
        val userData = hashMapOf(
            "id" to email,
            "username" to username,
            "displayName" to displayName,
            "profilePictureUrl" to profilePictureUrl,
            "bio" to bio,
            "statusMessage" to bio,
            "avatarUrl" to profilePictureUrl,
            "passwordHash" to hashedPassword
        )

        currentDb.collection("users")
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onComplete(false, "User with this email already exists.")
                } else {
                    currentDb.collection("users")
                        .document(email)
                        .set(userData)
                        .addOnSuccessListener {
                            onComplete(true, null)
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, e.localizedMessage)
                        }
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }

    fun loginUserInFirestore(
        email: String,
        passwordPlain: String,
        onComplete: (UserEntity?, String?) -> Unit
    ) {
        val currentDb = db
        if (currentDb == null) {
            onComplete(null, "Firestore is not connected. Connecting locally.")
            return
        }

        val hashedPassword = hashPassword(passwordPlain)

        currentDb.collection("users")
            .document(email)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val dbHash = doc.getString("passwordHash") ?: ""
                    if (dbHash == hashedPassword) {
                        val username = doc.getString("username") ?: ""
                        val displayName = doc.getString("displayName") ?: ""
                        val statusMessage = doc.getString("statusMessage") ?: doc.getString("bio") ?: ""
                        val avatarUrl = doc.getString("avatarUrl") ?: doc.getString("profilePictureUrl") ?: ""
                        val bio = doc.getString("bio") ?: ""
                        val profilePictureUrl = doc.getString("profilePictureUrl") ?: ""

                        val user = UserEntity(
                            id = email,
                            displayName = displayName.ifEmpty { username },
                            statusMessage = statusMessage,
                            avatarUrl = avatarUrl,
                            isCurrentUser = true,
                            username = username,
                            bio = bio,
                            profilePictureUrl = profilePictureUrl,
                            passwordHash = dbHash
                        )
                        onComplete(user, null)
                    } else {
                        onComplete(null, "Invalid secure credentials.")
                    }
                } else {
                    onComplete(null, "Account not found.")
                }
            }
            .addOnFailureListener { e ->
                onComplete(null, e.localizedMessage)
            }
    }

    fun updateUserProfileInFirestore(
        email: String,
        username: String,
        profilePictureUrl: String,
        bio: String,
        displayName: String,
        onComplete: (Boolean) -> Unit
    ) {
        val currentDb = db ?: run {
            onComplete(false)
            return
        }

        val updates = hashMapOf<String, Any>(
            "username" to username,
            "profilePictureUrl" to profilePictureUrl,
            "bio" to bio,
            "statusMessage" to bio,
            "avatarUrl" to profilePictureUrl,
            "displayName" to displayName
        )

        currentDb.collection("users")
            .document(email)
            .update(updates)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun fetchAllRegisteredUsers(onComplete: (List<UserEntity>) -> Unit) {
        val currentDb = db
        if (currentDb == null) {
            onComplete(emptyList())
            return
        }

        currentDb.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = arrayListOf<UserEntity>()
                for (doc in snapshot.documents) {
                    try {
                        val email = doc.id
                        val username = doc.getString("username") ?: ""
                        val displayName = doc.getString("displayName") ?: username
                        val bio = doc.getString("bio") ?: doc.getString("statusMessage") ?: ""
                        val profilePictureUrl = doc.getString("profilePictureUrl") ?: doc.getString("avatarUrl") ?: ""
                        val passwordHash = doc.getString("passwordHash") ?: ""

                        users.add(
                            UserEntity(
                                id = email,
                                displayName = displayName,
                                statusMessage = bio,
                                avatarUrl = profilePictureUrl,
                                isCurrentUser = false,
                                username = username,
                                bio = bio,
                                profilePictureUrl = profilePictureUrl,
                                passwordHash = passwordHash
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SigchaFirestore", "Error mapping user document", e)
                    }
                }
                onComplete(users)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    // --- Messages Sync ---
    fun sendMessage(chatId: String, message: MessageEntity, onComplete: (Boolean) -> Unit) {
        val currentDb = db
        if (currentDb == null) {
            onComplete(false)
            return
        }

        val data = hashMapOf(
            "id" to message.id,
            "chatId" to message.chatId,
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "content" to message.content,
            "timestamp" to message.timestamp,
            "status" to message.status
        )

        currentDb.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.id)
            .set(data)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("SigchaFirestore", "Failed to send message: ${e.message}")
                onComplete(false)
            }
    }

    fun listenForMessages(chatId: String, onNewMessages: (List<MessageEntity>) -> Unit) {
        messageListener?.remove()
        val currentDb = db ?: return

        messageListener = currentDb.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("SigchaFirestore", "Listen error: ${exception.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val msgs = arrayListOf<MessageEntity>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: ""
                            val content = doc.getString("content") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val status = doc.getLong("status")?.toInt() ?: 3

                            msgs.add(
                                MessageEntity(
                                    id = id,
                                    chatId = chatId,
                                    senderId = senderId,
                                    senderName = senderName,
                                    content = content,
                                    timestamp = timestamp,
                                    status = status
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("SigchaFirestore", "Document mapping error", e)
                        }
                    }
                    onNewMessages(msgs)
                }
            }
    }

    // --- Posts Sync ---
    fun submitPost(post: PostEntity, onComplete: (Boolean) -> Unit) {
        val currentDb = db
        if (currentDb == null) {
            onComplete(false)
            return
        }

        val data = hashMapOf(
            "id" to post.id,
            "authorName" to post.authorName,
            "authorAvatarUrl" to post.authorAvatarUrl,
            "content" to post.content,
            "timestamp" to post.timestamp,
            "likesCount" to post.likesCount,
            "commentsCount" to post.commentsCount
        )

        currentDb.collection("posts")
            .document(post.id)
            .set(data)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("SigchaFirestore", "Failed to push post: ${e.message}")
                onComplete(false)
            }
    }

    fun listenForPosts(onNewPosts: (List<PostEntity>) -> Unit) {
        postListener?.remove()
        val currentDb = db ?: return

        postListener = currentDb.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("SigchaFirestore", "Listen error: ${exception.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postsList = arrayListOf<PostEntity>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val authorName = doc.getString("authorName") ?: "Anonymous"
                            val authorAvatarUrl = doc.getString("authorAvatarUrl") ?: ""
                            val content = doc.getString("content") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                            val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0

                            postsList.add(
                                PostEntity(
                                    id = id,
                                    authorName = authorName,
                                    authorAvatarUrl = authorAvatarUrl,
                                    content = content,
                                    timestamp = timestamp,
                                    likesCount = likesCount,
                                    commentsCount = commentsCount,
                                    isLiked = false
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("SigchaFirestore", "Post parsing error", e)
                        }
                    }
                    onNewPosts(postsList)
                }
            }
    }
}
