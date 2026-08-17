package com.money.mimi.helpers.call;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import com.money.mimi.R;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PermissionHandler;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.models.calls.CallPusher;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.socket.client.Ack;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Abderrahim El imame on 10/21/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class WebRtcClient {
    private EglBase rootEglBase;

    public EglBase getRootEglBase() {
        if (rootEglBase == null) {
            rootEglBase = EglBase.create();
        }
        return rootEglBase;
    }
    private final static String TAG = WebRtcClient.class.getCanonicalName();
    private final static int MAX_PEER_CONNECTIONS = 2;
    private boolean[] endPoints = new boolean[MAX_PEER_CONNECTIONS];
    private PeerConnectionFactory peerConnectionFactory;
    private HashMap<String, Peer> peers = new HashMap<>();
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private PeerConnectionParameters peerConnectionParameters;
    private MediaConstraints mediaConstraints = new MediaConstraints();
    private MediaStream mediaStream;
    private int numberOfCameras;
    private boolean cameraIsOpened = false;
    private VideoCapturer videoCapturer;
    private CameraEnumerator cameraEnumerator;
    private String frontCameraId;
    private String backCameraId;
    private boolean currentCameraFront = true;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private Socket mSocket;
    private static final String PEER_CONNECTION_ID = "ARDAMS";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private String mFrontCameraName;
    private String mBackCameraName;
    private Activity mActivity;
    private boolean isAccepted;
    private static final int ICE_FAILURE_TIMEOUT_MS = 45000;
    private final HashMap<String, Runnable> iceTimeoutRunnables = new HashMap<>();
    private final android.os.Handler iceTimeoutHandler = new android.os.Handler();
    private final CountDownLatch iceServersReady = new CountDownLatch(1);

    private SignalingServerHandler signalingServerHandler;


    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_ECHO_CANCELLATION2_CONSTRAINT = "googEchoCancellation2";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_AUTO_GAIN_CONTROL2_CONSTRAINT = "googAutoGainControl2";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static final String AUDIO_NOISE_SUPPRESSION2_CONSTRAINT = "googNoisesuppression2";
    private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
    private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
    private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
    private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
    private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
    private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";


    private interface Command {
        void execute(String peerId, JSONObject payload) throws JSONException;
    }

    private class CreateOfferCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            AppHelper.LogCat("WebRTC CMD CreateOffer | peer=" + peerId);
            Peer peer = peers.get(peerId);
            peer.peerConnection.createOffer(peer, mediaConstraints);
        }
    }


    private class CreateAnswerCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            AppHelper.LogCat("WebRTC CMD CreateAnswer | peer=" + peerId);
            Peer peer = peers.get(peerId);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.peerConnection.setRemoteDescription(peer, sdp);
            peer.peerConnection.createAnswer(peer, mediaConstraints);
        }
    }

    private class SetRemoteSDPCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            AppHelper.LogCat("WebRTC CMD SetRemoteSDP | peer=" + peerId);
            Peer peer = peers.get(peerId);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.peerConnection.setRemoteDescription(peer, sdp);
        }
    }

    private class AddIceCandidateCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            AppHelper.LogCat("WebRTC CMD AddIceCandidate | peer=" + peerId
                    + " | sdpMid=" + payload.getString("sdpMid")
                    + " | sdpMLineIndex=" + payload.getInt("sdpMLineIndex"));
            PeerConnection pc = peers.get(peerId).peerConnection;
            if (pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("sdpMid"),
                        payload.getInt("sdpMLineIndex"),
                        payload.getString("candidate")
                );
                pc.addIceCandidate(candidate);
            } else {
                AppHelper.LogCat("WebRTC CMD AddIceCandidate | peer=" + peerId + " remote description is null, discarding candidate");
            }
        }
    }

    /**
     * Send a signal through the signaling server
     *
     * @param to      id this the first parameters of signalingServer method
     * @param type    type  his the second parameters of signalingServer method
     * @param payload payload his the thirded parameters of signalingServer method
     * @throws JSONException
     */
    public void signalingServer(String to, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        String fromId = PreferenceManager.getSocketID(WhatsCloneApplication.getInstance());
        message.put("from", fromId);
        message.put("type", type);
        message.put("payload", payload);
        AppHelper.LogCat("WebRTC OUT signaling_server | to=" + to + " | from=" + fromId
                + " | type=" + type
                + (type.equals("candidate") ? " | candidate present" : ""));
        mSocket.emit(AppConstants.SOCKET_SIGNALING_SERVER, message);

    }

    private class SignalingServerHandler {
        private HashMap<String, Command> commandMap;

        private SignalingServerHandler() {
            this.commandMap = new HashMap<>();
            commandMap.put("init", new CreateOfferCommand());
            commandMap.put("offer", new CreateAnswerCommand());
            commandMap.put("answer", new SetRemoteSDPCommand());
            commandMap.put("candidate", new AddIceCandidateCommand());
        }

        private Emitter.Listener onSignalingServerResponse = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = firstJson(args);
                if (data == null) return;
                try {

                    String from = data.getString("from");
                    String type = data.getString("type");

                    AppHelper.LogCat("WebRTC IN  signaling_server | from=" + from + " | type=" + type);

                    // if unknown command - just skip it
                    if (!commandMap.containsKey(type)) {
                        AppHelper.LogCat("WebRTC IN  signaling_server | unknown type=" + type + ", skipping");
                        return;
                    }
                    JSONObject payload = new JSONObject();
                    if (!type.equals("init")) {
                        payload = data.getJSONObject("payload");
                    }

                    // if peer is unknown, try to add him
                    if (!peers.containsKey(from)) {
                        // if MAX_PEER is reach, ignore the call
                        int endPoint = findEndPoint();
                        if (endPoint != MAX_PEER_CONNECTIONS) {
                            addPeer(from, endPoint);
                            AppHelper.LogCat("WebRTC IN  signaling_server | adding new peer from=" + from + " endpoint=" + endPoint);
                            commandMap.get(type).execute(from, payload);
                        } else {
                            AppHelper.LogCat("WebRTC IN  signaling_server | MAX_PEER reached, ignoring from=" + from);
                        }
                    } else {
                        commandMap.get(type).execute(from, payload);
                    }
                } catch (JSONException e) {
                    AppHelper.LogCat("WebRTC IN  signaling_server | JSONException " + e.getMessage());

                }
            }
        };

        private Emitter.Listener onRejectResponse = args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {
                String from = data.getString("userSocketId");
                if (from.equals(PreferenceManager.getSocketID(WhatsCloneApplication.getInstance())))
                    return;
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_REJECT_CALL));
            } catch (JSONException e) {
                AppHelper.LogCat(" onRejectResponse JSONException " + e.getMessage());

            }
        };

        private Emitter.Listener onHangUpCallResponse = args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {
                String from = data.getString("userSocketId");
                if (from.equals(PreferenceManager.getSocketID(WhatsCloneApplication.getInstance())))
                    return;
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_HANG_UP));
            } catch (JSONException e) {
                AppHelper.LogCat(" onHangUpCallResponse JSONException " + e.getMessage());

            }
        };

        private Emitter.Listener onAcceptResponse = args -> {
            JSONObject data = firstJson(args);
            if (data == null) return;
            try {
                String from = data.getString("userSocketId");
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_ACCEPT_CALL, from));
            } catch (JSONException e) {
                AppHelper.LogCat(" onAcceptResponse JSONException " + e.getMessage());

            }

        };


    }

    private class Peer implements SdpObserver, PeerConnection.Observer {
        private PeerConnection peerConnection;
        private String id;
        private int endPoint;


        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());
                payload.put("sdp", sdp.description);
                AppHelper.LogCat("WebRTC Peer onCreateSuccess | peer=" + id
                        + " | type=" + sdp.type.canonicalForm()
                        + " | sdpLen=" + (sdp.description != null ? sdp.description.length() : 0));
                signalingServer(id, sdp.type.canonicalForm(), payload);
                peerConnection.setLocalDescription(Peer.this, sdp);
            } catch (JSONException e) {
                AppHelper.LogCat("WebRTC Peer onCreateSuccess JSONException " + e.getMessage());

            }
        }

        @Override
        public void onSetSuccess() {
            AppHelper.LogCat("onSetSuccess ");
        }

        @Override
        public void onCreateFailure(String s) {
            AppHelper.LogCat("onCreateFailure " + s);
        }

        @Override
        public void onSetFailure(String s) {
            AppHelper.LogCat("onSetFailure " + s);

        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            AppHelper.LogCat("onSignalingChange " + signalingState);
            if (signalingState == PeerConnection.SignalingState.CLOSED) {
                removePeer(id);
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_ON_PEER_CLOSED));
            }
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            AppHelper.LogCat("onIceConnectionChange " + iceConnectionState);

            if (iceConnectionState == PeerConnection.IceConnectionState.NEW || iceConnectionState == PeerConnection.IceConnectionState.CHECKING) {
                Runnable timeoutRunnable = () -> {
                    AppHelper.LogCat("ICE connection timeout, hanging up");
                    EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_STATUS_CHANGED, AppConstants.USER_CLOSED, null));
                };
                iceTimeoutRunnables.put(id, timeoutRunnable);
                iceTimeoutHandler.postDelayed(timeoutRunnable, ICE_FAILURE_TIMEOUT_MS);
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_STATUS_CHANGED, AppConstants.USER_CONNECTING, null));
            } else if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                Runnable timeout = iceTimeoutRunnables.remove(id);
                if (timeout != null) iceTimeoutHandler.removeCallbacks(timeout);
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_STATUS_CHANGED, AppConstants.USER_CONNECTED, null));
            } else if (iceConnectionState == PeerConnection.IceConnectionState.COMPLETED) {
                Runnable timeout = iceTimeoutRunnables.remove(id);
                if (timeout != null) iceTimeoutHandler.removeCallbacks(timeout);
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_STATUS_CHANGED, AppConstants.USER_COMPLETED, null));
            } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                Runnable timeout = iceTimeoutRunnables.remove(id);
                if (timeout != null) iceTimeoutHandler.removeCallbacks(timeout);
                removePeer(id);
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_STATUS_CHANGED, AppConstants.USER_DISCONNECT, null));
            } else if (iceConnectionState == PeerConnection.IceConnectionState.FAILED || iceConnectionState == PeerConnection.IceConnectionState.CLOSED) {
                Runnable timeout = iceTimeoutRunnables.remove(id);
                if (timeout != null) iceTimeoutHandler.removeCallbacks(timeout);
                EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_STATUS_CHANGED, AppConstants.USER_CLOSED, null));
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            AppHelper.LogCat("onIceGatheringChange " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("sdpMLineIndex", candidate.sdpMLineIndex);
                payload.put("sdpMid", candidate.sdpMid);
                payload.put("candidate", candidate.sdp);
                signalingServer(id, "candidate", payload);
            } catch (JSONException e) {
                AppHelper.LogCat(" onIceCandidate JSONException " + e.getMessage());
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            AppHelper.LogCat("onAddStream " + mediaStream.getId());
            EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_ADD_REMOTE_STREAM, endPoint + 1, mediaStream));
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoveStream " + mediaStream.getId());
            removePeer(id);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
        }

        @Override
        public void onRenegotiationNeeded() {
            AppHelper.LogCat("onRenegotiationNeeded");

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        }

        Peer(String id, int endPoint) {
            AppHelper.LogCat("Peer new Peer: " + id + " " + endPoint);
            // This call stack still uses the legacy MediaStream addStream/onAddStream API.
            // Recent WebRTC builds default to Unified Plan, where addStream triggers a
            // native fatal assertion. Keep both peers on Plan B until the signaling
            // protocol is migrated atomically to addTrack/onTrack.
            PeerConnection.RTCConfiguration rtcConfiguration =
                    new PeerConnection.RTCConfiguration(new LinkedList<>(iceServers));
            rtcConfiguration.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B;
            peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, this);
            if (peerConnection == null) {
                throw new IllegalStateException("Unable to create WebRTC peer connection");
            }
            this.id = id;
            this.endPoint = endPoint;
            peerConnection.addStream(mediaStream);
            EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_STATUS_CHANGED, AppConstants.USER_CONNECTING, null));
        }
    }

    private Peer addPeer(String id, int endPoint) {
        try {
            iceServersReady.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            AppHelper.LogCat("ICE servers fetch interrupted, proceeding with defaults");
        }
        Peer peer = new Peer(id, endPoint);
        peers.put(id, peer);
        endPoints[endPoint] = true;
        return peer;
    }

    private void removePeer(String id) {
        Peer peer = peers.get(id);
        if (peer == null) return;
        EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_REMOVE_REMOTE_STREAM, peer.endPoint));
        peer.peerConnection.close();
        peers.remove(peer.id);
        endPoints[peer.endPoint] = false;
    }

    public WebRtcClient(Activity mActivity, PeerConnectionParameters params, String currentUserConnectedID, String callerSocketId, boolean isVideoCall, boolean isAccepted) {
        this.mActivity = mActivity;
        this.isAccepted = isAccepted;
        peerConnectionParameters = params;

        AppHelper.LogCat("WebRtcClient INIT | isAccepted=" + isAccepted + " | isVideo=" + isVideoCall
                + " | mySocketId=" + currentUserConnectedID + " | peerSocketId=" + callerSocketId);

        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(mActivity)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        org.webrtc.VideoEncoderFactory videoEncoderFactory;
        try {
            videoEncoderFactory = new DefaultVideoEncoderFactory(getRootEglBase().getEglBaseContext(), true, true);
        } catch (Exception e) {
            AppHelper.LogCat("HW encoder factory failed, using SW: " + e.getMessage());
            videoEncoderFactory = new DefaultVideoEncoderFactory(getRootEglBase().getEglBaseContext(), false, true);
        }
        org.webrtc.VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(getRootEglBase().getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .createPeerConnectionFactory();
        signalingServerHandler = new SignalingServerHandler();

        WhatsCloneApplication app = (WhatsCloneApplication) mActivity.getApplication();
        mSocket = app.getSocket();
        if (mSocket == null || !mSocket.connected()) {
            AppHelper.startMainService(mActivity);
            AppHelper.LogCat("WebRtcClient foreground socket is not ready");
            return;
        }

        mSocket.on(AppConstants.SOCKET_REJECT_NEW_CALL, signalingServerHandler.onRejectResponse);
        mSocket.on(AppConstants.SOCKET_ACCEPT_NEW_CALL, signalingServerHandler.onAcceptResponse);
        mSocket.on(AppConstants.SOCKET_HANGUP_CALL, signalingServerHandler.onHangUpCallResponse);
        mSocket.on(AppConstants.SOCKET_SIGNALING_SERVER, signalingServerHandler.onSignalingServerResponse);
        if (this.isAccepted) {
            start(currentUserConnectedID, callerSocketId);
        } else {
            // Wait for socket to be fully connected before emitting reset_socket_id
            // to avoid race condition where emit fires before the connection is established.
            if (mSocket.connected()) {
                AppHelper.LogCat("WebRtcClient: socket already connected, emitting reset_socket_id immediately");
                emitResetSocketId(currentUserConnectedID, callerSocketId);
            } else {
                AppHelper.LogCat("WebRtcClient: socket not yet connected, waiting for EVENT_CONNECT before reset_socket_id");
                mSocket.once(Socket.EVENT_CONNECT, args -> {
                    AppHelper.LogCat("WebRtcClient: socket connected, now emitting reset_socket_id");
                    emitResetSocketId(currentUserConnectedID, callerSocketId);
                });
            }
        }


        //   defaultIceServers();

        fetchIceServers();

        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideoCall ? "true" : "false"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));


    }

    private void emitResetSocketId(String currentUserConnectedID, String callerSocketId) {
        try {
            JSONObject message = new JSONObject();
            message.put("userSocketId", currentUserConnectedID);
            AppHelper.LogCat("WebRtcClient: emitting reset_socket_id with userSocketId=" + currentUserConnectedID);
            mSocket.emit(AppConstants.SOCKET_RESET_SOCKET_ID, message, (Ack) args -> {
                JSONObject data = firstJson(args);
                if (data == null) return;
                try {
                    String id = data.optString("userSocketId", currentUserConnectedID);
                    AppHelper.LogCat("WebRtcClient: reset_socket_id ACK received, resolved socketId=" + id);
                    if (id == null || id.trim().isEmpty() || "null".equalsIgnoreCase(id)) {
                        id = PreferenceManager.getSocketID(WhatsCloneApplication.getInstance());
                    }
                    if (id == null || id.trim().isEmpty() || "null".equalsIgnoreCase(id)) {
                        id = currentUserConnectedID;
                    }
                    PreferenceManager.setSocketID(WhatsCloneApplication.getInstance(), id);
                    start(id, callerSocketId);
                    EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_CALL_READY, id));

                } catch (Exception e) {
                    AppHelper.LogCat("WebRtcClient: onGetUserSocketId Exception " + e.getMessage());
                }
            });
        } catch (JSONException e) {
            AppHelper.LogCat("WebRtcClient: JSONException " + e.getMessage());
        }
    }


    private void fetchIceServers() {
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.services.mozilla.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.stunprotocol.org:3478"));

        if (mSocket != null && mSocket.connected()) {
            fetchIceServersViaSocket();
        } else {
            fetchIceServersViaHttp();
        }
    }

    private void fetchIceServersViaSocket() {
        try {
            mSocket.emit("socket_get_ice_servers", new JSONObject(), (Ack) args -> {
                try {
                    if (args == null || args.length == 0) {
                        AppHelper.LogCat("socket_get_ice_servers: no response, using default STUN servers");
                        iceServersReady.countDown();
                        return;
                    }
                    JSONObject response = firstJson(args);
                    if (response == null) {
                        iceServersReady.countDown();
                        return;
                    }
                    if (response.optBoolean("success", false)) {
                        JSONArray servers = response.getJSONArray("iceServers");
                        LinkedList<PeerConnection.IceServer> fetchedServers = new LinkedList<>();
                        for (int i = 0; i < servers.length(); i++) {
                            JSONObject server = servers.getJSONObject(i);
                            JSONArray urls = server.optJSONArray("urls");
                            if (urls == null) {
                                String singleUrl = server.optString("urls", null);
                                if (singleUrl != null) {
                                    urls = new JSONArray();
                                    urls.put(singleUrl);
                                }
                            }
                            if (urls == null) continue;
                            for (int j = 0; j < urls.length(); j++) {
                                String url = urls.getString(j);
                                String username = server.optString("username", null);
                                String credential = server.optString("credential", null);
                                if (username != null && credential != null && !username.isEmpty() && !credential.isEmpty()) {
                                    fetchedServers.add(new PeerConnection.IceServer(url, username, credential));
                                } else {
                                    fetchedServers.add(new PeerConnection.IceServer(url));
                                }
                            }
                        }
                        if (!fetchedServers.isEmpty()) {
                            mActivity.runOnUiThread(() -> {
                                iceServers.clear();
                                iceServers.addAll(fetchedServers);
                                AppHelper.LogCat("ICE servers replaced from socket_get_ice_servers, total count: " + iceServers.size());
                            });
                        }
                    } else {
                        AppHelper.LogCat("socket_get_ice_servers failed: " + response.optString("message", "unknown"));
                    }
                } catch (Exception e) {
                    AppHelper.LogCat("socket_get_ice_servers parse error: " + e.getMessage());
                } finally {
                    iceServersReady.countDown();
                }
            });

            new Thread(() -> {
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException ignored) {
                }
                if (iceServersReady.getCount() > 0) {
                    AppHelper.LogCat("socket_get_ice_servers timed out, using default STUN servers");
                    iceServersReady.countDown();
                }
            }).start();
        } catch (Exception e) {
            AppHelper.LogCat("socket_get_ice_servers emit error: " + e.getMessage());
            iceServersReady.countDown();
        }
    }

    private void fetchIceServersViaHttp() {
        new Thread(() -> {
            try {
                String token = PreferenceManager.getToken(mActivity);
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(EndPoints.BACKEND_BASE_URL + EndPoints.GET_ICE_SERVERS)
                        .header("token", token)
                        .build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray servers = json.getJSONArray("iceServers");
                        LinkedList<PeerConnection.IceServer> fetchedServers = new LinkedList<>();
                        for (int i = 0; i < servers.length(); i++) {
                            JSONObject server = servers.getJSONObject(i);
                            JSONArray urls = server.optJSONArray("urls");
                            if (urls == null) {
                                String singleUrl = server.optString("urls", null);
                                if (singleUrl != null) {
                                    urls = new JSONArray();
                                    urls.put(singleUrl);
                                }
                            }
                            if (urls == null) continue;
                            for (int j = 0; j < urls.length(); j++) {
                                String url = urls.getString(j);
                                String username = server.optString("username", null);
                                String credential = server.optString("credential", null);
                                if (username != null && credential != null && !username.isEmpty() && !credential.isEmpty()) {
                                    fetchedServers.add(new PeerConnection.IceServer(url, username, credential));
                                } else {
                                    fetchedServers.add(new PeerConnection.IceServer(url));
                                }
                            }
                        }
                        if (!fetchedServers.isEmpty()) {
                            mActivity.runOnUiThread(() -> {
                                iceServers.clear();
                                iceServers.addAll(fetchedServers);
                                AppHelper.LogCat("ICE servers replaced from HTTP fallback, total count: " + iceServers.size());
                            });
                        }
                    }
                }
            } catch (Exception e) {
                AppHelper.LogCat("Failed to fetch ICE servers via HTTP, using defaults: " + e.getMessage());
            } finally {
                iceServersReady.countDown();
            }
        }).start();
    }


    private int findEndPoint() {
        for (int i = 0; i < MAX_PEER_CONNECTIONS; i++) if (!endPoints[i]) return i;
        return MAX_PEER_CONNECTIONS;
    }


    private void closeConnection(String id) {
        if (!this.peers.containsKey(id)) return;
        removePeer(id);
    }

    /**
     * Close connections (hangup) on all open connections.
     */
    public void closeAllConnections() {
        iceTimeoutHandler.removeCallbacksAndMessages(null);
        iceTimeoutRunnables.clear();

        if (mSocket != null && signalingServerHandler != null) {
            mSocket.off(AppConstants.SOCKET_REJECT_NEW_CALL, signalingServerHandler.onRejectResponse);
            mSocket.off(AppConstants.SOCKET_ACCEPT_NEW_CALL, signalingServerHandler.onAcceptResponse);
            mSocket.off(AppConstants.SOCKET_HANGUP_CALL, signalingServerHandler.onHangUpCallResponse);
            mSocket.off(AppConstants.SOCKET_SIGNALING_SERVER, signalingServerHandler.onSignalingServerResponse);
        }

        Iterator<String> peerIds = this.peers.keySet().iterator();
        while (peerIds.hasNext()) {
            closeConnection(peerIds.next());
        }
        if (!isAccepted)
            stopOutgoingSound();
        stopMedia();

        if (rootEglBase != null) {
            try {
                rootEglBase.release();
            } catch (Exception e) {
                AppHelper.LogCat("EglBase release exception: " + e.getMessage());
            }
            rootEglBase = null;
        }

    }


    public void hangUpCall(JSONObject message) {
        if (mSocket == null) return;
        mSocket.emit(AppConstants.SOCKET_HANGUP_CALL, message);
    }

    /**
     * method to initialize a new call with the second peer
     *
     * @param callerSocketId this the first parameter of startNewCall method
     * @param from           this the first parameter of startNewCall method
     * @param callerPhone    this the second parameter of   startNewCall method
     */
    public void startNewCall(String callerSocketId, String from, String callerPhone, String callerImage, int callerID, boolean isVideoCall) {
        JSONObject message = new JSONObject();
        try {
            message.put("to", callerSocketId);
            message.put("callerPhone", callerPhone);
            if (callerImage == null)
                message.put("callerImage", "null");
            else
                message.put("callerImage", callerImage);
            message.put("from", from);
            message.put("callerID", callerID);
            message.put("isVideoCall", isVideoCall);
            AppHelper.LogCat("WebRTC OUT make_new_call | to=" + callerSocketId + " | from=" + from
                    + " | callerID=" + callerID + " | isVideo=" + isVideoCall);
            mSocket.emit(AppConstants.SOCKET_MAKE_NEW_CALL, message);
        } catch (JSONException e) {
            AppHelper.LogCat(" startNewCall JSONException " + e.getMessage());
        }
    }

    public void start(String userSocketId, String callerSocketId) {
        setMedia(callerSocketId, userSocketId);
    }

    private void stopMedia() {
        for (Peer peer : peers.values()) {
            peer.peerConnection.dispose();
        }

        if (peerConnectionParameters.videoCallEnabled) {

            if (videoCapturer != null) {
                AppHelper.LogCat("stopMedia");
                try {
                    videoCapturer.stopCapture();
                } catch (Exception e) {
                    AppHelper.LogCat("stopCapture exception: " + e.getMessage());
                }
                videoCapturer = null;
            }
            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }
        }

        if (audioSource != null) {
            audioSource = null;
        }

    }

    private void setMedia(String callerSocketId, String userSocketId) {
        mediaStream = peerConnectionFactory.createLocalMediaStream(PEER_CONNECTION_ID);
        if (peerConnectionParameters.videoCallEnabled) {
            if (PermissionHandler.checkPermission(mActivity, Manifest.permission.CAMERA)) {

                AppHelper.LogCat("camera permission already granted.");
                if (hasCameraDevice()) {
                    MediaConstraints videoConstraints = new MediaConstraints();
                    videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MAX_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(peerConnectionParameters.videoHeight)));
                    videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MAX_VIDEO_WIDTH_CONSTRAINT, Integer.toString(peerConnectionParameters.videoWidth)));
                    //videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MIN_VIDEO_WIDTH_CONSTRAINT, Integer.toString(peerConnectionParameters.videoWidth)));
                    // videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MIN_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(peerConnectionParameters.videoHeight)));
                    videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(peerConnectionParameters.videoFps)));
                    videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(peerConnectionParameters.videoFps)));

                    videoCapturer = createVideoCapturer();
                    if (videoCapturer != null) {
                        videoSource = peerConnectionFactory.createVideoSource(false);
                        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", getRootEglBase().getEglBaseContext());
                        videoCapturer.initialize(surfaceTextureHelper, mActivity, videoSource.getCapturerObserver());
                        videoCapturer.startCapture(peerConnectionParameters.videoWidth, peerConnectionParameters.videoHeight, peerConnectionParameters.videoFps);
                        mediaStream.addTrack(peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource));
                    } else {
                        AppHelper.LogCat("videoCapturer is null ");
                    }
                } else {
                    hangUpCall(callerSocketId, userSocketId, AppConstants.NO_CAMERA);
                }
            } else {
                AppHelper.LogCat("Please request camera  permission.");
                PermissionHandler.requestPermission(mActivity, Manifest.permission.CAMERA);
                hangUpCall(callerSocketId, userSocketId, AppConstants.AN_EXECPTION);
            }
        }

        if (PermissionHandler.checkPermission(mActivity, Manifest.permission.RECORD_AUDIO)) {
            AppHelper.LogCat("Record audio permission already granted.");
            MediaConstraints audioConstraints = new MediaConstraints();
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION2_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL2_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION2_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));

            audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
            mediaStream.addTrack(peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource));

        } else {
            AppHelper.LogCat("Please request Record audio permission.");
            PermissionHandler.requestPermission(mActivity, Manifest.permission.RECORD_AUDIO);
            hangUpCall(callerSocketId, userSocketId, AppConstants.AN_EXECPTION);
        }
        EventBus.getDefault().post(new CallPusher(AppConstants.EVENT_BUS_LOCAL_STREAM, mediaStream));

    }

    private void hangUpCall(String callerSocketId, String userSocketId, String reason) {
        try {
            JSONObject messageJSON = new JSONObject();
            messageJSON.put("callerSocketId", callerSocketId);
            messageJSON.put("userSocketId", userSocketId);
            messageJSON.put("reason", reason);
            hangUpCall(messageJSON);
            this.mActivity.finish();
        } catch (JSONException e) {
            AppHelper.LogCat("JSONException webrtc rejectCall " + e.getMessage());
        }
    }


    private VideoCapturer createVideoCapturer() {
        if (!cameraIsOpened) {
            if (cameraEnumerator == null) {
                return null;
            }
            String deviceId = frontCameraId != null ? frontCameraId : backCameraId;
            if (deviceId == null) {
                AppHelper.LogCat("No camera device available");
                return null;
            }
            currentCameraFront = (deviceId == frontCameraId);
            String[] deviceNames = cameraEnumerator.getDeviceNames();
            for (String name : deviceNames) {
                if (name.equals(deviceId) && cameraEnumerator.isFrontFacing(name) == currentCameraFront) {
                    try {
                        VideoCapturer capturer = cameraEnumerator.createCapturer(name, new CameraVideoCapturer.CameraEventsHandler() {
                            @Override
                            public void onCameraError(String errorMessage) {
                                AppHelper.LogCat("onCameraError " + errorMessage);
                            }
                            @Override
                            public void onCameraDisconnected() {
                                AppHelper.LogCat("onCameraDisconnected");
                            }
                            @Override
                            public void onCameraFreezed(String errorMessage) {
                                AppHelper.LogCat("onCameraFreezed " + errorMessage);
                            }
                            @Override
                            public void onCameraOpening(String cameraName) {
                                AppHelper.LogCat("onCameraOpening " + cameraName);
                            }
                            @Override
                            public void onFirstFrameAvailable() {
                                AppHelper.LogCat("onFirstFrameAvailable");
                            }
                            @Override
                            public void onCameraClosed() {
                                AppHelper.LogCat("onCameraClosed");
                            }
                        });
                        if (capturer != null) {
                            cameraIsOpened = true;
                            return capturer;
                        }
                    } catch (Exception e) {
                        AppHelper.LogCat("createCapturer failed for " + name + ": " + e.getMessage());
                    }
                }
            }
            for (String name : deviceNames) {
                if (name.equals(deviceId)) {
                    try {
                        VideoCapturer capturer = cameraEnumerator.createCapturer(name, null);
                        if (capturer != null) {
                            cameraIsOpened = true;
                            return capturer;
                        }
                    } catch (Exception e) {
                        AppHelper.LogCat("createCapturer fallback failed for " + name + ": " + e.getMessage());
                    }
                }
            }
        }
        return null;
    }


    private boolean hasCameraDevice() {
        try {
            boolean preferCamera1 = ("samsung".equalsIgnoreCase(Build.MANUFACTURER) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    || "tecno".equalsIgnoreCase(Build.MANUFACTURER)
                    || "tecno mobile".equalsIgnoreCase(Build.MANUFACTURER)
                    || "infinix".equalsIgnoreCase(Build.MANUFACTURER)
                    || "itel".equalsIgnoreCase(Build.MANUFACTURER);

            if (!preferCamera1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraEnumerator = new Camera2Enumerator(mActivity);
            }
            if (cameraEnumerator == null || cameraEnumerator.getDeviceNames().length == 0) {
                cameraEnumerator = new Camera1Enumerator(true);
            }
            String[] deviceNames = cameraEnumerator.getDeviceNames();
            for (String name : deviceNames) {
                if (cameraEnumerator.isFrontFacing(name)) {
                    frontCameraId = name;
                } else if (cameraEnumerator.isBackFacing(name)) {
                    backCameraId = name;
                }
            }
            numberOfCameras = deviceNames.length;
        } catch (Exception e) {
            AppHelper.LogCat(" hasCameraDevice Exception " + e.getMessage());
            cameraEnumerator = new Camera1Enumerator(true);
            try {
                String[] deviceNames = cameraEnumerator.getDeviceNames();
                for (String name : deviceNames) {
                    if (cameraEnumerator.isFrontFacing(name)) {
                        frontCameraId = name;
                    } else if (cameraEnumerator.isBackFacing(name)) {
                        backCameraId = name;
                    }
                }
                numberOfCameras = deviceNames.length;
            } catch (Exception e2) {
                AppHelper.LogCat(" hasCameraDevice Camera1 fallback Exception " + e2.getMessage());
            }
        }

        return (frontCameraId != null) || (backCameraId != null);
    }


    private void switchCameraInternal() {
        if (!peerConnectionParameters.videoCallEnabled || numberOfCameras < 2) {
            AppHelper.LogCat("Failed to switch camera. Video: " + peerConnectionParameters.videoCallEnabled + ". Number of cameras: " + numberOfCameras);
            return;
        }
        if (videoCapturer != null) {
            try {
                CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
                cameraVideoCapturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                    @Override
                    public void onCameraSwitchDone(boolean isFrontCamera) {
                        currentCameraFront = isFrontCamera;
                    }
                    @Override
                    public void onCameraSwitchError(String errorMessage) {
                        AppHelper.LogCat("switchCamera failed: " + errorMessage);
                    }
                });
            } catch (Exception e) {
                AppHelper.LogCat("switchCamera failed: " + e.getMessage());
            }
        }
    }

    public boolean toggleMic() {
        java.util.List<AudioTrack> audioTracks = mediaStream.audioTracks;
        if (audioTracks != null) {
            if (audioTracks.size() != 0) {
                for (AudioTrack audioTrack : audioTracks) {
                    if (audioTrack.enabled()) {
                        audioTrack.setEnabled(false);
                        return false;
                    } else {
                        audioTrack.setEnabled(true);
                        return true;
                    }
                }

            } else {
                mActivity.runOnUiThread(() -> AppHelper.CustomToast(mActivity, "You can't disable/enable mic"));
                return false;
            }
        }
        return false;
    }

    public boolean enableSpeaker() {
        AudioManager audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        AppHelper.LogCat("getMode " + audioManager.getMode());
        AppHelper.LogCat("getRingerMode " + audioManager.getRingerMode());
        java.util.List<AudioTrack> audioTracks = mediaStream.audioTracks;
        if (audioTracks != null) {
            if (audioTracks.size() != 0) {
                boolean wasOn = audioManager.isSpeakerphoneOn();
                if (wasOn) {
                   /* audioManager.setSpeakerphoneOn(false);
                    float percent = 0.7f;
                    int seventyVolume = (int) (maxVolume * percent);
                    audioManager.setStreamVolume(AudioManager.MODE_NORMAL, seventyVolume, 0);
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.MODE_NORMAL);*/
                    SetSpeaker(false, audioManager);
                    return false;
                } else {
                    float percent = 0.7f;
                    int seventyVolume = (int) (maxVolume * percent);
                  /*  audioManager.setMode(AudioManager.MODE_NORMAL);
                    audioManager.setSpeakerphoneOn(true);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
                    //To increase media player volume
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.MODE_NORMAL);

                    if (!isNoiseSuppressionOn(mActivity))
                        turnOnNoiseSuppression(mActivity, true);*/
                    SetSpeaker(true, audioManager);
                    return true;
                }
            } else {
                mActivity.runOnUiThread(() -> AppHelper.CustomToast(mActivity, "You can't disable/enable speaker"));
                return false;
            }
        }
        return false;
    }

    private void SetSpeaker(boolean loudspeakerOn, AudioManager audioManager) {
        // create audio manager if needed
        if (audioManager == null && mActivity != null) {
            audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        }

        if (audioManager == null) {
            AppHelper.LogCat("Could not change audio routing - no audio manager");
            return;
        }

        int SDK = android.os.Build.VERSION.SDK_INT;

        if ((3 == SDK) || (4 == SDK)) {
            // 1.5 and 1.6 devices
            if (loudspeakerOn) {
                // route audio to back speaker
                audioManager.setMode(AudioManager.MODE_NORMAL);
            } else {
                // route audio to earpiece
                audioManager.setMode(AudioManager.MODE_IN_CALL);
            }
        } else {
            // 2.x devices
            if ((android.os.Build.BRAND.equals("Samsung") ||
                    android.os.Build.BRAND.equals("samsung")) &&
                    ((5 == SDK) || (6 == SDK) ||
                            (7 == SDK))) {
                // Samsung 2.0, 2.0.1 and 2.1 devices
                if (loudspeakerOn) {
                    // route audio to back speaker
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                    audioManager.setSpeakerphoneOn(true);
                } else {
                    // route audio to earpiece
                    audioManager.setSpeakerphoneOn(false);
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                }
            } else {
                // Non-Samsung and Samsung 2.2 and up devices
                audioManager.setSpeakerphoneOn(loudspeakerOn);
            }
        }


        if (!isNoiseSuppressionOn(mActivity))
            turnOnNoiseSuppression(mActivity, true);
    }

    public void switchCamera(Activity activity) {
        activity.runOnUiThread(this::switchCameraInternal);
    }

    private void turnOnNoiseSuppression(Context context, boolean flag) {
        AppHelper.LogCat("turnOnNoiseSuppression: " + flag);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (flag) {
            audioManager.setParameters("noise_suppression=auto");
        } else {
            audioManager.setParameters("noise_suppression=off");
        }

    }

    private static JSONObject firstJson(Object... args) {
        if (args == null || args.length == 0 || !(args[0] instanceof JSONObject)) {
            AppHelper.LogCat("WebRtcClient ignored malformed socket payload");
            return null;
        }
        return (JSONObject) args[0];
    }

    private boolean isNoiseSuppressionOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        String noiseSuppression = audioManager.getParameters("noise_suppression");
        AppHelper.LogCat("isNoiseSuppressionOn: " + noiseSuppression);
        if (noiseSuppression.contains("off"))
            return false;
        else
            return true;
    }


    private MediaPlayer mMediaPlayer;

    public void stopOutgoingSound() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer = null;
        }
    }


    public void startOutgoingSound() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer = MediaPlayer.create(mActivity, R.raw.outgoin_call);
        if (mMediaPlayer == null) return;
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.setVolume(1, 1);
        mMediaPlayer.start();

    }
}
