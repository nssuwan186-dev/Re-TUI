package ohi.andre.consolelauncher.managers.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.LauncherActivity
import ohi.andre.consolelauncher.MainManager
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver
import ohi.andre.consolelauncher.tuils.PublicIOReceiver
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.IOException
import java.util.Collections
import android.net.Uri

class MusicService : Service(), OnPreparedListener, MediaPlayer.OnErrorListener,
    OnCompletionListener {
    private var player: MediaPlayer? = null
    private var songs: MutableList<Song>? = null
    var songIndex: Int = 0
        private set
    private val musicBind: IBinder = MusicBinder()
    private var songTitle: String? = Tuils.EMPTYSTRING
    private var shuffle = false

    private var lastNotificationChange: Long = 0

    //    do not touch the song playback from here
    override fun onCreate() {
        super.onCreate()
        this.songIndex = 0
        player = MediaPlayer()
        initMusicPlayer()

        lastNotificationChange = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra(EXTRA_CONTROL_CMD)) {
            val cmd = intent.getIntExtra(EXTRA_CONTROL_CMD, -1)
            when (cmd) {
                CONTROL_NEXT_INT -> playNext()
                CONTROL_PREV_INT -> playPrev()
                CONTROL_PLAY_PAUSE_INT -> if (this.isPlaying) pausePlayer()
                else playPlayer()
            }
        }

        updateForegroundStatus()
        return START_NOT_STICKY
    }

    private fun broadcastMusicState() {
        val intent: Intent = Intent(ACTION_MUSIC_CHANGED)
        intent.putExtra(SONG_TITLE, songTitle)
        intent.putExtra(MUSIC_PLAYING, this.isPlaying)
        intent.putExtra(SONG_POSITION, this.posn)
        intent.putExtra(SONG_DURATION, this.dur)
        intent.putExtra(MUSIC_SOURCE, SOURCE_INTERNAL)

        if (songs != null && this.songIndex >= 0 && this.songIndex < songs!!.size) {
            val s = songs!!.get(this.songIndex)
            intent.putExtra(SONG_SINGER, s.getSinger())
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        updateForegroundStatus()
    }

    private fun updateForegroundStatus() {
        if (songTitle == null || songTitle!!.isEmpty() || !this.isPlaying) {
            stopForeground(false)
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastNotificationChange < 1000) return
        lastNotificationChange = now

        val notification: Notification = buildNotification(this, songTitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFY_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFY_ID, notification)
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        if (songTitle == null || songTitle!!.length == 0) return
        mp.start()
        broadcastMusicState()
    }

    fun initMusicPlayer() {
        player!!.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK)
        player!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        player!!.setOnPreparedListener(this)
        player!!.setOnCompletionListener(this)
        player!!.setOnErrorListener(this)
    }

    fun setList(theSongs: MutableList<Song>?) {
        songs = theSongs
        if (shuffle) Collections.shuffle(songs)
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    fun playSong(): String? {
        try {
            player!!.reset()
        } catch (e: Exception) {
//            no need to log this error, as this will occur everytime
            Tuils.log(e)
        }

        val playSong = songs!!.get(this.songIndex)
        songTitle = playSong.getTitle()

        val id = playSong.getID()
        if (id == -1L) {
            val path = playSong.getPath()
            try {
                player!!.setDataSource(path)
            } catch (e: IOException) {
                Tuils.log(e)
                Tuils.toFile(e)
                return null
            }
        } else {
            val currSong = playSong.getID()
            val trackUri =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong)
            try {
                player!!.setDataSource(getApplicationContext(), trackUri)
            } catch (e: Exception) {
                Tuils.log(e)
                Tuils.toFile(e)
                return null
            }
        }
        player!!.prepareAsync()

        return playSong.getTitle()
    }

    fun setSong(songIndex: Int) {
        this.songIndex = songIndex
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (player!!.getCurrentPosition() > 0) {
            mp.reset()
            playNext()
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        mp.reset()
        return false
    }

    val posn: Int
        get() {
            try {
                return if (player != null) player!!.getCurrentPosition() else 0
            } catch (e: Exception) {
                return 0
            }
        }

    val dur: Int
        get() {
            try {
                return if (player != null) player!!.getDuration() else 0
            } catch (e: Exception) {
                return 0
            }
        }

    val isPlaying: Boolean
        get() {
            try {
                return player != null && player!!.isPlaying()
            } catch (e: Exception) {
                return false
            }
        }

    fun pausePlayer() {
        if (this.isPlaying) {
            player!!.pause()
        }
        broadcastMusicState()
    }

    fun stop() {
        try {
            player!!.stop()
        } catch (e: Exception) {
        }

        try {
            player!!.release()
        } catch (e: Exception) {
        }

        songTitle = null
        broadcastMusicState()
        setSong(0)
    }

    fun playPlayer() {
        if (player != null) {
            player!!.start()
        }
        broadcastMusicState()
    }

    fun seek(posn: Int) {
        player!!.seekTo(posn)
    }

    fun go() {
        player!!.start()
    }

    fun playPrev(): String? {
        if (songs!!.size == 0) return getString(R.string.no_songs)
        this.songIndex = previous()
        return playSong()
    }

    fun playNext(): String? {
        if (songs!!.size == 0) return getString(R.string.no_songs)
        this.songIndex = next()
        return playSong()
    }

    private fun next(): Int {
        var pos = this.songIndex + 1
        if (pos == songs!!.size) pos = 0
        return pos
    }

    private fun previous(): Int {
        var pos = this.songIndex - 1
        if (pos < 0) pos = songs!!.size - 1
        return pos
    }

    override fun onDestroy() {
        super.onDestroy()

        player!!.release()
        songs!!.clear()

        stopForeground(true)
    }

    fun setShuffle(shuffle: Boolean) {
        this.shuffle = shuffle
    }

    companion object {
        const val ACTION_MUSIC_CHANGED: String = "ohi.andre.consolelauncher.music_changed"
        const val ACTION_MUSIC_CONTROL: String = "ohi.andre.consolelauncher.music_control"

        const val SONG_TITLE: String = "song_title"
        const val SONG_SINGER: String = "song_singer"
        const val SONG_DURATION: String = "song_duration"
        const val SONG_POSITION: String = "song_position"
        const val MUSIC_PLAYING: String = "music_playing"
        const val MUSIC_SOURCE: String = "music_source"
        const val SOURCE_INTERNAL: String = "internal"
        const val SOURCE_EXTERNAL: String = "external"
        const val MUSIC_CONTROL: String = "music_control"
        const val CONTROL_PREVIOUS: String = "previous"
        const val CONTROL_PLAY_PAUSE: String = "play_pause"
        const val CONTROL_NEXT: String = "next"

        const val EXTRA_CONTROL_CMD: String = "control_cmd"
        const val CONTROL_NEXT_INT: Int = 1
        const val CONTROL_PREV_INT: Int = 2
        const val CONTROL_PLAY_PAUSE_INT: Int = 3

        const val NOTIFY_ID: Int = 100001

        private fun buildNotification(context: Context, songTitle: String?): Notification {
            val channelId = "music_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.setSound(null, null)
                val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
                if (manager != null) {
                    manager.createNotificationChannel(channel)
                }
            }

            val notIntent = Intent(context, LauncherActivity::class.java)
            val pendInt = PendingIntent.getActivity(
                context,
                0,
                notIntent,
                Tuils.pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
            )

            val notification: Notification
            val builder = NotificationCompat.Builder(context, channelId)
            builder.setContentIntent(pendInt)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val label = "cmd"
                val remoteInput = RemoteInput.Builder(PrivateIOReceiver.TEXT)
                    .setLabel(label)
                    .build()

                val i = Intent(context, PublicIOReceiver::class.java)
                i.setAction(PublicIOReceiver.ACTION_CMD)
                i.putExtra(MainManager.MUSIC_SERVICE, true)

                var flags = Tuils.pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags =
                        (flags and PendingIntent.FLAG_IMMUTABLE.inv()) or PendingIntent.FLAG_MUTABLE
                }

                val action = NotificationCompat.Action.Builder(
                    R.mipmap.ic_launcher, label,
                    PendingIntent.getBroadcast(context.getApplicationContext(), 10, i, flags)
                )
                    .addRemoteInput(remoteInput)
                    .build()

                builder.addAction(action)
            }

            notification = builder.build()
            return notification
        }
    }
}
