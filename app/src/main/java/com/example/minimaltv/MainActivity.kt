package com.example.minimaltv

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Wine Red & Coding Syntax Palette
private val WineRedPrimary = Color(0xFFC81D4E)
private val WineRedDeep = Color(0xFF750E2A)
private val WineRedBright = Color(0xFFE8356D)
private val CodeCyan = Color(0xFF00F0FF)
private val CodeGreen = Color(0xFF39FF14)
private val CodeYellow = Color(0xFFFFEA00)
private val CodePurple = Color(0xFFB026FF)
private val CodeOrange = Color(0xFFFF6B00)
private val CodePink = Color(0xFFFF007F)

enum class TvAppBrand {
    YOUTUBE,
    NETFLIX,
    JIO_HOTSTAR,
    PRIME_VIDEO
}

data class TvAppTile(
    val brand: TvAppBrand,
    val title: String,
    val codeTag: String,
    val primaryPackage: String,
    val fallbackPackages: List<String> = emptyList(),
    val neonGlowColor: Color,
    val secondaryGlowColor: Color,
    val labelSyntaxColor: Color
)

data class WeatherState(
    val temperatureText: String = "--°C",
    val conditionText: String = "Loading...",
    val cityText: String = "",
    val iconSymbol: String = "☀"
)

private val TV_APPS = listOf(
    TvAppTile(
        brand = TvAppBrand.YOUTUBE,
        title = "YouTube",
        codeTag = "<yt/>",
        primaryPackage = "com.google.android.youtube.tv",
        fallbackPackages = listOf("com.google.android.youtube"),
        neonGlowColor = Color(0xFFFF004D),
        secondaryGlowColor = Color(0xFFFF5500),
        labelSyntaxColor = WineRedBright
    ),
    TvAppTile(
        brand = TvAppBrand.NETFLIX,
        title = "Netflix",
        codeTag = "<nf/>",
        primaryPackage = "com.netflix.ninja",
        fallbackPackages = listOf("com.netflix.mediaclient"),
        neonGlowColor = Color(0xFFE50914),
        secondaryGlowColor = Color(0xFFFF0055),
        labelSyntaxColor = CodeOrange
    ),
    TvAppTile(
        brand = TvAppBrand.JIO_HOTSTAR,
        title = "JioHotstar",
        codeTag = "<jh/>",
        primaryPackage = "in.startv.hotstar",
        fallbackPackages = listOf(
            "in.startv.hotstar.dplus.tv",
            "com.jio.media.stb.ondemand"
        ),
        neonGlowColor = Color(0xFFA855F7),
        secondaryGlowColor = Color(0xFF00F0FF),
        labelSyntaxColor = CodePurple
    ),
    TvAppTile(
        brand = TvAppBrand.PRIME_VIDEO,
        title = "Prime Video",
        codeTag = "<pv/>",
        primaryPackage = "com.amazon.amazonvideo.livingroom",
        fallbackPackages = listOf("com.amazon.avod.thirdpartyclient"),
        neonGlowColor = Color(0xFF00F0FF),
        secondaryGlowColor = Color(0xFF0077FF),
        labelSyntaxColor = CodeCyan
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val codingTypography = Typography(
            bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, color = WineRedPrimary),
            bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, color = WineRedPrimary),
            titleLarge = TextStyle(fontFamily = FontFamily.Monospace, color = WineRedPrimary)
        )
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color(0xFF070707),
                    onBackground = WineRedPrimary,
                    onSurface = WineRedPrimary
                ),
                typography = codingTypography
            ) {
                BackHandler(enabled = true) {
                    // Keep user on launcher home screen when pressing Back on the TV remote
                }
                MinimalTvLauncherScreen()
            }
        }
    }
}

