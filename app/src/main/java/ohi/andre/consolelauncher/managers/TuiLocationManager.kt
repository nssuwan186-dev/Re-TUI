package ohi.andre.consolelauncher.managers

import ohi.andre.consolelauncher.BuildConfig
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.lang.ref.WeakReference
import java.util.ArrayList
import ohi.andre.consolelauncher.LauncherActivity
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Behavior
import ohi.andre.consolelauncher.tuils.Tuils

class TuiLocationManager private constructor(context: android.content.Context) {
    private val context: android.content.Context
    private var permissionActivity: java.lang.ref.WeakReference<Activity?>? = null
    var receiver: BroadcastReceiver

    var locationListener: LocationListener

    var handler: android.os.Handler? = null

    var locationAvailable: kotlin.Boolean = false
    var latitude: kotlin.Double = 0.0
    var longitude: kotlin.Double = 0.0

    private val actionsPool: kotlin.collections.MutableList<kotlin.String?>

    private fun setPermissionActivity(context: android.content.Context?) {
        if (context is Activity) {
            permissionActivity = java.lang.ref.WeakReference<Activity?>(context as Activity)
        }
    }

    private var receiverRegistered = false

    private fun registerPermissionReceiver() {
        if (receiverRegistered) return
        val filter: IntentFilter = IntentFilter()
        filter.addAction(TuiLocationManager.Companion.ACTION_GOT_PERMISSION)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        receiverRegistered = true
    }

    private var registered = false

    init {
        this.context = context.getApplicationContext()
        setPermissionActivity(context)
        actionsPool = java.util.ArrayList<kotlin.String?>()

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                clearHandler()

                latitude = location.getLatitude()
                longitude = location.getLongitude()

                locationAvailable = true

                val localBroadcastManager: LocalBroadcastManager =
                    LocalBroadcastManager.getInstance(this@TuiLocationManager.context)

                for (s in actionsPool) {
                    val i: Intent = Intent(s)
                    i.putExtra(TuiLocationManager.Companion.LATITUDE, location.getLatitude())
                    i.putExtra(TuiLocationManager.Companion.LONGITUDE, location.getLongitude())
                    localBroadcastManager.sendBroadcast(i)
                }

                dispose()
            }

            override fun onStatusChanged(provider: kotlin.String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: kotlin.String) {
            }

            override fun onProviderDisabled(provider: kotlin.String) {
            }
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent) {
                val action: kotlin.String? = intent.getAction()

                if (action == TuiLocationManager.Companion.ACTION_GOT_PERMISSION) {
                    if (intent.getIntExtra(
                            XMLPrefsManager.VALUE_ATTRIBUTE,
                            1
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        register()
                    }
                }
            }
        }

        registerPermissionReceiver()
    }

    @SuppressLint("MissingPermission")
    private fun register() {
        registerPermissionReceiver()
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val activity: Activity? =
                if (permissionActivity != null) permissionActivity!!.get() else null
            if (activity != null && !activity.isFinishing()) {
                ActivityCompat.requestPermissions(
                    activity,
                    kotlin.arrayOf<kotlin.String>(
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    ohi.andre.consolelauncher.LauncherActivity.LOCATION_REQUEST_PERMISSION
                )
            } else {
                broadcastFailure()
                dispose()
            }
            return
        }

        if (registered) return
        registered = true

        val c = android.location.Criteria()
        c.setAltitudeRequired(false)
        c.setAccuracy(android.location.Criteria.ACCURACY_COARSE)
        c.setBearingRequired(false)
        c.setCostAllowed(false)
        c.setHorizontalAccuracy(android.location.Criteria.ACCURACY_LOW)
        c.setPowerRequirement(android.location.Criteria.POWER_LOW)
        c.setSpeedRequired(false)

        val manager: LocationManager =
            context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager

        try {
            manager.requestLocationUpdates(
                (XMLPrefsManager.getInt(ohi.andre.consolelauncher.managers.xml.options.Behavior.location_update_mintime) * 60 * 1000).toLong(),
                XMLPrefsManager.getInt(ohi.andre.consolelauncher.managers.xml.options.Behavior.location_update_mindistance)
                    .toFloat(),
                c,
                locationListener,
                Looper.getMainLooper()
            )
        } catch (e: java.lang.Exception) {
            Tuils.log(e)
            Tuils.toFile(e)
        }

        handler = android.os.Handler(Looper.getMainLooper())
        handler!!.postDelayed(object : java.lang.Runnable {
            override fun run() {
                broadcastFailure()
                dispose()
            }
        }, TuiLocationManager.Companion.MAX_DELAY.toLong())
    }

    private fun broadcastFailure() {
        val localBroadcastManager: LocalBroadcastManager =
            LocalBroadcastManager.getInstance(context)
        for (s in actionsPool) {
            val i: Intent = Intent(s)
            i.putExtra(TuiLocationManager.Companion.FAIL, true)
            localBroadcastManager.sendBroadcast(i)
        }
    }

    fun add(action: kotlin.String?) {
        actionsPool.add(action)

        register()
    }

    fun rm(action: kotlin.String?) {
        actionsPool.remove(action)
    }

    @SuppressLint("MissingPermission")
    private fun dispose() {
        actionsPool.clear()
        if (receiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
            } catch (ignored: java.lang.Exception) {
            }
            receiverRegistered = false
        }
        registered = false

        val manager: LocationManager? =
            context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager?
        if (manager != null) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                manager.removeUpdates(locationListener)
            }
        }

        clearHandler()
    }

    private fun clearHandler() {
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
            handler = null
        }
    }

    companion object {
        val ACTION_GOT_PERMISSION: kotlin.String =
            BuildConfig.APPLICATION_ID + ".got_location_permission"

        const val LATITUDE: kotlin.String = "lat"
        const val LONGITUDE: kotlin.String = "long"
        const val FAIL: kotlin.String = "fail"

        private const val MAX_DELAY = 10000

        @SuppressLint("StaticFieldLeak")
        private var instance: TuiLocationManager? = null

        @Synchronized
        fun instance(context: android.content.Context): TuiLocationManager? {
            if (TuiLocationManager.Companion.instance == null) TuiLocationManager.Companion.instance =
                TuiLocationManager(context)
            else TuiLocationManager.Companion.instance!!.setPermissionActivity(context)
            return TuiLocationManager.Companion.instance
        }

        fun disposeStatic() {
            if (TuiLocationManager.Companion.instance != null) TuiLocationManager.Companion.instance!!.dispose()
            TuiLocationManager.Companion.instance = null
        }
    }
}
