package com.example.depthclock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { AdvancedDepthClockApp() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AdvancedDepthClockApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // --- СОСТОЯНИЕ ---
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var foregroundBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Храним "Автоматический" цвет отдельно, чтобы к нему можно было вернуться
    var extractedColor by remember { mutableStateOf(Color.White) }

    // Состояние ЧАСОВ
    var clockColor by remember { mutableStateOf(Color.White) } // Текущий цвет часов
    var clockScale by remember { mutableFloatStateOf(1f) }
    var clockOffset by remember { mutableStateOf(Offset.Zero) }
    var isClockAlphaEnabled by remember { mutableStateOf(false) }
    var clockAlpha by remember { mutableFloatStateOf(0.5f) }

    // Состояние КАЛЕНДАРЯ
    var isCalendarVisible by remember { mutableStateOf(true) }
    var calendarColor by remember { mutableStateOf(Color.White) } // Текущий цвет календаря
    var calendarScale by remember { mutableFloatStateOf(1f) }
    var calendarOffset by remember { mutableStateOf(Offset.Zero) }
    var isCalendarAlphaEnabled by remember { mutableStateOf(false) }
    var calendarAlpha by remember { mutableFloatStateOf(0.5f) }

    // Состояние ФОНА
    var isDepthEnabled by remember { mutableStateOf(true) }

    // РЕЖИМЫ
    var isBackgroundEditing by remember { mutableStateOf(false) }
    var isClockEditing by remember { mutableStateOf(false) }
    var isCalendarEditing by remember { mutableStateOf(false) }

    // ДИАЛОГИ
    var showClockSettingsDialog by remember { mutableStateOf(false) }
    var showCalendarSettingsDialog by remember { mutableStateOf(false) }
    var showBackgroundSettingsDialog by remember { mutableStateOf(false) }
    var showWarning by remember { mutableStateOf(false) }

    // --- ЗАГРУЗКА ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val savedState = loadAppState(context)
            savedState?.let { state ->
                withContext(Dispatchers.Main) {
                    originalBitmap = state.bgBitmap
                    foregroundBitmap = state.fgBitmap

                    clockColor = state.clockColor
                    clockScale = state.scale
                    clockOffset = state.offset
                    isClockAlphaEnabled = state.isAlphaEnabled
                    clockAlpha = state.clockAlpha

                    isDepthEnabled = state.isDepthEnabled

                    isCalendarVisible = state.isCalendarVisible
                    calendarColor = state.calendarColor
                    calendarScale = state.calendarScale
                    calendarOffset = state.calendarOffset
                    isCalendarAlphaEnabled = state.isCalendarAlphaEnabled
                    calendarAlpha = state.calendarAlpha

                    extractedColor = state.extractedColor // Загружаем авто-цвет
                }
            }
        }
    }

    fun saveCurrentState() {
        scope.launch(Dispatchers.IO) {
            saveAppState(
                context, originalBitmap, foregroundBitmap,
                clockColor, clockScale, clockOffset, isClockAlphaEnabled, clockAlpha,
                isDepthEnabled,
                isCalendarVisible, calendarColor, calendarScale, calendarOffset, isCalendarAlphaEnabled, calendarAlpha,
                extractedColor
            )
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream?.close()

                        val reqWidth = 1080; val reqHeight = 1920; var inSampleSize = 1
                        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                            val halfHeight = options.outHeight / 2; val halfWidth = options.outWidth / 2
                            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) inSampleSize *= 2
                        }

                        val inputStream2 = context.contentResolver.openInputStream(it)
                        val actualOptions = android.graphics.BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                        val decoded = android.graphics.BitmapFactory.decodeStream(inputStream2, null, actualOptions)
                        inputStream2?.close()
                        decoded?.copy(Bitmap.Config.ARGB_8888, true)
                    } catch (e: Exception) { null }
                }

                if (bitmap != null) {
                    originalBitmap = bitmap

                    // Авто-расстановка
                    clockScale = 1f; calendarScale = 1f
                    if (isCalendarVisible) {
                        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                        val quarterWidth = screenWidthPx / 4f
                        clockOffset = Offset(-quarterWidth, 0f)
                        calendarOffset = Offset(quarterWidth, 0f)
                    } else {
                        clockOffset = Offset.Zero; calendarOffset = Offset.Zero
                    }

                    foregroundBitmap = null
                    isBackgroundEditing = false; isClockEditing = false; isCalendarEditing = false
                    showWarning = false

                    // ГЕНЕРАЦИЯ ЦВЕТА
                    val palette = Palette.from(bitmap).generate()
                    val dom = palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
                    val newAutoColor = dom?.let { Color(it) } ?: Color.White

                    extractedColor = newAutoColor // Сохраняем как "Авто"
                    clockColor = newAutoColor     // Применяем к часам
                    calendarColor = newAutoColor  // Применяем к календарю

                    processImage(bitmap,
                        onSuccess = { fg -> foregroundBitmap = fg; saveCurrentState() },
                        onError = { showWarning = true }
                    )
                }
            }
        }
    }

    // --- UI ---
    Box(modifier = Modifier.fillMaxSize()) {
        // ... (Фон - код без изменений) ...
        if (originalBitmap != null) {
            Image(
                bitmap = originalBitmap!!.asImageBitmap(),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize().combinedClickable(
                    onClick = {
                        if (isBackgroundEditing || isClockEditing || isCalendarEditing) {
                            isBackgroundEditing = false; isClockEditing = false; isCalendarEditing = false
                            saveCurrentState()
                        }
                    },
                    onLongClick = {
                        if (!isClockEditing && !isCalendarEditing) {
                            isBackgroundEditing = true
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    },
                    indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().combinedClickable(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }), contentAlignment = Alignment.Center) { Text("Нажмите, чтобы выбрать фото") }
        }


        // 2. КАЛЕНДАРЬ
        if (originalBitmap != null && isCalendarVisible) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MovableResizableCalendarContent(
                    color = calendarColor, // Используем calendarColor
                    scale = calendarScale, offset = calendarOffset, alpha = if (isCalendarAlphaEnabled) calendarAlpha else 1f,
                    isEditing = isCalendarEditing,
                    onLongPress = {
                        if (!isBackgroundEditing && !isClockEditing) {
                            isCalendarEditing = true; isBackgroundEditing = false; isClockEditing = false
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
        }

        // 3. ЧАСЫ
        if (originalBitmap != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MovableResizableClockContent(
                    color = clockColor, // Используем clockColor
                    scale = clockScale, offset = clockOffset, alpha = if (isClockAlphaEnabled) clockAlpha else 1f,
                    isEditing = isClockEditing,
                    onLongPress = {
                        if (!isBackgroundEditing && !isCalendarEditing) {
                            isClockEditing = true; isBackgroundEditing = false; isCalendarEditing = false
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
        }

        // ... (Передний план, Слои жестов, Кнопки - без изменений) ...
        if (isDepthEnabled) { foregroundBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }

        if (isClockEditing) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> clockScale *= zoom; clockScale = clockScale.coerceIn(0.5f, 7f); clockOffset += pan } }.pointerInput(Unit) { detectTapGestures(onTap = { isClockEditing = false; saveCurrentState() }) }) { Text("Редактирование часов", color = Color.White.copy(0.8f), modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)) } }
        if (isCalendarEditing) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> calendarScale *= zoom; calendarScale = calendarScale.coerceIn(0.5f, 5f); calendarOffset += pan } }.pointerInput(Unit) { detectTapGestures(onTap = { isCalendarEditing = false; saveCurrentState() }) }) { Text("Редактирование календаря", color = Color.White.copy(0.8f), modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)) } }
        if (isBackgroundEditing) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).pointerInput(Unit) { detectTapGestures(onTap = { isBackgroundEditing = false }) }) }

        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.BottomEnd) {
            if (isBackgroundEditing) { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { IconButton(onClick = { showBackgroundSettingsDialog = true }, modifier = Modifier.size(64.dp).background(Color.Black.copy(0.4f), CircleShape)) { Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(32.dp)) }; IconButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(64.dp).background(Color.Black.copy(0.4f), CircleShape)) { Icon(Icons.Default.Image, null, tint = Color.White, modifier = Modifier.size(32.dp)) } } }
            if (isClockEditing) { IconButton(onClick = { showClockSettingsDialog = true }, modifier = Modifier.size(64.dp).background(Color.Black.copy(0.4f), CircleShape)) { Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(32.dp)) } }
            if (isCalendarEditing) { IconButton(onClick = { showCalendarSettingsDialog = true }, modifier = Modifier.size(64.dp).background(Color.Black.copy(0.4f), CircleShape)) { Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(32.dp)) } }
        }

        // 7. ДИАЛОГИ (ОБНОВЛЕННЫЕ)
        if (showClockSettingsDialog) {
            SettingsDialog(
                onDismiss = { showClockSettingsDialog = false; saveCurrentState() },
                isAlphaEnabled = isClockAlphaEnabled, onAlphaEnabledChange = { isClockAlphaEnabled = it },
                clockAlpha = clockAlpha, onClockAlphaChange = { clockAlpha = it },
                // Передаем цвета
                currentColor = clockColor,
                autoColor = extractedColor,
                onColorSelected = { clockColor = it }
            )
        }
        if (showCalendarSettingsDialog) {
            CalendarSettingsDialog(
                onDismiss = { showCalendarSettingsDialog = false; saveCurrentState() },
                isAlphaEnabled = isCalendarAlphaEnabled, onAlphaEnabledChange = { isCalendarAlphaEnabled = it },
                calendarAlpha = calendarAlpha, onCalendarAlphaChange = { calendarAlpha = it },
                // Передаем цвета
                currentColor = calendarColor,
                autoColor = extractedColor,
                onColorSelected = { calendarColor = it }
            )
        }
        if (showBackgroundSettingsDialog) {
            BackgroundSettingsDialog(
                onDismiss = { showBackgroundSettingsDialog = false; saveCurrentState() },
                isDepthEnabled = isDepthEnabled, onDepthEnabledChange = { isDepthEnabled = it },
                isCalendarVisible = isCalendarVisible, onCalendarVisibleChange = { isCalendarVisible = it }
            )
        }
    }
}

