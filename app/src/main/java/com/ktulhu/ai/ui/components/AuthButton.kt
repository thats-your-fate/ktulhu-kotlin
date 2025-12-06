package com.ktulhu.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ktulhu.ai.ui.theme.KColors

@Composable
fun AuthButton(
    text: String,
    icon: Painter?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KColors

    Surface(
        shape = RoundedCornerShape(2.dp),
        color = c.authBtnBg,                    // white / dark mode background
        border = BorderStroke(1.dp, c.authBtnBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center   // <-- CENTER CONTENT
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = text,
                color = c.authBtnText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
