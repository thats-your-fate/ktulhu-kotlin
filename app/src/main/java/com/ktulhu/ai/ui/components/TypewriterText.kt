package com.ktulhu.ai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import kotlinx.coroutines.delay

@Composable
fun TypewriterCarousel(
    messages: List<String>,
    modifier: Modifier = Modifier,
    typingSpeedMillis: Long = 70,
    pauseMillis: Long = 1500,
    textAlign: TextAlign = TextAlign.Center,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    if (messages.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var display by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(true) }

    val safeMessages = remember(messages) { messages.filter { it.isNotBlank() } }
    if (safeMessages.isEmpty()) return

    val density = LocalDensity.current
    val lineHeight = textStyle.lineHeight.takeOrElse { 24.sp }
    val boxHeight = with(density) { (lineHeight * 2).toDp() }

    LaunchedEffect(currentIndex, safeMessages) {
        val target = safeMessages[currentIndex % safeMessages.size]
        display = ""
        isTyping = true
        for (char in target) {
            display += char
            delay(typingSpeedMillis)
        }
        isTyping = false
        delay(pauseMillis)
        currentIndex = (currentIndex + 1) % safeMessages.size
    }

    Box(
        modifier = modifier.height(boxHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildString {
                append(display)
                if (isTyping) append("▌")
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            style = textStyle,
            color = textColor
        )
    }
}
