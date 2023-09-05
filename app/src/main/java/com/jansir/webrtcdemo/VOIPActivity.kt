package com.jansir.webrtcdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jansir.webrtcdemo.ui.theme.WebrtcDemoTheme
import org.json.JSONObject
import org.webrtc.Camera2Enumerator
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import java.util.concurrent.Executor
import kotlin.properties.Delegates

class VOIPActivity : ComponentActivity(), PeerConnectionClient.PeerConnectionEvents {
    val TAG = "VOIPActivity"

    var isCaller = false
    private var localRender: SurfaceViewRenderer? = null
    private var remoteRender: SurfaceViewRenderer? = null
    private var peerConnectionClient by Delegates.notNull<PeerConnectionClient>()
    private val rootEglBase = EglBase.create()

    private var surfaceTextureHelper by Delegates.notNull<SurfaceTextureHelper>()
    private var videoCapturer by Delegates.notNull<VideoCapturer>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCaller = intent.extras?.getBoolean("isCaller") ?: false
        VOIPService.readCallBack = {
            Log.d(TAG, "isCaller =$isCaller onCreate() called ${String(it)}")
            JSONObject(String(it)).apply {
                val sdp = if (getBoolean("type")) {
                    SessionDescription(
                        SessionDescription.Type.OFFER, getString("sdp")
                    )
                } else {
                    SessionDescription(
                        SessionDescription.Type.ANSWER, getString("sdp")
                    )
                }
                peerConnectionClient.setRemoteDescription(sdp)
                if (!isCaller) {
                    peerConnectionClient.createAnswer()
                }
            }

        }
        VOIPService.start(isCaller)
        setContent {
            WebrtcDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var text by remember {
                        mutableStateOf("111")
                    }
                    Column {
                        TextField(value = text, onValueChange = { text = it })
                        Button(onClick = { /*TODO*/
//                            VOIPService.sendMessage(text.toByteArray())
//                            VOIPService.sendMessage(
//                                byteArrayOf(
//                                    *ByteArray(1024 * 1024),
//                                    *text.toByteArray()
//                                )
//                            )
                            startStream()

                        }) {
                            Text(text = "发送")
                        }
                        LocalSurfaceView(modifier = Modifier.size(100.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        RemoteSurfaceView(modifier = Modifier.size(100.dp))
                    }

                }
            }
        }

    }


    internal class HandlerExecutor(private val handler: Handler) : Executor {
        override fun execute(command: Runnable) {
            handler.post(command)
        }
    }

    @Composable
    fun LocalSurfaceView(modifier: Modifier = Modifier) {
        AndroidView(factory = { context ->
            SurfaceViewRenderer(context).apply {
                localRender = this
                localRender!!.init(rootEglBase.eglBaseContext, null)
                localRender!!.setZOrderMediaOverlay(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

        }, modifier = modifier)
    }

    @Composable
    fun RemoteSurfaceView(modifier: Modifier = Modifier) {
        AndroidView(factory = { context ->
            SurfaceViewRenderer(context).apply {
                remoteRender = this
                remoteRender!!.init(rootEglBase.eglBaseContext, null)
                remoteRender!!.setZOrderMediaOverlay(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

        }, modifier = modifier)
    }

    fun startStream() {
        val peerConnectionParameters = PeerConnectionClient.PeerConnectionParameters(
            true,
            true,
            false,
            0,
            0,
            0,
            0,
            "VP8",
            true,
            0,
            "ISAC",
            false,
            false,
            false,
            false,
            false,
            false,
            false
        )
        peerConnectionClient =
            PeerConnectionClient(
                applicationContext,
                HandlerExecutor(Handler(Looper.getMainLooper())),
                peerConnectionParameters,
                this
            )
        val options = PeerConnectionFactory.Options()
        peerConnectionClient.createPeerConnectionFactory(options, rootEglBase)
        val server = IceServer("stun:stun.counterpath.net:3478")
        peerConnectionClient.clearIceServer()
        peerConnectionClient.addIceServer(server)
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer()
        }
        peerConnectionClient.createPeerConnection(localRender, remoteRender)
        if (peerConnectionParameters.videoCallEnabled) {
            surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext())
            videoCapturer.initialize(
                surfaceTextureHelper,
                applicationContext, peerConnectionClient.getVideoSource().getCapturerObserver()
            )
            //width height fps
            videoCapturer.startCapture(600, 600, 30)
        }
        if (this.isCaller) {
            peerConnectionClient.createOffer()
        }
    }

    private fun createVideoCapturer(): VideoCapturer {
        val c = Camera2Enumerator(this)
        return c.createCapturer(c.getDeviceNames()[0], null)
    }

    override fun onLocalDescription(sdp: SessionDescription?) {
        Log.d(TAG, "onLocalDescription() called with: sdp = $sdp")
        val json = JSONObject()
        json.put("sdp", sdp!!.description)
        json.put("type", isCaller)
        VOIPService.sendMessage(json.toString().toByteArray())
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d(TAG, "onIceCandidate() called with: candidate = $candidate")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        Log.d(TAG, "onIceCandidatesRemoved() called with: candidates = $candidates")
    }

    override fun onIceConnected() {
        Log.d(TAG, "onIceConnected() called")
    }

    override fun onIceDisconnected() {
        Log.d(TAG, "onIceDisconnected() called")
    }

    override fun onPeerConnectionClosed() {
        Log.d(TAG, "onPeerConnectionClosed() called")
    }

    override fun onPeerConnectionStatsReady(reports: Array<out StatsReport>?) {
        Log.d(TAG, "onPeerConnectionStatsReady() called with: reports = $reports")
    }

    override fun onPeerConnectionError(description: String?) {
        Log.d(TAG, "onPeerConnectionError() called with: description = $description")
    }
}