// --- КОМПОНЕНТЫ (ОБНОВЛЕННЫЕ ТЕНИ) ---

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MovableResizableClockContent(
    color: Color, scale: Float, offset: Offset, alpha: Float,
    isEditing: Boolean, onLongPress: () -> Unit
) {
    var currentTime by remember { mutableStateOf("00:00") }
    LaunchedEffect(Unit) { while (true) { currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()); delay(1000) } }
    Box(modifier = Modifier.graphicsLayer { translationX = offset.x; translationY = offset.y; scaleX = scale; scaleY = scale; this.alpha = alpha }.combinedClickable(onClick = {}, onLongClick = onLongPress, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })) {
        Text(
            text = currentTime, fontSize = 100.sp, fontWeight = FontWeight.Bold,
            color = if (isEditing) color.copy(alpha = 0.8f) else color,
            // ИЗМЕНЕНО: Более жесткая тень для читаемости
            style = LocalTextStyle.current.copy(shadow = Shadow(Color.Black, blurRadius = 10f))
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MovableResizableCalendarContent(
    color: Color, scale: Float, offset: Offset, alpha: Float,
    isEditing: Boolean, onLongPress: () -> Unit
) {
    val currentDate = remember { LocalDate.now() }
    val yearMonth = remember { YearMonth.from(currentDate) }
    Box(modifier = Modifier.graphicsLayer { translationX = offset.x; translationY = offset.y; scaleX = scale; scaleY = scale; this.alpha = alpha }.combinedClickable(onClick = {}, onLongClick = onLongPress, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${yearMonth.year}",
                fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = if (isEditing) color.copy(alpha = 0.8f) else color,
                // ИЗМЕНЕНО: Жесткая тень
                style = LocalTextStyle.current.copy(shadow = Shadow(Color.Black, blurRadius = 10f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Row { listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day -> Text(text = day, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.9f), style = LocalTextStyle.current.copy(shadow = Shadow(Color.Black, blurRadius = 5f))) } }
                val daysInMonth = yearMonth.lengthOfMonth(); val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value; var currentDay = 1
                for (week in 0..5) { if (currentDay > daysInMonth) break; Row { for (dayOfWeek in 1..7) { if (week == 0 && dayOfWeek < firstDayOfWeek) { Spacer(modifier = Modifier.width(30.dp)) } else if (currentDay <= daysInMonth) { val isToday = currentDay == currentDate.dayOfMonth; Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) { if (isToday) { Box(modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.3f), CircleShape)) }; Text(text = currentDay.toString(), fontSize = 16.sp, color = if (isToday) Color.Red else color, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, style = LocalTextStyle.current.copy(shadow = Shadow(Color.Black, blurRadius = 5f))) }; currentDay++ } else { Spacer(modifier = Modifier.width(30.dp)) } } } }
            }
        }
    }
}

