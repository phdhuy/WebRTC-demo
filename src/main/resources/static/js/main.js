const PORT = 8080;
const peerConnectionConfig = {
    'iceServers': [
        {'urls': 'stun:stun.l.google.com:19302'}
    ]
};
const MAPPING = "/call/123";

var ws;
var localStream;
var connections = {};
var uuidInBig;

const container = document.getElementById("remoteView");
const selfView = document.getElementById("selfView");

function init() {

    navigator.mediaDevices.getUserMedia({video: true, audio: true}).then(function (stream) {
        console.log("Stream OK");
        console.log(MAPPING);
        localStream = stream;
        selfView.srcObject = localStream;
        ws = new WebSocket('ws://' + window.location.hostname + ':' + PORT + "/call/123");
        ws.onmessage = processWsMessage;
        ws.onopen = logMessage;
        ws.onclose = logMessage;
        ws.onerror = logMessage;
    }).catch(function (error) {
        console.log("Stream NOT OK: " + error.name + ': ' + error.message);
    });

}

function processWsMessage(message) {
    const signal = JSON.parse(message.data);
    logMessage(signal);
    switch (signal.type) {
        case 'init':
            handleInit(signal);
            break;
        case 'logout':
            handleLogout(signal);
            break;
        case 'offer':
            handleOffer(signal);
            break;
        case 'answer':
            handleAnswer(signal);
            break;
        case 'ice':
            handleIce(signal);
            break;
    }

}

function handleInit(signal) {
    const peerId = signal.sender;
    const connection = getRTCPeerConnectionObject(peerId);

    connection.createOffer().then(function (sdp) {
        connection.setLocalDescription(sdp);
        console.log('Creating an offer for', peerId);
        sendMessage({
            type: "offer",
            receiver: peerId,
            data: sdp
        });
    }).catch(function (e) {
        console.log('Error in offer creation.', e);
    });

}

function handleLogout(signal) {
    var peerId = signal.sender;
    if (peerId == uuidInBig) {
        remoteView.srcObject = null;
    }
    delete connections[peerId];
    var videoElement = document.getElementById(peerId);
    videoElement.outerHTML = "";
    delete videoElement;
}

function handleOffer(signal) {
    var peerId = signal.sender;
    var connection = getRTCPeerConnectionObject(peerId);
    connection.setRemoteDescription(new RTCSessionDescription(signal.data)).then(function () {
        console.log('Setting remote description by offer from ' + peerId);
        connection.createAnswer().then(function (sdp) {
            connection.setLocalDescription(sdp);
            sendMessage({
                type: "answer",
                receiver: peerId,
                data: sdp
            });

        }).catch(function (e) {
            console.log('Error in offer handling.', e);
        });

    }).catch(function (e) {
        console.log('Error in offer handling.', e);
    });
}

function handleAnswer(signal) {
    var connection = getRTCPeerConnectionObject(signal.sender);
    connection.setRemoteDescription(new RTCSessionDescription(signal.data)).then(function () {
        console.log('Setting remote description by answer from' + signal.sender);
    }).catch(function (e) {
        console.log('Error in answer acceptance.', e);
    });
}

function handleIce(signal) {
    if (signal.data) {
        console.log('Adding ice candidate');
        var connection = getRTCPeerConnectionObject(signal.sender);
        connection.addIceCandidate(new RTCIceCandidate(signal.data));
    }
}

function getRTCPeerConnectionObject(uuid) {

    if (connections[uuid]) {
        return connections[uuid];
    }

    var connection = new RTCPeerConnection(peerConnectionConfig);

    connection.addStream(localStream);

    connection.onicecandidate = function (event) {
        console.log("candidate is: " + event.candidate);
        if (event.candidate) {
            sendMessage({
                type: "ice",
                receiver: uuid,
                data: event.candidate
            });
        }
    };

    connection.onaddstream = function (event) {
        console.log('Received new stream from ' + uuid);
        var video = document.createElement("video");
        container.appendChild(video);
        video.id = uuid;
        video.width = 160;
        video.height = 120;
        video.className += " videoElement";
        video.autoplay = true;
        video.srcObject = event.stream;
        video.addEventListener('click', function (event) {
            setBigVideo(uuid);
        }, false);
        if (!remoteView.srcObject) {
            setBigVideo(uuid);
        }
    };

    connections[uuid] = connection;
    return connection;
}

function setBigVideo(uuid) {
    remoteView.srcObject = document.getElementById(uuid).srcObject;
    if (uuidInBig && document.getElementById(uuidInBig)) {
        document.getElementById(uuidInBig).classList.remove("active");
    }
    document.getElementById(uuid).classList.add("active");
    uuidInBig = uuid;
}

function sendMessage(payload) {
    ws.send(JSON.stringify(payload));
}

function logMessage(message) {
    console.log(message);
}

function disconnect() {
    console.log('Disconnecting ');
    if (ws != null) {
        ws.close();
    }
}

function turnOnMicrophone() {
    localStream.getAudioTracks()[0].enabled = true;
    console.log("Turn on mic!")
}

function turnOffMicrophone() {
    localStream.getAudioTracks()[0].enabled = false;
    console.log("Turn off mic!")
}