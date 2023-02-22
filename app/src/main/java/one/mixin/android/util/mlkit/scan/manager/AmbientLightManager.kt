package one.mixin.android.util.mlkit.scan.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class AmbientLightManager(context: Context) : SensorEventListener {

    private var darkLightLux = DARK_LUX

    private var brightLightLux = BRIGHT_LUX
    private val sensorManager: SensorManager?
    private val lightSensor: Sensor?
    private var lastTime: Long = 0

    var isLightSensorEnabled: Boolean
    private var mOnLightSensorEventListener: OnLightSensorEventListener? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
        isLightSensorEnabled = true
    }

    fun register() {
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregister() {
        if (sensorManager != null && lightSensor != null) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (isLightSensorEnabled) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime < INTERVAL_TIME) {
                return
            }
            lastTime = currentTime
            if (mOnLightSensorEventListener != null) {
                val lightLux = sensorEvent.values[0]
                mOnLightSensorEventListener!!.onSensorChanged(lightLux)
                if (lightLux <= darkLightLux) {
                    mOnLightSensorEventListener!!.onSensorChanged(true, lightLux)
                } else if (lightLux >= brightLightLux) {
                    mOnLightSensorEventListener!!.onSensorChanged(false, lightLux)
                }
            }
        }
    }

    fun setDarkLightLux(lightLux: Float) {
        darkLightLux = lightLux
    }

    fun setBrightLightLux(lightLux: Float) {
        brightLightLux = lightLux
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // do nothing
    }

    fun setOnLightSensorEventListener(listener: OnLightSensorEventListener?) {
        mOnLightSensorEventListener = listener
    }

    interface OnLightSensorEventListener {

        fun onSensorChanged(lightLux: Float) {}

        fun onSensorChanged(dark: Boolean, lightLux: Float)
    }

    companion object {
        private const val INTERVAL_TIME = 200
        protected const val DARK_LUX = 45.0f
        protected const val BRIGHT_LUX = 100.0f
    }
}
