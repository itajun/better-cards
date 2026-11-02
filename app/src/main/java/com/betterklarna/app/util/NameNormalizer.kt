package com.betterklarna.app.util

object NameNormalizer {
    private val KNOWN_BRAND_NAMES = mapOf(
        "everyday rewards" to "Everyday Rewards",
        "flybuys" to "Flybuys",
        "rebel" to "Rebel",
        "iga" to "IGA",
        "ikea family" to "IKEA Family",
        "terrywhite chemmart" to "TerryWhite Chemmart",
        "country road & trenery" to "Country Road & Trenery",
        "dan murphy's" to "Dan Murphy's",
        "cincotta discount chemist" to "Cincotta Discount Chemist",
        "the source bulk foods" to "The Source Bulk Foods",
        "priceline pharmacy" to "Priceline Pharmacy",
        "harris farm markets" to "Harris Farm Markets",
        "david jones" to "David Jones",
        "cotton on" to "Cotton On",
        "just jeans" to "Just Jeans",
        "myer one" to "Myer One",
        "san churro" to "San Churro",
        "cook's club" to "Cook's Club",
        "grill'd" to "Grill'd"
    )

    private val ACRONYMS = setOf(
        "IGA", "IKEA", "BWS", "KFC", "JB", "HI-FI", "VIP", "NRMA", "RACV", "RACQ", "RAA", "RAC"
    )

    fun normalize(rawName: String): String {
        val cleaned = rawName.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isEmpty()) return "Loyalty Card"

        // Check exact case-insensitive dictionary first
        KNOWN_BRAND_NAMES[cleaned.lowercase()]?.let { return it }

        return cleaned.split(" ").mapIndexed { index, word ->
            normalizeWord(word, isFirstWord = index == 0)
        }.joinToString(" ")
    }

    private fun normalizeWord(word: String, isFirstWord: Boolean): String {
        if (word.isEmpty()) return word
        if (word == "&" || word == "+" || word == "-") return word

        val upper = word.uppercase()
        if (ACRONYMS.contains(upper)) return upper

        // If the word already has deliberate internal capitals (e.g. TerryWhite, McDonald's), keep it
        val hasInternalCaps = word.drop(1).any { it.isUpperCase() }
        if (hasInternalCaps && word.first().isUpperCase()) {
            return word
        }

        // If the word is 2-4 chars and already all uppercase (e.g. "IGA", "ANZ"), preserve it
        if (word.length in 2..4 && word.all { !it.isLetter() || it.isUpperCase() }) {
            return word
        }

        // Otherwise, Title Case the word
        val lower = word.lowercase()
        if (!isFirstWord && (lower == "and" || lower == "of" || lower == "the")) {
            return lower
        }

        return lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
