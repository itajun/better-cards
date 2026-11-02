package com.betterklarna.app.util

data class BrandPreset(
    val name: String,
    val colorHex: String,
    val domain: String,
    val bundledAsset: String? = null
) {
    val defaultLogoUrl: String
        get() = if (bundledAsset != null) {
            "asset://$bundledAsset"
        } else {
            "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$domain&size=256"
        }
}

object BrandCatalog {
    val PRESETS: List<BrandPreset> = listOf(
        // Bundled Klarna AU Loyalty Programs (Full wordmark PNGs in assets/logos/)
        BrandPreset("Amcal", "#018FCF", "amcal.com.au", "1ced99b56507b330e4577f2471fff22d.png"),
        BrandPreset("Bonds", "#000000", "bonds.com.au", "fd398ff46b3146970b3b900b84b12ca1.png"),
        BrandPreset("Cincotta Discount Chemist", "#F02315", "cincottachemist.com.au", "ffe094bcf5fbedc5c782ba5f44ea2067.png"),
        BrandPreset("Cotton On", "#FFFFFF", "cottonon.com", "7b4d912374ef0b9c7815f85689a1dfdc.png"),
        BrandPreset("Country Road & Trenery", "#FFFFFF", "countryroad.com.au", "cf90a74ec373ebcb149c4254b3cbb847.png"),
        BrandPreset("Dan Murphy's", "#1C4220", "danmurphys.com.au", "bcfbab9d76825f9cd26b731be6c85241.png"),
        BrandPreset("David Jones", "#000000", "davidjones.com", "9148de829f257719aef5c24b45bd85fc.png"),
        BrandPreset("Decathlon", "#3743BB", "decathlon.com.au", "c2a49b9b6900d4104422f686e6bfee46.png"),
        BrandPreset("Everyday Rewards", "#EB6901", "everyday.com.au", "7e7a5e4f555d56565cc9a8173f3026d8.png"),
        BrandPreset("Woolworths", "#178841", "woolworths.com.au", "7e7a5e4f555d56565cc9a8173f3026d8.png"),
        BrandPreset("Flybuys", "#1A7DCE", "flybuys.com.au", "8fef1ff7019e3e527dbbd7ca8ac98bb9.png"),
        BrandPreset("Grill'd", "#FF0006", "grilld.com.au", "4ce98397593f24e704f1f1545b0c2273.png"),
        BrandPreset("Harris Farm Markets", "#808080", "harrisfarm.com.au", "c3f585be5d354c2d73ec8bb756e06c9e.png"),
        BrandPreset("IGA", "#FFFFFF", "iga.com.au", "0ca9b4629d6ef6163e3f163209b6166a.png"),
        BrandPreset("IKEA Family", "#0057A4", "ikea.com", "b6d02914395f9b0fbf2fc601acf93b14.png"),
        BrandPreset("Just Jeans", "#0D5BA3", "justjeans.jgl.com.au", "0105e80ca7551dcc65af1a17e5ad1570.png"),
        BrandPreset("Myer One", "#000000", "myer.com.au", "aeb5455a6a2ede52f32a3217e93c13cd.png"),
        BrandPreset("Nespresso", "#000000", "nespresso.com", "fe08920a981f8f08c98f8e79f5801e7e.png"),
        BrandPreset("Priceline Pharmacy", "#FFFFFF", "priceline.com.au", "676a10585dd7f9fa3dbbad34e6fb0f89.png"),
        BrandPreset("Qantas", "#DA0101", "qantas.com", "23b53f32780eee9b67cb7914e5a6c58e.png"),
        BrandPreset("Rebel", "#000000", "rebelsport.com.au", "23874f062249320caad3b944723b94e0.png"),
        BrandPreset("San Churro", "#571D00", "sanchurro.com", "896fde7ffb048bc847f18392ecb6ce24.png"),
        BrandPreset("TerryWhite Chemmart", "#13A352", "terrywhitechemmart.com.au", "e81d71df7954ab9e0043fed25a7871cb.png"),
        BrandPreset("The Source Bulk Foods", "#572400", "thesourcebulkfoods.com.au", "cd6f7ec50169ee1516e31ffa12c75fbb.png"),

        // Popular Additional Australian & Global Loyalty Programs
        BrandPreset("Bunnings", "#0D5257", "bunnings.com.au"),
        BrandPreset("OnePass", "#5E2CED", "onepass.com.au"),
        BrandPreset("Chemist Warehouse", "#003DA5", "chemistwarehouse.com.au"),
        BrandPreset("Costco", "#E31837", "costco.com.au"),
        BrandPreset("Coles", "#E01A22", "coles.com.au"),
        BrandPreset("Kmart", "#E31837", "kmart.com.au"),
        BrandPreset("Target", "#CC0000", "target.com.au"),
        BrandPreset("Big W", "#0046AD", "bigw.com.au"),
        BrandPreset("BWS", "#F37021", "bws.com.au"),
        BrandPreset("Liquorland", "#000000", "liquorland.com.au"),
        BrandPreset("First Choice Liquor", "#53284F", "firstchoiceliquor.com.au"),
        BrandPreset("JB Hi-Fi", "#FFED00", "jbhifi.com.au"),
        BrandPreset("Officeworks", "#003B70", "officeworks.com.au"),
        BrandPreset("Spotlight", "#E31837", "spotlightstores.com"),
        BrandPreset("Anaconda", "#F57F20", "anacondastores.com"),
        BrandPreset("BCF", "#004B87", "bcf.com.au"),
        BrandPreset("Supercheap Auto", "#E31837", "supercheapauto.com.au"),
        BrandPreset("Mecca", "#000000", "mecca.com"),
        BrandPreset("Sephora", "#000000", "sephora.com.au"),
        BrandPreset("Adore Beauty", "#000000", "adorebeauty.com.au"),
        BrandPreset("Boost Juice", "#78BE20", "boostjuice.com.au"),
        BrandPreset("Guzman y Gomez", "#FFC72C", "guzmanygomez.com.au"),
        BrandPreset("Starbucks", "#006241", "starbucks.com.au"),
        BrandPreset("Subway", "#008C15", "subway.com.au"),
        BrandPreset("McDonald's", "#DB0007", "mcdonalds.com.au"),
        BrandPreset("Hungry Jack's", "#D62300", "hungryjacks.com.au"),
        BrandPreset("KFC", "#E4002B", "kfc.com.au"),
        BrandPreset("Hoyts", "#E31837", "hoyts.com.au"),
        BrandPreset("Event Cinemas", "#D51317", "eventcinemas.com.au"),
        BrandPreset("EB Games", "#004B8D", "ebgames.com.au"),
        BrandPreset("Dymocks", "#C8102E", "dymocks.com.au"),
        BrandPreset("Petbarn", "#FFCC00", "petbarn.com.au"),
        BrandPreset("PETstock", "#0054A6", "petstock.com.au"),
        BrandPreset("Kathmandu", "#000000", "kathmandu.com.au"),
        BrandPreset("Macpac", "#F37021", "macpac.com.au"),
        BrandPreset("Uniqlo", "#FF0000", "uniqlo.com"),
        BrandPreset("Lululemon", "#D31334", "lululemon.com.au"),
        BrandPreset("Bakers Delight", "#8B0037", "bakersdelight.com.au"),
        BrandPreset("T2 Tea", "#000000", "t2tea.com"),
        BrandPreset("Virgin Australia", "#51127F", "virginaustralia.com")
    )

    fun findMatch(queryName: String): BrandPreset? {
        val clean = queryName.trim().lowercase()
        if (clean.isEmpty()) return null
        return PRESETS.find { it.name.lowercase() == clean }
            ?: PRESETS.find { clean.contains(it.name.lowercase()) || it.name.lowercase().contains(clean) }
    }

    fun searchSuggestions(query: String, limit: Int = 6): List<BrandPreset> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return emptyList()
        return PRESETS.filter {
            it.name.lowercase().contains(clean) || it.domain.contains(clean)
        }.take(limit)
    }

    /**
     * Generates a fallback logo URL for any store name by inferring its domain
     * (e.g., "Harris Scarfe" -> harrisscarfe.com.au).
     */
    fun inferLogoUrlForStoreName(storeName: String): String? {
        val preset = findMatch(storeName)
        if (preset != null) return preset.defaultLogoUrl

        val slug = storeName.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
        if (slug.length < 2) return null

        val domain = "$slug.com.au"
        return "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$domain&size=256"
    }
}
