package com.daniel.flighttracker.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.daniel.flighttracker.data.AircraftIcon

private val AirborneFill = Color(0xFF4FA3FF)
private val GroundFill = Color(0xFF96A0B4)

@Composable
fun PlaneIcon(
    heading: Float,
    onGround: Boolean,
    icon: AircraftIcon,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val bucket = (((heading + 360) % 360).toInt() / 15 * 15).toFloat()
        rotate(bucket, pivot = center) {
            val fill = if (onGround) GroundFill else AirborneFill
            when (icon) {
                AircraftIcon.ARROW -> drawArrow(fill, Color.Black.copy(alpha = 0.7f))
                AircraftIcon.AIRCRAFT -> drawSilhouette(fill, Color.Black.copy(alpha = 0.7f))
            }
        }
    }
}

fun DrawScope.drawArrowPath(
    cx: Float,
    cy: Float,
    sizePx: Float,
    fill: Color,
    outline: Color
) {
    val s = sizePx
    val wingSpan = s * 0.38f
    val nose = cy - s * 0.36f
    val tail = cy + s * 0.36f
    val path = Path().apply {
        moveTo(cx, nose)
        lineTo(cx - wingSpan, tail)
        lineTo(cx, tail - s * 0.12f)
        lineTo(cx + wingSpan, tail)
        close()
    }
    drawPath(path, fill)
    drawPath(path, outline, style = Stroke(1.5f))
}

