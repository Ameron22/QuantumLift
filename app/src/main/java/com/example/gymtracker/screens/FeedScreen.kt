package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import com.example.gymtracker.data.*
import com.example.gymtracker.viewmodels.AuthViewModel
import com.example.gymtracker.components.LoadingSpinner
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.viewmodels.AuthState
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.WorkoutExercise
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.collections.forEachIndexed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val listState = rememberLazyListState()
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<FeedPost?>(null) }
    
    // State for tab selection
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Feed", "Friends")
    
    // Hidden space management - scroll to hide the 200dp spacer initially
    val density = LocalDensity.current
    val spacerHeightPx = with(density) { 200.dp.toPx() }.toInt()
    var hasScrolledToHideSpace by remember { mutableStateOf(false) }
    var isShowingRefreshAnimation by remember { mutableStateOf(false) }
    var isInRefreshSequence by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    
    // Simple function to scroll back and hide the empty space
    fun scrollBackToHideSpace() {
        scope.launch {
            android.util.Log.d("FeedScreen", "Scrolling back to hide space, offset: $spacerHeightPx")
            // Simply scroll back to the position that hides the 200dp spacer
            listState.animateScrollToItem(index = 0, scrollOffset = spacerHeightPx)
            android.util.Log.d("FeedScreen", "Scroll animation completed")
        }
    }
    
    // Load feed posts when screen is first displayed
    LaunchedEffect(Unit) {
        authViewModel.loadFeedPosts()
    }
    
    // Hide the spacer after the list is composed and has items
    LaunchedEffect(authState.feedPosts.size, listState.layoutInfo.totalItemsCount) {
        if (authState.feedPosts.isNotEmpty() && !hasScrolledToHideSpace && listState.layoutInfo.totalItemsCount > 0) {
            // Add a small delay to ensure the LazyColumn is fully laid out
            kotlinx.coroutines.delay(100)
            // Scroll down to hide the 200dp spacer, so first card appears at top
            listState.scrollToItem(0, spacerHeightPx)
            hasScrolledToHideSpace = true
        }
    }
    
    // Monitor scroll position and snap back when user releases after pulling down
    LaunchedEffect(listState.isScrollInProgress) {
        // Only trigger refresh logic if we've already done the initial setup scroll and not in refresh sequence
        if (hasScrolledToHideSpace && !isInRefreshSequence && !listState.isScrollInProgress && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < spacerHeightPx) {
            // Check if fully pulled down (hidden space fully revealed)
            if (listState.firstVisibleItemScrollOffset <= 10) {
                // Fully pulled down - scroll to 100dp, wait 5 seconds with animation, then back to 200dp
                val hundredDpPx = with(density) { 100.dp.toPx() }.toInt()
                
                // Launch refresh animation in separate coroutine to avoid recomposition issues
                scope.launch {
                    // Set flag to prevent other LaunchedEffect from interfering
                    isInRefreshSequence = true
                    android.util.Log.d("FeedScreen", "Starting refresh sequence - set isInRefreshSequence = true")
                    
                    // First scroll to 100dp position
                    android.util.Log.d("FeedScreen", "Scrolling to 100dp position")
                    listState.animateScrollToItem(index = 0, scrollOffset = hundredDpPx)
                    
                    // Show spinner and refresh feed from server
                    android.util.Log.d("FeedScreen", "Setting spinner visible = true")
                    isShowingRefreshAnimation = true
                    android.util.Log.d("FeedScreen", "Refreshing feed from server")
                    authViewModel.loadFeedPosts() // Refresh feed from server
                    
                    // Wait for the loading to complete by monitoring authState.isLoading
                    while (authState.isLoading) {
                        kotlinx.coroutines.delay(100) // Check every 100ms
                    }
                    
                    android.util.Log.d("FeedScreen", "Server refresh completed, setting spinner visible = false")
                    isShowingRefreshAnimation = false
                    
                    // Finally scroll back to 200dp (hidden position)
                    android.util.Log.d("FeedScreen", "Scrolling back to 200dp (hidden position)")
                    listState.animateScrollToItem(index = 0, scrollOffset = spacerHeightPx)
                    
                    // Clear flag to allow normal scroll behavior again
                    isInRefreshSequence = false
                    android.util.Log.d("FeedScreen", "Refresh sequence completed - set isInRefreshSequence = false")
                }
            } else {
                // Partially pulled down - just snap back normally
                scrollBackToHideSpace()
            }
        }
    }
    
        Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Social",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
                
                // Tab bar under TopAppBar
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        when (selectedTabIndex) {
            0 -> FeedTab(
                paddingValues = paddingValues,
                authState = authState,
                listState = listState,
                hasScrolledToHideSpace = hasScrolledToHideSpace,
                isShowingRefreshAnimation = isShowingRefreshAnimation,
                showCreatePostDialog = showCreatePostDialog,
                selectedPostForComments = selectedPostForComments,
                onShowCreatePostDialog = { showCreatePostDialog = true },
                onSelectedPostForComments = { selectedPostForComments = it },
                authViewModel = authViewModel,
                navController = navController
            )
            1 -> FeedFriendsTab(paddingValues = paddingValues, authViewModel = authViewModel)
        }
    }
    
    // Create post dialog
    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onConfirm = { request ->
                authViewModel.createPost(request)
                showCreatePostDialog = false
            }
        )
    }
    
    // Comments dialog
    selectedPostForComments?.let { post ->
        CommentsDialog(
            post = post,
            onDismiss = { selectedPostForComments = null },
            onAddComment = { content ->
                authViewModel.addComment(post.id, content)
            },
            authViewModel = authViewModel
        )
    }
}

