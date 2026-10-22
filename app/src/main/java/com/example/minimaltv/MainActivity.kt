package com.example.minimaltv

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class TvAppBrand {
    YOUTUBE,
    NETFLIX,
    JIO_HOTSTAR,
    PRIME_VIDEO
}

data class TvAppTile(
    val brand: TvAppBrand,
    val title: String,
    val primaryPackage: String,
    val fallbackPackages: List<String> = emptyList(),
    val accentColor: Color
)

data class WeatherState(
    val temperatureText: String = "--°C",
    val conditionText: String = "Loading weather",
    val cityText: String = "",
    val iconSymbol: String = "☀"
)

private val TV_APPS = listOf(
    TvAppTile(
        brand = TvAppBrand.YOUTUBE,
        title = "YouTube",
        primaryPackage = "com.google.android.youtube.tv",
        fallbackPackages = listOf("com.google.android.youtube"),
        accentColor = Color(0xFFFF0033)
    ),
    TvAppTile(
        brand = TvAppBrand.NETFLIX,
        title = "Netflix",
        primaryPackage = "com.netflix.ninja",
        fallbackPackages = listOf("com.netflix.mediaclient"),
        accentColor = Color(0xFFE50914)
    ),
    TvAppTile(
        brand = TvAppBrand.JIO_HOTSTAR,
        title = "JioHotstar",
        primaryPackage = "in.startv.hotstar",
        fallbackPackages = listOf(
            "in.startv.hotstar.dplus.tv",
            "com.jio.media.stb.ondemand"
        ),
        accentColor = Color(0xFF3B82F6)
    ),
    TvAppTile(
        brand = TvAppBrand.PRIME_VIDEO,
        title = "Prime Video",
        primaryPackage = "com.amazon.amazonvideo.livingroom",
        fallbackPackages = listOf("com.amazon.avod.thirdpartyclient"),
        accentColor = Color(0xFF00A8E1)
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color(0xFF0A0A0A),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
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
    val firstCardFocusRequester = remember { FocusRequester() }

    var wifiLabel by remember { mutableStateOf(resolveConnectedWifiName(context)) }
    var weather by remember { mutableStateOf(WeatherState()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        wifiLabel = resolveConnectedWifiName(context)
    }

    // Request location permission once so Android allows reading the connected Wi-Fi SSID
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

    // Poll Wi-Fi connectivity status every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            wifiLabel = resolveConnectedWifiName(context)
            delay(5_000L)
        }
    }

    // Fetch live weather periodically (every 15 minutes)
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
                .padding(horizontal = 64.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Big Date & Time on Left + Weather, Wi-Fi Name & Settings on Right
            TopStatusHeroBar(
                weather = weather,
                wifiName = wifiLabel,
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

            // Center Section: 4 Minimal D-Pad Focusable App Icon Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
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
                        onClick = { launchTvApp(context, app) }
                    )
                }
            }

            // Subtle Minimal Bottom Spacer
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TopStatusHeroBar(
    weather: WeatherState,
    wifiName: String,
    onWifiClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var timeMain by remember { mutableStateOf("") }
    var timeAmPm by remember { mutableStateOf("") }
    var dateFull by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val mainFmt = SimpleDateFormat("hh:mm", Locale.getDefault())
        val amPmFmt = SimpleDateFormat("a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
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
        // Big Font Time & Date
        Column {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeMain,
                    color = Color.White,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = timeAmPm,
                    color = Color(0xFF9CA3AF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFull,
                color = Color(0xFFD1D5DB),
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp
            )
        }

        // Right Status Cluster: Weather + Connected Wi-Fi Name + Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Weather Pill
            StatusInfoPill(
                icon = weather.iconSymbol,
                primaryText = weather.temperatureText,
                secondaryText = if (weather.cityText.isNotBlank()) {
                    "${weather.conditionText} • ${weather.cityText}"
                } else {
                    weather.conditionText
                }
            )

            // Connected Wi-Fi Name Pill (Focusable to open Wi-Fi settings or grant SSID permission)
            FocusableStatusPill(
                icon = "📶",
                label = wifiName,
                onClick = onWifiClick
            )

            // Minimal Settings Icon Button
            FocusableSettingsButton(onClick = onSettingsClick)
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
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(50))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            color = Color(0xFFFBBF24)
        )
        Text(
            text = primaryText,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = secondaryText,
            color = Color(0xFF9CA3AF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun FocusableStatusPill(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "wifiScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF262626),
        label = "wifiBorder"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(if (isFocused) Color(0xFF1A1A1A) else Color(0xFF0D0D0D))
            .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(50))
            .onFocusChanged { isFocused = it.isFocused }
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            color = Color(0xFF38BDF8)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FocusableSettingsButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "settingsScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF262626),
        label = "settingsBorder"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(if (isFocused) Color(0xFF1A1A1A) else Color(0xFF0D0D0D))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50)
            )
            .onFocusChanged { isFocused = it.isFocused }
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚙",
                color = Color.White,
                fontSize = 17.sp
            )
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MinimalAppIconCard(
    app: TvAppTile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tileScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF1F1F1F),
        label = "tileBorder"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF8E8E93),
        label = "labelColor"
    )

    val tileShape = RoundedCornerShape(28.dp)

    Column(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
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
        Box(
            modifier = Modifier
                .size(width = 188.dp, height = 188.dp)
                .clip(tileShape)
                .background(
                    if (isFocused) Color(0xFF141414) else Color(0xFF0A0A0A)
                )
                .border(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = borderColor,
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (app.brand) {
                TvAppBrand.YOUTUBE -> YouTubeVectorLogo()
                TvAppBrand.NETFLIX -> NetflixVectorLogo()
                TvAppBrand.JIO_HOTSTAR -> JioHotstarVectorLogo()
                TvAppBrand.PRIME_VIDEO -> PrimeVideoVectorLogo()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = app.title,
            color = labelColor,
            fontSize = 18.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Focus indicator dot underneath active app
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isFocused) app.accentColor else Color.Transparent)
        )
    }
}

