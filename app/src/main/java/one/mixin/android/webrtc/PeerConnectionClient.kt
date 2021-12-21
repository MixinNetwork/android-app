package one.mixin.android.webrtc

import android.content.Context
import androidx.collection.arrayMapOf
import kotlinx.coroutines.delay
import one.mixin.android.BuildConfig
import one.mixin.android.RxBus
import one.mixin.android.event.FrameKeyEvent
import one.mixin.android.event.VoiceEvent
import one.mixin.android.session.Session
import org.webrtc.AddIceObserver
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCFrameDecryptor
import org.webrtc.RTCFrameEncryptor
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import timber.log.Timber

class PeerConnectionClient(context: Context, private val events: PeerConnectionEvents) {
    private var factory: PeerConnectionFactory? = null
    private var isError = false

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
    }

    private val pcObserver = PCObserver()
    private val iceServers = arrayListOf<PeerConnection.IceServer>()
    private var remoteCandidateCache = arrayListOf<IceCandidate>()
    private var peerConnection: PeerConnection? = null
    private var audioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null
    private var rtpTransceiver: RtpTransceiver? = null
    private val rtpReceivers = arrayMapOf<String, RtpReceiver>()
    private val receiverIdUserIdMap = arrayMapOf<String, String>()
    private val receiverIdUserIdNoKeyMap = arrayMapOf<String, String>()

    suspend fun observeStats(callPipShown: () -> Boolean) {
        while (peerConnection != null && peerConnection?.connectionState() == PeerConnection.PeerConnectionState.CONNECTED) {
            if (!callPipShown.invoke()) {
                requireNotNull(peerConnection).getStats { report ->
                    val map = report.statsMap
                    map.entries.forEach { (k, v) ->
                        if (k.startsWith("RTCMediaStreamTrack_receive")) {
                            val trackIdentifier = v.members["trackIdentifier"]
                            val audioLevel = v.members["audioLevel"] as? Double?
                            var userId = receiverIdUserIdMap[trackIdentifier]
                            // Timber.d("$TAG_CALL userId: $userId, trackIdentifier: $trackIdentifier, audioLevel: $audioLevel")
                            if (userId != null) {
                                RxBus.publish(VoiceEvent(userId, audioLevel ?: 0.0))
                            } else {
                                userId = receiverIdUserIdNoKeyMap[trackIdentifier]
                                if (userId != null) {
                                    RxBus.publish(FrameKeyEvent(userId, false))
                                }
                            }
                        } else if (k.startsWith("RTCAudioSource")) {
                            if (audioTrack?.enabled() == true) {
                                val audioLevel = v.members["audioLevel"] as? Double?
                                RxBus.publish(
                                    VoiceEvent(
                                        requireNotNull(Session.getAccountId()),
                                        audioLevel ?: 0.0
                                    )
                                )
                            } else {
                                RxBus.publish(
                                    VoiceEvent(
                                        requireNotNull(Session.getAccountId()),
                                        0.0
                                    )
                                )
                            }
                        }
                    }
                }
            }
            delay(500)
        }
    }

    fun createPeerConnectionFactory(options: PeerConnectionFactory.Options) {
        if (factory != null) {
            reportError("PeerConnectionFactory has already been constructed")
            return
        }
        createPeerConnectionFactoryInternal(options)
    }

    fun createOffer(
        iceServerList: List<PeerConnection.IceServer>? = null,
        setLocalSuccess: ((sdp: SessionDescription) -> Unit),
        frameKey: ByteArray? = null,
        doWhenSetFailure: (() -> Unit)? = null
    ) {
        if (iceServerList != null) {
            iceServers.clear()
            iceServers.addAll(iceServerList)
        } else if (peerConnection != null && peerConnection?.connectionState() != PeerConnection.PeerConnectionState.CLOSED) {
            peerConnection?.restartIce()
        }
        val connectionState = peerConnection?.connectionState()
        if (peerConnection == null || connectionState == PeerConnection.PeerConnectionState.CLOSED) {
            peerConnection = createPeerConnectionInternal(frameKey)
        }
        val offerSdpObserver = object : SdpObserverWrapper() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(
                    object : SdpObserverWrapper() {
                        override fun onSetFailure(error: String?) {
                            if (doWhenSetFailure != null) {
                                Timber.d("$TAG_CALL createOffer setLocalSdp onSetFailure error: $error")
                                doWhenSetFailure.invoke()
                            } else {
                                reportError("createOffer setLocalSdp onSetFailure error: $error")
                            }
                        }
                        override fun onSetSuccess() {
                            Timber.d("$TAG_CALL createOffer setLocalSdp onSetSuccess")
                            setLocalSuccess(sdp)
                        }
                        override fun onCreateSuccess(sdp: SessionDescription) {
                            Timber.d("$TAG_CALL createOffer setLocalSdp onCreateSuccess")
                        }
                        override fun onCreateFailure(error: String?) {
                            if (doWhenSetFailure != null) {
                                Timber.d("$TAG_CALL createOffer setLocalSdp onCreateFailure error: $error")
                                doWhenSetFailure.invoke()
                            } else {
                                reportError("createOffer setLocalSdp onCreateFailure error: $error")
                            }
                        }
                    },
                    sdp
                )
            }

            override fun onCreateFailure(error: String?) {
                if (doWhenSetFailure != null) {
                    Timber.d("$TAG_CALL createOffer onCreateFailure error: $error")
                    doWhenSetFailure.invoke()
                } else {
                    reportError("createOffer onCreateFailure error: $error")
                }
            }

            override fun onSetFailure(error: String?) {
                if (doWhenSetFailure != null) {
                    Timber.d("$TAG_CALL createOffer onSetFailure error: $error")
                    doWhenSetFailure.invoke()
                } else {
                    reportError("createOffer onSetFailure error: $error")
                }
            }

            override fun onSetSuccess() {
                Timber.d("$TAG_CALL createOffer onSetSuccess")
            }
        }
        peerConnection?.createOffer(offerSdpObserver, MediaConstraints())
    }

    fun createAnswer(
        iceServerList: List<PeerConnection.IceServer>? = null,
        remoteSdp: SessionDescription,
        setLocalSuccess: (sdp: SessionDescription) -> Unit,
        doWhenSetFailure: (() -> Unit)? = null
    ) {
        if (iceServerList != null) {
            iceServers.clear()
            iceServers.addAll(iceServerList)
        }
        if (peerConnection == null || peerConnection?.connectionState() == PeerConnection.PeerConnectionState.CLOSED) {
            peerConnection = createPeerConnectionInternal()
        }
        peerConnection?.setRemoteDescription(
            object : SdpObserverWrapper() {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    Timber.d("$TAG_CALL createAnswer setRemoteSdp onCreateSuccess")
                }

                override fun onCreateFailure(error: String?) {
                    if (doWhenSetFailure != null) {
                        Timber.d("$TAG_CALL createAnswer setRemoteSdp onCreateFailure error: $error")
                        doWhenSetFailure.invoke()
                    } else {
                        reportError("createAnswer setRemoteSdp onCreateFailure error: $error")
                    }
                }

                override fun onSetFailure(error: String?) {
                    if (doWhenSetFailure != null) {
                        Timber.d("$TAG_CALL createAnswer setRemoteSdp onSetFailure error: $error")
                        doWhenSetFailure.invoke()
                    } else {
                        reportError("createAnswer setRemoteSdp onSetFailure error: $error")
                    }
                }

                override fun onSetSuccess() {
                    Timber.d("$TAG_CALL createAnswer setRemoteSdp onSetSuccess remoteCandidateCache: ${remoteCandidateCache.size}")
                    remoteCandidateCache.forEach {
                        peerConnection?.addIceCandidate(it, iceObserver)
                    }
                    remoteCandidateCache.clear()
                }
            },
            remoteSdp
        )
        val answerSdpObserver = object : SdpObserverWrapper() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(
                    object : SdpObserverWrapper() {
                        override fun onSetFailure(error: String?) {
                            if (doWhenSetFailure != null) {
                                Timber.d("$TAG_CALL createAnswer setLocalSdp onSetSuccess")
                                doWhenSetFailure.invoke()
                            } else {
                                reportError("createAnswer setLocalSdp onSetFailure error: $error")
                            }
                        }
                        override fun onSetSuccess() {
                            Timber.d("$TAG_CALL createAnswer setLocalSdp onSetSuccess")
                            setLocalSuccess(sdp)
                        }
                        override fun onCreateSuccess(sdp: SessionDescription) {
                            Timber.d("$TAG_CALL createAnswer setLocalSdp onCreateSuccess")
                        }
                        override fun onCreateFailure(error: String?) {
                            if (doWhenSetFailure != null) {
                                Timber.d("$TAG_CALL createAnswer setLocalSdp onCreateFailure error: $error")
                                doWhenSetFailure.invoke()
                            } else {
                                reportError("createAnswer setLocalSdp onCreateFailure error: $error")
                            }
                        }
                    },
                    sdp
                )
            }

            override fun onCreateFailure(error: String?) {
                if (doWhenSetFailure != null) {
                    Timber.d("$TAG_CALL createAnswer setLocalSdp onCreateFailure error: $error")
                    doWhenSetFailure.invoke()
                } else {
                    reportError("createAnswer setLocalSdp onCreateFailure error: $error")
                }
            }

            override fun onSetFailure(error: String?) {
                if (doWhenSetFailure != null) {
                    Timber.d("$TAG_CALL createAnswer setLocalSdp onSetFailure error: $error")
                    doWhenSetFailure.invoke()
                } else {
                    reportError("createAnswer setLocalSdp onSetFailure error: $error")
                }
            }

            override fun onSetSuccess() {
                Timber.d("$TAG_CALL createAnswer setLocalSdp onSetSuccess")
            }
        }
        peerConnection?.createAnswer(answerSdpObserver, MediaConstraints())
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        Timber.d("$TAG_CALL addRemoteIceCandidate peerConnection: $peerConnection")
        if (peerConnection != null && peerConnection!!.remoteDescription != null) {
            peerConnection?.addIceCandidate(candidate, iceObserver)
        } else {
            remoteCandidateCache.add(candidate)
        }
    }

    fun setAnswerSdp(
        sdp: SessionDescription,
        doWhenSetFailure: (() -> Unit)? = null
    ) {
        peerConnection?.setRemoteDescription(
            object : SdpObserverWrapper() {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    Timber.d("$TAG_CALL setAnswerSdp setRemoteSdp onCreateSuccess")
                }

                override fun onCreateFailure(error: String?) {
                    if (doWhenSetFailure != null) {
                        Timber.d("$TAG_CALL setAnswerSdp setRemoteSdp onCreateFailure error: $error")
                        doWhenSetFailure.invoke()
                    } else {
                        reportError("setAnswerSdp setRemoteSdp onCreateFailure error: $error")
                    }
                }

                override fun onSetFailure(error: String?) {
                    if (doWhenSetFailure != null) {
                        Timber.d("$TAG_CALL setAnswerSdp setRemoteSdp onSetFailure error: $error")
                        doWhenSetFailure.invoke()
                    } else {
                        reportError("setAnswerSdp setRemoteSdp onSetFailure error: $error")
                    }
                }

                override fun onSetSuccess() {
                    Timber.d("$TAG_CALL setAnswerSdp setRemoteSdp onSetSuccess remoteCandidateCache: ${remoteCandidateCache.size}")
                    remoteCandidateCache.forEach {
                        peerConnection?.addIceCandidate(it, iceObserver)
                    }
                    remoteCandidateCache.clear()
                }
            },
            sdp
        )
    }

    fun setAudioEnable(enable: Boolean) {
        if (peerConnection == null || rtpTransceiver == null || isError) return

        rtpTransceiver?.sender?.track()?.setEnabled(enable)
    }

    fun enableCommunication() {
        if (peerConnection == null || isError) return

        peerConnection?.setAudioPlayout(true)
        peerConnection?.setAudioRecording(true)
    }

    fun hasLocalSdp(): Boolean {
        if (peerConnection == null) return false

        return peerConnection!!.localDescription != null
    }

    fun dispose() {
        peerConnection?.dispose()
        peerConnection = null
        audioSource?.dispose()
        audioSource = null
        isError = false
        rtpTransceiver = null
        rtpReceivers.clear()
        events.onPeerConnectionClosed()
    }

    fun release() {
        dispose()
        factory?.dispose()
        factory = null
    }

    fun getPeerConnection() = peerConnection

    fun getPCMessage(error: String? = null): String {
        val msgBuilder = StringBuilder().append("$TAG_CALL WebRTC peer connection").appendLine()
        if (!error.isNullOrBlank()) {
            msgBuilder.append("error: $error").appendLine()
        }
        peerConnection?.let { pc ->
            val localSdp = "{ localDescription: { description: ${pc.localDescription?.description}, type: ${pc.localDescription?.type} }"
            val remoteSdp = "{ remoteDescription: { description: ${pc.remoteDescription?.description}, type: ${pc.remoteDescription?.type} }"
            msgBuilder.append(localSdp).appendLine().append(remoteSdp).appendLine()

            pc.getStats { report ->
                msgBuilder.append("{ stats: $report }").appendLine()
            }
        }
        return msgBuilder.toString()
    }

    private fun reportError(error: String) {
        val msg = getPCMessage(error)
        if (!isError) {
            events.onPeerConnectionError(msg)
            isError = true
        }
    }

    private fun createPeerConnectionInternal(frameKey: ByteArray? = null): PeerConnection? {
        if (factory == null || isError) {
            reportError("PeerConnectionFactory is not created")
            return null
        }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            iceTransportsType = PeerConnection.IceTransportsType.RELAY
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        }
        val peerConnection = factory!!.createPeerConnection(rtcConfig, pcObserver)
        if (peerConnection == null) {
            reportError("PeerConnection is not created")
            return null
        }
        if (BuildConfig.DEBUG) {
            Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO)
        }
        peerConnection.setAudioPlayout(false)
        peerConnection.setAudioRecording(false)

        rtpTransceiver = peerConnection.addTransceiver(
            createAudioTrack(),
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
        )
        setSenderFrameKey(frameKey)
        return peerConnection
    }

    fun setSenderFrameKey(frameKey: ByteArray? = null) {
        if (frameKey != null && rtpTransceiver != null) {
            rtpTransceiver?.sender?.setFrameEncryptor(RTCFrameEncryptor(frameKey))
        }
    }

    fun setReceiverFrameKey(userId: String, sessionId: String, frameKey: ByteArray? = null) {
        val key = "$userId~$sessionId"
        if (rtpReceivers.containsKey(key) && frameKey != null) {
            val receiver = rtpReceivers[key] ?: return
            val receiverId = receiver.id()
            Timber.d("$TAG_CALL setReceiverFrameKey receiver id: $receiverId")
            receiver.setFrameDecryptor(RTCFrameDecryptor(frameKey))
            receiver.track()?.setEnabled(true)
            receiverIdUserIdMap[receiverId] = userId
            receiverIdUserIdNoKeyMap.remove(receiverId)
            RxBus.publish(FrameKeyEvent(userId, true))
        }
    }

    private fun createPeerConnectionFactoryInternal(options: PeerConnectionFactory.Options) {
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    private fun createAudioTrack(): AudioTrack {
        audioSource = factory!!.createAudioSource(MediaConstraints())
        audioTrack = factory!!.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        audioTrack!!.setEnabled(true)
        return audioTrack!!
    }

    private val iceObserver = object : AddIceObserver {
        override fun onAddSuccess() {
            Timber.d("$TAG_CALL iceObserver onAddSuccess")
        }

        override fun onAddFailure(error: String?) {
            Timber.d("$TAG_CALL iceObserver onAddFailure error: $error")
        }
    }

    private inner class PCObserver : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate) {
            events.onIceCandidate(candidate)
        }

        override fun onDataChannel(dataChannel: DataChannel?) {
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            Timber.d("$TAG_CALL onIceConnectionChange: $newState")
            if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
                events.onIceDisconnected()
            } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                events.onIceFailed()
            }
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            Timber.d("$TAG_CALL onConnectionChange: $newState")
            if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                events.onConnected()
            } else if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                events.onDisconnected()
            } else if (newState == PeerConnection.PeerConnectionState.CLOSED) {
                events.onClosed()
            }
        }

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
            Timber.d("$TAG_CALL onIceGatheringChange: $newState")
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState) {
            Timber.d("$TAG_CALL SignalingState: $newState")
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            Timber.d("$TAG_CALL onIceCandidatesRemoved")
            events.onIceCandidatesRemoved(candidates)
        }

        override fun onAddStream(stream: MediaStream) {
        }

        override fun onRemoveStream(stream: MediaStream) {
        }

        override fun onRenegotiationNeeded() {
            Timber.d("$TAG_CALL onRenegotiationNeeded")
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
            Timber.d("$TAG_CALL onTrack=%s", transceiver.toString())
        }

        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
            var hasAllMediaStreamKey = true
            for (m in mediaStreams) {
                Timber.d("$TAG_CALL mediaStream: $m")
                val userSession = m.id.split("~")
                if (userSession.size != 2) {
                    continue
                }
                if (userSession[0] == Session.getAccountId()) {
                    continue
                }
                val frameKey = events.getSenderPublicKey(userSession[0], userSession[1])
                Timber.d("$TAG_CALL getSenderPublicKey userId: ${userSession[0]}, sessionId: ${userSession[1]}, frameKey: $frameKey")
                rtpReceivers[m.id] = receiver
                if (frameKey != null) {
                    receiverIdUserIdMap[receiver.id()] = userSession[0]
                    receiver.setFrameDecryptor(RTCFrameDecryptor(frameKey))
                } else {
                    receiverIdUserIdNoKeyMap[receiver.id()] = userSession[0]
                    hasAllMediaStreamKey = false
                }
            }
            Timber.d("$TAG_CALL onAddTrack id: ${receiver.id()}, hasAllMediaStreamKey: $hasAllMediaStreamKey")
            receiver.track()?.setEnabled(hasAllMediaStreamKey)
        }
    }

    private open class SdpObserverWrapper : SdpObserver {
        override fun onSetFailure(error: String?) {
        }

        override fun onSetSuccess() {
        }

        override fun onCreateSuccess(sdp: SessionDescription) {
        }

        override fun onCreateFailure(error: String?) {
        }
    }

    /**
     * Peer connection events.
     */
    interface PeerConnectionEvents {

        /**
         * Callback fired once local Ice candidate is generated.
         */
        fun onIceCandidate(candidate: IceCandidate)

        /**
         * Callback fired once local ICE candidates are removed.
         */
        fun onIceCandidatesRemoved(candidates: Array<IceCandidate>)

        /**
         * Callback fired once connection is established (IceConnectionState is
         * CONNECTED).
         */
        fun onIceConnected()

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * DISCONNECTED).
         */
        fun onIceDisconnected()

        fun onIceFailed()

        /**
         * Callback fired once DTLS connection is established (PeerConnectionState
         * is CONNECTED).
         */
        fun onConnected()

        /**
         * Callback fired once DTLS connection is disconnected (PeerConnectionState
         * is DISCONNECTED).
         */
        fun onDisconnected()

        fun onClosed()

        /**
         * Callback fired once peer connection is closed.
         */
        fun onPeerConnectionClosed()

        /**
         * Callback fired once peer connection statistics is ready.
         */
        fun onPeerConnectionStatsReady(reports: Array<StatsReport>)

        /**
         * Callback fired once peer connection error happened.
         */
        fun onPeerConnectionError(errorMsg: String)

        fun getSenderPublicKey(userId: String, sessionId: String): ByteArray?
    }

    companion object {
        const val TAG = "PeerConnectionClient"

        private const val AUDIO_TRACK_ID = "ARDAMSa0"
    }
}
