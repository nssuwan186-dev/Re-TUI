package ohi.andre.consolelauncher.managers.xml.classes

interface XMLPrefsElement {
    fun getValues(): XMLPrefsList?
    fun write(save: XMLPrefsSave, value: String)
    fun delete(): Array<out String?>?
    fun path(): String?
}
