package com.example.togetherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.togetherapp.ui.theme.TogetherAppTheme
import com.yandex.mapkit.MapKitFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. ИНИЦИАЛИЗАЦИЯ
        MapKitFactory.setApiKey("e4f380a3-c4fa-4ddf-87e4-ac585da8b4f2")
        MapKitFactory.initialize(this)

        setContent {
            // Здесь вызываем наш созданный тестовый экран
            TestMapScreen()
        }
    }
    // 2. Жизненный цикл, иначе карта будет пустой или зависнет
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TogetherAppTheme {
        Greeting("Android")
    }
}