package com.ktulhu.ai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import com.ktulhu.ai.R

private val provider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val montserrat = GoogleFont("Montserrat")

private val montserratFamily = FontFamily(
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Bold)
)

private val base = Typography()

val typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = montserratFamily),
    displayMedium = base.displayMedium.copy(fontFamily = montserratFamily),
    displaySmall = base.displaySmall.copy(fontFamily = montserratFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = montserratFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = montserratFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = montserratFamily),
    titleLarge = base.titleLarge.copy(fontFamily = montserratFamily),
    titleMedium = base.titleMedium.copy(fontFamily = montserratFamily),
    titleSmall = base.titleSmall.copy(fontFamily = montserratFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = montserratFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = montserratFamily),
    bodySmall = base.bodySmall.copy(fontFamily = montserratFamily),
    labelLarge = base.labelLarge.copy(fontFamily = montserratFamily),
    labelMedium = base.labelMedium.copy(fontFamily = montserratFamily),
    labelSmall = base.labelSmall.copy(fontFamily = montserratFamily)
)