@Composable
private fun YouTubeVectorLogo() {
    Canvas(modifier = Modifier.size(104.dp)) {
        val w = size.width
        val h = size.height
        val rectHeight = h * 0.70f
        val topOffset = (h - rectHeight) / 2f

        // Iconic YouTube Red Rounded Play Screen
        drawRoundRect(
            color = Color(0xFFFF0033),
            topLeft = Offset(0f, topOffset),
            size = Size(w, rectHeight),
            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
        )

        // White Play Triangle
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
    Canvas(modifier = Modifier.size(92.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.22f
        val leftX = w * 0.18f
        val rightX = w * 0.60f

        // Left darker crimson vertical stem
        drawRect(
            color = Color(0xFFB1060F),
            topLeft = Offset(leftX, h * 0.08f),
            size = Size(barWidth, h * 0.84f)
        )

        // Right darker crimson vertical stem
        drawRect(
            color = Color(0xFFB1060F),
            topLeft = Offset(rightX, h * 0.08f),
            size = Size(barWidth, h * 0.84f)
        )

        // Center bright Netflix red diagonal ribbon
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
            .size(104.dp)
            .clip(RoundedCornerShape(26.dp))
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
        Canvas(modifier = Modifier.size(68.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Four-point sparkling star emblem
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

            // Secondary sparkle accent
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
        modifier = Modifier.size(116.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "prime",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .width(86.dp)
                .height(22.dp)
        ) {
            val w = size.width
            val h = size.height

            // Iconic Prime Cyan Smile Curve
            val smilePath = Path().apply {
                moveTo(w * 0.08f, h * 0.25f)
                quadraticBezierTo(w * 0.50f, h * 0.95f, w * 0.88f, h * 0.28f)
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF00A8E1),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Arrowhead on the right tip of the smile
            val arrowHead = Path().apply {
                moveTo(w * 0.75f, h * 0.18f)
                lineTo(w * 0.92f, h * 0.24f)
                lineTo(w * 0.84f, h * 0.56f)
            }
            drawPath(
                path = arrowHead,
                color = Color(0xFF00A8E1),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun resolveConnectedWifiName(context: Context): String {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork ?: return "Not Connected"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Not Connected"

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
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Hotspot / Mobile"
            else -> "Connected"
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

        // Step 1: Resolve approximate TV location by IP (no GPS hardware required on Android TV)
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
            // Keep default coordinates if IP lookup is unavailable
        }

        // Step 2: Query Open-Meteo current weather (free, no API key needed)
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
        WeatherState(temperatureText = "--°C", conditionText = "Weather unavailable", iconSymbol = "☁")
    } catch (_: Exception) {
        WeatherState(temperatureText = "--°C", conditionText = "Offline", iconSymbol = "☁")
    }
}

private fun mapWmoWeatherCode(code: Int): Pair<String, String> {
    return when (code) {
        0 -> "☀" to "Clear Sky"
        1, 2 -> "⛅" to "Partly Cloudy"
        3 -> "☁" to "Overcast"
        45, 48 -> "🌫" to "Foggy"
        51, 53, 55, 56, 57 -> "🌦" to "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧" to "Rain"
        71, 73, 75, 77, 85, 86 -> "❄" to "Snow"
        95, 96, 99 -> "⛈" to "Thunderstorm"
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
                // Continue trying next fallback package
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
