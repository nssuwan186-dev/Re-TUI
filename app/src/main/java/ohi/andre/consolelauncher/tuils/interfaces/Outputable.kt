package ohi.andre.consolelauncher.tuils.interfaces

interface Outputable {
    fun onOutput(output: CharSequence?, category: Int)
    fun onOutput(color: Int, output: CharSequence?)
    fun onOutput(output: CharSequence?)
    fun dispose()
}
