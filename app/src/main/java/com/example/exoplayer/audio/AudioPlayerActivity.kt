package com.example.exoplayer.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.exoplayer.R
import com.example.exoplayer.tracks.Track
import kotlinx.android.synthetic.main.activity_audio_player.btn_play_pause
import kotlinx.android.synthetic.main.activity_audio_player.btn_repeat
import kotlinx.android.synthetic.main.activity_audio_player.img_album_art
import kotlinx.android.synthetic.main.activity_audio_player.tv_track_title

class AudioPlayerActivity : AppCompatActivity() {

    private val TAG = "AudioPlayerActivity"

    private lateinit var mMediaBrowserCompat: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var tbar: Toolbar

    private var currentTrack: Track? = null

    companion object {
        private val KEY_TRACK = "track"
        fun getCallingIntent(context: Context, track: Track?): Intent {
            var intent = Intent(context, AudioPlayerActivity::class.java)
            intent.putExtra(KEY_TRACK, track)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            return intent
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Log.d(TAG, "Inside onNewIntent")

        currentTrack = intent?.getParcelableExtra(KEY_TRACK)
    }

    private val mediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            try {
                mediaController = MediaControllerCompat(
                    this@AudioPlayerActivity,
                    mMediaBrowserCompat.sessionToken
                )

                // for sync
                mediaController.registerCallback(mediaControllerCallback)

                initialiseUIStates(mediaController.metadata, mediaController.playbackState)
                buildTransportControls()
            } catch (e: RemoteException) {
                Log.d("tag", "Remote exception")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)
        tbar = findViewById(R.id.app_bar)
        setSupportActionBar(tbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        currentTrack = intent.getParcelableExtra(KEY_TRACK)

        mMediaBrowserCompat = MediaBrowserCompat(
            this, ComponentName(this, AudioService::class.java),
            mediaBrowserConnectionCallback, null
        )
    }

    override fun onStart() {
        super.onStart()
        mMediaBrowserCompat.connect()
    }

    override fun onStop() {
        mediaController.unregisterCallback(mediaControllerCallback)
        mMediaBrowserCompat.disconnect()
        super.onStop()
    }

    fun buildTransportControls() {
        if (currentTrack != null)
            mediaController.transportControls.playFromUri(
                Uri.parse(currentTrack?.path),
                Bundle().apply { putParcelable("track", currentTrack) })

        btn_play_pause.apply {
            setOnClickListener {
                val pbState = mediaController.playbackState.state
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        btn_play_pause.background = resources.getDrawable(R.drawable.ic_play, theme)
                    } else {
                        btn_play_pause.background = resources.getDrawable(R.drawable.ic_play)
                    }
                    mediaController.transportControls.pause()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        btn_play_pause.background =
                            resources.getDrawable(R.drawable.ic_pause, theme)
                    } else {
                        btn_play_pause.background = resources.getDrawable(R.drawable.ic_pause)
                    }
                    mediaController.transportControls.play()
                }
            }
        }

        btn_repeat.setOnClickListener {
            if (mediaController.repeatMode.equals(PlaybackStateCompat.REPEAT_MODE_ALL)) {
                mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btn_repeat.background = resources.getDrawable(R.drawable.ic_repeat, theme)
                } else {
                    btn_repeat.background = resources.getDrawable(R.drawable.ic_repeat)
                }
            } else {
                mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btn_repeat.background = resources.getDrawable(R.drawable.ic_repeat_on, theme)
                } else {
                    btn_repeat.background = resources.getDrawable(R.drawable.ic_repeat_on)
                }
            }
        }
    }

    private var mediaControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {

            metadata?.apply { updateMetaData(metadata) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {

            state?.apply { updateUIStates(state) }
        }
    }

    fun updateMetaData(metadata: MediaMetadataCompat) {
        tbar.title = metadata.getString(METADATA_KEY_TITLE)
        tv_track_title.setText(metadata.getString(METADATA_KEY_TITLE))

        val mediaDataRetriever = MediaMetadataRetriever()
        val uriIcon = metadata.description?.iconUri

        if (uriIcon != null) {
            mediaDataRetriever.setDataSource(this@AudioPlayerActivity, uriIcon)

            var songImage: Bitmap? = null
            mediaDataRetriever.embeddedPicture?.let {

                val albumArt: ByteArray = mediaDataRetriever.embeddedPicture
                songImage = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size);
            }
            img_album_art.setImageBitmap(songImage)
        }
    }

    fun updateUIStates(state: PlaybackStateCompat) {
        if (state?.state == PlaybackStateCompat.STATE_PLAYING) {
            btn_play_pause.background = resources.getDrawable(R.drawable.ic_pause)
        } else {
            btn_play_pause.background = resources.getDrawable(R.drawable.ic_play)
        }
    }

    fun initialiseUIStates(metadata: MediaMetadataCompat?, state: PlaybackStateCompat?) {
        metadata?.apply { updateMetaData(metadata) }
        state?.apply { updateUIStates(state) }
    }
}
