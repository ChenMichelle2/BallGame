package com.example.ballgame

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ballgame.ui.theme.BallGameTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

// Data classes
data class Obstacle(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class Goal(val left: Float, val top: Float, val right: Float, val bottom: Float)

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private val sensitivity = 10f

    // Define a fixed game area (800 x 1200)
    private val gameWidth = 800f
    private val gameHeight = 1200f

    // Ball’s starting position
    private var ballX by mutableStateOf(120f)
    private var ballY by mutableStateOf(40f)
    private val ballRadius = 30f

    // Velocity components for continuous movement
    private var ballVX by mutableStateOf(0f)
    private var ballVY by mutableStateOf(0f)

    // Define the goal area
    private val goal = Goal(left = 740f, top = 750f, right = 780f, bottom = 790f)
    private var hasWon by mutableStateOf(false)

    // Maze obstacles
    private val outerBorders = listOf(
        Obstacle(0f, 0f, gameWidth, 20f),                       // Top border
        Obstacle(0f, gameHeight - 20f, gameWidth, gameHeight),  // Bottom border
        Obstacle(0f, 0f, 20f, gameHeight),                       // Left border
        Obstacle(gameWidth - 20f, 0f, gameWidth, gameHeight)    // Right border
    )
    private val innerObstacles = listOf(
        Obstacle(220f, 20f, 780f, 700f),
        Obstacle(20f, 900f, 780f, gameHeight - 20f)
    )
    private val obstacles = outerBorders + innerObstacles

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            BallGameTheme {
                Box(modifier = Modifier.size(800.dp, 1200.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw maze obstacles
                        obstacles.forEach { obstacle ->
                            drawRect(
                                color = Color.Gray,
                                topLeft = Offset(obstacle.left, obstacle.top),
                                size = Size(obstacle.right - obstacle.left, obstacle.bottom - obstacle.top)
                            )
                        }
                        // Goal area
                        drawRect(
                            color = Color.Green,
                            topLeft = Offset(goal.left, goal.top),
                            size = Size(goal.right - goal.left, goal.bottom - goal.top)
                        )
                        // Ball
                        drawCircle(
                            color = Color.Red,
                            center = Offset(ballX, ballY),
                            radius = ballRadius
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasWon) {
                            Text(text = "You Win!", color = Color.Black)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(onClick = { resetGame() }) {
                            Text(text = "Reset")
                        }
                    }
                }

                // Game loop
                LaunchedEffect(Unit) {
                    while (true) {
                        if (!hasWon) {
                            val newX = (ballX + ballVX).coerceIn(ballRadius, gameWidth - ballRadius)
                            val newY = (ballY + ballVY).coerceIn(ballRadius, gameHeight - ballRadius)

                            // Check for collisions
                            val collides = obstacles.any { obstacle ->
                                circleRectCollision(newX, newY, ballRadius, obstacle)
                            }
                            if (!collides) {
                                ballX = newX
                                ballY = newY
                            }
                            // Check if the ball has reached the goal
                            if (ballX in goal.left..goal.right && ballY in goal.top..goal.bottom) {
                                hasWon = true
                            }
                        }
                        delay(16L) // Roughly 60 frames per second
                    }
                }
            }
        }
    }

    // Reset
    private fun resetGame() {
        ballX = 120f
        ballY = 40f
        ballVX = 0f
        ballVY = 0f
        hasWon = false
    }

    // If the sensor readings are below a threshold, the velocity is set to zero
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val threshold = 0.05f  // Minimal rotation to register
            val horizontal = -it.values[1] // Rotation around Y-axis for horizontal movement
            val vertical = it.values[0]      // Rotation around X-axis for vertical movement
            ballVX = if (abs(horizontal) > threshold) horizontal * sensitivity else 0f
            ballVY = if (abs(vertical) > threshold) vertical * sensitivity else 0f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onResume() {
        super.onResume()
        gyroscopeSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // Collision detection
    private fun circleRectCollision(cx: Float, cy: Float, radius: Float, obstacle: Obstacle): Boolean {
        val closestX = cx.coerceIn(obstacle.left, obstacle.right)
        val closestY = cy.coerceIn(obstacle.top, obstacle.bottom)
        val distanceX = cx - closestX
        val distanceY = cy - closestY
        return (distanceX * distanceX + distanceY * distanceY) < (radius * radius)
    }
}
