package tokyo.ibot.ironman

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.net.NetworkInterface
import java.net.SocketException

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

    private val pinAppStatus: String = "BCM26" // App LED
    private val pinLanStatus: String = "BCM19" // Lan LED

    private val pin1a: String = "BCM5" // 右タイヤ
    private val pin1b: String = "BCM6"
    private val pin2a: String = "BCM23" // 左タイヤ
    private val pin2b: String = "BCM24"
    private val pin3a: String = "BCM17" // 前タイヤ
    private val pin3b: String = "BCM27"
    private val pin4a: String = "BCM20" // リフト
    private val pin4b: String = "BCM21"

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
        val signal4a = service.openGpio(pin4a)
        val signal4b = service.openGpio(pin4b)
        signal1a.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal1b.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal2a.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal2b.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal3a.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal3b.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal4a.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signal4b.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        val database = FirebaseDatabase.getInstance()
        val key = "mode"
        val liftKey = "lift"
        val ref = database.getReference(key)
        val liftRef = database.getReference(liftKey)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.value
                Log.d("onDataChange", value.toString())
                modeText.text = value.toString()

                when (modeText.text) {
                    "go" -> {
                        forward(signal1a, signal1b)
                        forward(signal2a, signal2b)
                        stop(signal3a, signal3b)
                    }
                    "back" -> {
                        reverse(signal1a, signal1b)
                        reverse(signal2a, signal2b)
                        stop(signal3a, signal3b)
                    }
                    "turnRight" -> {
                        forward(signal1a, signal1b)
                        reverse(signal2a, signal2b)
                        forward(signal3a, signal3b)
                    }
                    "turnLeft" -> {
                        reverse(signal1a, signal1b)
                        forward(signal2a, signal2b)
                        reverse(signal3a, signal3b)
                    }
                    "slideRight" -> {
                        stop(signal1a, signal1b)
                        stop(signal2a, signal2b)
                        forward(signal3a, signal3b)
                    }
                    "slideLeft" -> {
                        stop(signal1a, signal1b)
                        stop(signal2a, signal2b)
                        reverse(signal3a, signal3b)
                    }
                    else -> {
                        stop(signal1a, signal1b)
                        stop(signal2a, signal2b)
                        stop(signal3a, signal3b)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "Failed to read value.", error.toException())
            }
        })

        liftRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.value
                Log.d("onDataChange", value.toString())
                modeText.text = value.toString()

                when (modeText.text) {
                    "up" -> {
                        forward(signal4a, signal4b)
                    }
                    "down" -> {
                        reverse(signal4a, signal4b)
                    }
                    else -> {
                        stop(signal4a, signal4b)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "Failed to read value.", error.toException())
            }
        })
    }

    override fun onResume() {
        super.onResume()

        val service = PeripheralManagerService()
        val signalAppStatus = service.openGpio(pinAppStatus)
        val signalLanStatus = service.openGpio(pinLanStatus)
        signalAppStatus.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        signalLanStatus.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        val context = this.applicationContext
        val textView = findViewById<TextView>(R.id.ipAddressText)

        signalAppStatus.value = true

        if (!isOnline()) {
            textView.text = "isOffline"
            return
        }

        val text = getIpAddress()

        signalLanStatus.value = true
        textView.text = text
        sendToSlack(context, text)
    }


    private fun isOnline(): Boolean {
        val connMgr = this.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return (networkInfo != null) && networkInfo.isConnected
    }

    private fun getIpAddress(): String {
        var text = ""
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val network = interfaces.nextElement()
                val addresses = network.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    Log.d("address", address.toString())
                    text += address.toString() + System.lineSeparator()
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }

        return text;
    }

    private fun sendToSlack(context: Context, text: String) {
        // todo: set webHookUrl
        val webHookUrl: String = "https://hooks.slack.com/services/xxxxx/xxxxx/xxxxxx"

        Fuel.post(webHookUrl).body("{ \"text\" : \"$text\" }").responseString { _, response, result ->
            result.fold({ _ ->
                val data: String = String(response.data)
                Log.d("data", data)
            }, { err ->
                Log.e("err", err.toString())
                Toast.makeText(context, "error: sendToSlack", Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun forward(signalA: Gpio, signalB: Gpio) {
        signalA.value = true
        signalB.value = true
    }

    private fun reverse(signalA: Gpio, signalB: Gpio) {
        signalA.value = false
        signalB.value = true
    }

    private fun stop(signalA: Gpio, signalB: Gpio) {
        signalA.value = false
        signalB.value = false
    }
}
