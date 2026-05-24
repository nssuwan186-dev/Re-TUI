package ohi.andre.consolelauncher.managers.status

import ohi.andre.consolelauncher.UIManager

fun interface StatusUpdateListener {
    fun onUpdate(label: UIManager.Label?, text: CharSequence?)
}
