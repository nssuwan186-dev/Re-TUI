@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.managers.status

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.managers.HTMLExtractManager
import ohi.andre.consolelauncher.managers.TuiLocationManager
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Behavior
import ohi.andre.consolelauncher.tuils.Tuils
import ohi.andre.consolelauncher.UIManager

class WeatherManager(
    context: Context,
    delay: Long,
    private val size: Int,
    private val listener: StatusUpdateListener?
) : StatusManager(context, delay) {
    private var key: String? = null
    private var url: String? = null

    private var fixedLocation = false
    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var hasLocation = false

    init {
        key = if (XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
            XMLPrefsManager.get(Behavior.weather_key)
        } else {
            Behavior.weather_key.defaultValue()
        }

        var where = XMLPrefsManager.get(Behavior.weather_location)
        if (where == null || where.isEmpty() || (!Tuils.isNumber(where) && !where.contains(","))) {
            val location = TuiLocationManager.instance(context)
            location?.add(ACTION_WEATHER_GOT_LOCATION)
        } else {
            fixedLocation = true
            if (where.contains(",")) {
                val split = where.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                where = "lat=" + split[0] + "&lon=" + split[1]
            } else {
                where = "id=$where"
            }
            setUrl(where)
        }
    }

    override fun update() {
        updateWeather()
    }

    fun updateWeather() {
        if (!fixedLocation && !hasLocation) {
            return
        }

        if (!fixedLocation) {
            setUrl(lastLatitude, lastLongitude)
        }

        val currentUrl = url
        if (currentUrl != null) {
            val intent = Intent(HTMLExtractManager.ACTION_WEATHER)
            intent.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, currentUrl)
            intent.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount)
            LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(intent)
        }
    }

    fun setLocation(lat: Double, lon: Double) {
        lastLatitude = lat
        lastLongitude = lon
        hasLocation = true
        updateWeather()
    }

    private fun setUrl(where: String) {
        url = "https://api.openweathermap.org/data/2.5/weather?$where&appid=$key&units=" +
            XMLPrefsManager.get(Behavior.weather_temperature_measure)
    }

    private fun setUrl(latitude: Double, longitude: Double) {
        url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$key&units=" +
            XMLPrefsManager.get(Behavior.weather_temperature_measure)
    }

    fun setDelay(delay: Int) {
        this.delay = delay.toLong()
    }

    companion object {
        const val ACTION_WEATHER_GOT_LOCATION: String = "ohi.andre.consolelauncher.WEATHER_GOT_LOCATION"
    }
}
