package com.example.depthclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun BackgroundSettingsDialog(
    onDismiss: () -> Unit,
    isDepthEnabled: Boolean,
    onDepthEnabledChange: (Boolean) -> Unit,
    isCalendarVisible: Boolean,             // НОВОЕ
    onCalendarVisibleChange: (Boolean) -> Unit // НОВОЕ
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
                // Заголовок
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
                    // Опция Глубины
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Эффект глубины", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (isDepthEnabled) "Объект перекрывает часы" else "Часы поверх всего",
                                fontSize = 12.sp, color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isDepthEnabled,
                            onCheckedChange = onDepthEnabledChange,
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = OneUIBlue,
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f)) // Разделитель
                    Spacer(modifier = Modifier.height(12.dp))

                    // Опция Календаря (НОВОЕ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Календарь", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Показывать календарь на фоне", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isCalendarVisible,
                            onCheckedChange = onCalendarVisibleChange,
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = OneUIBlue,
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}