@Composable
fun MinimalTvLauncherScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val firstCardFocusRequester = remember { FocusRequester() }

    var wifiLabel by remember { mutableStateOf(resolveConnectedWifiName(context)) }
    var weather by remember { mutableStateOf(WeatherState()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        wifiLabel = resolveConnectedWifiName(context)
    }

    LaunchedEffect(Unit) {
        firstCardFocusRequester.requestFocus()
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            wifiLabel = resolveConnectedWifiName(context)
            delay(5_000L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            weather = fetchLiveWeather()
            delay(15 * 60 * 1_000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 72.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Greeting + Scaled-down Clock & Date on Left, Status Pills on Right
            TopStatusHeroBar(
                weather = weather,
                wifiName = wifiLabel,
                onNavigateSound = { playRemoteMoveSound(context, view) },
                onSelectSound = { playRemoteSelectSound(context, view) },
                onWifiClick = {
                    val hasLoc = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasLoc) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        launchWifiSettings(context)
                    }
                },
                onSettingsClick = { launchSystemSettings(context) }
            )

            // Center Section: 4 Compact D-Pad Focusable App Tiles with Refined Neon Glow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TV_APPS.forEachIndexed { index, app ->
                    val modifier = if (index == 0) {
                        Modifier.focusRequester(firstCardFocusRequester)
                    } else {
                        Modifier
                    }

                    MinimalAppIconCard(
                        app = app,
                        modifier = modifier,
                        onFocusSound = { playRemoteMoveSound(context, view) },
                        onClick = {
                            playRemoteSelectSound(context, view)
                            launchTvApp(context, app)
                        }
                    )
                }
            }

            // Bottom Section: Scaled-down "Developed by {L-PATH}"
            DeveloperSignatureFooter()
        }
    }
}

@Composable
private fun TopStatusHeroBar(
    weather: WeatherState,
    wifiName: String,
    onNavigateSound: () -> Unit,
    onSelectSound: () -> Unit,
    onWifiClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var timeMain by remember { mutableStateOf("") }
    var timeAmPm by remember { mutableStateOf("") }
    var dateFull by remember { mutableStateOf("") }
    var greetingText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val mainFmt = SimpleDateFormat("hh:mm", Locale.getDefault())
        val amPmFmt = SimpleDateFormat("a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            val cal = Calendar.getInstance().apply { time = now }
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            greetingText = when (hour) {
                in 5..11 -> "Good Morning"
                in 12..16 -> "Good Noon"
                else -> "Good Evening"
            }

            timeMain = mainFmt.format(now)
            timeAmPm = amPmFmt.format(now).uppercase(Locale.getDefault())
            dateFull = dateFmt.format(now)
            delay(1_000L)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Time-of-Day Greeting + Compact Clock & Date in Wine Red Coding Font
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = CodeGreen, fontWeight = FontWeight.Bold)) {
                        append(">> ")
                    }
                    withStyle(SpanStyle(color = WineRedBright, fontWeight = FontWeight.Bold)) {
                        append(greetingText)
                    }
                    withStyle(SpanStyle(color = CodeCyan, fontWeight = FontWeight.Normal)) {
                        append(" ();")
                    }
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Clock reduced to 46.sp for balanced TV viewing
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeMain,
                    color = WineRedPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "[$timeAmPm]",
                    color = CodeYellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Compact Date
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = CodePurple)) { append("date: ") }
                    withStyle(SpanStyle(color = WineRedBright, fontWeight = FontWeight.SemiBold)) {
                        append("\"$dateFull\"")
                    }
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        // Right Status Cluster: Weather + Connected Wi-Fi Name + Settings (compact badges)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Weather Pill
            StatusInfoPill(
                icon = weather.iconSymbol,
                primaryText = weather.temperatureText,
                secondaryText = if (weather.cityText.isNotBlank()) {
                    "${weather.conditionText} @ ${weather.cityText}"
                } else {
                    weather.conditionText
                }
            )

            // Connected Wi-Fi SSID Pill
            FocusableStatusPill(
                icon = "WIFI:",
                label = wifiName,
                onFocusSound = onNavigateSound,
                onClick = {
                    onSelectSound()
                    onWifiClick()
                }
            )

            // Settings Button
            FocusableSettingsButton(
                onFocusSound = onNavigateSound,
                onClick = {
                    onSelectSound()
                    onSettingsClick()
                }
            )
        }
    }
}