// --- ЛОГИКА СОХРАНЕНИЯ (Добавляем новые поля) ---

fun saveAppState(
    context: Context, bgBitmap: Bitmap?, fgBitmap: Bitmap?,
    clockColor: Color, clockScale: Float, clockOffset: Offset, isClockAlphaEnabled: Boolean, clockAlpha: Float,
    isDepthEnabled: Boolean,
    isCalendarVisible: Boolean, calendarColor: Color, calendarScale: Float, calendarOffset: Offset, isCalendarAlphaEnabled: Boolean, calendarAlpha: Float,
    extractedColor: Color // НОВОЕ
) {
    val prefs = context.getSharedPreferences("ClockPrefs", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putInt("clockColor", clockColor.toArgb()) // Переименовали color -> clockColor
        putFloat("scale", clockScale); putFloat("offsetX", clockOffset.x); putFloat("offsetY", clockOffset.y)
        putBoolean("isAlphaEnabled", isClockAlphaEnabled); putFloat("clockAlpha", clockAlpha)

        putBoolean("isCalendarVisible", isCalendarVisible)
        putInt("calendarColor", calendarColor.toArgb()) // НОВОЕ
        putFloat("calendarScale", calendarScale); putFloat("calendarOffsetX", calendarOffset.x); putFloat("calendarOffsetY", calendarOffset.y)
        putBoolean("isCalendarAlphaEnabled", isCalendarAlphaEnabled); putFloat("calendarAlpha", calendarAlpha)

        putBoolean("isDepthEnabled", isDepthEnabled); putBoolean("hasImages", bgBitmap != null)
        putInt("extractedColor", extractedColor.toArgb()) // НОВОЕ
        apply()
    }
    if (bgBitmap != null) saveBitmapToFile(context, bgBitmap, "background.png")
    if (fgBitmap != null) saveBitmapToFile(context, fgBitmap, "foreground.png")
}

