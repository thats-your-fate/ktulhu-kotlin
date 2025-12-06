package com.ktulhu.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColors = KtulhuSemanticColors(
    appBg = AppBg,
    appText = AppText,

    headerBg = HeaderBg,
    headerBorder = HeaderBorder,
    headerTitle = HeaderTitle,

    messageUserBg = MessageUserBg,
    messageUserText = MessageUserText,
    messageAssistantBg = MessageAssistantBg,
    messageAssistantText = MessageAssistantText,

    cardBg = CardBg,
    cardBorder = CardBorder,
    cardDivider = CardDivider,
    cardTitle = CardTitle,
    cardSubtitle = CardSubtitle,
    cardText = CardText,

    badgeBg = BadgeBg,
    badgeText = BadgeText,

    btnDefaultBg = BtnDefaultBg,
    btnDefaultText = BtnDefaultText,
    btnDefaultBgDark = BtnDefaultBgDark,
    btnDefaultTextDark = BtnDefaultTextDark,

    btnGhostText = BtnGhostText,
    btnGhostTextDark = BtnGhostTextDark,

    btnOutlineBorder = BtnOutlineBorder,
    btnOutlineBorderDark = BtnOutlineBorderDark,
    btnOutlineText = BtnOutlineText,
    btnOutlineTextDark = BtnOutlineTextDark,

    textareaBg = TextareaBg,
    textareaBgDark = TextareaBgDark,
    textareaText = TextareaText,
    textareaTextDark = TextareaTextDark,
    textareaPlaceholder = TextareaPlaceholder,
    textareaPlaceholderDark = TextareaPlaceholderDark,
    textareaBorder = TextareaBorder,
    textareaBorderHover = TextareaBorderHover,
    textareaBorderDark = TextareaBorderDark,
    textareaBorderHoverDark = TextareaBorderHoverDark,
    textareaRing = TextareaRing,
    textareaRingDark = TextareaRingDark,

    navActive = NavActive,
    navActiveDark = NavActiveDark,
    navInactive = NavInactive,
    navInactiveDark = NavInactiveDark,

    footerText = FooterText,
    footerTextDark = FooterTextDark,
    // ===== Auth Buttons =====
    authBtnBg = AuthBtnBg,
    authBtnBgDark = AuthBtnBgDark,
    authBtnText = AuthBtnText,
    authBtnTextDark = AuthBtnTextDark,
    authBtnBorder = AuthBtnBorder,
    authBtnBorderDark = AuthBtnBorderDark,

    // ===== Generic button border =====
    buttonBorder = ButtonBorder,
    buttonBorderDark = ButtonBorderDark,

    // ===== Provider icon tints =====
    googleIconTint = GoogleIconTint,
    googleIconTintDark = GoogleIconTintDark,

    appleIconTint = AppleIconTint,
    appleIconTintDark = AppleIconTintDark,

    facebookIconTint = FacebookIconTint,
    facebookIconTintDark = FacebookIconTintDark,

    emailIconTint = EmailIconTint,
    emailIconTintDark = EmailIconTintDark,
)



private val DarkColors = KtulhuSemanticColors(
    appBg = AppBgDark,
    appText = AppTextDark,

    headerBg = HeaderBgDark,
    headerBorder = HeaderBorderDark,
    headerTitle = HeaderTitleDark,

    messageUserBg = MessageUserBgDark,
    messageUserText = MessageUserTextDark,
    messageAssistantBg = MessageAssistantBgDark,
    messageAssistantText = MessageAssistantTextDark,

    cardBg = CardBgDark,
    cardBorder = CardBorderDark,
    cardDivider = CardDividerDark,
    cardTitle = CardTitleDark,
    cardSubtitle = CardSubtitleDark,
    cardText = CardTextDark,

    badgeBg = BadgeBgDark,
    badgeText = BadgeTextDark,

    btnDefaultBg = BtnDefaultBgDark,
    btnDefaultText = BtnDefaultTextDark,
    btnDefaultBgDark = BtnDefaultBgDark,
    btnDefaultTextDark = BtnDefaultTextDark,

    btnGhostText = BtnGhostTextDark,
    btnGhostTextDark = BtnGhostTextDark,

    btnOutlineBorder = BtnOutlineBorderDark,
    btnOutlineBorderDark = BtnOutlineBorderDark,
    btnOutlineText = BtnOutlineTextDark,
    btnOutlineTextDark = BtnOutlineTextDark,

    textareaBg = TextareaBgDark,
    textareaBgDark = TextareaBgDark,
    textareaText = TextareaTextDark,
    textareaTextDark = TextareaTextDark,
    textareaPlaceholder = TextareaPlaceholderDark,
    textareaPlaceholderDark = TextareaPlaceholderDark,
    textareaBorder = TextareaBorderDark,
    textareaBorderHover = TextareaBorderHoverDark,
    textareaBorderDark = TextareaBorderDark,
    textareaBorderHoverDark = TextareaBorderHoverDark,
    textareaRing = TextareaRingDark,
    textareaRingDark = TextareaRingDark,

    navActive = NavActiveDark,
    navActiveDark = NavActiveDark,
    navInactive = NavInactiveDark,
    navInactiveDark = NavInactiveDark,

    footerText = FooterTextDark,
    footerTextDark = FooterTextDark,
    // ===== Auth Buttons =====
    authBtnBg = AuthBtnBg,
    authBtnBgDark = AuthBtnBgDark,
    authBtnText = AuthBtnText,
    authBtnTextDark = AuthBtnTextDark,
    authBtnBorder = AuthBtnBorder,
    authBtnBorderDark = AuthBtnBorderDark,

    // ===== Generic button border =====
    buttonBorder = ButtonBorder,
    buttonBorderDark = ButtonBorderDark,

    // ===== Provider icon tints =====
    googleIconTint = GoogleIconTint,
    googleIconTintDark = GoogleIconTintDark,

    appleIconTint = AppleIconTint,
    appleIconTintDark = AppleIconTintDark,

    facebookIconTint = FacebookIconTint,
    facebookIconTintDark = FacebookIconTintDark,

    emailIconTint = EmailIconTint,
    emailIconTintDark = EmailIconTintDark,
)



val LocalKtulhuColors = staticCompositionLocalOf<KtulhuSemanticColors> {
    error("KtulhuSemanticColors not provided")
}

val KColors: KtulhuSemanticColors
    @Composable get() = LocalKtulhuColors.current

@Composable
fun KtulhuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val semantic = if (darkTheme) DarkColors else LightColors
    val material = if (darkTheme) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(LocalKtulhuColors provides semantic) {
        MaterialTheme(
            colorScheme = material,
            typography = typography,
            content = content
        )
    }
}
