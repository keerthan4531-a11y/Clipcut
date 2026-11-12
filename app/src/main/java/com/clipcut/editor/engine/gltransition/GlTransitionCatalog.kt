package com.clipcut.editor.engine.gltransition

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One tunable uniform exposed by a gl-transitions shader.
 *
 * [defaultValue] mirrors the `// = <value>` comment in the upstream GLSL. GLSL ES has no
 * uniform initialisers, so the value must be uploaded explicitly or the shader renders with
 * zeros (e.g. a wipe with `count = 0`, which divides by zero on some drivers).
 */
data class GlTransitionParam(
    val name: String,
    val type: String,
    val defaultValue: FloatArray,
    val min: Float?,
    val max: Float?
) {
    /** True for parameters a user could sensibly drag (single scalar with a range). */
    val isScalar: Boolean get() = type == "float" && min != null && max != null
}

/** A gl-transitions shader, as described by `assets/gltransitions/catalog.json`. */
data class GlTransitionDef(
    val id: String,
    val displayName: String,
    val description: String,
    val tags: List<String>,
    val author: String,
    val params: List<GlTransitionParam>
) {
    val assetPath: String get() = "$ASSET_DIR/$id.glsl"
}

private const val ASSET_DIR = "gltransitions"
private const val CATALOG_ASSET = "$ASSET_DIR/catalog.json"

/**
 * Loads the bundled gl-transitions catalogue.
 *
 * The catalogue is a single ~120 KB JSON asset parsed once and cached for the process
 * lifetime; individual shaders stay on disk and are read only when an effect is actually
 * instantiated, so a project that uses two transitions never pays for 212.
 */
object GlTransitionCatalog {

    private val lock = Any()

    @Volatile private var cachedAll: List<GlTransitionDef>? = null

    @Volatile private var cachedById: Map<String, GlTransitionDef> = emptyMap()

    /** Every bundled transition, sorted by display name. Empty if the asset is unreadable. */
    fun all(context: Context): List<GlTransitionDef> {
        cachedAll?.let { return it }
        synchronized(lock) {
            cachedAll?.let { return it }
            val parsed = try {
                parse(context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() })
            } catch (_: Throwable) {
                emptyList()
            }
            cachedById = parsed.associateBy { it.id }
            cachedAll = parsed
            return parsed
        }
    }

    fun find(context: Context, id: String): GlTransitionDef? {
        all(context) // warm the index
        return cachedById[id]
    }

    /** Case-insensitive search over name, description and tags. */
    fun search(context: Context, query: String, tag: String? = null): List<GlTransitionDef> {
        val q = query.trim().lowercase()
        return all(context).filter { def ->
            (tag == null || def.tags.contains(tag)) &&
                (q.isEmpty() ||
                    def.id.lowercase().contains(q) ||
                    def.description.lowercase().contains(q) ||
                    def.tags.any { it.contains(q) })
        }
    }

    /** Tags with at least [MIN_TAG_COUNT] transitions, most common first — used for filter chips. */
    fun popularTags(context: Context, limit: Int = 12): List<Pair<String, Int>> =
        all(context)
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .filterValues { it >= MIN_TAG_COUNT }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key to it.value }

    private const val MIN_TAG_COUNT = 8

    private fun parse(raw: String): List<GlTransitionDef> {
        val root = JSONObject(raw)
        val arr: JSONArray = root.optJSONArray("transitions") ?: return emptyList()
        val out = ArrayList<GlTransitionDef>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
            val paramsArr = o.optJSONArray("params")
            val params = if (paramsArr == null) emptyList() else (0 until paramsArr.length()).mapNotNull { p ->
                val po = paramsArr.optJSONObject(p) ?: return@mapNotNull null
                val name = po.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val type = po.optString("type", "float")
                // JSON null (JSONObject.NULL) means "no default declared upstream".
                val defaultRaw = if (po.isNull("default")) null else po.optString("default").takeIf { it.isNotBlank() }
                GlTransitionParam(
                    name = name,
                    type = type,
                    defaultValue = parseDefault(type, defaultRaw),
                    min = if (po.isNull("min")) null else po.optDouble("min").toFloat(),
                    max = if (po.isNull("max")) null else po.optDouble("max").toFloat()
                )
            }
            val tagsArr = o.optJSONArray("tags")
            val tags = if (tagsArr == null) emptyList() else (0 until tagsArr.length())
                .mapNotNull { tagsArr.optString(it).takeIf { it.isNotBlank() } }
            out += GlTransitionDef(
                id = id,
                displayName = o.optString("name", id).takeIf { it.isNotBlank() } ?: id,
                description = o.optString("description", ""),
                tags = tags,
                author = o.optString("author", ""),
                params = params
            )
        }
        return out.sortedBy { it.displayName.lowercase() }
    }

    /**
     * `"0.4"` / `"vec2(1.0, 0.0)"` / `"false"` / `"true"` → component floats.
     *
     * Unparseable values fall back to a neutral 1.0 for booleans-as-zero and 0.0 otherwise,
     * which keeps the shader defined instead of feeding NaN into a uniform.
     */
    internal fun parseDefault(type: String, raw: String?): FloatArray {
        val width = when (type) {
            "vec2" -> 2
            "vec3" -> 3
            "vec4" -> 4
            else -> 1
        }
        val zeros = FloatArray(width)
        if (raw == null) return zeros
        val cleaned = raw.trim()
        if (cleaned == "true") return floatArrayOf(1f)
        if (cleaned == "false") return floatArrayOf(0f)
        val inner = if (cleaned.contains('(')) {
            cleaned.substringAfter('(').substringBeforeLast(')')
        } else {
            cleaned
        }
        val parts = inner.split(',').mapNotNull { it.trim().toFloatOrNull() }
        if (parts.isEmpty()) return zeros
        val out = FloatArray(width)
        for (i in 0 until minOf(width, parts.size)) out[i] = parts[i]
        return out
    }
}
