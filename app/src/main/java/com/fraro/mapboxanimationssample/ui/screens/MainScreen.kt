package com.fraro.mapboxanimationssample.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fraro.composable_realtime_animations.data.models.StateHolder
import com.fraro.composable_realtime_animations.ui.screens.RealtimeBox
import com.fraro.mapboxanimationssample.ui.viewmodels.MainViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.fraro.mapboxanimationssample.R

lateinit var carBitmap: ImageBitmap

@OptIn(MapboxExperimental::class)
@Composable
fun MapboxScreen() {
    val context = LocalContext.current
    val lifecycleOwner = context as ViewModelStoreOwner
    val viewModel: MainViewModel = ViewModelProvider(lifecycleOwner)[MainViewModel::class.java]

    remember {
        carBitmap = Bitmap.createScaledBitmap(
            ContextCompat.getDrawable(
                context, R.drawable.red_car
            )?.toBitmap()!!,
            75,
            100,
            false
        ).asImageBitmap()
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapInitOptionsFactory = {
            MapInitOptions(
                context = it,
                styleUri = Style.OUTDOORS,
                textureView = true,
                cameraOptions = CameraOptions.Builder()
                    .center(
                        Point.fromLngLat(
                            7.64506,
                            45.06069
                        )
                    )
                    .zoom(12.0)
                    .pitch(15.0)
                    .build()
            )
        }
    ) {
        var isCameraChanged = remember { false }
        // Get reference to the raw MapView using MapEffect
        MapEffect(Unit) { mapView ->

            mapView.mapboxMap.subscribeCameraChanged {
                viewModel.restartAnimation()
            }

            viewModel.createStreamProducer(
                context = context,
                mapView = mapView
            )

            mapView.mapboxMap.loadStyle(Style.OUTDOORS) { style -> /*setupBuildings(style)*/ }
        }
    }

    val flowDelegate by viewModel.streamProducerFlow.collectAsStateWithLifecycle(null)
    println("delegato $flowDelegate")

    RealtimeBox(
        animationState = flowDelegate,
        initialOffset = Offset(-100f, -100f),
        initialRotation = 0f,
    ) {
        Image(
            painter = painterResource(R.drawable.red_car),
            contentDescription = "car",
            modifier = Modifier.size(40.dp)
        )
    }

    //if (isReady) {
        //AnimatedMarker(viewModel.streamProducer?.getStreamFlow())
        /*with(viewModel.streamProducer!!.getStreamFlow()) {
            val flow =  this
            val flowDelegate by flow.collectAsStateWithLifecycle()

            println("nuovo valore ${this.value}")

            RealtimeBox(
                animationFlow = this,
                initialOffset = Offset(-100f, -100f),
                initialRotation = 0f,
            ) {
                Box(modifier = Modifier.background(Color.Red).size(500.dp)) {

                }
            }
        }*/
    //}
    //AnimatedMarker(mainFlow.value)
}

class MapboxScreenProjector(val mapView: MapView) {
    fun projectPointCoordinatesToScreen(point: Point): Offset {
        val projected = mapView.mapboxMap.pixelForCoordinate(point)
        return Offset(
            x = projected.x.toFloat(),
            y = projected.y.toFloat()
        )
    }
}