package ohi.andre.consolelauncher.managers.music

import it.andreuzzi.comparestring2.StringableObject
import java.io.File

class Song : StringableObject {
    private var id: Long
    private var title: String
    private var path: String? = null
    private var lowercaseTitle: String
    private var singer: String

    constructor(songID: Long, songTitle: String, singer: String) {
        id = songID
        title = songTitle
        this.singer = singer
        lowercaseTitle = title.lowercase()
    }

    constructor(file: File) {
        var name = file.name
        val dot = name.lastIndexOf(".")
        if (dot != -1) {
            name = name.substring(0, dot)
        }

        title = name
        path = file.absolutePath
        id = -1
        singer = "Unknown"
        lowercaseTitle = title.lowercase()
    }

    fun getID(): Long = id

    fun getTitle(): String = title

    fun getSinger(): String = singer

    fun getPath(): String? = path

    override fun getLowercaseString(): String = lowercaseTitle

    override fun getString(): String = title
}
