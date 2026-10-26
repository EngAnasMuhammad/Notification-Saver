package com.anasdev.notificationsaver.presentation.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anasdev.notificationsaver.R
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.presentation.state.NotificationViewModel
import com.anasdev.notificationsaver.presentation.ui.components.AppIconCache
import com.anasdev.notificationsaver.presentation.ui.components.formatTimestamp
import java.io.File

private val STRICT_WEB_LINK_REGEX = Regex(
    "(?i)\\b(?:https?://|ftps?://|www\\.)[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    conversationKey: String,
    pm: PackageManager,
    viewModel: NotificationViewModel,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val historyFlow = remember(conversationKey) {
        viewModel.getConversationHistory(conversationKey)
    }
    val notifications by historyFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val displayTitle = remember(conversationKey) {
        conversationKey.substringAfter(':').ifBlank { "Conversation History" }
    }

    val packageName = remember(conversationKey) {
        conversationKey.substringBefore(':')
    }

    val (appIcon, appLabel) = remember(packageName) {
        AppIconCache.getAppInfo(packageName, pm)
    }

    val isAppLaunchable = remember(packageName) {
        context.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    val senderIconPath = remember(notifications) {
        notifications.firstOrNull { !it.senderIconPath.isNullOrBlank() }?.senderIconPath
    }

    val headerImageRequest = remember(senderIconPath) {
        senderIconPath?.let { path ->
            ImageRequest.Builder(context.applicationContext)
                .data(File(path))
                .crossfade(true)
                .size(128)
                .build()
        }
    }

    val cannotOpenAppText = stringResource(id = R.string.cannot_open_app)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (headerImageRequest != null) {
                                AsyncImage(
                                    model = headerImageRequest,
                                    contentDescription = displayTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = appLabel,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        }

                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isAppLaunchable) {
                        FilledTonalButton(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                if (launchIntent != null) {
                                    try {
                                        context.startActivity(launchIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, cannotOpenAppText, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, cannotOpenAppText, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.open_app),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (notifications.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = notifications,
                    key = { it.id }
                ) { item ->
                    ChatBubbleItem(
                        item = item,
                        conversationTitle = displayTitle,
                        onDelete = { viewModel.deleteNotification(item) }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                    text = stringResource(id = R.string.empty_conversation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun rememberLinkifiedText(text: String, isDark: Boolean): AnnotatedString {
    val uriHandler = LocalUriHandler.current
    val linkColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF2196F3)

    return remember(text, isDark) {
        buildAnnotatedString {
            append(text)
            STRICT_WEB_LINK_REGEX.findAll(text).forEach { match ->
                val start = match.range.first
                val rawUrl = match.value.trimEnd('.', ',', ')', ']', '}', ':', ';')
                if (rawUrl.isBlank()) return@forEach
                val actualEnd = start + rawUrl.length

                val normalizedUrl = if (rawUrl.startsWith("www.", ignoreCase = true)) {
                    "https://$rawUrl"
                } else {
                    rawUrl
                }

                val link = LinkAnnotation.Url(
                    url = normalizedUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold
                        )
                    ),
                    linkInteractionListener = {
                        try {
                            uriHandler.openUri(normalizedUrl)
                        } catch (_: Exception) {}
                    }
                )
                addLink(link, start, actualEnd)
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    item: NotificationItem,
    conversationTitle: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(item.timestamp) {
        formatTimestamp(item.timestamp)
    }

    val displayContent = remember(item.content, conversationTitle, item.title) {
        cleanMessageContent(item.content, conversationTitle, item.title)
    }

    val isDark = isSystemInDarkTheme()
    val annotatedContent = rememberLinkifiedText(text = displayContent, isDark = isDark)
    val msgCopiedText = stringResource(id = R.string.msg_copied)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (displayContent.isNotBlank()) {
                        Text(
                            text = annotatedContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.btn_copy)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(id = R.string.btn_copy),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        showMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Notification Message", displayContent)
                        @Suppress("UsePropertyAccessSyntax")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, msgCopiedText, Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(id = R.string.btn_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private fun cleanMessageContent(
    rawContent: String,
    conversationTitle: String,
    itemTitle: String
): String {
    if (rawContent.isBlank()) return rawContent

    val senders = listOf(conversationTitle.trim(), itemTitle.trim())
        .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }
        .distinct()

    var text = rawContent.trim()

    for (sender in senders) {
        val multiAccountColon = Regex("^[^:\\n]{1,35}:\\s*${Regex.escape(sender)}\\s*[:\\n]\\s*", RegexOption.IGNORE_CASE)
        val afterMulti = text.replaceFirst(multiAccountColon, "")

        val directPrefix = Regex("^${Regex.escape(sender)}\\s*:\\s*", RegexOption.IGNORE_CASE)
        val cleaned = afterMulti.replaceFirst(directPrefix, "").trim()

        if (cleaned.isNotBlank()) {
            text = cleaned
            break // Cleaned strictly once
        }
    }

    return text.ifBlank { rawContent }
}
