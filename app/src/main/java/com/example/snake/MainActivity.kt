package com.example.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.snake.ui.theme.DarkGreen
import com.example.snake.ui.theme.LightGreen
import com.example.snake.ui.theme.Shapes
import com.example.snake.ui.theme.SnakeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = Game(lifecycleScope)

        setContent {
            SnakeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LightGreen
                ) {
                    Snake(game)
                }
            }
        }
    }
}

data class State(
    val food: Pair<Int, Int>,
    val snake: List<Pair<Int, Int>>,
    val score: Int = 0,
    val isGameOver: Boolean = false
)

class Game(private val scope: CoroutineScope) {

    private val mutex = Mutex()
    private val mutableState =
        MutableStateFlow(State(food = Pair(5, 5), snake = listOf(Pair(7, 7))))
    val state: Flow<State> = mutableState

    private var snakeLength = 4

    var move = Pair(1, 0)
        private set

    fun tryChangeDirection(newDir: Pair<Int, Int>) {
        scope.launch {
            mutex.withLock {
                val (nx, ny) = newDir
                val (cx, cy) = move
                if (nx + cx == 0 && ny + cy == 0) {
                    return@withLock
                }
                move = newDir
            }
        }
    }

    init {
        scope.launch {

            while (true) {
                delay(150)
                mutableState.update { old ->
                    if (old.isGameOver) return@update old

                    val newHead = old.snake.first().let { pos ->
                        mutex.withLock {
                            Pair(
                                (pos.first + move.first + BOARD_SIZE) % BOARD_SIZE,
                                (pos.second + move.second + BOARD_SIZE) % BOARD_SIZE
                            )
                        }
                    }

                    if (old.snake.contains(newHead)) {
                        return@update old.copy(isGameOver = true)
                    }

                    var newScore = old.score
                    var newSnakeLength = snakeLength

                    if (newHead == old.food) {
                        newSnakeLength++
                        snakeLength = newSnakeLength
                        newScore += 100
                    }

                    val nextFood = if (newHead == old.food) {
                        Pair(Random().nextInt(BOARD_SIZE), Random().nextInt(BOARD_SIZE))
                    } else old.food

                    old.copy(
                        food = nextFood,
                        snake = listOf(newHead) + old.snake.take(newSnakeLength - 1),
                        score = newScore
                    )
                }
            }
        }
    }

    fun reset() {
        scope.launch {
            mutex.withLock {
                snakeLength = 4
                mutableState.value = State(
                    food = Pair(5, 5),
                    snake = listOf(Pair(7, 7)),
                    score = 0,
                    isGameOver = false
                )
                move = Pair(1, 0)
            }
        }
    }

    companion object {
        const val BOARD_SIZE = 16
    }
}

@Composable
fun Buttons(onDirectionChange: (Pair<Int, Int>) -> Unit) {
    val buttonSize = Modifier.size(64.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp),
    ) {
        Button(
            onClick = { onDirectionChange(Pair(0, -1)) },
            modifier = buttonSize,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkGreen
            )
        ) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = LightGreen)
        }
        Row {
            Button(
                onClick = { onDirectionChange(Pair(-1, -0)) },
                modifier = buttonSize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkGreen
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = LightGreen)
            }
            Spacer(modifier = buttonSize)
            Button(
                onClick = { onDirectionChange(Pair(1, 0)) },
                modifier = buttonSize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkGreen
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = LightGreen)
            }
        }
        Button(
            onClick = { onDirectionChange(Pair(0, 1)) },
            modifier = buttonSize,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkGreen
            )
        ) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = LightGreen)
        }
    }
}

@Composable
fun Board(state: State) {
    BoxWithConstraints(Modifier.padding(16.dp)) {
        val tileSize = maxWidth / Game.BOARD_SIZE

        Box(
            Modifier
                .size(maxWidth)
                .border(2.dp, DarkGreen)
        )
        Box(
            Modifier
                .offset(x = tileSize * state.food.first, y = tileSize * state.food.second)
                .size(tileSize)
                .background(
                    DarkGreen, CircleShape
                )
        )

        state.snake.forEach {
            Box(
                modifier = Modifier
                    .offset(x = tileSize * it.first, y = tileSize * it.second)
                    .size(tileSize)
                    .background(
                        DarkGreen, Shapes.small
                    )
            )
        }
    }
}

@Composable
fun Snake(game: Game) {
    val state by game.state.collectAsState(
        initial = State(
            food = Pair(5, 5),
            snake = listOf(Pair(7, 7)),
            score = 0,
            isGameOver = false
        )
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Score: ${state.score}",
                color = DarkGreen
            )
            Board(state)
            Buttons(onDirectionChange = { dir ->
                game.tryChangeDirection(dir)
            })
        }

        if (state.isGameOver) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Game Over",
                        style = MaterialTheme.typography.headlineLarge,
                        color = LightGreen
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { game.reset() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                    ) {
                        Text("Continue", color = LightGreen)
                    }
                }
            }
        }
    }
}

