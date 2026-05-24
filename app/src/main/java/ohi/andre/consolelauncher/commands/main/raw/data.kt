package ohi.andre.consolelauncher.commands.main.raw

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.main.specific.APICommand
import ohi.andre.consolelauncher.tuils.Tuils
import android.net.NetworkInfo.State
import java.lang.reflect.Field

@Suppress("DEPRECATION")
class data : APICommand, CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        val active = toggle(info)
        return info.res.getString(R.string.output_data) + Tuils.SPACE + active.toString()
    }

    private fun toggle(info: MainPack): Boolean {
        if (info.connectivityMgr == null) {
            try {
                init(info)
            } catch (ignored: Exception) {
            }
        }

        val mobileConnected: Boolean

        if (info.wifi == null) {
            info.wifi = info.context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        }

        if (info.wifi!!.isWifiEnabled) {
            mobileConnected = true
        } else {
            val mobileInfo = info.connectivityMgr!!.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            val state = mobileInfo!!.state
            mobileConnected = state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING
        }

        try {
            info.setMobileDataEnabledMethod!!.invoke(info.connectMgr, !mobileConnected)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return !mobileConnected
    }

    @Throws(Exception::class)
    private fun init(info: MainPack) {
        info.connectivityMgr = info.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val conmanClass = Class.forName(info.connectivityMgr!!::class.java.name)
        val iConnectivityManagerField = conmanClass.getDeclaredField("mService")
        iConnectivityManagerField.isAccessible = true
        info.connectMgr = iConnectivityManagerField.get(info.connectivityMgr)
        val iConnectivityManagerClass = Class.forName(info.connectMgr!!::class.java.name)
        info.setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", java.lang.Boolean.TYPE)
        info.setMobileDataEnabledMethod!!.isAccessible = true
    }

    override fun helpRes(): Int = R.string.help_data

    override fun argType(): IntArray = IntArray(0)

    override fun priority(): Int = 2

    override fun onNotArgEnough(info: ExecutePack, nArgs: Int): String? = null

    override fun onArgNotFound(info: ExecutePack, index: Int): String? = onNotArgEnough(info, 0)

    override fun willWorkOn(api: Int): Boolean = api < Build.VERSION_CODES.LOLLIPOP
}
