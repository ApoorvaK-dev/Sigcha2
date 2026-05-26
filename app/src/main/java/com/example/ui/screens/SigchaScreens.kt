package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserEntity
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SigchaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom beautiful color constants for the Sigcha Theme
val SigchaTealDark = Color(0xFF005C4B)
val SigchaTealLight = Color(0xFFE7FFDB)
val SigchaTealBrand = Color(0xFF128C7E)
val SigchaTealAccent = Color(0xFF25D366)
val SigchaBackgroundDark = Color(0xFF0B141A)
val SigchaCardDark = Color(0xFF202C33)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigchaAppBody(viewModel: SigchaViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isConnected by viewModel.isFirestoreConnected.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showCreateChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sigcha",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        // Soft elegant status badge
                        Surface(
                            color = if (isConnected) SigchaTealAccent.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isConnected) "ONLINE" else "LOCAL CACHE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) SigchaTealAccent else Color.LightGray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("settings_top_button")
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = "Cloud configurations",
                            tint = if (isConnected) SigchaTealAccent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SigchaTealBrand,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentScreen is Screen.ChatList || currentScreen is Screen.Conversation,
                    onClick = { viewModel.navigateTo(Screen.ChatList) },
                    icon = { Icon(imageVector = Icons.Default.Chat, contentDescription = "Chats") },
                    label = { Text("Chats", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_chats")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.SocialFeed,
                    onClick = { viewModel.navigateTo(Screen.SocialFeed) },
                    icon = { Icon(imageVector = Icons.Default.Share, contentDescription = "Social Feed") },
                    label = { Text("Feed", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_feed")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Settings,
                    onClick = { viewModel.navigateTo(Screen.Settings) },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentScreen is Screen.ChatList) {
                FloatingActionButton(
                    onClick = { showCreateChatDialog = true },
                    containerColor = SigchaTealBrand,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_create_chat")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create New Chat")
                }
            }
        },
        modifier = Modifier.testTag("sigcha_main_layout")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "ScreenTransition"
            ) { targetState ->
                when (targetState) {
                    is Screen.ChatList -> ChatListScreen(viewModel)
                    is Screen.Conversation -> ConversationScreen(
                        viewModel = viewModel,
                        screen = targetState
                    )
                    is Screen.SocialFeed -> SocialFeedScreen(viewModel)
                    is Screen.Settings -> SettingsScreen(viewModel)
                }
            }
        }
    }

    if (showCreateChatDialog) {
        CreateChatDialog(
            onDismiss = { showCreateChatDialog = false },
            onCreate = { contactName, isGroup ->
                viewModel.createChat(contactName, isGroup)
                showCreateChatDialog = false
            }
        )
    }
}

