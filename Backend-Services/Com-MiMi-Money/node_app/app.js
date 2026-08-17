/**
 * Created by abderrahimelimame on 9/24/16.
 */

var app = require('express')();
var fs = require('fs');
var https = require('https');
var http = require('http');
var crypto = require('crypto');

app.use(require('body-parser').json());

var server;
var sslKeyPath = process.env.SSL_KEY_PATH || '';
var sslCertPath = process.env.SSL_CERT_PATH || '';

if (fs.existsSync(sslKeyPath) && fs.existsSync(sslCertPath)) {
    var sslOptions = {
        key: fs.readFileSync(sslKeyPath),
        cert: fs.readFileSync(sslCertPath),
        minVersion: 'TLSv1.2',
        secureOptions: crypto.constants.SSL_OP_NO_SSLv2 | crypto.constants.SSL_OP_NO_SSLv3 | crypto.constants.SSL_OP_NO_TLSv1 | crypto.constants.SSL_OP_NO_TLSv1_1
    };
    server = https.createServer(sslOptions, app);
    console.log('Using HTTPS server (TLS 1.2/1.3 only)');
} else {
    server = http.createServer(app);
    console.log('SSL certificates not found, using HTTP server');
}
var users = require('./users.js')();
var db = require('./db.js');
var pingInterval = 25000;
var Socket = require('socket.io');
var io = Socket(server, {
    'pingInterval': pingInterval,
    'pingTimeout': 60000,
    'maxHttpBufferSize': 1e8,
    'allowEIO3': true,
    'cors': {
        'origin': '*',
        'methods': ['GET', 'POST']
    }
});

var serverPort = Number(process.env.PORT || 9001);
var app_key_secret = process.env.APP_KEY_SECRET || 'change-me-local-only';
var debugging_mode = process.env.SOCKET_DEBUG === 'true';

function handleEmitRequest(eventName, eventData) {
    if (!eventName || !eventData) return;
    if (eventName === 'socket_new_message_server') {
        var recipientId = String(eventData.recipientId || '');
        var user = users.getUser(recipientId);
        if (user != null) {
            eventData.recipientId = parseInt(eventData.recipientId) || eventData.recipientId;
            eventData.senderId = parseInt(eventData.senderId) || eventData.senderId;
            io.to(user.socketID).emit('socket_new_message_server', eventData);
        }
    } else if (eventName === 'socket_new_message_group_server') {
        eventData.senderId = parseInt(eventData.senderId) || eventData.senderId;
        io.sockets.emit('socket_new_message_group_server', eventData);
    } else {
        io.sockets.emit(eventName, eventData);
    }
}

app.post('/emit', function(req, res) {
    handleEmitRequest(req.body.event, req.body.data);
    res.json({success: true});
});

app.post('/push', function(req, res) {
    var eventName = req.body.event;
    var eventData = req.body.data;
    var targetUserId = req.body.targetUserId;
    var targetGroupId = req.body.targetGroupId;

    if (!eventName || !eventData) {
        return res.json({success: false, error: 'missing event or data'});
    }

    if (targetUserId) {
        var user = users.getUser(String(targetUserId));
        if (user != null) {
            eventData.recipientId = parseInt(eventData.recipientId) || eventData.recipientId;
            eventData.senderId = parseInt(eventData.senderId) || eventData.senderId;
            io.to(user.socketID).emit(eventName, eventData);
        }
    } else {
        handleEmitRequest(eventName, eventData);
    }
    res.json({success: true});
});

app.get('/health', function(req, res) {
    var usersArray = users.getUsers();
    res.json({
        status: 'ok',
        uptime: process.uptime(),
        connectedUsers: usersArray.length,
        port: port
    });
});

app.get('/poll/messages', function(req, res) {
    var recipientId = req.query.recipientId;
    if (!recipientId) {
        return res.status(400).json({error: 'recipientId required'});
    }
    var internalSecret = req.headers['x-internal-secret'];
    if (internalSecret !== app_key_secret) {
        return res.status(403).json({error: 'unauthorized'});
    }
    db.fetchMissedMessages(recipientId)
        .then(function(messages) {
            res.json({success: true, messages: messages});
        })
        .catch(function(err) {
            res.status(500).json({success: false, error: err.message});
        });
});

var port = process.env.PORT || serverPort;

server.on('error', function(err) {
    if (err.code === 'EADDRINUSE') {
        console.error('Port %d is already in use. Exiting.', port);
        process.exit(1);
    } else {
        console.error('Server error:', err);
    }
});

server.listen(port, function () {
    console.log('Server listening at port %d', port);
});

process.on('uncaughtException', function(err) {
    console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', function(reason, promise) {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

io.use(function (socket, next) {
        var token = socket.handshake.query.token;
        if (token === app_key_secret) {
            if (debugging_mode) {
                console.log("token valid  authorized", token);
            }
            next();
        } else {
            if (debugging_mode) {
                console.log("not a valid token Unauthorized to access ");
            }
            next(new Error("not valid token"));
        }
    }
);

require('./socketHandler.js')(io, users, debugging_mode, pingInterval, db);
