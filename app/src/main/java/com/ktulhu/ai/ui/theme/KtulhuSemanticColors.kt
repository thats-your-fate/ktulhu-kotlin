package com.ktulhu.ai.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class KtulhuSemanticColors(

    // ===== App =====
    val appBg: Color,
    val appText: Color,

    // ===== Header =====
    val headerBg: Color,
    val headerBorder: Color,
    val headerTitle: Color,

    // ===== Messages =====
    val messageUserBg: Color,
    val messageUserText: Color,
    val messageAssistantBg: Color,
    val messageAssistantText: Color,

    // ===== Cards / Panels =====
    val cardBg: Color,
    val cardBorder: Color,
    val cardDivider: Color,
    val cardTitle: Color,
    val cardSubtitle: Color,
    val cardText: Color,

    // ===== Badge =====
    val badgeBg: Color,
    val badgeText: Color,

    // ===== Default Button =====
    val btnDefaultBg: Color,
    val btnDefaultText: Color,
    val btnDefaultBgDark: Color,
    val btnDefaultTextDark: Color,

    // ===== Ghost Button =====
    val btnGhostText: Color,
    val btnGhostTextDark: Color,

    // ===== Outline Button =====
    val btnOutlineBorder: Color,
    val btnOutlineBorderDark: Color,
    val btnOutlineText: Color,
    val btnOutlineTextDark: Color,

    // ===== Textarea =====
    val textareaBg: Color,
    val textareaBgDark: Color,
    val textareaText: Color,
    val textareaTextDark: Color,
    val textareaPlaceholder: Color,
    val textareaPlaceholderDark: Color,
    val textareaBorder: Color,
    val textareaBorderHover: Color,
    val textareaBorderDark: Color,
    val textareaBorderHoverDark: Color,
    val textareaRing: Color,
    val textareaRingDark: Color,

    // ===== Navigation =====
    val navActive: Color,
    val navActiveDark: Color,
    val navInactive: Color,
    val navInactiveDark: Color,

    // ===== Footer =====
    val footerText: Color,
    val footerTextDark: Color,

    // ===== Auth Buttons =====
    val authBtnBg: Color,
    val authBtnBgDark: Color,
    val authBtnText: Color,
    val authBtnTextDark: Color,
    val authBtnBorder: Color,
    val authBtnBorderDark: Color,

    // ===== Generic Button Border =====
    val buttonBorder: Color,
    val buttonBorderDark: Color,

    // ===== Provider Icon Tints =====
    val googleIconTint: Color,
    val googleIconTintDark: Color,

    val appleIconTint: Color,
    val appleIconTintDark: Color,

    val facebookIconTint: Color,
    val facebookIconTintDark: Color,

    val emailIconTint: Color,
    val emailIconTintDark: Color
)
