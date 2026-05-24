package ohi.andre.consolelauncher.managers.music

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.widget.MediaController.MediaPlayerControl
import androidx.core.content.ContextCompat
import ohi.andre.consolelauncher.managers.music.MusicService.MusicBinder
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Behavior
import ohi.andre.consolelauncher.tuils.StoppableThread
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.File
import java.util.Collections
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.widget.MediaController
import java.util.ArrayList

/**
 * Created by francescoandreuzzi on 17/08/2017.
 */
class MusicManager2(var mContext: Context) : MediaPlayerControl {
    val WAITING_NEXT: Int = 10
    val WAITING_PREVIOUS: Int = 11
    val WAITING_PLAY: Int = 12
    val WAITING_LISTEN: Int = 13

    @get:JvmName("getSongList")
    @set:JvmName("setSongList")
    var songs: MutableList<Song>? = null

    var musicSrv: MusicService? = null
    var musicBound: Boolean = false
    var playIntent: Intent? = null

    var playbackPaused: Boolean = true
    var stopped: Boolean = true

    var loader: Thread? = null

    var waitingMethod: Int = 0
    var savedParam: String? = null

    var headsetBroadcast: BroadcastReceiver?
    var headsetReceiverRegistered: Boolean = false

    fun init() {
        playIntent = Intent(mContext, MusicService::class.java)
        mContext.bindService(playIntent!!, musicConnection, Context.BIND_AUTO_CREATE)
        mContext.startService(playIntent)
    }

    fun refresh() {
        destroy()
        updateSongs()
        registerHeadsetReceiver()
    }

    fun destroy() {
        if (musicSrv != null && musicBound) {
            musicSrv!!.stop()
            mContext.unbindService(musicConnection)
            mContext.stopService(playIntent)
            musicSrv = null
        }

        unregisterHeadsetReceiver()

        musicBound = false
        playbackPaused = true
        stopped = true
    }

    private fun registerHeadsetReceiver() {
        if (headsetReceiverRegistered) {
            return
        }

        val action: String?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            action = AudioManager.ACTION_HEADSET_PLUG
        } else {
            action = Intent.ACTION_HEADSET_PLUG
        }

        ContextCompat.registerReceiver(
            mContext.getApplicationContext(),
            headsetBroadcast,
            IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        headsetReceiverRegistered = true
    }

    private fun unregisterHeadsetReceiver() {
        if (!headsetReceiverRegistered) {
            return
        }

        try {
            mContext.getApplicationContext().unregisterReceiver(headsetBroadcast)
        } catch (e: Exception) {
            Tuils.log(e)
        }

        headsetReceiverRegistered = false
    }

    fun playNext(): String? {
        if (!musicBound) {
            init()
            waitingMethod = WAITING_NEXT

            return null
        }

        playbackPaused = false
        stopped = false

        return musicSrv!!.playNext()
    }

    fun playPrev(): String? {
        if (!musicBound) {
            init()
            waitingMethod = WAITING_PREVIOUS

            return null
        }

        playbackPaused = false
        stopped = false

        return musicSrv!!.playPrev()
    }

    override fun pause() {
        if (musicSrv == null || playbackPaused) return

        playbackPaused = true
        musicSrv!!.pausePlayer()
    }

    fun play(): String? {
        if (!musicBound) {
            init()
            waitingMethod = WAITING_PLAY

            return null
        }

        if (stopped) {
            musicSrv!!.playSong()
            playbackPaused = false
            stopped = false
        } else if (playbackPaused) {
            playbackPaused = false
            musicSrv!!.playPlayer()
        } else pause()

        return null
    }

    fun lsSongs(): String {
        if (songs!!.size == 0) return "[]"

        val ss: MutableList<String?> = ArrayList()
        for (s in songs!!) {
            ss.add(s.getTitle())
        }

        ss.sortWith(compareBy { it ?: "" })
        Tuils.addPrefix(ss, Tuils.DOUBLE_SPACE)
        Tuils.insertHeaders(ss, false)

        return Tuils.toPlanString(ss, Tuils.NEWLINE)
    }

