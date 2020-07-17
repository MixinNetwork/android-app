package one.mixin.android.webrtc

import android.content.Context
import androidx.collection.arrayMapOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import one.mixin.android.util.Session
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCFrameDecryptor
import org.webrtc.RTCFrameEncryptor
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
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
    private var rtpSender: RtpSender? = null
    private val rtpReceivers = arrayMapOf<String, RtpReceiver>()
    private val sdpConstraint = MediaConstraints()
    private val restartConstraint = MediaConstraints.KeyValuePair("IceRestart", "true")

    fun createPeerConnectionFactory(options: PeerConnectionFactory.Options) {
        if (factory != null) {
            reportError("PeerConnectionFactory has already been constructed")
            return
        }
        createPeerConnectionFactoryInternal(options)
    }

    fun createOffer(iceServerList: List<PeerConnection.IceServer>? = null, setLocalSuccess: ((sdp: SessionDescription) -> Unit), frameKey: ByteArray? = null) {
        if (iceServerList != null) {
            iceServers.addAll(iceServerList)
        }
        peerConnection = createPeerConnectionInternal(frameKey)
        val offerSdpObserver = object : SdpObserverWrapper() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(
                    object : SdpObserverWrapper() {
                        override fun onSetFailure(error: String?) {
                            reportError("createOffer setLocalSdp onSetFailure error: $error")
                        }
                        override fun onSetSuccess() {
                            Timber.d("$TAG_CALL createOffer setLocalSdp onSetSuccess")
                            setLocalSuccess(sdp)
                        }
                    },
                    sdp
                )
            }

            override fun onCreateFailure(error: String?) {
                reportError("createOffer onCreateFailure error: $error")
            }
        }
        if (iceServerList == null) {
            sdpConstraint.mandatory.add(restartConstraint)
        } else {
            sdpConstraint.mandatory.remove(restartConstraint)
        }
        peerConnection?.createOffer(offerSdpObserver, sdpConstraint)
    }

    fun createAnswer(remoteSdp: SessionDescription, setLocalSuccess: (sdp: SessionDescription) -> Unit) {
        peerConnection = createPeerConnectionInternal()
        peerConnection?.setRemoteDescription(remoteSdpObserver, remoteSdp)
        val answerSdpObserver = object : SdpObserverWrapper() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(
                    object : SdpObserverWrapper() {
                        override fun onSetFailure(error: String?) {
                            reportError("createAnswer setLocalSdp onSetFailure error: $error")
                        }
                        override fun onSetSuccess() {
                            Timber.d("$TAG_CALL createAnswer setLocalSdp onSetSuccess")
                            setLocalSuccess(sdp)
                        }
                    },
                    sdp
                )
            }

            override fun onCreateFailure(error: String?) {
                reportError("createAnswer onCreateFailure error: $error")
            }
        }
        sdpConstraint.mandatory.remove(restartConstraint)
        peerConnection?.createAnswer(answerSdpObserver, sdpConstraint)
    }

    fun createAnswerWithIceServer(
        iceServerList: List<PeerConnection.IceServer>,
        remoteSdp: SessionDescription,
        setLocalSuccess: (sdp: SessionDescription) -> Unit
    ) {
        iceServers.addAll(iceServerList)
        createAnswer(remoteSdp, setLocalSuccess)
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        Timber.d("$TAG_CALL addRemoteIceCandidate peerConnection: $peerConnection")
        if (peerConnection != null && peerConnection!!.remoteDescription != null) {
            peerConnection?.addIceCandidate(candidate)
        } else {
            remoteCandidateCache.add(candidate)
        }
    }

    fun setAnswerSdp(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(remoteSdpObserver, sdp)
    }

    fun setAudioEnable(enable: Boolean) {
        if (peerConnection == null || audioTrack == null || isError) return

        audioTrack?.setEnabled(enable)
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

    fun close() {
        peerConnection?.dispose()
        peerConnection = null
        audioSource?.dispose()
        audioSource = null
        isError = false
        rtpSender = null
        rtpReceivers.clear()
        events.onPeerConnectionClosed()
    }

    fun release() {
        close()
        factory?.dispose()
        factory = null
    }

    private fun reportError(error: String) {
        Timber.d("$TAG_CALL reportError: $error")
        peerConnection?.let { pc ->
            val localSdp = "{ localDescription: { description: ${pc.localDescription?.description}, type: ${pc.localDescription?.type} }"
            val remoteSdp = "{ remoteDescription: { description: ${pc.remoteDescription?.description}, type: ${pc.remoteDescription?.type} }"
            pc.getStats { report ->
                FirebaseCrashlytics.getInstance().log(
                    "WebRTC peer connection error " +
                        """
                        { stats: $report },
                        $localSdp,
                        $remoteSdp
                    """
                )
            }
        }
        if (!isError) {
            events.onPeerConnectionError(error)
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
            enableDtlsSrtp = true
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        }
        val peerConnection = factory!!.createPeerConnection(rtcConfig, pcObserver)
        if (peerConnection == null) {
            reportError("PeerConnection is not created")
            return null
        }
        // Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO)
        peerConnection.setAudioPlayout(false)
        peerConnection.setAudioRecording(false)

        rtpSender = peerConnection.addTrack(createAudioTrack())
        setSenderFrameKey(frameKey)
        return peerConnection
    }

    fun setSenderFrameKey(frameKey: ByteArray? = null) {
        if (frameKey != null && rtpSender != null) {
            rtpSender!!.setFrameEncryptor(RTCFrameEncryptor(frameKey))
        }
    }

    fun setReceiverFrameKey(userId: String, sessionId: String, frameKey: ByteArray? = null) {
        val key = "$userId~$sessionId"
        if (rtpReceivers.containsKey(key) && frameKey != null) {
            val receiver = rtpReceivers[key]
            Timber.d("$TAG_CALL setFrameDecryptor")
            receiver?.setFrameDecryptor(RTCFrameDecryptor(frameKey))
        }
    }

    private fun createPeerConnectionFactoryInternal(options: PeerConnectionFactory.Options) {
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        sdpConstraint.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        sdpConstraint.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
    }

    private fun createAudioTrack(): AudioTrack {
        audioSource = factory!!.createAudioSource(MediaConstraints())
        audioTrack = factory!!.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        audioTrack!!.setEnabled(true)
        return audioTrack!!
    }

    private val remoteSdpObserver = object : SdpObserverWrapper() {
        override fun onSetFailure(error: String?) {
            reportError("setRemoteSdp onSetFailure error: $error")
        }

        override fun onSetSuccess() {
            Timber.d("$TAG_CALL setRemoteSdp onSetSuccess remoteCandidateCache: ${remoteCandidateCache.size}")
            remoteCandidateCache.forEach {
                peerConnection?.addIceCandidate(it)
            }
            remoteCandidateCache.clear()
        }
    }

    private inner class PCObserver() : PeerConnection.Observer {

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
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
            Timber.d("$TAG_CALL onTrack=%s", transceiver.toString())
        }

        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
            for (m in mediaStreams) {
                val userSession = m.id.split("~")
                if (userSession.size != 2) {
                    continue
                }
                if (userSession[0] == Session.getAccountId()) {
                    continue
                }
                val frameKey = events.getSenderPublicKey(userSession[0], userSession[1])
                if (frameKey != null) {
                    rtpReceivers[m.id] = receiver
                    receiver.setFrameDecryptor(RTCFrameDecryptor(frameKey))
                }
            }
            Timber.d("$TAG_CALL onAddTrack=%s", receiver.toString())
            receiver.track()?.setEnabled(true)
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
        fun onPeerConnectionError(description: String)

        fun getSenderPublicKey(userId: String, sessionId: String): ByteArray?
    }

    companion object {
        const val TAG = "PeerConnectionClient"

        private const val AUDIO_TRACK_ID = "ARDAMSa0"
    }
}
