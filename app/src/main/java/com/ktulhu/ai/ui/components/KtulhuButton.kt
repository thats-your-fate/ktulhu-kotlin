package com.ktulhu.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import com.ktulhu.ai.ui.theme.KColors

enum class KButtonVariant { Solid, Ghost, Outline }

@Composable
fun KtulhuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KButtonVariant = KButtonVariant.Solid,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(2.dp), // square
        color = when (variant) {
            KButtonVariant.Solid -> KColors.btnDefaultBg

            KButtonVariant.Outline -> Color.Transparent
            KButtonVariant.Ghost -> Color.Transparent
        },
        border = if (variant == KButtonVariant.Outline)
            BorderStroke(1.dp, KColors.buttonBorder)
        else null,
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
            horizontalArrangement = Arrangement.Start
        ) {
            content()
        }
    }
}
