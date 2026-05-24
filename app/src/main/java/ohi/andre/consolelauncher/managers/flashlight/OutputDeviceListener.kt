package ohi.andre.consolelauncher.managers.flashlight

interface OutputDeviceListener : DeviceListener {
    fun onStatusChanged(status: Boolean)
}