fun DrawScope.drawSilhouettePath(
    cx: Float,
    cy: Float,
    sizePx: Float,
    fill: Color,
    outline: Color
) {
    val s = sizePx
    val path = Path().apply {
        moveTo(cx + 0.1602f * s, cy + 0.4990f * s)
        lineTo(cx + 0.1445f * s, cy + 0.4951f * s)
        lineTo(cx + 0.1348f * s, cy + 0.4951f * s)
        lineTo(cx + 0.1328f * s, cy + 0.4932f * s)
        lineTo(cx + 0.1230f * s, cy + 0.4932f * s)
        lineTo(cx + 0.1113f * s, cy + 0.4893f * s)
        lineTo(cx + 0.0781f * s, cy + 0.4854f * s)
        lineTo(cx + 0.0664f * s, cy + 0.4814f * s)
        lineTo(cx + 0.0449f * s, cy + 0.4795f * s)
        lineTo(cx + 0.0332f * s, cy + 0.4756f * s)
        lineTo(cx + 0.0117f * s, cy + 0.4736f * s)
        lineTo(cx + 0.0098f * s, cy + 0.4717f * s)
        lineTo(cx + -0.0117f * s, cy + 0.4717f * s)
        lineTo(cx + -0.0234f * s, cy + 0.4756f * s)
        lineTo(cx + -0.1016f * s, cy + 0.4873f * s)
        lineTo(cx + -0.1035f * s, cy + 0.4893f * s)
        lineTo(cx + -0.1348f * s, cy + 0.4932f * s)
        lineTo(cx + -0.1465f * s, cy + 0.4971f * s)
        lineTo(cx + -0.1631f * s, cy + 0.4980f * s)
        lineTo(cx + -0.1631f * s, cy + 0.4453f * s)
        lineTo(cx + -0.0537f * s, cy + 0.3848f * s)
        lineTo(cx + -0.0576f * s, cy + 0.2676f * s)
        lineTo(cx + -0.0596f * s, cy + 0.2656f * s)
        lineTo(cx + -0.0596f * s, cy + 0.2227f * s)
        lineTo(cx + -0.0615f * s, cy + 0.2207f * s)
        lineTo(cx + -0.0615f * s, cy + 0.1777f * s)
        lineTo(cx + -0.0635f * s, cy + 0.1758f * s)
        lineTo(cx + -0.0635f * s, cy + 0.1328f * s)
        lineTo(cx + -0.0654f * s, cy + 0.1309f * s)
        lineTo(cx + -0.0654f * s, cy + 0.0879f * s)
        lineTo(cx + -0.0674f * s, cy + 0.0859f * s)
        lineTo(cx + -0.0684f * s, cy + 0.0557f * s)
        lineTo(cx + -0.2148f * s, cy + 0.1143f * s)
        lineTo(cx + -0.2246f * s, cy + 0.1201f * s)
        lineTo(cx + -0.2480f * s, cy + 0.1279f * s)
        lineTo(cx + -0.2578f * s, cy + 0.1338f * s)
        lineTo(cx + -0.2812f * s, cy + 0.1416f * s)
        lineTo(cx + -0.2910f * s, cy + 0.1475f * s)
        lineTo(cx + -0.3145f * s, cy + 0.1553f * s)
        lineTo(cx + -0.3242f * s, cy + 0.1611f * s)
        lineTo(cx + -0.4043f * s, cy + 0.1924f * s)
        lineTo(cx + -0.4141f * s, cy + 0.1982f * s)
        lineTo(cx + -0.4238f * s, cy + 0.2021f * s)
        lineTo(cx + -0.4268f * s, cy + 0.2012f * s)
        lineTo(cx + -0.4268f * s, cy + 0.1211f * s)
        lineTo(cx + -0.2705f * s, cy + 0.0059f * s)
        lineTo(cx + -0.2686f * s, cy + 0.0020f * s)
        lineTo(cx + -0.2676f * s, cy + -0.0869f * s)
        lineTo(cx + -0.2051f * s, cy + -0.0869f * s)
        lineTo(cx + -0.2041f * s, cy + -0.0469f * s)
        lineTo(cx + -0.2012f * s, cy + -0.0459f * s)
        lineTo(cx + -0.0732f * s, cy + -0.1406f * s)
        lineTo(cx + -0.0732f * s, cy + -0.3496f * s)
        lineTo(cx + -0.0713f * s, cy + -0.3516f * s)
        lineTo(cx + -0.0713f * s, cy + -0.3730f * s)
        lineTo(cx + -0.0693f * s, cy + -0.3750f * s)
        lineTo(cx + -0.0674f * s, cy + -0.3984f * s)
        lineTo(cx + -0.0576f * s, cy + -0.4355f * s)
        lineTo(cx + -0.0439f * s, cy + -0.4668f * s)
        lineTo(cx + -0.0361f * s, cy + -0.4785f * s)
        lineTo(cx + -0.0195f * s, cy + -0.4951f * s)
        lineTo(cx + -0.0078f * s, cy + -0.5010f * s)
        lineTo(cx + 0.0059f * s, cy + -0.5010f * s)
        lineTo(cx + 0.0176f * s, cy + -0.4951f * s)
        lineTo(cx + 0.0342f * s, cy + -0.4785f * s)
        lineTo(cx + 0.0479f * s, cy + -0.4551f * s)
        lineTo(cx + 0.0635f * s, cy + -0.4082f * s)
        lineTo(cx + 0.0674f * s, cy + -0.3750f * s)
        lineTo(cx + 0.0693f * s, cy + -0.3730f * s)
        lineTo(cx + 0.0693f * s, cy + -0.3516f * s)
        lineTo(cx + 0.0713f * s, cy + -0.3496f * s)
        lineTo(cx + 0.0713f * s, cy + -0.1406f * s)
        lineTo(cx + 0.1992f * s, cy + -0.0459f * s)
        lineTo(cx + 0.2021f * s, cy + -0.0469f * s)
        lineTo(cx + 0.2031f * s, cy + -0.0869f * s)
        lineTo(cx + 0.2656f * s, cy + -0.0869f * s)
        lineTo(cx + 0.2666f * s, cy + 0.0020f * s)
        lineTo(cx + 0.2686f * s, cy + 0.0059f * s)
        lineTo(cx + 0.4248f * s, cy + 0.1211f * s)
        lineTo(cx + 0.4238f * s, cy + 0.2021f * s)
        lineTo(cx + 0.3652f * s, cy + 0.1787f * s)
        lineTo(cx + 0.3555f * s, cy + 0.1729f * s)
        lineTo(cx + 0.3320f * s, cy + 0.1650f * s)
        lineTo(cx + 0.3223f * s, cy + 0.1592f * s)
        lineTo(cx + 0.2988f * s, cy + 0.1514f * s)
        lineTo(cx + 0.2891f * s, cy + 0.1455f * s)
        lineTo(cx + 0.2656f * s, cy + 0.1377f * s)
        lineTo(cx + 0.2559f * s, cy + 0.1318f * s)
        lineTo(cx + 0.2324f * s, cy + 0.1240f * s)
        lineTo(cx + 0.2227f * s, cy + 0.1182f * s)
        lineTo(cx + 0.1992f * s, cy + 0.1104f * s)
        lineTo(cx + 0.1895f * s, cy + 0.1045f * s)
        lineTo(cx + 0.1660f * s, cy + 0.0967f * s)
        lineTo(cx + 0.1562f * s, cy + 0.0908f * s)
        lineTo(cx + 0.1328f * s, cy + 0.0830f * s)
        lineTo(cx + 0.1230f * s, cy + 0.0771f * s)
        lineTo(cx + 0.0996f * s, cy + 0.0693f * s)
        lineTo(cx + 0.0898f * s, cy + 0.0635f * s)
        lineTo(cx + 0.0703f * s, cy + 0.0557f * s)
        lineTo(cx + 0.0654f * s, cy + 0.0566f * s)
        lineTo(cx + 0.0518f * s, cy + 0.3848f * s)
        lineTo(cx + 0.1611f * s, cy + 0.4453f * s)
        lineTo(cx + 0.1602f * s, cy + 0.4990f * s)
        close()
    }
    drawPath(path, fill)
    drawPath(path, outline, style = Stroke(1.5f))
}

private fun DrawScope.drawArrow(fill: Color, outline: Color) {
    drawArrowPath(size.width / 2f, size.height / 2f, size.minDimension, fill, outline)
}

private fun DrawScope.drawSilhouette(fill: Color, outline: Color) {
    drawSilhouettePath(size.width / 2f, size.height / 2f, size.minDimension, fill, outline)
}
