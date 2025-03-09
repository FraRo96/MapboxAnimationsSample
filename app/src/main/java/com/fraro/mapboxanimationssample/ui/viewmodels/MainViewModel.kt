package com.fraro.mapboxanimationssample.ui.viewmodels

import com.fraro.mapboxanimationssample.R
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraro.composable_realtime_animations.data.models.Animation
import com.fraro.composable_realtime_animations.data.models.AnimationType
import com.fraro.composable_realtime_animations.data.models.State
import com.fraro.composable_realtime_animations.data.models.State.Start
import com.fraro.composable_realtime_animations.data.models.StateHolder
import com.fraro.composable_realtime_animations.data.models.VisualDescriptor
import com.fraro.composable_realtime_animations.ui.screens.toBatchedStateFlow
import com.fraro.mapboxanimationssample.ui.screens.MapboxScreenProjector
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

class MainViewModel : ViewModel() {
    private lateinit var streamProducer: StreamProducer

    private val _streamProducerFlow = MutableStateFlow<StateHolder<*,*>?>(null)
    val streamProducerFlow: StateFlow<StateHolder<*,*>?> = _streamProducerFlow.asStateFlow()

    fun restartAnimation() {
        streamProducer.stopStream()
    }

    fun createStreamProducer(
        context: Context,
        mapView: MapView
    ) {
        streamProducer = StreamProducerBuilder(
            context = context,
            mapView = mapView,
            callback = { streamState -> streamStateUpdate(streamState) }
        ).build()
        viewModelScope.launch {
            streamProducer.startStream()
        }
    }

    fun streamStateUpdate(newState: StateHolder<*,*>?) {
        _streamProducerFlow.update { newState }
    }
}

class StreamReader(context: Context) {
    private val inputStream: InputStream = context.resources.openRawResource(
        R.raw.coords2
    )

    fun readCsvAsFlow(): Flow<List<String>> = flow {
        csvReader().openAsync(inputStream) {
            try {
                readAllAsSequence().asFlow().collect {
                    val delay = 600L
                    delay(delay)
                    emit(it)
                    println("lettura da file $it")
                }
            } catch (e: Exception) {
                //close(e)
            }
        }
        //awaitClose { channel.close() }
    }
}

class StreamProducerBuilder(
    val context: Context,
    val mapView: MapView,
    val callback: (StateHolder<*,*>?) -> Unit
) {

    fun build() = StreamProducer(
            streamReader = StreamReader(context),
            mapProjector = MapProjector(),
            mapView = mapView,
            callback = callback
        )
}

class StreamProducer(
    val streamReader: StreamReader,
    val mapProjector: MapProjector,
    val mapView: MapView,
    val callback: (StateHolder<*, *>?) -> Unit
) {

    var isPaused = false

    fun stopStream() {
        val animationState = StateHolder(
            state = State.Stop,
            animationType = AnimationType.OFFSET
        )
        isPaused = true
        callback(animationState)
    }

    suspend fun startStream() {
        var isFirstTime = true
        withContext(Dispatchers.IO) {
            streamReader.readCsvAsFlow().collect {
                val isStart = isFirstTime || isPaused
                if (isPaused)
                    isPaused = false

                val animationState = mapProjector.mapProjectAndTransform(
                    latLong = it,
                    isStart = isStart,
                    mapView = mapView
                )
                isFirstTime = false

                callback(animationState)
            }
        }
    }
}

class MapProjector() {

    var isMapChanged = false

    fun mapProjectAndTransform(
        latLong: List<String>,
        isStart: Boolean,
        mapView: MapView
    ): StateHolder<*,*> {
        val lat = latLong.first().toDouble()
        val long = latLong[1].toDouble()
        val heading = latLong.last().toFloat()

        val screenProjector = MapboxScreenProjector(mapView)


        val offset = screenProjector.projectPointCoordinatesToScreen(
            Point.fromLngLat(long, lat)
        )

        val delay = 600
        val duration = (delay * 1.5).roundToInt()

        if (isStart) {

            val rotationStateHolder = StateHolder<Float, AnimationVector1D>(
                state = Start(
                    visualDescriptor = VisualDescriptor(
                        currentValue = heading,
                        animationType = AnimationType.ROTATION,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        ),
                        animatable = Animatable(
                            initialValue = heading,
                            typeConverter = Float.VectorConverter
                        ),
                        targetValue = heading,
                        durationMillis = duration,
                        isAnimated = true
                    )
                ),
                animationType = AnimationType.ROTATION
            )

            return StateHolder<Offset, AnimationVector2D>(
                state = Start(
                    visualDescriptor = VisualDescriptor(
                        currentValue = offset,
                        animationType = AnimationType.OFFSET,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        ),
                        animatable = Animatable(
                            initialValue = offset,
                            typeConverter = Offset.VectorConverter),
                        isAnimated = true,
                        durationMillis = duration
                    )
                ),
                animationType = AnimationType.OFFSET,
                wrappedStateHolders = listOf(rotationStateHolder)
            )
        }
        else {

            val rotationStateHolder = StateHolder<Float, AnimationVector>(
                state = State.Animated(
                    animation = Animation(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        ),
                        targetValue = heading,
                        durationMillis = duration
                    )
                ),
                animationType = AnimationType.ROTATION
            )

            return StateHolder<Offset, AnimationVector>(
                state = State.Animated(
                    animation = Animation(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        ),
                        targetValue = offset,
                        durationMillis = duration
                    )
                ),
                animationType = AnimationType.OFFSET,
                wrappedStateHolders = listOf(rotationStateHolder)
            )
        }
    }
}