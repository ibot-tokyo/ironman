package tokyo.ibot.ironman

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {

    val pin1a: String = "BCM5" // 右タイヤ
    val pin1b: String = "BCM6"
    val pin2a: String = "BCM23" // 左タイヤ
    val pin2b: String = "BCM24"
    val pin3a: String = "BCM17" // 前タイヤ
    val pin3b: String = "BCM27"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val modeText = findViewById<TextView>(R.id.modeText)
        val service = PeripheralManagerService()
        val signal1a = service.openGpio(pin1a)
        val signal1b = service.openGpio(pin1b)
        val signal2a = service.openGpio(pin2a)
        val signal2b = service.openGpio(pin2b)
        val signal3a = service.openGpio(pin3a)
        val signal3b = service.openGpio(pin3b)
        signal1a.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal1b.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal2a.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal2b.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal3a.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal3b.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        val key = "mode"
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference(key)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.value
                Log.d("onDataChange", value.toString())
                modeText.text = value.toString()

                when (modeText.text) {
                    "go" -> {
                        signal1a.value = true
                        signal1b.value = false
                        signal2a.value = true
                        signal2b.value = false
                        signal3a.value = false
                        signal3b.value = false
                    }
                    "back" -> {
                        signal1a.value = false
                        signal1b.value = true
                        signal2a.value = false
                        signal2b.value = true
                        signal3a.value = false
                        signal3b.value = false
                    }
                    "turnRight" -> {
                        signal1a.value = true
                        signal1b.value = false
                        signal2a.value = false
                        signal2b.value = true
                        signal3a.value = true
                        signal3b.value = false
                    }
                    "turnLeft" -> {
                        signal1a.value = false
                        signal1b.value = true
                        signal2a.value = true
                        signal2b.value = false
                        signal3a.value = false
                        signal3b.value = true
                    }
                    "slideRight" -> {
                        signal1a.value = false
                        signal1b.value = false
                        signal2a.value = false
                        signal2b.value = false
                        signal3a.value = true
                        signal3b.value = false
                    }
                    "slideLeft" -> {
                        signal1a.value = false
                        signal1b.value = false
                        signal2a.value = false
                        signal2b.value = false
                        signal3a.value = false
                        signal3b.value = true
                    }
                    else -> {
                        signal1a.value = false
                        signal1b.value = false
                        signal2a.value = false
                        signal2b.value = false
                        signal3a.value = false
                        signal3b.value = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "Failed to read value.", error.toException())
            }
        })
    }
}