@Composable
fun ChatListScreen(viewModel: SigchaViewModel) {
    val chats by viewModel.allChats.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredChats = chats.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar with Material 3 styling
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, SigchaTealBrand.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .testTag("chat_search_input"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = SigchaTealBrand) },
            singleLine = true
        )

        // Status Stories Section
        Text(
            text = "Active Contacts",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = SigchaTealBrand,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { viewModel.navigateTo(Screen.SocialFeed) },
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(
                Pair("Anya (Designer)", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80"),
                Pair("Tony (Innovator)", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80")
            ).forEach { (name, url) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .border(3.dp, SigchaTealAccent, CircleShape)
                            .padding(3.dp)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Text(
                        text = name.split(" ")[0],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Post action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable { viewModel.navigateTo(Screen.SocialFeed) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Post status update",
                        tint = SigchaTealBrand,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Post status",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // Contact / Chats list
        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "No chats",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "No active conversations",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Tap the plus button below to start chatting!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_list")
            ) {
                items(filteredChats) { chat ->
                    ChatRowItem(chat = chat, onClick = {
                        viewModel.navigateTo(Screen.Conversation(chat.id, chat.title, chat.isGroup))
                    })
                }
            }
        }
    }
}

@Composable
fun ChatRowItem(chat: ChatEntity, onClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(chat.lastMessageTime))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_row_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar setup
        val initial = chat.title.firstOrNull()?.toString() ?: "S"
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (chat.isGroup) MaterialTheme.colorScheme.tertiaryContainer else SigchaTealBrand.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (chat.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = chat.avatarUrl,
                    contentDescription = chat.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = initial,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SigchaTealBrand
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(SigchaTealAccent, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.4.dp, modifier = Modifier.padding(start = 82.dp))
}

@Composable
fun ConversationScreen(viewModel: SigchaViewModel, screen: Screen.Conversation) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                lazyListState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5DDD5)) // Iconic WhatsApp beige-white background color
    ) {
        // Chat screen subheader bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SigchaTealBrand.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.ChatList) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SigchaTealBrand)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Contact Detail Header
            val initial = screen.chatTitle.firstOrNull()?.toString() ?: "S"
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SigchaTealBrand.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = SigchaTealBrand
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = screen.chatTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = "active",
                    fontSize = 11.sp,
                    color = SigchaTealBrand,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Messages Feed
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("conversation_messages_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
        ) {
            items(messages) { message ->
                MessageBubbleItem(message = message)
            }
        }

        // Send Text Input Field Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...", color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("conversation_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                leadingIcon = {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Add media", tint = SigchaTealBrand)
                    }
                },
                singleLine = true
            )

            // High tactile circular FAB send button
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(screen.chatId, messageText)
                        messageText = ""
                    }
                },
                containerColor = SigchaTealBrand,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("conversation_send_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send message",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubbleItem(message: MessageEntity) {
    val isMe = message.senderId == "me"
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Sender display name for groups
            if (!isMe) {
                Text(
                    text = message.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = SigchaTealBrand,
                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                )
            }

            Surface(
                color = if (isMe) Color(0xFFDCF8C6) else Color.White, // Traditional WhatsApp bubble colors
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isMe) 12.dp else 0.dp,
                    bottomEnd = if (isMe) 0.dp else 12.dp
                ),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                        if (isMe) {
                            Icon(
                                imageVector = if (message.status >= 2) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = "Message read confirmation",
                                tint = if (message.status >= 3) Color(0xFF34B7F1) else Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialFeedScreen(viewModel: SigchaViewModel) {
    val posts by viewModel.allPosts.collectAsState()
    var postDraftText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Quick post creation input card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Share What's Happening",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SigchaTealBrand
                )

                TextField(
                    value = postDraftText,
                    onValueChange = { postDraftText = it },
                    placeholder = { Text("What is on your mind today? Write a status update...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_draft_input"),
                    minLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (postDraftText.isNotBlank()) {
                                viewModel.submitPost(postDraftText)
                                postDraftText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SigchaTealBrand),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("submit_post_button")
                    ) {
                        Text("Share Status", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = "Trending Feed",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // Activity feed list
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No status posts yet. Share your first updates!",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("social_posts_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(posts) { post ->
                    SocialPostCard(post = post, onLikeClick = { viewModel.likePost(post) })
                }
            }
        }
    }
}

@Composable
fun SocialPostCard(post: PostEntity, onLikeClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(post.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dummy circular background as avatar placeholder
                val initial = post.authorName.firstOrNull()?.toString() ?: "S"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SigchaTealBrand.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.authorAvatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = post.authorName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = initial,
                            fontWeight = FontWeight.Bold,
                            color = SigchaTealBrand
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = post.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Shared status at $formattedTime",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = post.content,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 20.sp
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like status post",
                        tint = if (post.isLiked) SigchaTealAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = post.likesCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isLiked) SigchaTealAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Post comments count",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = post.commentsCount.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SigchaViewModel) {
    val isConnected by viewModel.isFirestoreConnected.collectAsState()
    val context = LocalContext.current

    // Fields for Custom credentials
    var fbProjectId by remember { mutableStateOf("") }
    var fbApiKey by remember { mutableStateOf("") }
    var fbAppId by remember { mutableStateOf("") }

    var statusMessageState by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val (pid, key, aid) = viewModel.getFirestoreCredentials()
        fbProjectId = pid
        fbApiKey = key
        fbAppId = aid
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "My Profile Info",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = SigchaTealBrand
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(SigchaTealBrand.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User avatar",
                            modifier = Modifier.size(36.dp),
                            tint = SigchaTealBrand
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Sigcha User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Status: Sleek and connection-focused.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Firestore Credentials",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = SigchaTealBrand
            )
        }

        item {
            Text(
                text = "Fill in your Firestore config parameters. This allows real-time cloud synchronization natively! When empty, the app runs smoothly in offline Local Caching mode.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = fbProjectId,
                    onValueChange = { fbProjectId = it },
                    label = { Text("Firestore Project ID") },
                    placeholder = { Text("e.g. My-Sigcha-App") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fb_project_id_input")
                )

                TextField(
                    value = fbApiKey,
                    onValueChange = { fbApiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("e.g. AIzaSy...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fb_api_key_input")
                )

                TextField(
                    value = fbAppId,
                    onValueChange = { fbAppId = it },
                    label = { Text("Application ID") },
                    placeholder = { Text("e.g. 1:12345:android:abcd") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fb_app_id_input")
                )
            }
        }

        // Notification panels
        if (errorMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (successMessage != null) {
            item {
                Surface(
                    color = SigchaTealAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successMessage ?: "",
                        color = SigchaTealBrand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Action Buttons Setup
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isConnected) {
                    Button(
                        onClick = {
                            viewModel.disconnectFirestore()
                            successMessage = "Disconnected successfully! Operating locally."
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("disconnect_cloud_button")
                    ) {
                        Text("Disconnect Cloud")
                    }
                } else {
                    Button(
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            if (fbProjectId.isBlank() || fbApiKey.isBlank() || fbAppId.isBlank()) {
                                errorMessage = "All credentials are required to establish cloud sync."
                            } else {
                                viewModel.connectToFirestore(
                                    projectId = fbProjectId.trim(),
                                    apiKey = fbApiKey.trim(),
                                    appId = fbAppId.trim(),
                                    onSuccess = {
                                        successMessage = "Firestore synced dynamically!"
                                    },
                                    onError = { err ->
                                        errorMessage = "Failed to connect: $err"
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SigchaTealBrand),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("connect_cloud_button")
                    ) {
                        Text("Establish Connection")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreate: (contactName: String, isGroup: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isGroup by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Start Dialogue thread",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = SigchaTealBrand
                )

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Contact name or Group topic") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_chat_name_input"),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Group Chat Room",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isGroup,
                        onCheckedChange = { isGroup = it },
                        modifier = Modifier.testTag("create_chat_group_toggle")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(name, isGroup) },
                        colors = ButtonDefaults.buttonColors(containerColor = SigchaTealBrand),
                        modifier = Modifier.testTag("confirm_create_chat")
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
