package com.example.formats.binary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.ui.component.formatFileSize
import com.example.ui.component.formatElapsedTime
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun HexViewer(
    hexRows: List<String>,
    asciiRows: List<String>,
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = "BINARY / HEX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E)) // Dark code editor styled background for hex contrast
                .padding(8.dp)
        ) {
            items(hexRows.size, key = { it }, contentType = { "HexRow" }) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).animateItemPlacement(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hexRows[index],
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFD4D4D4)
                    )
                    Text(
                        text = "| ${if (index < asciiRows.size) asciiRows[index] else ""}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF9CDCFE),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
