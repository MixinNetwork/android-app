package one.mixin.android.ui.device

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import one.mixin.android.R

class DeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
    }
}