@Composable
fun FeedTab(
    paddingValues: PaddingValues,
    authState: AuthState,
    listState: LazyListState,
    hasScrolledToHideSpace: Boolean,
    isShowingRefreshAnimation: Boolean,
    showCreatePostDialog: Boolean,
    selectedPostForComments: FeedPost?,
    onShowCreatePostDialog: () -> Unit,
    onSelectedPostForComments: (FeedPost?) -> Unit,
    authViewModel: AuthViewModel,
    navController: NavController
) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Initial loading indicator - centered on screen
            if (authState.isLoading && authState.feedPosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingSpinner(
                        modifier = Modifier.size(80.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hidden expandable space at the top
                        item {
                            Spacer(modifier = Modifier.height(200.dp))
                        }
                        // Error message
                        authState.error?.let { error ->
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = error,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Success message
                        authState.success?.let { success ->
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = success,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Empty state
                        if (!authState.isLoading && authState.feedPosts.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = "No posts",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No posts yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Be the first to share your workout achievements!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                        onClick = onShowCreatePostDialog
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Create Post")
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Feed posts
                        items(authState.feedPosts) { post ->
                            FeedPostCard(
                                post = post,
                                onLike = { authViewModel.likePost(post.id) },
                                onComment = { onSelectedPostForComments(post) },
                                onDelete = { authViewModel.deletePost(post.id) },
                                isOwnPost = post.user.id == authState.user?.id,
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }
                    }
                    
                    // Loading spinner overlay - shown during refresh animation
                    if (isShowingRefreshAnimation) {
                        android.util.Log.d("FeedScreen", "Rendering spinner overlay - isShowingRefreshAnimation = true")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            LoadingSpinner(
                                modifier = Modifier.size(40.dp),
                                scale = 0.3f //Don't change this
                            )
                    }
                }
            }
        }
        
        // Add Post FAB - positioned in bottom right corner
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FloatingActionButton(
                onClick = { onShowCreatePostDialog() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Post",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun FeedFriendsTab(paddingValues: PaddingValues, authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsState()
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var friendEmail by remember { mutableStateOf("") }
    
    // Load friends list and pending invitations when the tab is selected
    LaunchedEffect(Unit) {
        authViewModel.loadFriendsList()
        authViewModel.loadPendingInvitations()
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add Friend Button (always visible at the top)
        item {
            Button(
                onClick = { showAddFriendDialog = true },
                modifier = Modifier.fillMaxWidth()
                                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add friend",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Friend")
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Friend Invitations Section
        if (authState.pendingInvitations.isNotEmpty()) {
            item {
                Text(
                    text = "Friend Invitations",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(authState.pendingInvitations) { invitation ->
                FeedInvitationCard(
                    invitation = invitation,
                    onAccept = { authViewModel.acceptFriendInvitation(invitation.invitationCode) },
                    onDecline = { authViewModel.declineFriendInvitation(invitation.invitationCode) },
                    isLoading = authState.isLoading
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Friends List Section
        if (authState.friends.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "No friends",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No friends yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add friends to share your fitness journey",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(authState.friends) { friend ->
                FeedFriendCard(friend = friend)
            }
        }
    }
    
    // Add Friend Dialog
    if (showAddFriendDialog) {
        FeedAddFriendDialog(
            onDismiss = { 
                showAddFriendDialog = false
                friendEmail = ""
                authViewModel.clearError()
                authViewModel.clearSuccess()
            },
            onConfirm = {
                if (friendEmail.isNotBlank()) {
                    authViewModel.sendFriendInvitation(friendEmail)
                    showAddFriendDialog = false
                    friendEmail = ""
                }
            },
            email = friendEmail,
            onEmailChange = { email -> friendEmail = email },
            isLoading = authState.isLoading,
            error = authState.error,
            success = authState.success
        )
    }
}

@Composable
fun FeedInvitationCard(
    invitation: com.example.gymtracker.data.FriendInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = invitation.senderUsername.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = invitation.senderUsername,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Wants to be your friend",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sent ${invitation.createdAt.substring(0, 10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Accept")
                    }
                }
                
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
fun FeedFriendCard(friend: com.example.gymtracker.data.Friend) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Friend avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Friends since ${friend.friendshipDate.substring(0, 10)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FeedAddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    success: String?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Friend")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Friend's Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )
                )
                
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                if (success != null) {
                    Text(
                        text = success,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = email.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Invitation")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FeedPostCard(
    post: FeedPost,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: () -> Unit,
    isOwnPost: Boolean,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Post header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.user.username.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = post.user.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatPostDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Post type icon
                Icon(
                    imageVector = when (post.postType) {
                        "WORKOUT_COMPLETED" -> Icons.Default.FitnessCenter
                        "WORKOUT_SHARED" -> Icons.Default.Share
                        "ACHIEVEMENT" -> Icons.Default.Star
                        "CHALLENGE" -> Icons.Default.EmojiEvents
                        else -> Icons.Default.Chat
                    },
                    contentDescription = post.postType,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                // Delete button for own posts
                if (isOwnPost) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete post",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Post content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Workout data if available
            post.workoutData?.let { workoutData ->
                Spacer(modifier = Modifier.height(12.dp))
                
                // State for expandable workout details
                var isWorkoutDetailsExpanded by remember { mutableStateOf(false) }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        isWorkoutDetailsExpanded = !isWorkoutDetailsExpanded
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "Workout",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Workout Details",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            // Expand/collapse icon
                            Icon(
                                imageVector = if (isWorkoutDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isWorkoutDetailsExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        workoutData.exercises?.let { exercises ->
                            if (exercises.isNotEmpty()) {
                                Text(
                                    text = "Exercises:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Column {
                                    if (isWorkoutDetailsExpanded) {
                                        // Show all exercises when expanded
                                        exercises.forEach { exercise ->
                                            Text(
                                                text = "• $exercise",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    } else {
                                        // Show only first 5 exercises when collapsed
                                        exercises.take(5).forEach { exercise ->
                                            Text(
                                                text = "• $exercise",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        if (exercises.size > 5) {
                                            Text(
                                                text = "• ... and ${exercises.size - 5} more (tap to see all)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Achievement data if available
            post.achievementData?.let { achievementData ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Achievement",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = achievementData.type,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = achievementData.value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLikedByUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLikedByUser) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onComment,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Privacy indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (post.privacyLevel) {
                            "PUBLIC" -> Icons.Default.Public
                            "FRIENDS" -> Icons.Default.People
                            else -> Icons.Default.Lock
                        },
                        contentDescription = "Privacy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.privacyLevel.lowercase().capitalize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Copy button for shared workouts
                if (post.postType == "WORKOUT_SHARED") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val context = LocalContext.current
                        IconButton(
                            onClick = {
                                post.workoutShareData?.workoutId?.let { workoutId ->
                                    authViewModel.copyWorkout(
                                        sharedWorkoutId = workoutId,
                                        onSuccess = { workoutName: String, exercises: List<EntityExercise> ->
                                            Log.d("FeedScreen", "Copy workout success callback received")
                                            Log.d("FeedScreen", "Workout name: $workoutName")
                                            Log.d("FeedScreen", "Exercises count: ${exercises.size}")
                                            Log.d("FeedScreen", "Exercises: $exercises")
                                            
                                            // Use withContext to handle database operations properly
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                try {
                                                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                        val db = AppDatabase.getDatabase(context)
                                                        val dao = db.exerciseDao()
                                                        
                                                        // Create new workout
                                                        val newWorkout = EntityWorkout(
                                                            id = 0, // Auto-generated
                                                            name = workoutName
                                                        )
                                                        
                                                        val workoutId = dao.insertWorkout(newWorkout).toInt()
                                                        Log.d("FeedScreen", "Created new workout with ID: $workoutId")
                                                        
                                                        // Add exercises to workout
                                                        exercises.forEachIndexed { index, exercise ->
                                                            Log.d("FeedScreen", "Adding exercise $index: ${exercise.name} (ID: ${exercise.id})")
                                                            Log.d("FeedScreen", "Exercise useTime: ${exercise.useTime}")
                                                            
                                                            val workoutExercise = WorkoutExercise(
                                                                id = 0, // Auto-generated
                                                                workoutId = workoutId,
                                                                exerciseId = exercise.id,
                                                                sets = 3, // Default: 3 sets
                                                                reps = if (exercise.useTime) 120 else 12, // 2 minutes (120 seconds) for time-based, 12 reps for rep-based
                                                                weight = 5, // Default: 5 kg
                                                                order = index
                                                            )
                                                            val exerciseId = dao.insertWorkoutExercise(workoutExercise)
                                                            Log.d("FeedScreen", "Inserted workout exercise with ID: $exerciseId")
                                                        }
                                                        
                                                        Log.d("FeedScreen", "Successfully copied workout with ${exercises.size} exercises")
                                                        
                                                        // Navigate on main thread
                                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                            navController.navigate(Screen.Routes.workoutDetails(workoutId))
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("FeedScreen", "Error saving copied workout: ${e.message}", e)
                                                }
                                            }
                                        },
                                        onError = { error ->
                                            // Error will be shown in the AuthState
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Workout",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onConfirm: (CreatePostRequest) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var postType by remember { mutableStateOf("TEXT_POST") }
    var privacyLevel by remember { mutableStateOf("FRIENDS") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Post")
        },
        text = {
            Column {
                // Post type selector
                Text(
                    text = "Post Type",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = postType == "TEXT_POST",
                        onClick = { postType = "TEXT_POST" },
                        label = { Text("Text") }
                    )
                    FilterChip(
                        selected = postType == "WORKOUT_COMPLETED",
                        onClick = { postType = "WORKOUT_COMPLETED" },
                        label = { Text("Workout") }
                    )
                    FilterChip(
                        selected = postType == "ACHIEVEMENT",
                        onClick = { postType = "ACHIEVEMENT" },
                        label = { Text("Achievement") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Privacy selector
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = privacyLevel == "PUBLIC",
                        onClick = { privacyLevel = "PUBLIC" },
                        label = { Text("Public") }
                    )
                    FilterChip(
                        selected = privacyLevel == "FRIENDS",
                        onClick = { privacyLevel = "FRIENDS" },
                        label = { Text("Friends") }
                    )
                    FilterChip(
                        selected = privacyLevel == "PRIVATE",
                        onClick = { privacyLevel = "PRIVATE" },
                        label = { Text("Private") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onConfirm(
                            CreatePostRequest(
                                postType = postType,
                                content = content,
                                privacyLevel = privacyLevel
                            )
                        )
                    }
                },
                enabled = content.isNotBlank()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CommentsDialog(
    post: FeedPost,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    authViewModel: AuthViewModel
) {
    var commentText by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf<List<FeedComment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(true) }
    val authState by authViewModel.authState.collectAsState()
    
    // Add LazyListState for controlling scroll position
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Function to scroll to the last comment
    fun scrollToLastComment() {
        if (comments.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(comments.size - 1)
            }
        }
    }
    
    // Load comments when dialog opens
    LaunchedEffect(post.id) {
        isLoadingComments = true
        val result = authViewModel.getComments(post.id)
        result.fold(
            onSuccess = { commentsList ->
                comments = commentsList
                isLoadingComments = false
                // Scroll to last comment after loading
                scrollToLastComment()
            },
            onFailure = { exception ->
                // Handle error
                isLoadingComments = false
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Comments")
        },
        text = {
            Column {
                // Comments list - much bigger height
                LazyColumn(
                    modifier = Modifier.height(400.dp), // Increased from 200.dp to 400.dp
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoadingComments) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "No comments yet. Be the first to comment!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        items(comments) { comment ->
                            CommentItem(comment = comment)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add comment input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank() && authState.user != null) {
                                // Create a new comment object with current user info
                                val newComment = FeedComment(
                                    id = "temp-${System.currentTimeMillis()}", // Temporary ID
                                    content = commentText,
                                    createdAt = Instant.now().toString(),
                                    user = com.example.gymtracker.data.FeedUser(
                                        id = authState.user!!.id,
                                        username = authState.user!!.username,
                                        profilePicture = null // User class doesn't have profilePicture field
                                    )
                                )
                                
                                // Add comment to local state immediately
                                comments = comments + newComment
                                
                                // Send to server
                                onAddComment(commentText)
                                commentText = ""
                                
                                // Scroll to the new comment
                                scrollToLastComment()
                            }
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send comment"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CommentItem(comment: FeedComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // User avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.user.username.first().uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.user.username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatPostDate(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Cache for parsed dates to avoid repeated parsing
private val dateCache = mutableMapOf<String, String>()
private var lastCacheUpdate = 0L

private fun formatPostDate(dateString: String): String {
    val now = System.currentTimeMillis()
    
    // Update cache every 30 seconds to refresh relative times
    if (now - lastCacheUpdate > 30_000) {
        dateCache.clear()
        lastCacheUpdate = now
    }
    
    // Return cached result if available
    dateCache[dateString]?.let { return it }
    
    return try {
        // Try multiple date formats to handle different server responses
        val dateFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS"
        )
        
        var date: Date? = null
        for (format in dateFormats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                // Set timezone to UTC for formats with 'Z'
                if (format.contains("'Z'")) {
                    inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                date = inputFormat.parse(dateString)
                break
            } catch (e: Exception) {
                // Try next format
                continue
            }
        }
        
        if (date == null) {
            return "Unknown time"
        }
        
        val currentTime = Date()
        val diffInMillis = currentTime.time - date.time
        val diffInSeconds = diffInMillis / 1000
        val diffInMinutes = diffInSeconds / 60
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24
        
        val result = when {
            diffInSeconds < 30 -> "Just now"
            diffInSeconds < 60 -> "${diffInSeconds}s ago"
            diffInMinutes < 60 -> "${diffInMinutes}m ago"
            diffInHours < 24 -> "${diffInHours}h ago"
            diffInDays < 7 -> "${diffInDays}d ago"
            else -> {
                val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                outputFormat.format(date)
            }
        }
        
        // Cache the result
        dateCache[dateString] = result
        result
    } catch (e: Exception) {
        "Unknown time"
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 