package ohi.andre.consolelauncher.managers.xml.classes

class XMLPrefsEntry(
    @JvmField var key: String,
    @JvmField var value: String
) {
    override fun equals(other: Any?): Boolean {
        if (other is XMLPrefsEntry) return this === other
        if (other is XMLPrefsSave) return key == other.label()
        return other == key
    }

    override fun toString(): String = "$key --> $value"
}
