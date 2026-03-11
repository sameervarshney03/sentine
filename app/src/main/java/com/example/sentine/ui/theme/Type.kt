package com.example.sentine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.sentine.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val DMSansFont = GoogleFont("DM Sans")

val DMSansFamily = FontFamily(
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.ExtraBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DMSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DMSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DMSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DMSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DMSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
)
