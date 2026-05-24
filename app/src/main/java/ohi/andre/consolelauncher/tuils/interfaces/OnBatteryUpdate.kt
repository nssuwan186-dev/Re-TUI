package ohi.andre.consolelauncher.tuils.interfaces

interface OnBatteryUpdate {
    fun update(percentage: Float)
    fun onCharging()
    fun onNotCharging()
}
