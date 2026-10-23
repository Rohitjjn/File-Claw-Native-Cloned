package com.example.formats.model3d.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.formats.model3d.Mesh
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun Model3dViewer(mesh: Mesh, label: String, modifier: Modifier = Modifier) {
    var rotX by remember { mutableStateOf(-0.5f) }
    var rotY by remember { mutableStateOf(0.6f) }
    var zoom by remember { mutableStateOf(1f) }
    val bounds = remember(mesh) { mesh.bounds() }
    val wire = MaterialTheme.colorScheme.primary
    val maxDrawn = 25_000
    val stride = remember(mesh) { max(1, mesh.triangleCount / maxDrawn) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, z, _ ->
                    rotY += pan.x * 0.01f
                    rotX += pan.y * 0.01f
                    zoom = (zoom * z).coerceIn(0.2f, 10f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = (bounds[0] + bounds[3]) / 2f
            val cy = (bounds[1] + bounds[4]) / 2f
            val cz = (bounds[2] + bounds[5]) / 2f
            val extent = max(bounds[3] - bounds[0], max(bounds[4] - bounds[1], bounds[5] - bounds[2])).coerceAtLeast(1e-6f)
            val scale = (minOf(size.width, size.height) / extent) * 0.8f * zoom
            val sx = sin(rotX); val cxr = cos(rotX)
            val sy = sin(rotY); val cyr = cos(rotY)
            val mid = Offset(size.width / 2f, size.height / 2f)
            val p = mesh.positions
            val path = Path()

            fun project(i: Int): Offset {
                val x = p[i] - cx
                val y = p[i + 1] - cy
                val z = p[i + 2] - cz
                // rotate around Y, then X
                val x1 = x * cyr + z * sy
                val z1 = -x * sy + z * cyr
                val y1 = y * cxr - z1 * sx
                return Offset(mid.x + x1 * scale, mid.y - y1 * scale)
            }

            var t = 0
            while (t < mesh.triangleCount) {
                val b = t * 9
                val a = project(b)
                val c = project(b + 3)
                val d = project(b + 6)
                path.moveTo(a.x, a.y)
                path.lineTo(c.x, c.y)
                path.lineTo(d.x, d.y)
                path.close()
                t += stride
            }
            drawPath(path, wire.copy(alpha = 0.85f), style = Stroke(width = 1f))
        }
        Text(
            text = buildString {
                append(label).append(" \u2022 ").append(mesh.totalTriangles).append(" triangles")
                if (stride > 1 || mesh.truncated) append(" (showing a sample)")
                append("\nDrag to rotate \u2022 pinch to zoom")
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )
    }
}
