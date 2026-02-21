package com.example.depthclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign

@Composable
fun BackgroundSettingsDialog(
    onDismiss: () -> Unit,
    isDepthEnabled: Boolean,
    onDepthEnabledChange: (Boolean) -> Unit,
    isCalendarVisible: Boolean,
    onCalendarVisibleChange: (Boolean) -> Unit,
    onSelectFontClick: () -> Unit,
    onResetFontClick: () -> Unit,
    hasCustomFont: Boolean,
    orientation: Int,
    onOrientationChange: (Int) -> Unit
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
            Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp)) {
                // Заголовок (без изменений)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Настройки фона", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    // Опция Глубины (без изменений) ...
                    // [оставьте существующий код переключателя isDepthEnabled]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Эффект глубины", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(if (isDepthEnabled) "Объект перекрывает часы" else "Часы поверх всего", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(checked = isDepthEnabled, onCheckedChange = onDepthEnabledChange, modifier = Modifier.scale(0.8f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OneUIBlue, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f), uncheckedBorderColor = Color.Transparent))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Опция Календаря (без изменений) ...
                    // [оставьте существующий код переключателя isCalendarVisible]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Календарь", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Показывать календарь на фоне", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(checked = isCalendarVisible, onCheckedChange = onCalendarVisibleChange, modifier = Modifier.scale(0.8f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OneUIBlue, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f), uncheckedBorderColor = Color.Transparent))
                    }

                    // --- НОВЫЙ БЛОК: Выбор ориентации ---
                    Spacer(modifier = Modifier.height(12.dp))
                    //HorizontalDivider(color = OneUIBlue)
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Ориентация экрана", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val options = listOf(
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR to "Авто",
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to "Вертик.",
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to "Гориз."
                        )
                        options.forEach { (value, label) ->
                            val isSelected = orientation == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) OneUIBlue else Color.Gray.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onOrientationChange(value) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // --- НОВЫЙ БЛОК: Опция Шрифта ---
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Пользовательский шрифт", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Выберите файл (.ttf или .otf)", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    /*Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Пользовательский шрифт", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Выберите файл (.ttf или .otf)", fontSize = 12.sp, color = Color.Gray)
                        }*/
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (hasCustomFont) {
                                IconButton(
                                    onClick = onResetFontClick,
                                    modifier = Modifier.size(48.dp).background(Color.Red.copy(0.1f),RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Сбросить", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                            Button(
                                onClick = onSelectFontClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (hasCustomFont) "Изменить" else "Выбрать", fontSize = 12.sp)
                            }
                        //}
                    }
                }
            }
        }
    }
}