package one.mixin.android.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import one.mixin.android.MixinApplication

object SensorOrientationChangeNotifier {
    private const val ORIENTATION_CHANGE_INTERVAL = 800L

    private var orientation = 0
    private val sensorManager = MixinApplication.appContext.getSystemService<SensorManager>()

    private var lastOrientationChangeTime = 0L

    var listener: Listener? = null

    fun resume() {
        sensorManager?.registerListener(notifierSensorEventListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun pause() {
        sensorManager?.unregisterListener(notifierSensorEventListener)
    }

    private val notifierSensorEventListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            var newOrientation = orientation
            if (x < 5 && x > -5 && y > 5)
                newOrientation = 0
            else if (x < -5 && y < 5 && y > -5)
                newOrientation = 90
            else if (x < 5 && x > -5 && y < -5)
                newOrientation = 180
            else if (x > 5 && y < 5 && y > -5)
                newOrientation = 270

            if (orientation != newOrientation &&
                System.currentTimeMillis() - lastOrientationChangeTime >= ORIENTATION_CHANGE_INTERVAL) {
                val oldOrientation = orientation
                orientation = newOrientation
                listener?.onOrientationChange(oldOrientation, newOrientation)
                lastOrientationChangeTime = System.currentTimeMillis()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    interface Listener {
        fun onOrientationChange(oldOrientation: Int, newOrientation: Int)
    }
}
