package com.example.minimaltv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TvAppTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val primaryPackage: String,
    val fallbackPackages: List<String> = emptyList(),
    val primaryColor: Color,
    val secondaryColor: Color
)

private val TV_APPS = listOf(
    TvAppTile(
        id = "youtube",
        title = "YouTube",
        subtitle = "4K Streams & Subscriptions",
        badgeText = "YT",
        primaryPackage = "com.google.android.youtube.tv",
        fallbackPackages = listOf("com.google.android.youtube"),
        primaryColor = Color(0xFFFF0033),
        secondaryColor = Color(0xFF99001F)
    ),
    TvAppTile(
        id = "netflix",
        title = "Netflix",
        subtitle = "Films & Series",
        badgeText = "N",
        primaryPackage = "com.netflix.ninja",
        fallbackPackages = listOf("com.netflix.mediaclient"),
        primaryColor = Color(0xFFE50914),
        secondaryColor = Color(0xFF83050B)
    ),
    TvAppTile(
        id = "hotstar",
        title = "Hotstar",
        subtitle = "Live Sports & Originals",
        badgeText = "D+",
        primaryPackage = "in.startv.hotstar",
        fallbackPackages = listOf("in.startv.hotstar.dplus.tv"),
        primaryColor = Color(0xFF2563EB),
        secondaryColor = Color(0xFF1E3A8A)
    ),
    TvAppTile(
        id = "prime_video",
        title = "Prime Video",
        subtitle = "Amazon Originals & Movies",
        badgeText = "PV",
        primaryPackage = "com.amazon.amazonvideo.livingroom",
        fallbackPackages = listOf("com.amazon.avod.thirdpartyclient"),
        primaryColor = Color(0xFF00A8E1),
        secondaryColor = Color(0xFF0369A1)
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0D14),
                    surface = Color(0xFF131926),
                    onBackground = Color(0xFFF8FAFC),
                    onSurface = Color(0xFFF8FAFC)
                )
            ) {
                BackHandler(enabled = true) {
                    // Prevent exiting the home launcher when pressing Back on the TV remote
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

    LaunchedEffect(Unit) {
        firstCardFocusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0D14)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF162038),
                            Color(0xFF0A0D14)
                        ),
                        radius = 1400f
                    )
                )
                .padding(horizontal = 56.dp, vertical = 40.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TopHeaderBar(
                    onOpenSettings = { launchSystemSettings(context) }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TV_APPS.forEachIndexed { index, app ->
                        val cardModifier = if (index == 0) {
                            Modifier
                                .weight(1f)
                                .focusRequester(firstCardFocusRequester)
                        } else {
                            Modifier.weight(1f)
                        }

                        TvAppFocusableCard(
                            app = app,
                            modifier = cardModifier,
                            onClick = { launchTvApp(context, app) }
                        )
                    }
                }

                FooterHintBar()
            }
        }
    }
}

@Composable
private fun TopHeaderBar(onOpenSettings: () -> Unit) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1_000L)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currentTime,
                color = Color(0xFFF8FAFC),
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentDate,
                color = Color(0xFF94A3B8),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        FocusableSettingsButton(onClick = onOpenSettings)
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
        targetValue = if (isFocused) Color(0xFF38BDF8) else Color(0xFF334155),
        label = "settingsBorder"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF1E293B) else Color(0xFF111827),
        label = "settingsBackground"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(width = if (isFocused) 2.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(50))
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
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚙",
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                fontSize = 18.sp
            )
            Text(
                text = "Settings",
                color = if (isFocused) Color(0xFFF8FAFC) else Color(0xFFCBD5E1),
                fontSize = 16.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TvAppFocusableCard(
    app: TvAppTile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF1E293B),
        label = "cardBorder"
    )

    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .height(270.dp)
            .scale(scale)
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isFocused) {
                        listOf(app.primaryColor, app.secondaryColor)
                    } else {
                        listOf(
                            Color(0xFF182032),
                            Color(0xFF101624)
                        )
                    }
                )
            )
            .border(
                width = if (isFocused) 3.5.dp else 1.5.dp,
                color = borderColor,
                shape = cardShape
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
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFocused) Color.Black.copy(alpha = 0.28f) else app.primaryColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.badgeText,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            Column {
                Text(
                    text = app.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.subtitle,
                    color = if (isFocused) Color(0xFFF1F5F9) else Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isFocused) "PRESS OK TO LAUNCH" else app.primaryPackage,
                    color = if (isFocused) Color.White else Color(0xFF475569),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun FooterHintBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MINIMAL TV LAUNCHER",
            color = Color(0xFF475569),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Use D-Pad ◀ ▶ to select",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "•   Press OK to open app",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        }
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
                // Continue trying fallback package
            }
        }
    }

    // If app is not installed, attempt to open its Google Play Store listing on Android TV
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
            "${app.title} (${app.primaryPackage}) is not installed on this TV.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun launchSystemSettings(context: Context) {
    try {
        val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(settingsIntent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Unable to open TV Settings",
            Toast.LENGTH_SHORT
        ).show()
    }
}