@Composable
private fun StatusInfoPill(
    icon: String,
    primaryText: String,
    secondaryText: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF090909))
            .border(1.dp, WineRedDeep, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = icon,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = CodeYellow
        )
        Text(
            text = primaryText,
            fontFamily = FontFamily.Monospace,
            color = WineRedBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = secondaryText,
            fontFamily = FontFamily.Monospace,
            color = CodeCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FocusableStatusPill(
    icon: String,
    label: String,
    onFocusSound: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "wifiScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) CodeCyan else WineRedDeep,
        label = "wifiBorder"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) Color(0xFF14080C) else Color(0xFF090909))
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .onFocusChanged {
                if (it.isFocused && !isFocused) {
                    onFocusSound()
                }
                isFocused = it.isFocused
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = icon,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CodeGreen
        )
        Text(
            text = "\"$label\"",
            fontFamily = FontFamily.Monospace,
            color = WineRedBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FocusableSettingsButton(
    onFocusSound: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "settingsScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) WineRedBright else WineRedDeep,
        label = "settingsBorder"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) Color(0xFF19080D) else Color(0xFF090909))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged {
                if (it.isFocused && !isFocused) {
                    onFocusSound()
                }
                isFocused = it.isFocused
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = CodeOrange, fontWeight = FontWeight.Bold)) {
                    append("⚙ ")
                }
                withStyle(SpanStyle(color = WineRedBright, fontWeight = FontWeight.Bold)) {
                    append("cfg()")
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MinimalAppIconCard(
    app: TvAppTile,
    modifier: Modifier = Modifier,
    onFocusSound: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var hasInitializedFocus by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Reduced scale for minimal footprint
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tileScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "neonPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) app.neonGlowColor else Color(0xFF1A0D12),
        label = "tileBorder"
    )

    val tileShape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { state ->
                if (state.isFocused && !isFocused && hasInitializedFocus) {
                    onFocusSound()
                }
                hasInitializedFocus = true
                isFocused = state.isFocused
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact 136.dp x 136.dp box with soft neon glow aura
        Box(
            modifier = Modifier
                .size(width = 136.dp, height = 136.dp)
                .drawBehind {
                    if (isFocused) {
                        val glowRadius = size.maxDimension * 0.80f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    app.neonGlowColor.copy(alpha = 0.55f * pulseAlpha),
                                    app.secondaryGlowColor.copy(alpha = 0.22f * pulseAlpha),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = center
                        )
                    }
                }
                .clip(tileShape)
                .background(
                    if (isFocused) Color(0xFF0E0508) else Color(0xFF060606)
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = borderColor,
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isFocused) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                app.neonGlowColor.copy(alpha = 0.32f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.58f
                    )
                }
            }

            when (app.brand) {
                TvAppBrand.YOUTUBE -> YouTubeVectorLogo()
                TvAppBrand.NETFLIX -> NetflixVectorLogo()
                TvAppBrand.JIO_HOTSTAR -> JioHotstarVectorLogo()
                TvAppBrand.PRIME_VIDEO -> PrimeVideoVectorLogo()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Compact Coding Font App Title
        Text(
            text = buildAnnotatedString {
                if (isFocused) {
                    withStyle(SpanStyle(color = CodeGreen, fontWeight = FontWeight.Bold)) {
                        append("> ")
                    }
                }
                withStyle(
                    SpanStyle(
                        color = if (isFocused) WineRedBright else WineRedPrimary,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
                    )
                ) {
                    append(app.title)
                }
                withStyle(
                    SpanStyle(
                        color = if (isFocused) app.labelSyntaxColor else WineRedDeep,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                ) {
                    append(" ${app.codeTag}")
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtle neon underline indicator
        Box(
            modifier = Modifier
                .width(if (isFocused) 36.dp else 6.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (isFocused) app.neonGlowColor else Color.Transparent)
        )
    }
}

@Composable
private fun DeveloperSignatureFooter() {
    val lPathColors = listOf(
        '{' to CodeCyan,
        'L' to CodeGreen,
        '-' to CodeYellow,
        'P' to CodePink,
        'A' to CodeOrange,
        'T' to CodePurple,
        'H' to Color(0xFF38BDF8),
        '}' to Color(0xFFFF3131)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = WineRedPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append("Developed by ")
                }
                lPathColors.forEach { (ch, color) ->
                    withStyle(
                        SpanStyle(
                            color = color,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append(ch)
                    }
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            letterSpacing = 1.0.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun YouTubeVectorLogo() {
    Canvas(modifier = Modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val rectHeight = h * 0.70f
        val topOffset = (h - rectHeight) / 2f

        drawRoundRect(
            color = Color(0xFFFF0033),
            topLeft = Offset(0f, topOffset),
            size = Size(w, rectHeight),
            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
        )

        val playPath = Path().apply {
            moveTo(w * 0.40f, h * 0.35f)
            lineTo(w * 0.66f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.65f)
            close()
        }
        drawPath(path = playPath, color = Color.White)
    }
}

@Composable
private fun NetflixVectorLogo() {
    Canvas(modifier = Modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.22f
        val leftX = w * 0.18f
        val rightX = w * 0.60f

        drawRect(
            color = Color(0xFFB1060F),
            topLeft = Offset(leftX, h * 0.08f),
            size = Size(barWidth, h * 0.84f)
        )

        drawRect(
            color = Color(0xFFB1060F),
            topLeft = Offset(rightX, h * 0.08f),
            size = Size(barWidth, h * 0.84f)
        )

        val ribbon = Path().apply {
            moveTo(leftX, h * 0.08f)
            lineTo(leftX + barWidth, h * 0.08f)
            lineTo(rightX + barWidth, h * 0.92f)
            lineTo(rightX, h * 0.92f)
            close()
        }
        drawPath(path = ribbon, color = Color(0xFFE50914))
    }
}

@Composable
private fun JioHotstarVectorLogo() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1D4ED8),
                        Color(0xFF9333EA)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(46.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val starPath = Path().apply {
                moveTo(cx, h * 0.04f)
                quadraticBezierTo(cx + w * 0.08f, cy - h * 0.08f, w * 0.96f, cy)
                quadraticBezierTo(cx + w * 0.08f, cy + h * 0.08f, cx, h * 0.96f)
                quadraticBezierTo(cx - w * 0.08f, cy + h * 0.08f, w * 0.04f, cy)
                quadraticBezierTo(cx - w * 0.08f, cy - h * 0.08f, cx, h * 0.04f)
                close()
            }
            drawPath(
                path = starPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color.White)
                )
            )

            drawCircle(
                color = Color.White,
                radius = w * 0.06f,
                center = Offset(w * 0.78f, h * 0.22f)
            )
        }
    }
}

@Composable
private fun PrimeVideoVectorLogo() {
    Column(
        modifier = Modifier.size(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "prime",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Canvas(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
        ) {
            val w = size.width
            val h = size.height

            val smilePath = Path().apply {
                moveTo(w * 0.08f, h * 0.25f)
                quadraticBezierTo(w * 0.50f, h * 0.95f, w * 0.88f, h * 0.28f)
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF00A8E1),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            val arrowHead = Path().apply {
                moveTo(w * 0.75f, h * 0.18f)
                lineTo(w * 0.92f, h * 0.24f)
                lineTo(w * 0.84f, h * 0.56f)
            }
            drawPath(
                path = arrowHead,
                color = Color(0xFF00A8E1),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun playRemoteMoveSound(context: Context, view: View) {
    try {
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_RIGHT)
    } catch (_: Exception) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, 35)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 28)
        } catch (_: Exception) {
        }
    }
}

private fun playRemoteSelectSound(context: Context, view: View) {
    try {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
    } catch (_: Exception) {
    }
}

private fun resolveConnectedWifiName(context: Context): String {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork ?: return "Disconnected"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Disconnected"

        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                val rawSsid = wifiManager?.connectionInfo?.ssid
                    ?.removeSurrounding("\"")
                    ?.trim()

                if (!rawSsid.isNullOrBlank() &&
                    !rawSsid.equals("<unknown ssid>", ignoreCase = true) &&
                    rawSsid != "0x"
                ) {
                    rawSsid
                } else {
                    "Wi-Fi Connected"
                }
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Hotspot"
            else -> "Online"
        }
    } catch (_: Exception) {
        "Wi-Fi"
    }
}

