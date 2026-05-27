package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.SigchaDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserEntity
import com.example.data.remote.SigchaFirestoreManager
import com.example.data.repository.SigchaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object ChatList : Screen()
    data class Conversation(val chatId: String, val chatTitle: String, val isGroup: Boolean = false) : Screen()
    object SocialFeed : Screen()
    object Settings : Screen()
    object Authentication : Screen()
    object ExploreUsers : Screen()
}

class SigchaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        SigchaDatabase::class.java,
        "sigcha_db_local"
    ).fallbackToDestructiveMigration().build()

    private val firestoreManager = SigchaFirestoreManager(application)
    private val repository = SigchaRepository(db.dao(), firestoreManager, application)

    // --- Screen Navigation ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.ChatList)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- State Mappings ---
    val allChats: StateFlow<List<ChatEntity>> = repository.allChats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPosts: StateFlow<List<PostEntity>> = repository.allPosts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isFirestoreConnected: StateFlow<Boolean> = firestoreManager.isConnected

    // --- Active Chat Messages State ---
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId != null) {
                repository.getMessages(chatId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
            repository.startPostSync()
        }
    }

    // --- Actions ---
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Conversation) {
            _activeChatId.value = screen.chatId
        } else {
            _activeChatId.value = null
        }
    }

    fun sendMessage(chatId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(chatId, content)
        }
    }

    fun createChat(contactName: String, isGroup: Boolean) {
        if (contactName.isBlank()) return
        viewModelScope.launch {
            repository.insertNewChat(contactName, isGroup)
            navigateTo(Screen.ChatList)
        }
    }

    fun submitPost(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.submitPost(content)
        }
    }

    fun likePost(post: PostEntity) {
        viewModelScope.launch {
            repository.likePost(post.id, post.likesCount, post.isLiked)
        }
    }

    // --- Connection Configuration ---
    fun connectToFirestore(
        projectId: String,
        apiKey: String,
        appId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestoreManager.setupFirebase(projectId, apiKey, appId, onSuccess = {
            onSuccess()
            repository.startPostSync()
        }, onError = onError)
    }

    fun disconnectFirestore() {
        firestoreManager.disconnect()
    }

    fun getFirestoreCredentials(): Triple<String, String, String> {
        return firestoreManager.getSavedCredentials()
    }

    fun isFirestoreConfigured(): Boolean {
        return firestoreManager.isConfigured()
    }

    fun signUp(
        email: String,
        passwordPlain: String,
        username: String,
        profilePictureUrl: String,
        bio: String,
        displayName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            repository.signUp(email, passwordPlain, username, profilePictureUrl, bio, displayName, onResult)
        }
    }

    fun login(email: String, passwordPlain: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.login(email, passwordPlain, onResult)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            navigateTo(Screen.ChatList)
        }
    }

    fun updateProfile(
        username: String,
        profilePictureUrl: String,
        bio: String,
        displayName: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            repository.updateProfile(username, profilePictureUrl, bio, displayName, onResult)
        }
    }

    fun fetchRegisteredUsers(onComplete: (List<UserEntity>) -> Unit) {
        repository.getRegisteredUsers(onComplete)
    }

    fun startOneOnOneChat(otherUser: UserEntity) {
        viewModelScope.launch {
            val chat = repository.getOrCreateOneOnOneChat(otherUser)
            navigateTo(Screen.Conversation(chat.id, chat.title, false))
        }
    }

    override fun onCleared() {
        super.onCleared()
        // clean up firestore subscriptions
        firestoreManager.disconnect()
    }
}
