package ohi.andre.consolelauncher.tuils

import java.util.ArrayList
import java.util.regex.Pattern

object TextUtils {
    @JvmStatic
    fun toPlanString(strings: List<*>?, separator: String): String {
        if (strings == null) {
            return ""
        }
        val output = StringBuilder()
        for (count in strings.indices) {
            output.append(strings[count])
            if (count < strings.size - 1) {
                output.append(separator)
            }
        }
        return output.toString()
    }

    @JvmStatic
    fun toPlanString(objs: Array<out Any?>?, separator: String): String {
        if (objs == null) {
            return ""
        }
        val output = StringBuilder()
        for (count in objs.indices) {
            output.append(objs[count])
            if (count < objs.size - 1) {
                output.append(separator)
            }
        }
        return output.toString()
    }

    @JvmStatic
    fun removeUnncesarySpaces(string: String?): String? {
        if (string == null) {
            return null
        }
        val sb = StringBuilder()
        var inDoubleQuote = false
        var inSingleQuote = false
        var escaped = false
        val chars = string.toCharArray()

        for (c in chars) {
            if (escaped) {
                sb.append(c)
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                sb.append(c)
                continue
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote
                sb.append(c)
                continue
            }
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote
                sb.append(c)
                continue
            }

            if (Character.isWhitespace(c) && !inDoubleQuote && !inSingleQuote) {
                if (sb.isNotEmpty() && !Character.isWhitespace(sb[sb.length - 1])) {
                    sb.append(" ")
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString().trim()
    }

    @JvmStatic
    fun splitArgs(input: String?): List<String> {
        val args: MutableList<String> = ArrayList()
        if (input == null) {
            return args
        }

        val currentArg = StringBuilder()
        var inDoubleQuote = false
        var inSingleQuote = false
        var escaped = false

        for (c in input) {
            if (escaped) {
                currentArg.append(c)
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote
                continue
            }
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote
                continue
            }
            if (Character.isWhitespace(c) && !inDoubleQuote && !inSingleQuote) {
                if (currentArg.isNotEmpty()) {
                    args.add(currentArg.toString())
                    currentArg.setLength(0)
                }
            } else {
                currentArg.append(c)
            }
        }
        if (currentArg.isNotEmpty()) {
            args.add(currentArg.toString())
        }
        return args
    }
}
