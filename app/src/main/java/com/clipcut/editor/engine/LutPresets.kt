package com.clipcut.editor.engine

import java.util.Collections

/**
 * Built-in colour LUTs, synthesised on device instead of shipped as files.
 *
 * A 33³ LUT is ~700 KB of text and a 17³ is still ~100 KB, so bundling 25 of them would add
 * megabytes to the APK for data that is a handful of float parameters. Each preset here is a
 * short recipe (exposure, contrast, split-tone, saturation, …) that is evaluated into a real
 * [LutEngine.Lut3D] the first time it is used and then cached — the render path is unchanged,
 * it still goes through [LutEngine.createLutEffect]'s 3D texture program.
 *
 * Everything is authored here, so there is no upstream licence obligation for these presets.
 */
data class LutPreset(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String
)

object LutPresets {

    /** Edge length of the generated cube. 17 is the sweet spot: trilinear filtering hides the
     *  grid, and the texture is 4913 RGB texels (~59 KB as floats). */
    const val SIZE = 17

    /** Bounded cache: a project rarely uses more than a handful of looks at once. */
    private const val CACHE_CAPACITY = 8

    private class Recipe(
        val exposure: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val vibrance: Float = 0f,
        val temperature: Float = 0f,
        val tint: Float = 0f,
        val lift: FloatArray = floatArrayOf(0f, 0f, 0f),
        val gamma: FloatArray = floatArrayOf(1f, 1f, 1f),
        val gain: FloatArray = floatArrayOf(1f, 1f, 1f),
        val shadowTone: FloatArray = floatArrayOf(0f, 0f, 0f),
        val highlightTone: FloatArray = floatArrayOf(0f, 0f, 0f),
        val fade: Float = 0f,
        val monochrome: Boolean = false
    )

    private val presets: List<LutPreset> = listOf(
        // --- Cinematic -------------------------------------------------------
        LutPreset("cinema_teal_orange", "Teal & Orange", "Cinematic",
            "Blockbuster contrast: warm skin, cold shadows"),
        LutPreset("cinema_blockbuster", "Blockbuster", "Cinematic",
            "Deep blacks, punchy mids, subtle warm highlights"),
        LutPreset("cinema_moody_blue", "Moody Blue", "Cinematic",
            "Desaturated shadows with a cold blue cast"),
        LutPreset("cinema_desat", "Cinema Desat", "Cinematic",
            "Muted saturation and a soft filmic roll-off"),
        LutPreset("cinema_bleach", "Bleach Bypass", "Cinematic",
            "Silver-retention look: crushed colour, hard highlights"),

        // --- Film stocks -----------------------------------------------------
        LutPreset("film_portra", "Portra Warm", "Film",
            "Creamy warm midtones and gentle skin tones"),
        LutPreset("film_superia", "Superia Green", "Film",
            "Fuji-style green shadows with vivid reds"),
        LutPreset("film_cinestill", "Cinestill 800T", "Film",
            "Tungsten night film: cyan shadows, red halation tint"),
        LutPreset("film_polaroid", "Instant", "Film",
            "Low-contrast instant print with milky blacks"),
        LutPreset("film_agfa", "Agfa Vintage", "Film",
            "Warm, slightly flat seventies emulsion"),

        // --- Retro -----------------------------------------------------------
        LutPreset("retro_faded", "Faded Matte", "Retro",
            "Lifted blacks and washed highlights"),
        LutPreset("retro_nostalgia", "Nostalgia", "Retro",
            "Golden hour warmth with a soft vignette feel"),
        LutPreset("retro_sepia", "Sepia 70s", "Retro",
            "Brown-toned monochrome with warm highlights"),
        LutPreset("retro_vhs", "VHS Colour", "Retro",
            "Tape-era colour bleed: magenta highlights, green midtones"),

        // --- Monochrome ------------------------------------------------------
        LutPreset("mono_noir", "Noir", "Mono",
            "Hard contrast black and white"),
        LutPreset("mono_silver", "Silver Screen", "Mono",
            "Soft silver-gelatin tonality"),
        LutPreset("mono_gritty", "Gritty B&W", "Mono",
            "Crushed blacks with a cool highlight tint"),

        // --- Pop / HDR -------------------------------------------------------
        LutPreset("pop_vivid", "Vivid Pop", "Pop",
            "Saturated, high-contrast social-ready colour"),
        LutPreset("pop_hdr", "HDR Punch", "Pop",
            "Lifted shadows, tamed highlights, strong clarity"),
        LutPreset("pop_sunset", "Sunset Glow", "Pop",
            "Orange highlights and violet shadows"),
        LutPreset("pop_arctic", "Arctic Cool", "Pop",
            "Icy cyan with clean whites"),
        LutPreset("pop_forest", "Forest Moss", "Pop",
            "Deep green midtones with warm skin"),
        LutPreset("pop_cyberpunk", "Cyberpunk", "Pop",
            "Neon magenta and electric cyan split-tone"),
        LutPreset("pop_pastel", "Pastel Dream", "Pop",
            "Airy pastel wash with lifted shadows"),
        LutPreset("pop_matrix", "Matrix Green", "Pop",
            "Green-shifted digital look")
    )

