package com.wissli.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wissli.app.R

// Bundled instead of downloadable fonts so the first frame renders without network or fallback.
// Licenses (SIL OFL) are in licenses/fonts/.

val SerifDisplay = FontFamily(Font(R.font.instrument_serif_regular, FontWeight.Normal))

val JakartaSans =
    FontFamily(
        Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
        Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
    )

private val DefaultTypography = Typography()

val Typography =
    Typography(
        displayLarge = DefaultTypography.displayLarge.copy(fontFamily = SerifDisplay),
        displayMedium = DefaultTypography.displayMedium.copy(fontFamily = SerifDisplay),
        displaySmall =
            TextStyle(
                fontFamily = SerifDisplay,
                fontWeight = FontWeight.Normal,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.01).em,
            ),
        headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = SerifDisplay),
        headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = SerifDisplay),
        headlineSmall =
            TextStyle(
                fontFamily = SerifDisplay,
                fontWeight = FontWeight.Normal,
                fontSize = 26.sp,
                lineHeight = 30.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = JakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.01).em,
            ),
        titleMedium =
            TextStyle(
                fontFamily = JakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            ),
        titleSmall =
            DefaultTypography.titleSmall.copy(fontFamily = JakartaSans, fontWeight = FontWeight.Bold),
        bodyLarge =
            TextStyle(
                fontFamily = JakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 26.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = JakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall = DefaultTypography.bodySmall.copy(fontFamily = JakartaSans),
        labelLarge =
            TextStyle(
                fontFamily = JakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = JakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = JakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.1.em,
            ),
    )
