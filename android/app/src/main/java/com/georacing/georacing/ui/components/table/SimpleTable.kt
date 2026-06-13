package com.georacing.georacing.ui.components.table

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SimpleTable(headers: List<String>, rows: List<List<String>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            headers.forEach { h ->
                Text(
                    h.uppercase(),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        HorizontalDivider()
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                row.forEach { cell ->
                    Text(
                        cell,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            HorizontalDivider()
        }
    }
}
