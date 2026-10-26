package it.maicol07.mmail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.maicol07.mmail.data.OutlookProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val provider = OutlookProvider(this, BuildConfig.OUTLOOK_CLIENT_ID, BuildConfig.OUTLOOK_SIGNATURE_HASH)
        setContent { App(provider) }
    }
}
