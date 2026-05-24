package ohi.andre.consolelauncher.tuils.interfaces

fun interface CommandExecuter {
    fun execute(input: String?, obj: Any?)

    fun execute(input: String?) {
        execute(input, null)
    }
}
