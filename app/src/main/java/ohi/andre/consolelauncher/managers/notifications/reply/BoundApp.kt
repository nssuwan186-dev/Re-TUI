package ohi.andre.consolelauncher.managers.notifications.reply

import it.andreuzzi.comparestring2.StringableObject

class BoundApp(
    @JvmField var applicationId: Int,
    @JvmField var packageName: String,
    @JvmField var label: String
) : StringableObject {
    @JvmField
    var lowercaseLabel: String = label.lowercase()

    override fun equals(other: Any?): Boolean {
        return other is BoundApp && applicationId == other.applicationId
    }

    override fun getLowercaseString(): String = lowercaseLabel

    override fun getString(): String = label
}
