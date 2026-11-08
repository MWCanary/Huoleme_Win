package com.huoleme.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Period

@Composable
fun BirthDateEditor(
    initialDate: String,
    onSave: (String) -> Unit,
    saveLabel: String = "保存生日",
    modifier: Modifier = Modifier
) {
    var year by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(initialDate) {
        val parts = initialDate.split("-")
        year = parts.getOrNull(0).orEmpty()
        month = parts.getOrNull(1).orEmpty()
        day = parts.getOrNull(2).orEmpty()
        error = false
    }

    val date = parseDate(year, month, day)
    val ageText = date?.let { birth ->
        val period = Period.between(birth, LocalDate.now())
        "已生存 ${period.years} 年 ${period.months} 月 ${period.days} 天"
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = year,
                onValueChange = {
                    year = it.filter(Char::isDigit).take(4)
                    error = false
                },
                label = { Text("年") },
                placeholder = { Text("1995") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.35f)
            )
            OutlinedTextField(
                value = month,
                onValueChange = {
                    month = it.filter(Char::isDigit).take(2)
                    error = false
                },
                label = { Text("月") },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = day,
                onValueChange = {
                    day = it.filter(Char::isDigit).take(2)
                    error = false
                },
                label = { Text("日") },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        if (error) {
            Text(
                text = "请输入正确的日期（年份 1900 至今年）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (ageText != null) {
            Text(
                text = ageText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                when {
                    year.isBlank() && month.isBlank() && day.isBlank() -> {
                        error = false
                        onSave("")
                    }

                    year.isBlank() || month.isBlank() || day.isBlank() -> {
                        error = true
                    }

                    date == null -> {
                        error = true
                    }

                    else -> {
                        error = false
                        onSave(date.toString())
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(top = 2.dp)
        ) {
            Text(saveLabel)
        }
    }
}

private fun parseDate(yearText: String, monthText: String, dayText: String): LocalDate? {
    if (yearText.isEmpty() || monthText.isEmpty() || dayText.isEmpty()) return null
    val year = yearText.toIntOrNull() ?: return null
    val month = monthText.toIntOrNull() ?: return null
    val day = dayText.toIntOrNull() ?: return null
    if (year !in 1900..LocalDate.now().year) return null
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}
