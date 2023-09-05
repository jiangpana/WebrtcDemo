package com.jansir.webrtcdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jansir.webrtcdemo.ui.theme.WebrtcDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebrtcDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize(),  verticalArrangement = Arrangement.Center) {

                        Button(onClick = { /* TODO */
                            startActivity(Intent(this@MainActivity,VOIPActivity::class.java).also {
                                it.putExtra("isCaller" ,true)
                            })
                        }) {
                            Text(text = "offer")

                        }
                        Button(onClick = { /*TODO*/
                            startActivity(Intent(this@MainActivity,VOIPActivity::class.java).also {
                                it.putExtra("isCaller" ,false)
                            })
                        }) {
                            Text(text = "answer")

                        }


                    }
                }
            }
        }
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
    WebrtcDemoTheme {
        Greeting("Android")
    }
}