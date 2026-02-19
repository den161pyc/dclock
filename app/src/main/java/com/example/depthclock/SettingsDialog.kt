package com.example.depthclock

import androidx.compose.foundation.background
import androidx.compose.foundation.border // Добавили
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Добавили
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

// Синий цвет Samsung OneUI
val OneUIBlue = Color(0xFF0072DE)

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    isAlphaEnabled: Boolean,
    onAlphaEnabledChange: (Boolean) -> Unit,
    clockAlpha: Float,
    onClockAlphaChange: (Float) -> Unit,
    // НОВЫЕ ПАРАМЕТРЫ ДЛЯ ЦВЕТА
    currentColor: Color,
    autoColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, start = 20.dp, end = 12.dp)) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Настройки часов", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    // --- ВЫБОР ЦВЕТА (НОВОЕ) ---
                    Text("Цвет текста", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // 1. Белый
                        ColorCircle(color = Color.White, isSelected = currentColor == Color.White, onClick = { onColorSelected(Color.White) })
                        // 2. Черный
                        ColorCircle(color = Color.Black, isSelected = currentColor == Color.Black, onClick = { onColorSelected(Color.Black) })
                        // 3. Авто (из фото)
                        ColorCircle(color = autoColor, isSelected = currentColor == autoColor, onClick = { onColorSelected(autoColor) }, isAuto = true)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- ПРОЗРАЧНОСТЬ ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Прозрачность", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${(clockAlpha * 100).roundToInt()}%",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = if (isAlphaEnabled) OneUIBlue else Color.Gray
                            )
                        }
                        Switch(
                            checked = isAlphaEnabled,
                            onCheckedChange = onAlphaEnabledChange,
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = OneUIBlue,
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = clockAlpha, onValueChange = onClockAlphaChange, enabled = isAlphaEnabled,
                        valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().height(20.dp),
                        colors = SliderDefaults.colors(thumbColor = OneUIBlue, activeTrackColor = OneUIBlue, inactiveTrackColor = OneUIBlue.copy(0.2f))
                    )
                }
            }
        }
    }
}

// Компонент кружка с цветом
@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit, isAuto: Boolean = false) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) OneUIBlue else Color.Gray.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isAuto) {
            Text("A", color = if (color.luminance() > 0.5) Color.Black else Color.White, fontWeight = FontWeight.Bold)
        }
    }
}