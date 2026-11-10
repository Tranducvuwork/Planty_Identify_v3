package com.expeditee.plant.identifier.model

import android.content.Context
import androidx.annotation.DrawableRes
import com.expeditee.plant.identifier.R
import java.text.Normalizer

enum class PlantCategory {
    FLOWERS,
    FRUIT,
    VEGETABLES,
    CREEPERS,
    TREES,
    WEEDS,
    TOXIC,
    SHRUBS,
    HERBS;

    fun getDisplayText(
        context: Context
    ): String {
        return when (this) {
            FLOWERS -> context.getString(R.string.flower)
            FRUIT -> context.getString(R.string.fruit)
            VEGETABLES -> context.getString(R.string.vegetables)
            CREEPERS -> context.getString(R.string.creepers)
            TREES -> context.getString(R.string.trees)
            WEEDS -> context.getString(R.string.weeds)
            TOXIC -> context.getString(R.string.toxic)
            SHRUBS -> context.getString(R.string.shrubs)
            HERBS -> context.getString(R.string.herbs)
        }
    }

    @DrawableRes
    fun getDisplayImageResId(): Int {
        return when (this) {
            FLOWERS -> R.drawable.ic_flowers
            FRUIT -> R.drawable.ic_fruit
            VEGETABLES -> R.drawable.ic_vegetables
            CREEPERS -> R.drawable.ic_creepers
            TREES -> R.drawable.ic_trees
            WEEDS -> R.drawable.ic_weeds
            TOXIC -> R.drawable.ic_toxic
            SHRUBS -> R.drawable.ic_shrubs
            HERBS -> R.drawable.ic_herbs
        }
    }

    companion object {
        fun fromQuery(raw: String?): PlantCategory? {
            val folded = fold(raw)
            if (folded.isBlank()) return null
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }?.let { return it }
            return when (folded) {
                "hoa", "flower", "flowers", "blossom", "bloom" -> FLOWERS
                "qua", "trai cay", "fruit", "fruits" -> FRUIT
                "rau", "rau cu", "vegetable", "vegetables" -> VEGETABLES
                "day leo", "cay leo", "creeper", "creepers" -> CREEPERS
                "cay go", "tree", "trees" -> TREES
                "co dai", "weed", "weeds" -> WEEDS
                "cay co doc", "doc", "toxic", "poison" -> TOXIC
                "bui cay", "shrub", "shrubs" -> SHRUBS
                "thao moc", "herb", "herbs" -> HERBS
                else -> null
            }
        }

        fun fold(value: String?): String {
            val nfd = Normalizer.normalize(value.orEmpty().trim().lowercase(), Normalizer.Form.NFD)
            return nfd.replace("\\p{Mn}+".toRegex(), "")
                .replace('đ', 'd')
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
