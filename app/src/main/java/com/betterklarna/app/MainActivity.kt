package com.betterklarna.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.betterklarna.app.ui.navigation.AppNavigation
import com.betterklarna.app.ui.theme.BetterKlarnaTheme
import com.betterklarna.app.ui.viewmodel.CardViewModel
import com.betterklarna.app.util.ShareHelper

class MainActivity : ComponentActivity() {
    private val viewModel: CardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingCardIntent(intent)

        setContent {
            BetterKlarnaTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingCardIntent(intent)
    }

    private fun handleIncomingCardIntent(intent: Intent?) {
        val dataString = intent?.dataString ?: return
        val card = ShareHelper.parseShareUrl(dataString)
        if (card != null) {
            viewModel.saveCard(card) {
                Toast.makeText(this, "Added ${card.name} card to your wallet!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
