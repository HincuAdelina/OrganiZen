package com.organizen.app.home.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organizen.app.home.data.ChatViewModel
import com.organizen.app.home.models.Message
import com.organizen.app.home.models.MessageType

@Composable
fun ChatSection(
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = remember { ChatViewModel() },
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Autoscroll la ultimul mesaj
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colors.background,
                        MaterialTheme.colors.background.copy(alpha = 0.96f)
                    )
                )
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // LISTA DE MESAJe
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState
        ) {
            itemsIndexed(uiState.messages) { index, msg ->
                val nextFromSameSender = uiState.messages.getOrNull(index + 1)?.type == msg.type
                MessageRow(
                    message = msg,
                    isGroupedWithNext = nextFromSameSender
                )
                Spacer(Modifier.height(4.dp))
            }

            if (uiState.agentIsTyping) {
                item {
                    TypingIndicator(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // INPUT BAR
        ChatInputBar(
            onSend = { text ->
                if (text.isNotBlank()) chatViewModel.sendMessage(text)
            }
        )
    }
}

@Composable
private fun MessageRow(
    message: Message,
    isGroupedWithNext: Boolean
) {
    val isUser = message.type == MessageType.USER
    val bubbleColor =
        if (isUser) MaterialTheme.colors.primary
        else MaterialTheme.colors.surface

    val contentColor =
        if (isUser) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onSurface

    val shape = if (isUser) {
        // colț „coadă” spre dreapta
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    } else {
        // coadă spre stânga
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = if (isGroupedWithNext) 2.dp else 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
//            AvatarBadge(label = "AI")
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.widthIn(min = 48.dp))
        }

        Card(
            backgroundColor = bubbleColor,
            contentColor = contentColor,
            elevation = 2.dp,
            shape = shape,
            modifier = Modifier
                .widthIn(max = 320.dp)
        ) {
            Text(
                text = message.message,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.body1
            )
        }

        if (isUser) {
            Spacer(Modifier.width(6.dp))
//            AvatarBadge(label = "TU")
        } else {
            Spacer(Modifier.widthIn(min = 48.dp))
        }
    }
}


@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface,
            shape = RoundedCornerShape(20.dp),
            elevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotsBouncing()
                Spacer(Modifier.width(8.dp))
                Text("Assistant is typing…", style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun DotsBouncing() {
    val delays = listOf(0, 120, 240)
    Row(verticalAlignment = Alignment.CenterVertically) {
        delays.forEach { delay ->
            val scale by rememberInfiniteTransition().animateFloat(
                initialValue = 0.6f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            )
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun ChatInputBar(
    onSend: (String) -> Unit
) {
    val textFieldState = rememberTextFieldState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun sendAndClear() {
        val text = textFieldState.text.toString()
        if (text.isBlank()) return
        onSend(text)

        // goliți corect TextFieldState (API nou)
        textFieldState.edit {
            // șterge tot conținutul
            delete(0, length)
        }

        // ascunde tastatura și scoate focusul
        focusManager.clearFocus()
        keyboard?.hide()
    }

    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colors.surface,
            elevation = 2.dp
        ) {
            TextField(
                state = textFieldState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                placeholder = { Text("Write…", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                onKeyboardAction = { sendAndClear() },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = { sendAndClear() },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colors.onPrimary
            )
        }
    }
}

