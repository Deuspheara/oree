package fr.deuspheara.oree.core.designsystem.glass

import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Trace
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.IntSize
import dev.chrisbanes.haze.HazeEffectDrawScope
import dev.chrisbanes.haze.HazeEffectLayoutScope
import dev.chrisbanes.haze.HazeEffectRenderer
import dev.chrisbanes.haze.TrimMemoryLevel
import kotlin.math.ceil

/** Public Haze extension only: one bounded source layer and one map/background shader. */
@RequiresApi(33)
internal class MappedGlassRenderer(private val onFailure: () -> Unit) : HazeEffectRenderer<MappedGlassStyle> {
    private var context: GraphicsContext? = null
    private var layer: GraphicsLayer? = null
    private var shader: RuntimeShader? = null
    private var previousStyle: MappedGlassStyle? = null
    private var previousBounds: Rect? = null

    override fun HazeEffectLayoutScope.calculateLayerBounds(style: MappedGlassStyle): Rect = modifierBounds.inflate(padding(style))

    override fun HazeEffectDrawScope.draw(style: MappedGlassStyle) {
        val geometry = style.geometry
        // A layout callback can precede recomposition. Never stretch an obsolete optical map.
        if (modifierBounds.width != geometry.width.toFloat() || modifierBounds.height != geometry.height.toFloat()) {
            drawCurrentInput()
            return
        }
        if (!drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
            fallBack()
            return
        }
        Trace.beginSection("Oree:mapped-glass-optics")
        try {
            val graphics = currentValueOf(LocalGraphicsContext)
            if (context !== graphics) {
                releaseLayer()
                context = graphics
            }
            val output = layer ?: graphics.createGraphicsLayer().also { layer = it }
            val bounds = modifierBounds
            val raster = IntSize(ceil(bounds.right + bounds.left).toInt(), ceil(bounds.bottom + bounds.top).toInt())
            if (previousStyle != style || previousBounds != bounds || output.renderEffect == null) {
                val optics = shader ?: RuntimeShader(MAPPED_GLASS_SHADER).also { shader = it }
                val buffer = BitmapShader(style.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                    setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
                }
                // Raw numeric channels: disable color-space conversion of displacement values.
                optics.setInputBuffer("opticalMap", buffer)
                optics.setFloatUniform("materialOrigin", bounds.left, bounds.top)
                optics.setFloatUniform("materialSize", bounds.width, bounds.height)
                optics.setFloatUniform("mapCap", geometry.capX, geometry.capY)
                optics.setFloatUniform("mapScale", geometry.mapWidth / (2f * geometry.capX), geometry.mapHeight / (2f * geometry.capY))
                optics.setFloatUniform("sampleSize", raster.width.toFloat(), raster.height.toFloat())
                optics.setFloatUniform("displacement", geometry.displacement * style.intensity)
                optics.setFloatUniform("lighting", style.intensity)
                optics.setFloatUniform("neutralInset", geometry.neutralInset)
                optics.setFloatUniform("cornerRadius", geometry.radius)
                output.renderEffect = RenderEffect.createRuntimeShaderEffect(optics, "content").asComposeRenderEffect()
                previousStyle = style
                previousBounds = bounds
            }
            // DrawScope.record retargets this scope's canvas, including public Haze.drawInput.
            // Always record current input commands: source transforms/pixels may change without size.
            output.record(raster) { this@draw.drawInput() }
            translate(-bounds.left, -bounds.top) { drawLayer(output) }
        } catch (_: RuntimeException) {
            releaseLayer()
            // Preserve current pixels on the failure frame; recomposition restores full Haze.
            fallBack()
        } finally {
            Trace.endSection()
        }
    }

    override fun onTrimMemory(level: TrimMemoryLevel) {
        if (level == TrimMemoryLevel.UI_HIDDEN || level.severity >= TrimMemoryLevel.MODERATE.severity) releaseLayer()
    }

    private fun HazeEffectDrawScope.drawCurrentInput() {
        translate(-modifierBounds.left, -modifierBounds.top) { this@drawCurrentInput.drawInput() }
    }

    private fun HazeEffectDrawScope.fallBack() {
        Trace.beginSection("Oree:mapped-glass-fallback")
        try {
            onFailure()
            drawCurrentInput()
        } finally {
            Trace.endSection()
        }
    }

    override fun dispose() {
        releaseLayer()
        context = null
    }

    private fun releaseLayer() {
        layer?.let { context?.releaseGraphicsLayer(it) }
        layer = null
        shader = null
        previousStyle = null
        previousBounds = null
    }

    private fun padding(style: MappedGlassStyle): Float = style.geometry.inputPadding
}

/** No SDF, normal reconstruction, square roots, powers, ray tracing or dispersion per pixel. */
internal const val MAPPED_GLASS_SHADER = """
    uniform shader content;
    uniform shader opticalMap;
    uniform float2 materialOrigin;
    uniform float2 materialSize;
    uniform float2 mapCap;
    uniform float2 mapScale;
    uniform float2 sampleSize;
    uniform float displacement;
    uniform float lighting;
    uniform float neutralInset;
    uniform float cornerRadius;

    half4 main(float2 coord) {
        float2 local = clamp(coord - materialOrigin, float2(0.0), materialSize);
        float2 edge = min(local, materialSize - local);
        if (edge.x >= neutralInset && edge.y >= neutralInset && (edge.x >= cornerRadius || edge.y >= cornerRadius)) {
            return content.eval(clamp(coord, float2(0.5), sampleSize - float2(0.5)));
        }
        float2 compressed = min(edge, mapCap);
        float2 mapCoord = mix(compressed, 2.0 * mapCap - compressed, step(materialSize * 0.5, local));
        half3 optics = opticalMap.eval(mapCoord * mapScale).rgb;
        float2 bend = (float2(optics.rg) * 255.0 - 128.0) * (displacement / 127.0);
        half4 color = content.eval(clamp(coord + bend, float2(0.5), sampleSize - float2(0.5)));
        half light = half((float(optics.b) * 255.0 - 128.0) * (lighting / 127.0));
        half3 target = half3(step(half(0.0), light)) * color.a;
        return half4(mix(color.rgb, target, abs(light)), color.a);
    }
"""