    fun updateSongs() {
        loader = object : StoppableThread() {
            override fun run() {
                try {
                    if (songs == null) songs = ArrayList<Song>()
                    else songs!!.clear()

                    if (XMLPrefsManager.getBoolean(Behavior.songs_from_mediastore)) {
                        val musicResolver = mContext.getContentResolver()
                        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        val musicCursor = musicResolver.query(musicUri, null, null, null, null)
                        if (musicCursor != null && musicCursor.moveToFirst()) {
                            val titleColumn =
                                musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                            val idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
                            val artistColumn =
                                musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                            do {
                                val thisId = musicCursor.getLong(idColumn)
                                val thisTitle = musicCursor.getString(titleColumn)
                                val thisArtist = musicCursor.getString(artistColumn)
                                songs!!.add(Song(thisId, thisTitle, thisArtist))
                            } while (musicCursor.moveToNext())
                        }
                        musicCursor!!.close()
                    } else {
                        val path = XMLPrefsManager.get(Behavior.songs_folder)
                        if (path.length == 0) return

                        val file: File?
                        if (path.startsWith(File.separator)) {
                            file = File(path)
                        } else {
                            file = File(XMLPrefsManager.get(Behavior.home_path), path)
                        }

                        if (file.exists() && file.isDirectory()) {
                            songs!!.addAll(Tuils.getSongsInFolder(file).filterNotNull())
                        }
                    }
                } catch (e: Exception) {
                    Tuils.toFile(e)
                }

                synchronized(songs!!) {
                    (songs as Object).notify()
                }
            }
        }
        loader!!.start()
    }

    private val musicConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicBinder
            musicSrv = binder.service
            musicSrv!!.setShuffle(XMLPrefsManager.getBoolean(Behavior.random_play))

            if (loader!!.isAlive()) {
                synchronized(songs!!) {
                    try {
                        (songs as Object).wait()
                    } catch (e: InterruptedException) {
                    }
                }
            }

            musicSrv!!.setList(songs)
            musicBound = true

            when (waitingMethod) {
                WAITING_NEXT -> playNext()
                WAITING_PREVIOUS -> playPrev()
                WAITING_PLAY -> play()
                WAITING_LISTEN -> select(savedParam)
            }

            waitingMethod = 0
            savedParam = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicBound = false
        }
    }

    init {
        updateSongs()

        headsetBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.getIntExtra("state", 0) == 0) pause()
            }
        }

        registerHeadsetReceiver()

        init()
    }

    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun getCurrentPosition(): Int {
        if (musicSrv != null && musicBound && musicSrv!!.isPlaying) return musicSrv!!.posn
        else return -1
    }

    override fun getDuration(): Int {
        if (musicSrv != null && musicBound && musicSrv!!.isPlaying) return musicSrv!!.dur
        else return -1
    }

    val songIndex: Int
        get() {
            if (musicSrv != null) return musicSrv!!.songIndex
            return -1
        }

    override fun isPlaying(): Boolean {
        if (musicSrv != null && musicBound) return musicSrv!!.isPlaying
        return false
    }

    fun stop() {
        destroy()
    }

    fun get(index: Int): Song? {
        if (index < 0 || index >= songs!!.size) return null
        return songs!!.get(index)
    }

    override fun seekTo(pos: Int) {
        musicSrv!!.seek(pos)
    }

    fun select(song: String?) {
        if (!musicBound) {
            init()
            waitingMethod = WAITING_LISTEN
            savedParam = song

            return
        }

        var i = -1
        for (index in songs!!.indices) {
            if (songs!!.get(index).getTitle() == song) i = index
        }

        if (i == -1) {
            return
        }

        musicSrv!!.setSong(i)
        musicSrv!!.playSong()
    }

    override fun start() {
        musicSrv!!.go()
    }

    fun getSongs(): MutableList<Song> {
        return ArrayList<Song>(songs ?: emptyList())
    }

    companion object {
        val MUSIC_EXTENSIONS: Array<String?> = arrayOf<String?>(".mp3", ".wav", ".ogg", ".flac")
    }
}
