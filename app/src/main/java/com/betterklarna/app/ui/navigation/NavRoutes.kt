package com.betterklarna.app.ui.navigation

sealed class NavRoutes(val route: String) {
    data object CardList : NavRoutes("card_list")
    data object BatchImport : NavRoutes("batch_import")
    data object Scanner : NavRoutes("scanner")

    data object CardDetail : NavRoutes("card_detail/{cardId}") {
        fun createRoute(cardId: Long) = "card_detail/$cardId"
    }

    data object AddEditCard : NavRoutes("add_edit_card?cardId={cardId}&cardNumber={cardNumber}&format={format}") {
        fun createRoute(
            cardId: Long? = null,
            cardNumber: String? = null,
            format: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (cardId != null) params.add("cardId=$cardId")
            if (!cardNumber.isNullOrBlank()) params.add("cardNumber=$cardNumber")
            if (!format.isNullOrBlank()) params.add("format=$format")
            return if (params.isEmpty()) "add_edit_card" else "add_edit_card?${params.joinToString("&")}"
        }
    }
}
