package com.driverapp.repartidor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.driverapp.repartidor.R

/** Paleta B/N de la app (espejo de res/values/colors.xml). */
object DriverColors {
    val Black = Color(0xFF111111)
    val PureBlack = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Muted = Color(0xFF737373)
    val MutedDark = Color(0xFFA3A3A3)
    val Border = Color(0xFFE5E5E5)
    val BorderDark = Color(0xFF262626)
    val SurfaceDark = Color(0xFF161616)
    val Grey = Color(0xFF525252)
    val Info = Color(0xFF404040)
}

private val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    headlineSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

private val LightScheme = lightColorScheme(
    primary = DriverColors.Black,
    onPrimary = DriverColors.White,
    secondary = DriverColors.Info,
    background = DriverColors.White,
    onBackground = DriverColors.Black,
    surface = DriverColors.White,
    onSurface = DriverColors.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = DriverColors.Muted,
    outline = DriverColors.Border,
    error = DriverColors.Grey
)

private val DarkScheme = darkColorScheme(
    primary = DriverColors.White,
    onPrimary = DriverColors.PureBlack,
    secondary = DriverColors.MutedDark,
    background = DriverColors.PureBlack,
    onBackground = DriverColors.White,
    surface = DriverColors.SurfaceDark,
    onSurface = DriverColors.White,
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = DriverColors.MutedDark,
    outline = DriverColors.BorderDark,
    error = DriverColors.MutedDark
)

@Composable
fun DriverAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
