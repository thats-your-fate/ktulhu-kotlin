package com.ktulhu.ai.ui.util

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

data class ViewportInfo(
    val visibleHeight: Int,
    val bottomInset: Int,
    val topInset: Int,
    val imeVisible: Boolean,
    val navigationInset: Int
)

@Composable
fun rememberViewportInfo(): ViewportInfo {
    val view = LocalView.current
    var info by remember { mutableStateOf(ViewportInfo(0, 0, 0, false, 0)) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rootInsets = ViewCompat.getRootWindowInsets(view)
            if (rootInsets != null) {
                val statusTop = rootInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                val navBottom = rootInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val imeBottom = rootInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val bottomInset = maxOf(navBottom, imeBottom)
                val visibleHeight = (view.rootView.height - statusTop - bottomInset).coerceAtLeast(0)
                info = ViewportInfo(
                    visibleHeight = visibleHeight,
                    bottomInset = bottomInset,
                    topInset = statusTop,
                    imeVisible = imeBottom > navBottom,
                    navigationInset = navBottom
                )
                return@OnGlobalLayoutListener
            }

            // Fallback for older devices
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val fullHeight = view.rootView.height
            val visibleHeight = rect.height()
            val bottomInset = fullHeight - rect.bottom
            val topInset = rect.top

            info = ViewportInfo(
                visibleHeight = visibleHeight,
                bottomInset = bottomInset,
                topInset = topInset,
                imeVisible = bottomInset > 0,
                navigationInset = 0
            )
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return info
}
