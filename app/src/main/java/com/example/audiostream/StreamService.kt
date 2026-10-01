package com.example.audiostream

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.os.Build
import android.os.IBinder
import java.net.InetSocketAddress
import java.net.Socket

class StreamService : Service() {
    companion object { @Volatile var status = "Idle" }

    @Volatile private var running = false
    private var socket: Socket? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopStream(); stopSelf(); return START_NOT_STICKY }
        if (running) return START_NOT_STICKY
        goForeground()
        val host = intent?.getStringExtra("host") ?: return START_NOT_STICKY
        val port = intent.getIntExtra("port", 5555)
        running = true
        Thread { stream(host, port) }.start()
        return START_NOT_STICKY
    }

    private fun goForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel("a", "Audio", NotificationManager.IMPORTANCE_LOW))
        }
        val n = (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "a") else Notification.Builder(this))
            .setContentTitle("Streaming PC audio")
            .setSmallIcon(android.R.drawable.ic_media_play).build()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(1, n)
    }

    private fun stream(host: String, port: Int) {
        var track: AudioTrack? = null
        try {
            status = "Connecting..."
            val s = Socket().also { socket = it }
            s.tcpNoDelay = true
            s.connect(InetSocketAddress(host, port), 5000)
            val min = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(maxOf(min * 2, 16384))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            track.play()
            status = "Playing from $host"
            val input = s.getInputStream()
            val buf = ByteArray(4096)
            while (running) {
                val n = input.read(buf)
                if (n < 0) break
                track.write(buf, 0, n)
            }
            status = "Disconnected"
        } catch (e: Exception) {
            status = if (running) "Error: ${e.message}" else "Stopped"
        } finally {
            track?.release()
            running = false
            try { socket?.close() } catch (_: Exception) {}
            stopForeground(true); stopSelf()
        }
    }

    private fun stopStream() {
        running = false
        try { socket?.close() } catch (_: Exception) {}
    }

    override fun onDestroy() { stopStream(); super.onDestroy() }
}