    private val recipes: Map<String, Recipe> = mapOf(
        "cinema_teal_orange" to Recipe(
            contrast = 1.18f, saturation = 1.05f,
            shadowTone = floatArrayOf(-0.05f, 0.01f, 0.07f),
            highlightTone = floatArrayOf(0.07f, 0.02f, -0.05f),
            gain = floatArrayOf(1.03f, 1f, 0.98f)
        ),
        "cinema_blockbuster" to Recipe(
            contrast = 1.22f, saturation = 0.98f, exposure = -0.05f,
            lift = floatArrayOf(-0.015f, -0.01f, -0.005f),
            shadowTone = floatArrayOf(-0.02f, 0f, 0.03f),
            highlightTone = floatArrayOf(0.04f, 0.02f, 0f)
        ),
        "cinema_moody_blue" to Recipe(
            contrast = 1.12f, saturation = 0.78f, exposure = -0.12f,
            temperature = -0.35f,
            shadowTone = floatArrayOf(-0.06f, -0.01f, 0.08f),
            highlightTone = floatArrayOf(-0.02f, 0.01f, 0.04f)
        ),
        "cinema_desat" to Recipe(
            contrast = 1.08f, saturation = 0.68f,
            fade = 0.12f, highlightTone = floatArrayOf(0.02f, 0.02f, 0.01f)
        ),
        "cinema_bleach" to Recipe(
            contrast = 1.35f, saturation = 0.42f, exposure = 0.06f,
            gamma = floatArrayOf(0.96f, 0.97f, 1f),
            shadowTone = floatArrayOf(-0.03f, -0.01f, 0.02f)
        ),

        "film_portra" to Recipe(
            contrast = 1.05f, saturation = 0.98f, exposure = 0.06f,
            temperature = 0.22f, tint = 0.06f,
            lift = floatArrayOf(0.012f, 0.006f, 0.004f),
            highlightTone = floatArrayOf(0.04f, 0.015f, -0.01f)
        ),
        "film_superia" to Recipe(
            contrast = 1.1f, saturation = 1.15f, temperature = 0.08f,
            shadowTone = floatArrayOf(-0.03f, 0.05f, -0.02f),
            highlightTone = floatArrayOf(0.05f, 0.01f, 0.01f),
            gamma = floatArrayOf(1f, 1.02f, 1f)
        ),
        "film_cinestill" to Recipe(
            contrast = 1.14f, saturation = 1.05f, exposure = -0.08f,
            temperature = -0.28f, tint = 0.1f,
            shadowTone = floatArrayOf(-0.08f, 0.02f, 0.09f),
            highlightTone = floatArrayOf(0.1f, -0.02f, 0.02f)
        ),
        "film_polaroid" to Recipe(
            contrast = 0.86f, saturation = 0.92f, exposure = 0.1f,
            fade = 0.22f, temperature = 0.12f,
            lift = floatArrayOf(0.03f, 0.025f, 0.02f)
        ),
        "film_agfa" to Recipe(
            contrast = 0.98f, saturation = 0.9f, temperature = 0.18f,
            fade = 0.1f, shadowTone = floatArrayOf(0.02f, 0.01f, -0.02f),
            highlightTone = floatArrayOf(0.04f, 0.02f, 0f)
        ),

        "retro_faded" to Recipe(
            contrast = 0.9f, saturation = 0.82f, fade = 0.3f,
            lift = floatArrayOf(0.02f, 0.02f, 0.02f),
            shadowTone = floatArrayOf(0.01f, 0f, 0.03f)
        ),
        "retro_nostalgia" to Recipe(
            contrast = 1.02f, saturation = 0.95f, exposure = 0.08f,
            temperature = 0.3f, fade = 0.14f,
            highlightTone = floatArrayOf(0.07f, 0.03f, -0.02f)
        ),
        "retro_sepia" to Recipe(
            contrast = 1.08f, monochrome = true, temperature = 0.35f,
            shadowTone = floatArrayOf(0.05f, 0.02f, -0.02f),
            gain = floatArrayOf(1.06f, 0.98f, 0.86f),
            fade = 0.1f
        ),
        "retro_vhs" to Recipe(
            contrast = 1.06f, saturation = 1.2f, tint = 0.08f,
            shadowTone = floatArrayOf(-0.04f, 0.06f, -0.03f),
            highlightTone = floatArrayOf(0.09f, -0.04f, 0.06f),
            fade = 0.08f
        ),

        "mono_noir" to Recipe(
            contrast = 1.4f, monochrome = true, exposure = -0.06f,
            gamma = floatArrayOf(0.94f, 0.94f, 0.94f)
        ),
        "mono_silver" to Recipe(
            contrast = 1.1f, monochrome = true, exposure = 0.04f,
            fade = 0.08f, highlightTone = floatArrayOf(0.02f, 0.02f, 0.025f)
        ),
        "mono_gritty" to Recipe(
            contrast = 1.45f, monochrome = true,
            shadowTone = floatArrayOf(-0.02f, 0f, 0.04f),
            highlightTone = floatArrayOf(0.02f, 0.01f, -0.01f)
        ),

        "pop_vivid" to Recipe(
            contrast = 1.18f, saturation = 1.35f, vibrance = 0.25f, exposure = 0.04f
        ),
        "pop_hdr" to Recipe(
            contrast = 1.1f, saturation = 1.12f, vibrance = 0.3f,
            gamma = floatArrayOf(0.92f, 0.92f, 0.92f),
            lift = floatArrayOf(0.02f, 0.02f, 0.025f),
            gain = floatArrayOf(0.98f, 0.98f, 0.97f)
        ),
        "pop_sunset" to Recipe(
            contrast = 1.12f, saturation = 1.1f, temperature = 0.3f,
            shadowTone = floatArrayOf(0.03f, -0.03f, 0.08f),
            highlightTone = floatArrayOf(0.1f, 0.02f, -0.04f)
        ),
        "pop_arctic" to Recipe(
            contrast = 1.08f, saturation = 0.98f, temperature = -0.32f,
            lift = floatArrayOf(0f, 0.01f, 0.025f),
            highlightTone = floatArrayOf(-0.03f, 0.02f, 0.06f)
        ),
        "pop_forest" to Recipe(
            contrast = 1.06f, saturation = 1.02f, temperature = -0.08f,
            shadowTone = floatArrayOf(-0.03f, 0.06f, -0.01f),
            highlightTone = floatArrayOf(0.04f, 0.03f, -0.02f),
            gamma = floatArrayOf(1f, 1.03f, 1f)
        ),
        "pop_cyberpunk" to Recipe(
            contrast = 1.2f, saturation = 1.25f, tint = 0.12f,
            shadowTone = floatArrayOf(-0.09f, -0.02f, 0.12f),
            highlightTone = floatArrayOf(0.12f, -0.05f, 0.1f)
        ),
        "pop_pastel" to Recipe(
            contrast = 0.85f, saturation = 0.9f, exposure = 0.14f, fade = 0.26f,
            tint = 0.05f, highlightTone = floatArrayOf(0.03f, 0.01f, 0.05f)
        ),
        "pop_matrix" to Recipe(
            contrast = 1.16f, saturation = 1.05f, temperature = -0.12f,
            shadowTone = floatArrayOf(-0.07f, 0.09f, -0.05f),
            highlightTone = floatArrayOf(-0.02f, 0.07f, -0.03f),
            gamma = floatArrayOf(0.98f, 1.05f, 0.98f)
        )
    )

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, LutEngine.Lut3D>(CACHE_CAPACITY, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LutEngine.Lut3D>?) =
                size > CACHE_CAPACITY
        }
    )

    fun all(): List<LutPreset> = presets

    fun byCategory(category: String): List<LutPreset> = presets.filter { it.category == category }

    fun find(id: String): LutPreset? = presets.firstOrNull { it.id == id }

    /** Categories in presentation order. */
    val categories: List<String> = presets.map { it.category }.distinct()

    /** Generates (or returns the cached) 3D LUT for [id]; null for an unknown id. */
    fun lut3d(id: String): LutEngine.Lut3D? {
        find(id) ?: return null
        cache[id]?.let { return it }
        val recipe = recipes[id] ?: return null
        val lut = build(recipe)
        cache[id] = lut
        return lut
    }

    private fun build(recipe: Recipe): LutEngine.Lut3D {
        val size = SIZE
        val data = FloatArray(size * size * size * 3)
        val exposureGain = Math.pow(2.0, recipe.exposure.toDouble()).toFloat()
        var index = 0
        // Cube layout matches LutEngine's parser: red varies fastest, then green, then blue.
        for (b in 0 until size) {
            val bf = b / (size - 1f)
            for (g in 0 until size) {
                val gf = g / (size - 1f)
                for (r in 0 until size) {
                    val rf = r / (size - 1f)
                    var cr = rf
                    var cg = gf
                    var cb = bf

                    // 1. exposure
                    cr *= exposureGain; cg *= exposureGain; cb *= exposureGain
                    // 2. white balance
                    cr += recipe.temperature * 0.12f; cb -= recipe.temperature * 0.12f
                    cg += recipe.tint * 0.1f
                    // 3. contrast around mid grey
                    cr = (cr - 0.5f) * recipe.contrast + 0.5f
                    cg = (cg - 0.5f) * recipe.contrast + 0.5f
                    cb = (cb - 0.5f) * recipe.contrast + 0.5f
                    // 4. lift / gamma / gain
                    cr = powSafe((cr + recipe.lift[0]) * recipe.gain[0], 1f / recipe.gamma[0])
                    cg = powSafe((cg + recipe.lift[1]) * recipe.gain[1], 1f / recipe.gamma[1])
                    cb = powSafe((cb + recipe.lift[2]) * recipe.gain[2], 1f / recipe.gamma[2])
                    // 5. split-toning weighted by luma
                    val luma = (0.2126f * cr + 0.7152f * cg + 0.0722f * cb).coerceIn(0f, 1f)
                    val shadowWeight = (1f - luma) * (1f - luma)
                    val highlightWeight = luma * luma
                    cr += recipe.shadowTone[0] * shadowWeight + recipe.highlightTone[0] * highlightWeight
                    cg += recipe.shadowTone[1] * shadowWeight + recipe.highlightTone[1] * highlightWeight
                    cb += recipe.shadowTone[2] * shadowWeight + recipe.highlightTone[2] * highlightWeight
                    // 6. saturation / vibrance / monochrome
                    if (recipe.monochrome) {
                        val y = 0.2126f * cr + 0.7152f * cg + 0.0722f * cb
                        cr = y; cg = y; cb = y
                    } else {
                        val s = recipe.saturation
                        val y = 0.2126f * cr + 0.7152f * cg + 0.0722f * cb
                        cr = y + (cr - y) * s
                        cg = y + (cg - y) * s
                        cb = y + (cb - y) * s
                        if (recipe.vibrance != 0f) {
                            // Vibrance pushes low-saturation pixels harder than saturated ones.
                            val maxC = maxOf(cr, cg, cb)
                            val minC = minOf(cr, cg, cb)
                            val sat = maxC - minC
                            val boost = recipe.vibrance * (1f - sat).coerceIn(0f, 1f)
                            cr = y + (cr - y) * (1f + boost)
                            cg = y + (cg - y) * (1f + boost)
                            cb = y + (cb - y) * (1f + boost)
                        }
                    }
                    // 7. film fade: pull the black point up and the white point down
                    if (recipe.fade > 0f) {
                        cr = cr * (1f - recipe.fade) + recipe.fade * 0.06f
                        cg = cg * (1f - recipe.fade) + recipe.fade * 0.06f
                        cb = cb * (1f - recipe.fade) + recipe.fade * 0.06f
                    }

                    data[index++] = cr.coerceIn(0f, 1f)
                    data[index++] = cg.coerceIn(0f, 1f)
                    data[index++] = cb.coerceIn(0f, 1f)
                }
            }
        }
        return LutEngine.Lut3D(size = size, data = data)
    }

    private fun powSafe(base: Float, exponent: Float): Float {
        val b = base.coerceAtLeast(0f)
        val e = if (exponent.isFinite()) exponent.coerceIn(0.05f, 20f) else 1f
        return Math.pow(b.toDouble(), e.toDouble()).toFloat()
    }
}