data class AppState(
    val bgBitmap: Bitmap?, val fgBitmap: Bitmap?,
    val clockColor: Color, val scale: Float, val offset: Offset, val isAlphaEnabled: Boolean, val clockAlpha: Float,
    val isDepthEnabled: Boolean,
    val isCalendarVisible: Boolean, val calendarColor: Color, val calendarScale: Float, val calendarOffset: Offset, val isCalendarAlphaEnabled: Boolean, val calendarAlpha: Float,
    val extractedColor: Color
)

fun loadAppState(context: Context): AppState? {
    val prefs = context.getSharedPreferences("ClockPrefs", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("hasImages", false)) return null

    val clockColor = Color(prefs.getInt("clockColor", prefs.getInt("color", Color.White.toArgb()))) // Обратная совместимость
    val scale = prefs.getFloat("scale", 1f); val offsetX = prefs.getFloat("offsetX", 0f); val offsetY = prefs.getFloat("offsetY", 0f)
    val isAlphaEnabled = prefs.getBoolean("isAlphaEnabled", false); val clockAlpha = prefs.getFloat("clockAlpha", 0.5f)

    val isCalendarVisible = prefs.getBoolean("isCalendarVisible", true)
    val calendarColor = Color(prefs.getInt("calendarColor", clockColor.toArgb())) // По умолчанию как часы
    val calendarScale = prefs.getFloat("calendarScale", 1f); val calendarOffsetX = prefs.getFloat("calendarOffsetX", 0f); val calendarOffsetY = prefs.getFloat("calendarOffsetY", 0f)
    val isCalendarAlphaEnabled = prefs.getBoolean("isCalendarAlphaEnabled", false); val calendarAlpha = prefs.getFloat("calendarAlpha", 0.5f)

    val isDepthEnabled = prefs.getBoolean("isDepthEnabled", true)
    val extractedColor = Color(prefs.getInt("extractedColor", Color.White.toArgb()))

    val bgBitmap = loadBitmapFromFile(context, "background.png")
    val fgBitmap = loadBitmapFromFile(context, "foreground.png")

    return AppState(
        bgBitmap, fgBitmap, clockColor, scale, Offset(offsetX, offsetY), isAlphaEnabled, clockAlpha,
        isDepthEnabled,
        isCalendarVisible, calendarColor, calendarScale, Offset(calendarOffsetX, calendarOffsetY), isCalendarAlphaEnabled, calendarAlpha,
        extractedColor
    )
}
// saveBitmapToFile и loadBitmapFromFile остаются прежними (они у вас есть)
// Вспомогательные функции сохранения файлов
fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String) {
    try {
        val file = File(context.filesDir, fileName)
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush(); stream.close()
    } catch (e: Exception) { e.printStackTrace() }
}

fun loadBitmapFromFile(context: Context, fileName: String): Bitmap? {
    return try {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            val source = ImageDecoder.createSource(file)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
        } else null
    } catch (e: Exception) { e.printStackTrace(); null }
}

fun processImage(
    bitmap: Bitmap,
    onSuccess: (Bitmap?) -> Unit,
    onError: () -> Unit
) {
    try {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()

        val segmenter = SubjectSegmentation.getClient(options)
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        segmenter.process(inputImage)
            .addOnSuccessListener { result ->
                // result.foregroundBitmap может быть null, если объект не найден
                onSuccess(result.foregroundBitmap)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onError()
            }
    } catch (e: Exception) {
        e.printStackTrace()
        onError()
    }
}