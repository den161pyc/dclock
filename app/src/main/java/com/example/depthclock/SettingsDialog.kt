package com.example.depthclock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,              // Функция закрытия окна
    isAlphaEnabled: Boolean,            // Включена ли прозрачность
    onAlphaEnabledChange: (Boolean) -> Unit, // Что делать при переключении
    clockAlpha: Float,                  // Текущее значение прозрачности
    onClockAlphaChange: (Float) -> Unit // Что делать при движении слайдера
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Настройки вида",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Контейнер настроек
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    // 1. Заголовок и переключатель
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Прозрачность",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(clockAlpha * 100).roundToInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isAlphaEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        Switch(
                            checked = isAlphaEnabled,
                            onCheckedChange = onAlphaEnabledChange
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Слайдер
                    Slider(
                        value = clockAlpha,
                        onValueChange = onClockAlphaChange,
                        enabled = isAlphaEnabled,
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Шкала с рисками (0, 25, 50, 75, 100)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val steps = listOf(0, 25, 50, 75, 100)
                        steps.forEach { step ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable(enabled = isAlphaEnabled) {
                                        onClockAlphaChange(step / 100f)
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(4.dp)
                                        .background(
                                            if (isAlphaEnabled) Color.Gray.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.2f)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$step",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAlphaEnabled) Color.Gray else Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка Закрыть
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}