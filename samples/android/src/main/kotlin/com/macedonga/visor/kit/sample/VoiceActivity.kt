package com.macedonga.visor.kit.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.Visor
import com.macedonga.visor.kit.VoiceLaunch
import kotlinx.coroutines.launch

/** What Visor starts when the wearer says one of the app's phrases while it is not running: it answers, then shows the exchange. */
class VoiceActivity : ComponentActivity() {
    private var asked by mutableStateOf("")
    private var answered by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = LabColors) {
                Surface(Modifier.fillMaxSize(), color = Ink.page) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(asked.ifEmpty { "Waiting for a question" }, color = Ink.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(answered, color = Ink.dim, fontSize = 16.sp)
                    }
                }
            }
        }
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    // Visor waits about 20 seconds for the answer.
    private fun handle(intent: Intent?) {
        val request = VoiceLaunch.request(intent) ?: return
        asked = request.text
        lifecycleScope.launch {
            answered = try {
                Visor.connect(this@VoiceActivity).use { visor ->
                    val text = answer(visor, request.text)
                    visor.voice.reply(request, text)
                    text
                }
            } catch (e: KitException) {
                "${e.code}: ${e.message}"
            }
        }
    }
}