private suspend fun fetchLiveWeather(): WeatherState = withContext(Dispatchers.IO) {
    try {
        var lat = 13.0827
        var lon = 80.2707
        var city = ""

        try {
            val geoConn = (URL("https://ipwho.is/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 4_000
                readTimeout = 4_000
                requestMethod = "GET"
            }
            if (geoConn.responseCode == 200) {
                val geoBody = geoConn.inputStream.bufferedReader().use { it.readText() }
                val geoJson = JSONObject(geoBody)
                if (geoJson.optBoolean("success", true)) {
                    lat = geoJson.optDouble("latitude", lat)
                    lon = geoJson.optDouble("longitude", lon)
                    city = geoJson.optString("city", "")
                }
            }
            geoConn.disconnect()
        } catch (_: Exception) {
        }

        val weatherUrl =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
        val weatherConn = (URL(weatherUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
        }

        if (weatherConn.responseCode == 200) {
            val body = weatherConn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val current = json.optJSONObject("current_weather")
            if (current != null) {
                val temp = current.optDouble("temperature", Double.NaN)
                val code = current.optInt("weathercode", 0)
                val (symbol, desc) = mapWmoWeatherCode(code)
                val tempStr = if (!temp.isNaN()) "${temp.roundToInt()}°C" else "--°C"
                weatherConn.disconnect()
                return@withContext WeatherState(
                    temperatureText = tempStr,
                    conditionText = desc,
                    cityText = city,
                    iconSymbol = symbol
                )
            }
        }
        weatherConn.disconnect()
        WeatherState(temperatureText = "--°C", conditionText = "Unavailable", iconSymbol = "☁")
    } catch (_: Exception) {
        WeatherState(temperatureText = "--°C", conditionText = "Offline", iconSymbol = "☁")
    }
}

private fun mapWmoWeatherCode(code: Int): Pair<String, String> {
    return when (code) {
        0 -> "☀" to "Clear"
        1, 2 -> "⛅" to "Partly Cloudy"
        3 -> "☁" to "Overcast"
        45, 48 -> "🌫" to "Foggy"
        51, 53, 55, 56, 57 -> "🌦" to "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧" to "Rain"
        71, 73, 75, 77, 85, 86 -> "❄" to "Snow"
        95, 96, 99 -> "⛈" to "Storm"
        else -> "☀" to "Fair"
    }
}

private fun launchTvApp(context: Context, app: TvAppTile) {
    val pm = context.packageManager
    val candidates = listOf(app.primaryPackage) + app.fallbackPackages

    for (pkg in candidates) {
        val leanbackIntent = pm.getLeanbackLaunchIntentForPackage(pkg)
        val standardIntent = pm.getLaunchIntentForPackage(pkg)
        val launchIntent = leanbackIntent ?: standardIntent

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                return
            } catch (_: Exception) {
            }
        }
    }

    try {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${app.primaryPackage}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "${app.title} is not installed on this TV.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun launchWifiSettings(context: Context) {
    try {
        val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(wifiIntent)
    } catch (_: Exception) {
        launchSystemSettings(context)
    }
}

private fun launchSystemSettings(context: Context) {
    try {
        val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(settingsIntent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Unable to open TV Settings",
            Toast.LENGTH_SHORT
        ).show()
    }
}
