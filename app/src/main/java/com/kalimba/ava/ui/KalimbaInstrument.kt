package com.kalimba.ava.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.kalimba.ava.core.KalimbaLayout

/** Layout constants shared by the instrument and the falling-note lanes in the trainer. */
object KalimbaGeom {
    const val MARGIN = 0.07f
    fun slotWidth(w: Float) = w * (1f - 2f * MARGIN) / KalimbaLayout.KEY_COUNT
    fun slotCenterX(w: Float, slot: Int) = w * MARGIN + (slot + 0.5f) * slotWidth(w)
    const val TOP = 0.06f
    const val BRIDGE = 0.30f
    const val BASE_LEN = 0.60f
}

/**
 * Realistic 17-key kalimba drawn on a Canvas with multi-touch support.
 * activeKeys glow (e.g. detected from the microphone); guideKeys are the next expected notes.
 */
@Composable
fun KalimbaInstrument(
    modifier: Modifier = Modifier,
    activeKeys: Set<Int> = emptySet(),
    guideKeys: Set<Int> = emptySet(),
    showNames: Boolean = true,
    showNumbers: Boolean = true,
    interactive: Boolean = true,
    onPress: (Int) -> Unit = {},
) {
    val pressedAt = remember { mutableStateMapOf<Int, Long>() }
    var held by remember { mutableStateOf(setOf<Int>()) }
    var pressCount by remember { mutableIntStateOf(0) }
    var frameTick by remember { mutableLongStateOf(0L) }
    val onPressState by rememberUpdatedState(onPress)

    LaunchedEffect(pressCount) {
        if (pressCount == 0) return@LaunchedEffect
        val end = System.nanoTime() + 600_000_000L
        while (System.nanoTime() < end) withFrameNanos { frameTick = it }
        frameTick = 0L
    }

    val tm = rememberTextMeasurer()
    val numStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A2A1E), fontFamily = VazirFont)
    val nameStyle = TextStyle(fontSize = 8.sp, color = Color(0xFFF3E3C8), fontFamily = VazirFont)
    val numLayouts = remember(showNumbers) {
        if (showNumbers) (1..17).map { tm.measure(AnnotatedString(KalimbaLayout.numberLabel(it)), numStyle) } else emptyList()
    }
    val nameLayouts = remember(showNames) {
        if (showNames) (1..17).map { tm.measure(AnnotatedString(KalimbaLayout.noteLabel(it)), nameStyle) } else emptyList()
    }

    fun keyAt(x: Float, y: Float, w: Float, h: Float): Int? {
        val sw = KalimbaGeom.slotWidth(w)
        val rel = (x - w * KalimbaGeom.MARGIN) / sw
        if (rel < 0f || rel >= KalimbaLayout.KEY_COUNT) return null
        val key = KalimbaLayout.keyAtSlot(rel.toInt())
        val top = h * KalimbaGeom.TOP
        val bottom = top + h * KalimbaGeom.BASE_LEN * KalimbaLayout.tineLengthFraction(key) + h * 0.03f
        return if (y in top..bottom) key else null
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(interactive) {
                if (!interactive) return@pointerInput
                val owner = HashMap<Long, Int>()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (c in event.changes) {
                            val id = c.id.value
                            if (c.pressed) {
                                val key = keyAt(c.position.x, c.position.y, size.width.toFloat(), size.height.toFloat())
                                val prev = owner[id]
                                if (key != null && key != prev) {
                                    owner[id] = key
                                    pressedAt[key] = System.nanoTime()
                                    pressCount++
                                    onPressState(key)
                                }
                                c.consume()
                            } else owner.remove(id)
                        }
                        held = owner.values.toSet()
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val subscribe = frameTick
        val w = size.width
        val h = size.height
        val now = System.nanoTime()

        // wooden body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFA25F3B), Color(0xFF7A4326), Color(0xFF512815))),
            cornerRadius = CornerRadius(w * 0.07f), size = Size(w, h),
        )
        // grain
        for (i in 0 until 14) {
            val y = h * (0.05f + i * 0.068f)
            drawLine(Color(0x22000000), Offset(w * 0.04f, y), Offset(w * 0.96f, y + h * 0.006f), strokeWidth = 1.2f)
        }
        drawRoundRect(Color(0x55FFE2B8), cornerRadius = CornerRadius(w * 0.07f), size = Size(w, h), style = Stroke(width = w * 0.006f))

        // sound hole with gold ring
        val hc = Offset(w / 2f, h * 0.84f)
        drawCircle(Color(0xFF160B06), radius = w * 0.085f, center = hc)
        drawCircle(Color(0xFFD5AA63), radius = w * 0.092f, center = hc, style = Stroke(width = w * 0.008f))
        drawCircle(Color(0x33D5AA63), radius = w * 0.13f, center = hc, style = Stroke(width = w * 0.004f))

        // rear rail and bridge bar
        val left = w * KalimbaGeom.MARGIN * 0.6f
        val barW = w - 2 * left
        drawRoundRect(Color(0xFF3B1E10), Offset(left, h * 0.045f), Size(barW, h * 0.035f), CornerRadius(8f))
        val by = h * KalimbaGeom.BRIDGE
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFF2F3F5), Color(0xFF9EA2A9)), startY = by, endY = by + h * 0.028f),
            Offset(left, by), Size(barW, h * 0.028f), CornerRadius(10f),
        )

        // tines
        val sw = KalimbaGeom.slotWidth(w)
        val tw = sw * 0.76f
        val top = h * KalimbaGeom.TOP
        for (slot in 0 until KalimbaLayout.KEY_COUNT) {
            val key = KalimbaLayout.keyAtSlot(slot)
            val cx = KalimbaGeom.slotCenterX(w, slot)
            val len = h * KalimbaGeom.BASE_LEN * KalimbaLayout.tineLengthFraction(key)
            val age = pressedAt[key]?.let { (now - it) / 1_000_000_000f } ?: 9f
            val decay = (1f - age / 0.5f).coerceIn(0f, 1f)
            val isHeld = key in held
            val glow = when {
                key in activeKeys -> 1f
                isHeld -> 1f
                else -> decay
            }
            val dy = if (isHeld || key in activeKeys) h * 0.006f else h * 0.006f * decay
            val x0 = cx - tw / 2f
            // metal
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFF9FA3AA), Color(0xFFF4F5F7), Color(0xFFA7ABB2)), startX = x0, endX = x0 + tw),
                Offset(x0, top + dy), Size(tw, len), CornerRadius(tw * 0.45f),
            )
            if (glow > 0f) {
                drawRoundRect(Color(0xFFFFB347).copy(alpha = 0.75f * glow), Offset(x0, top + dy), Size(tw, len), CornerRadius(tw * 0.45f))
            }
            if (key in guideKeys) {
                drawRoundRect(Color(0xFF3DDC84).copy(alpha = 0.55f), Offset(x0, top + dy), Size(tw, len), CornerRadius(tw * 0.45f))
                drawRoundRect(Color(0xFF1B8F4B), Offset(x0 - 2f, top + dy - 2f), Size(tw + 4f, len + 4f), CornerRadius(tw * 0.5f), style = Stroke(width = 3f))
            }
            // labels
            if (showNumbers) {
                val l = numLayouts[key - 1]
                drawText(l, topLeft = Offset(cx - l.size.width / 2f, top + dy + len - l.size.height - tw * 0.25f))
            }
            if (showNames) {
                val l = nameLayouts[key - 1]
                drawText(l, topLeft = Offset(cx - l.size.width / 2f, top + len + h * 0.012f))
            }
        }
    }
}
