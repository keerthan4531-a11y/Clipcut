package com.clipcut.editor.engine.gltransition

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.clipcut.editor.model.TransitionEasing
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A real two-source GLSL transition ported from gl-transitions (MIT).
 *
 * Media3 hands a [GlEffect] only the *incoming* frame, so the outgoing frame is supplied
 * separately as a still [fromFrame] bitmap captured from the end of the previous clip. The
 * program therefore binds two texture units — the pipeline texture on 0 and the frozen
 * previous frame on 1 — which is exactly how [com.clipcut.editor.engine.LutEngine] binds its
 * 3D LUT, so it stays inside the single-input `BaseGlShaderProgram` contract instead of
 * needing Media3's multi-input composition.
 *
 * Vertical orientation: the bundled bitmaps are uploaded with `GLUtils.texImage2D` and
 * sampled with the same `vTexCoord` the pipeline texture uses, matching how
 * `SegmentationGlEffect` aligns its mask bitmap with the video frame.
 */
@UnstableApi
class GlTransitionEffect(
    private val transitionId: String,
    private val fromFrame: Bitmap?,
    private val durationUs: Long,
    private val easing: TransitionEasing = TransitionEasing.LINEAR
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        GlTransitionShaderProgram(context, transitionId, fromFrame, durationUs, easing, useHdr)
}

