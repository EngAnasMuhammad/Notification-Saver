package com.anasdev.notificationsaver.presentation.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anasdev.notificationsaver.R
import com.anasdev.notificationsaver.presentation.state.GroupedUiState
import com.anasdev.notificationsaver.presentation.state.NotificationViewModel
import com.anasdev.notificationsaver.presentation.state.SettingsViewModel
import com.anasdev.notificationsaver.presentation.ui.components.AppHeaderItem
import com.anasdev.notificationsaver.presentation.ui.components.AppIconCache
import com.anasdev.notificationsaver.presentation.ui.components.ConversationRowItem
import com.anasdev.notificationsaver.presentation.ui.components.PrivacyDisclaimerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pm: PackageManager,
    viewModel: NotificationViewModel,
    isBatteryOptimized: Boolean,
    onConversationClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val groupedUiState by viewModel.groupedUiState.collectAsStateWithLifecycle()
    val expandedApps by viewModel.expandedApps.collectAsStateWithLifecycle()
    val hasSeenDisclaimer by settingsViewModel.hasSeenDisclaimer.collectAsStateWithLifecycle()

    var pendingDeleteAppPackage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteConversationKey by remember { mutableStateOf<String?>(null) }

    if (!hasSeenDisclaimer) {
        PrivacyDisclaimerDialog(
            onConfirm = { settingsViewModel.markDisclaimerSeen() },
            isDismissable = false
        )
    }

    if (pendingDeleteAppPackage != null) {
        val targetPkg = pendingDeleteAppPackage!!
        val appLabel = remember(targetPkg) {
            AppIconCache.getAppInfo(targetPkg, pm).second
        }
        AlertDialog(
            onDismissRequest = { pendingDeleteAppPackage = null },
            title = { Text(stringResource(id = R.string.dialog_delete_app_title)) },
            text = { Text(stringResource(id = R.string.dialog_delete_app_msg, appLabel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAppGroup(targetPkg)
                        pendingDeleteAppPackage = null
                    }
                ) {
                    Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAppPackage = null }) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }

    if (pendingDeleteConversationKey != null) {
        val targetKey = pendingDeleteConversationKey!!
        val displayTitle = remember(targetKey) {
            targetKey.substringAfter(':').ifBlank { "Conversation" }
        }
        AlertDialog(
            onDismissRequest = { pendingDeleteConversationKey = null },
            title = { Text(stringResource(id = R.string.dialog_delete_conversation_title)) },
            text = { Text(stringResource(id = R.string.dialog_delete_conversation_msg, displayTitle)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(targetKey)
                        pendingDeleteConversationKey = null
                    }
                ) {
                    Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConversationKey = null }) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_home),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(id = R.string.title_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isBatteryOptimized) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.battery_warning_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.battery_warning_sub),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = androidx.compose.ui.unit.TextUnit(
                                    14f,
                                    androidx.compose.ui.unit.TextUnitType.Sp
                                )
                            )
                        }
                        Button(
                            onClick = {
                                @android.annotation.SuppressLint("BatteryLife")
                                val intent =
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(id = R.string.btn_fix))
                        }
                    }
                }
            }

            when (val state = groupedUiState) {
                is GroupedUiState.Loading -> {
                    // Solid background during initial 10ms Room query so Empty State illustration NEVER flashes!
                }
                is GroupedUiState.Success -> {
                    val groupedNotifications = state.groups
                    if (groupedNotifications.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            groupedNotifications.forEachIndexed { index, appGroup ->
                                val isAppExpanded = appGroup.packageName in expandedApps

                                // Level 1: App Header Row with Swipe-to-Dismiss
                                item(
                                    key = "app_${appGroup.packageName}",
                                    contentType = "app_header"
                                ) {
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    val appDismissState = rememberSwipeToDismissBoxState()
                                    LaunchedEffect(appDismissState.targetValue) {
                                        if (appDismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                            pendingDeleteAppPackage = appGroup.packageName
                                            appDismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                        }
                                    }

                                    SwipeToDismissBox(
                                        state = appDismissState,
                                        enableDismissFromStartToEnd = false,
                                        enableDismissFromEndToStart = true,
                                        backgroundContent = {
                                            val backgroundColor by animateColorAsState(
                                                targetValue = if (appDismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                    MaterialTheme.colorScheme.errorContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                label = "app_swipe_bg_color"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(backgroundColor)
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = stringResource(id = R.string.dialog_delete_app_title),
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    ) {
                                        AppHeaderItem(
                                            appGroup = appGroup,
                                            isExpanded = isAppExpanded,
                                            pm = pm,
                                            onToggleExpand = { viewModel.toggleAppExpansion(appGroup.packageName) }
                                        )
                                    }
                                }

                                // Level 2: Conversation Rows with Smooth Expand/Collapse Animation
                                item(
                                    key = "app_convs_${appGroup.packageName}",
                                    contentType = "conv_list"
                                ) {
                                    AnimatedVisibility(
                                        visible = isAppExpanded,
                                        enter = expandVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeIn(animationSpec = tween(300)),
                                        exit = shrinkVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeOut(animationSpec = tween(300))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            appGroup.conversations.forEach { conversation ->
                                                val dismissState = rememberSwipeToDismissBoxState()
                                                LaunchedEffect(dismissState.targetValue) {
                                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                        pendingDeleteConversationKey = conversation.conversationKey
                                                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                                    }
                                                }

                                                SwipeToDismissBox(
                                                    state = dismissState,
                                                    enableDismissFromStartToEnd = false,
                                                    enableDismissFromEndToStart = true,
                                                    backgroundContent = {
                                                        val backgroundColor by animateColorAsState(
                                                            targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                                MaterialTheme.colorScheme.errorContainer
                                                            } else {
                                                                MaterialTheme.colorScheme.surface
                                                            },
                                                            label = "swipe_bg_color"
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(16.dp))
                                                                .background(backgroundColor)
                                                                .padding(horizontal = 20.dp),
                                                            contentAlignment = Alignment.CenterEnd
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.DeleteOutline,
                                                                contentDescription = stringResource(id = R.string.dialog_delete_conversation_title),
                                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    ConversationRowItem(
                                                        conversation = conversation,
                                                        packageName = appGroup.packageName,
                                                        pm = pm,
                                                        onClick = { onConversationClick(conversation.conversationKey) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.empty_home),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.empty_home_sub),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