@UnstableApi
private class GlTransitionShaderProgram(
    private val context: Context,
    private val transitionId: String,
    private val fromFrame: Bitmap?,
    private val durationUs: Long,
    private val easing: TransitionEasing,
    useHdr: Boolean
) : BaseGlShaderProgram(useHdr, 1) {

    private var glProgram = 0
    private var vao = 0
    private var vbo = 0
    private var fromTexture = 0
    private var width = 1
    private var height = 1
    private var usedFallback = false

    private val paramLocations = HashMap<String, Int>(8)
    private var progressLocation = -1
    private var ratioLocation = -1
    private var toSamplerLocation = -1
    private var fromSamplerLocation = -1
    private var params: List<GlTransitionParam> = emptyList()

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        width = inputWidth.coerceAtLeast(1)
        height = inputHeight.coerceAtLeast(1)
        if (glProgram == 0) setupGl()
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        if (glProgram == 0) return
        GLES30.glUseProgram(glProgram)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexId)
        if (toSamplerLocation >= 0) GLES30.glUniform1i(toSamplerLocation, 0)

        if (fromTexture == 0) fromTexture = uploadFromFrame()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fromTexture)
        if (fromSamplerLocation >= 0) GLES30.glUniform1i(fromSamplerLocation, 1)

        if (progressLocation >= 0) {
            GLES30.glUniform1f(progressLocation, easedProgress(presentationTimeUs))
        }
        if (ratioLocation >= 0) {
            GLES30.glUniform1f(ratioLocation, width.toFloat() / height.toFloat())
        }
        // Defaults matter: the ported shaders declare plain uniforms with no initialiser, so
        // an unset `count`/`blades`/`pleats` would be 0 and the wipe would divide by zero.
        params.forEach { param ->
            val loc = paramLocations[param.name] ?: return@forEach
            if (loc < 0) return@forEach
            val v = param.defaultValue
            when (param.type) {
                "float" -> GLES30.glUniform1f(loc, v.getOrElse(0) { 0f })
                "int", "bool" -> GLES30.glUniform1i(loc, v.getOrElse(0) { 0f }.toInt())
                "vec2" -> GLES30.glUniform2f(loc, v.getOrElse(0) { 0f }, v.getOrElse(1) { 0f })
                "vec3" -> GLES30.glUniform3f(
                    loc, v.getOrElse(0) { 0f }, v.getOrElse(1) { 0f }, v.getOrElse(2) { 0f }
                )
                "vec4" -> GLES30.glUniform4f(
                    loc,
                    v.getOrElse(0) { 0f }, v.getOrElse(1) { 0f },
                    v.getOrElse(2) { 0f }, v.getOrElse(3) { 0f }
                )
            }
        }

        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    }

    private fun easedProgress(presentationTimeUs: Long): Float {
        if (durationUs <= 0L) return 1f
        val raw = (presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
        return when (easing) {
            TransitionEasing.EASE_IN -> raw * raw
            TransitionEasing.EASE_OUT -> 1f - (1f - raw) * (1f - raw)
            TransitionEasing.EASE_IN_OUT -> if (raw < 0.5f) {
                2f * raw * raw
            } else {
                1f - 2f * (1f - raw) * (1f - raw)
            }
            else -> raw
        }
    }

    override fun release() {
        super.release()
        if (glProgram != 0) { GLES30.glDeleteProgram(glProgram); glProgram = 0 }
        if (vao != 0) { GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0); vao = 0 }
        if (vbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0); vbo = 0 }
        if (fromTexture != 0) { GLES30.glDeleteTextures(1, intArrayOf(fromTexture), 0); fromTexture = 0 }
        usedFallback = false
    }

    private fun setupGl() {
        val def = GlTransitionCatalog.find(context, transitionId)
        params = def?.params ?: emptyList()
        val source = def?.let { readAsset(it.assetPath) }
        val fragment = source ?: FALLBACK_DISSOLVE
        usedFallback = source == null

        glProgram = buildProgram(fragment)
        if (glProgram == 0 && !usedFallback) {
            // Driver rejected one shader out of 212: degrade to a dissolve instead of failing
            // the whole export.
            glProgram = buildProgram(FALLBACK_DISSOLVE)
            usedFallback = true
        }
        if (glProgram == 0) throw RuntimeException("Transition program failed to link: $transitionId")

        val vaos = IntArray(1); GLES30.glGenVertexArrays(1, vaos, 0); vao = vaos[0]
        val vbos = IntArray(1); GLES30.glGenBuffers(1, vbos, 0); vbo = vbos[0]
        val quad = floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        val buf = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(quad).apply { position(0) }
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, buf, GLES30.GL_STATIC_DRAW)
        val p = GLES30.glGetAttribLocation(glProgram, "aPosition")
        if (p >= 0) {
            GLES30.glEnableVertexAttribArray(p)
            GLES30.glVertexAttribPointer(p, 2, GLES30.GL_FLOAT, false, 16, 0)
        }
        val t = GLES30.glGetAttribLocation(glProgram, "aTexCoord")
        if (t >= 0) {
            GLES30.glEnableVertexAttribArray(t)
            GLES30.glVertexAttribPointer(t, 2, GLES30.GL_FLOAT, false, 16, 8)
        }
        GLES30.glBindVertexArray(0)

        toSamplerLocation = GLES30.glGetUniformLocation(glProgram, "uToTexSampler")
        fromSamplerLocation = GLES30.glGetUniformLocation(glProgram, "uFromTexSampler")
        progressLocation = GLES30.glGetUniformLocation(glProgram, "progress")
        ratioLocation = GLES30.glGetUniformLocation(glProgram, "ratio")
        paramLocations.clear()
        params.forEach { paramLocations[it.name] = GLES30.glGetUniformLocation(glProgram, it.name) }
    }

    private fun readAsset(path: String): String? = try {
        context.assets.open(path).bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        null
    }

    /** Compiles + links [fragment]; returns 0 on any failure so the caller can fall back. */
    private fun buildProgram(fragment: String): Int {
        val vs = compile(GLES30.GL_VERTEX_SHADER, VERT) ?: return 0
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragment) ?: run {
            GLES30.glDeleteShader(vs); return 0
        }
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            GLES30.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private fun compile(type: Int, src: String): Int? {
        val sh = GLES30.glCreateShader(type)
        GLES30.glShaderSource(sh, src)
        GLES30.glCompileShader(sh)
        val status = IntArray(1)
        GLES30.glGetShaderiv(sh, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES30.glDeleteShader(sh)
            return null
        }
        return sh
    }

    private fun uploadFromFrame(): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, ids[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        val bitmap = fromFrame?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        try {
            if (bitmap != null) {
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            } else {
                // No previous frame available (first clip, or the capture failed): a black
                // plate makes every wipe read as a fade-from-black rather than a glitch.
                val px = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
                    .put(byteArrayOf(0, 0, 0, -1))
                px.position(0)
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0,
                    GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, px
                )
            }
        } catch (_: Throwable) {
            // Upload is best-effort; a failed texture leaves getFromColor sampling black.
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return ids[0]
    }

    companion object {
        private const val VERT = "#version 300 es\n" +
            "in vec4 aPosition;\nin vec2 aTexCoord;\nout vec2 vTexCoord;\n" +
            "void main() { gl_Position = aPosition; vTexCoord = aTexCoord; }"

        private const val FALLBACK_DISSOLVE = "#version 300 es\n" +
            "precision highp float;\n" +
            "uniform sampler2D uFromTexSampler;\nuniform sampler2D uToTexSampler;\n" +
            "uniform float progress;\n" +
            "in vec2 vTexCoord;\nout vec4 fragColor;\n" +
            "void main() {\n" +
            "  vec4 a = texture(uFromTexSampler, vTexCoord);\n" +
            "  vec4 b = texture(uToTexSampler, vTexCoord);\n" +
            "  fragColor = mix(a, b, clamp(progress, 0.0, 1.0));\n" +
            "}"
    }
}
