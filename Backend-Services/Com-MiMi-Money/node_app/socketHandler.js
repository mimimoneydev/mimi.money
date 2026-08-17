/**
 * Created by abderrahimelimame on 4/7/17.
 */

module.exports = function(io, users, debugging_mode, pingInterval, db) {

  var http = require('http');
  var https = require('https');

  function fetchJson(url, options) {
    return new Promise(function(resolve, reject) {
      var mod = url.startsWith('https') ? https : http;
      var req = mod.request(url, options, function(res) {
        var body = '';
        res.on('data', function(chunk) { body += chunk; });
        res.on('end', function() {
          if (res.statusCode === 200) {
            try { resolve(JSON.parse(body)); } catch (e) { reject(e); }
          } else {
            reject(new Error('HTTP ' + res.statusCode));
          }
        });
      });
      req.on('error', reject);
      req.on('timeout', function() { req.destroy(); reject(new Error('timeout')); });
      if (options && options.timeout) req.setTimeout(options.timeout);
      if (options && options.body) req.write(options.body);
      req.end();
    });
  }

  var crypto = require('crypto');

  function generateTurnCredentials(secret, ttl) {
    var username = String(Math.floor(Date.now() / 1000) + (ttl || 86400));
    var hmac = crypto.createHmac('sha1', secret);
    hmac.update(username);
    var credential = hmac.digest('base64');
    return { username: username, credential: credential };
  }

  var TURN_HOST = process.env.TURN_HOST || '';
  var TURN_SECRET = process.env.TURN_SECRET || '';
  var TURN_PORT = Number(process.env.TURN_PORT || 3478);
  var TURNS_PORT = Number(process.env.TURNS_PORT || 5349);
  var INTERNAL_API_PROTOCOL = process.env.INTERNAL_API_PROTOCOL || 'http';
  var INTERNAL_API_HOST = process.env.INTERNAL_API_HOST || '127.0.0.1';
  var INTERNAL_API_PORT = Number(process.env.INTERNAL_API_PORT || 8080);
  var INTERNAL_API_SECRET = process.env.APP_KEY_SECRET || 'change-me-local-only';
  var internalApiTransport = INTERNAL_API_PROTOCOL === 'https' ? https : http;

  function getLocalTurnCredentials() {
    var ttl = 86400;
    var username = String(Math.floor(Date.now() / 1000) + ttl);
    var hmac = crypto.createHmac('sha1', TURN_SECRET);
    hmac.update(username);
    var credential = hmac.digest('base64');
    return { username: username, credential: credential };
  }

  function getIceServers() {
    var iceServers = [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      { urls: 'stun:stun2.l.google.com:19302' }
    ];

    if (TURN_HOST && TURN_SECRET) {
      var creds = getLocalTurnCredentials();
      iceServers.push(
        { urls: 'turn:' + TURN_HOST + ':' + TURN_PORT + '?transport=udp', username: creds.username, credential: creds.credential },
        { urls: 'turn:' + TURN_HOST + ':' + TURN_PORT + '?transport=tcp', username: creds.username, credential: creds.credential },
        { urls: 'turns:' + TURN_HOST + ':' + TURNS_PORT + '?transport=tcp', username: creds.username, credential: creds.credential }
      );
    }

    return db.getSettings([
      'meteredTurnApiKey',
      'turnServerUrl', 'turnServerUsername', 'turnServerCredential'
    ]).then(function(settings) {
      var meteredTurnApiKey = settings.meteredTurnApiKey || '';
      var turnServerUrl = settings.turnServerUrl || '';
      var turnServerCredential = settings.turnServerCredential || '';

      if (meteredTurnApiKey) {
        return fetchJson('https://turn.metered.ca/api/v1/turn/credential?api_key=' + encodeURIComponent(meteredTurnApiKey), { timeout: 5000 })
          .then(function(credential) {
            if (credential && credential.iceServers) {
              credential.iceServers.forEach(function(server) {
                iceServers.push(server);
              });
            } else if (credential && credential.urls) {
              var urls = Array.isArray(credential.urls) ? credential.urls : [credential.urls];
              urls.forEach(function(url) {
                var server = { urls: url };
                if (credential.username) server.username = credential.username;
                if (credential.credential) server.credential = credential.credential;
                iceServers.push(server);
              });
            }
            return iceServers;
          })
          .catch(function(err) {
            console.log('Metered TURN failed:', err.message);
            return iceServers;
          });
      }

      if (turnServerUrl && turnServerCredential) {
        var extraCreds = generateTurnCredentials(turnServerCredential);
        iceServers.push({ urls: turnServerUrl, username: extraCreds.username, credential: extraCreds.credential });
      }

      return iceServers;
    }).catch(function(err) {
      console.log('getSettings failed, returning local TURN only:', err.message);
      return iceServers;
    });
  }

  function persistMessageStatus(messageId, status, token) {
    try {
      if (!messageId || messageId == 0) return;
      var postData = JSON.stringify({ messageId: String(messageId), status: String(status) });
      var options = {
        hostname: INTERNAL_API_HOST,
        port: INTERNAL_API_PORT,
        path: '/updateMessageStatus',
        method: 'POST',
        rejectUnauthorized: false,
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData),
          'X-Internal-Secret': INTERNAL_API_SECRET
        }
      };
      var req = internalApiTransport.request(options, function(res) {
        res.on('data', function() {});
        res.on('end', function() {
          if (debugging_mode) {
            console.log('persistMessageStatus response:', res.statusCode);
          }
        });
      });
      req.on('error', function(err) {
        if (debugging_mode) {
          console.log('persistMessageStatus error:', err.message);
        }
      });
      req.on('timeout', function() {
        req.destroy();
      });
      req.setTimeout(3000);
      req.write(postData);
      req.end();
    } catch (e) {
      if (debugging_mode) {
        console.log('persistMessageStatus exception:', e.message);
      }
    }
  }

  function sendCallPush(recipientId, callData) {
    try {
      var postData = JSON.stringify({
        recipientId: String(recipientId),
        callerId: callData.callerId || '',
        callerName: callData.callerName || '',
        callerImage: callData.callerImage || '',
        callType: callData.callType || 'voice',
        callId: callData.callId || '',
        roomName: callData.roomName || ''
      });
      var options = {
        hostname: INTERNAL_API_HOST,
        port: INTERNAL_API_PORT,
        path: '/SendCallPush',
        method: 'POST',
        rejectUnauthorized: false,
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData),
          'X-Internal-Secret': INTERNAL_API_SECRET
        }
      };
      var req = internalApiTransport.request(options, function(res) {
        res.on('data', function() {});
        if (debugging_mode) {
          console.log('sendCallPush response:', res.statusCode);
        }
      });
      req.on('error', function(err) {
        if (debugging_mode) {
          console.log('sendCallPush error:', err.message);
        }
      });
      req.on('timeout', function() {
        req.destroy();
      });
      req.setTimeout(3000);
      req.write(postData);
      req.end();
    } catch (e) {
      if (debugging_mode) {
        console.log('sendCallPush exception:', e.message);
      }
    }
  }

  io.on("connection", function(socket) {


    /*****************************************************************************************************************************************
     ********************************************* Users Connection Methods  *****************************************************************
     *****************************************************************************************************************************************/

    /**
     * Note: Custom ping/pong (socket_ping/socket_pong) removed.
     * Socket.IO v4 has a built-in heartbeat mechanism where the server
     * sends pings and the client responds with pongs automatically.
     * This is more reliable than the old custom approach.
     */


    /**
     * method to save user as connected
     */
    socket.on("socket_user_connect", function(data) {
      if (debugging_mode) {
        console.log("the user with id " + data.connectedId + " connected " + +data.connected + " token " + data.userToken + "socket.id " + socket.id);
      }
      if (data.connectedId != null && data.connectedId != 0) {
        var user = users.getUser(data.connectedId);
        if (user != null) {
          users.updateUser(data.connectedId, data.connected, socket.id);
        } else {
          users.addUser(data.connectedId, data.connected, socket.id);
        }

        io.sockets.emit("socket_user_connect", {
          connectedId: data.connectedId,
          connected: true,
          socketId: data.socketId
        });

        // Fetch and deliver missed messages
        if (db && db.fetchMissedMessages) {
          db.fetchMissedMessages(String(data.connectedId))
            .then(function(messages) {
              if (messages && messages.length > 0) {
                if (debugging_mode) {
                  console.log("Delivering " + messages.length + " missed messages to user " + data.connectedId);
                }

                messages.forEach(function(message) {
                  socket.emit("socket_new_message_server", {
                    actionType: "socket_new_message_server",
                    recipientId: parseInt(data.connectedId) || data.connectedId,
                    messageId: message.messageId,
                    messageBody: message.messageBody,
                    senderId: parseInt(message.senderId) || message.senderId,
                    walletAddress: message.walletAddress || message.wallet_address,
                    senderName: message.senderName,
                    date: message.date,
                    isGroup: message.isGroup,
                    image: message.image,
                    video: message.video,
                    audio: message.audio,
                    document: message.document,
                    thumbnail: message.thumbnail,
                    duration: message.duration,
                    fileSize: message.fileSize,
                    senderImage: message.senderImage
                  });
                });

                socket.emit("socket_missed_messages", { messages: messages });
              }
            })
            .catch(function(err) {
              if (debugging_mode) {
                console.log("fetchMissedMessages error:", err.message);
              }
            });
        }

      }


    });

    /**
     * method to acknowledge missed messages were received by client
     * Only then mark them as delivered (status=1)
     */
    socket.on("socket_missed_messages_received", function(data) {
      if (!data || !data.messageIds || data.messageIds.length === 0) return;
      
      var user = users.getUserBySocketID(socket.id);
      if (!user) return;
      
      var recipientId = user.ID;

      if (debugging_mode) {
        console.log("Client confirmed receipt of " + data.messageIds.length + " missed messages for user " + recipientId);
      }

      if (db && db.markMessagesDelivered) {
        db.markMessagesDelivered(String(recipientId), data.messageIds)
          .catch(function(err) {
            if (debugging_mode) {
              console.log("markMessagesDelivered error:", err.message);
            }
          });
      }
    });


    /**
     * method if a user is disconnect from sockets
     * and then remove him from array of current users connected
     */
    socket.on("disconnect", function() {
      var usersArray = users.getUsers();
      if (usersArray.length != 0) {
        for (var i = 0; i < usersArray.length; i++) {
          var user = usersArray[i];
          if (user != null) {

            if (user.socketID == socket.id) {
              if (debugging_mode) {
                console.log("the user with id  " + user.ID + " is disconnect 1 ");
              }
              io.sockets.emit("socket_user_connect", {
                connectedId: user.ID,
                connected: false,
                socketId: user.socketID
              });

              users.removeUser(user.ID);
              if (debugging_mode) {
                console.log("the users list size disconnect " + usersArray.length);
              }
              break;
            }

          } else {
            if (debugging_mode) {
              console.log("the user is null disconnect ");
            }
          }


        }
      }
    });


    socket.on("socket_save_new_message", function(data) {
      var recipientId = data.recipientId;
      var user = users.getUser(String(recipientId));
      if (user != null) {
        var messageData = {
          actionType: "socket_new_message_server",
          recipientId: parseInt(data.recipientId) || data.recipientId,
          messageId: data.messageId,
          messageBody: data.messageBody,
          senderId: parseInt(data.senderId) || data.senderId,
          walletAddress: data.walletAddress,
          senderName: data.senderName,
          date: data.date,
          isGroup: data.isGroup,
          image: data.image,
          video: data.video,
          audio: data.audio,
          document: data.document,
          thumbnail: data.thumbnail,
          duration: data.duration,
          fileSize: data.fileSize,
          senderImage: data.senderImage
        };
        socket.to(user.socketID).emit("socket_new_message_server", messageData);
        
        // Mark as delivered (status=1) since recipient is online and message was pushed
        if (db && db.upsertMessageReceipt && data.messageId) {
          db.upsertMessageReceipt(data.messageId, String(recipientId), 1)
            .catch(function(err) {
              if (debugging_mode) {
                console.log("upsertMessageReceipt error:", err.message);
              }
            });
        }
      }
    });

    socket.on("socket_save_new_group_message", function(data) {
      io.sockets.emit("socket_new_message_group_server", {
        actionType: "socket_new_message_group_server",
        recipientId: parseInt(data.recipientId) || data.recipientId,
        messageId: data.messageId,
        messageBody: data.messageBody,
        senderId: parseInt(data.senderId) || data.senderId,
        walletAddress: data.walletAddress,
        senderName: data.senderName,
        date: data.date,
        isGroup: data.isGroup,
        image: data.image,
        video: data.video,
        audio: data.audio,
        document: data.document,
        thumbnail: data.thumbnail,
        duration: data.duration,
        fileSize: data.fileSize,
        senderImage: data.senderImage,
        groupId: data.groupId
      });
    });


    /**
     * method to check if recipient is Online
     */
    socket.on("socket_is_online", function(data) {
      var targetUserId = data.userId || data.senderId;
      if (targetUserId != null) {
        var user = users.getUser(targetUserId);
        var isOnline = (user != null && user.connected);
        socket.emit("socket_is_online", {
          userId: targetUserId,
          connected: isOnline
        });
      } else {
        io.sockets.emit("socket_is_online", {
          senderId: data.senderId,
          connected: data.connected
        });
      }
    });


    /*****************************************************************************************************************************************
     ********************************************* Single User Messages Methods  *****************************************************************
     *****************************************************************************************************************************************/



    socket.on("socket_update_register_id", function(data) {
      var user = users.getUser(data.recipientId);
      if (user != null) {
        socket.to(user.socketID).emit("socket_update_register_id", data);
      }
    });

    ////////////////////////////////////  Those to change status messages on application side //////////////////////////////////////////
    /**
     * method to check if a message is delivered to the recipient / and make it as delivered
     */
    socket.on("socket_delivered", function(data) {
      var statusId = data.serverMessageId || data.messageId;
      if (statusId) {
        persistMessageStatus(statusId, 2);
      }
      var user = users.getUser(data.recipientId);
      if (user != null) {
        socket.to(user.socketID).emit("socket_delivered", {
          messageId: data.messageId,
          senderId: data.senderId
        });
      }
    });

    /**
     * method to check if user is read (seen) a specific message
     */
    socket.on("socket_seen", function(data) {
      var statusId = data.serverMessageId || data.messageId;
      if (statusId) {
        persistMessageStatus(statusId, 3);
      }
      var user = users.getUser(data.recipientId);
      if (user != null) {
        var seenData = {
          senderId: data.senderId,
          recipientId: data.recipientId
        };
        if (data.messageId != null) {
          seenData.messageId = data.messageId;
        }
        socket.to(user.socketID).emit("socket_seen", seenData);
      }
    });

    /**
     * method to relay download status to the sender and persist status 4 (downloaded)
     */
    socket.on("socket_downloaded", function(data) {
      var statusId = data.serverMessageId || data.messageId;
      if (statusId) {
        persistMessageStatus(statusId, 4);
      }
      var user = users.getUser(data.recipientId);
      if (user != null) {
        var downloadedData = {
          senderId: data.senderId,
          recipientId: data.recipientId
        };
        if (data.messageId != null) {
          downloadedData.messageId = data.messageId;
        }
        socket.to(user.socketID).emit("socket_downloaded", downloadedData);
      }
    });

    /*****************************************************************************************************************************************
     ********************************************* Groups Messages Methods  *****************************************************************
     *****************************************************************************************************************************************/


    /**
     * method to check if a message is delivered to the recipient / and make it as delivered
     */
    socket.on("socket_group_delivered", function(data) {
      var statusId = data.serverMessageId || data.messageId;
      if (statusId) {
        persistMessageStatus(statusId, 2);
      }
      io.sockets.emit("socket_group_delivered", data);
    });


    socket.on("socket_group_seen", function(data) {
      var statusId = data.serverMessageId || data.messageId;
      if (statusId) {
        persistMessageStatus(statusId, 3);
      }
      io.sockets.emit("socket_group_seen", data);
    });

    socket.on("socket_group_downloaded", function(data) {
      var statusId = data.serverMessageId || data.messageId;
      if (statusId) {
        persistMessageStatus(statusId, 4);
      }
      io.sockets.emit("socket_group_downloaded", data);
    });

    /*****************************************************************************************************************************************
     ********************************************* Others Methods  *****************************************************************
     *****************************************************************************************************************************************/


    /**
     * method to notify all members
     */
    socket.on("socket_groupImageUpdated", function(dataString) {

      if (debugging_mode) {
        console.log("socket_groupImageUpdated ");
      }
      io.sockets.emit("socket_groupImageUpdated", dataString);
    });

    /**
     * method to check if  member of group  is start typing
     */
    socket.on("socket_member_typing", function(data) {
      io.sockets.emit("socket_member_typing", {
        recipientId: data.recipientId,
        groupId: data.groupId,
        senderId: data.senderId
      });
    });

    /**
     * method to check if a member of group  is stop typing
     */
    socket.on("socket_member_stop_typing", function(data) {
      io.sockets.emit("socket_member_stop_typing", {
        recipientId: data.recipientId,
        groupId: data.groupId,
        senderId: data.senderId
      });
    });

    /**
     * method to notify all users by the new user joined
     */
    socket.on("socket_new_user_has_joined", function(dataString) {
      io.sockets.emit("socket_new_user_has_joined", {
        userId: dataString.userId || dataString.senderId,
        walletAddress: dataString.walletAddress || null,
        name: dataString.name || null
      });
    });

    /**
     * method to notify all users
     */
    socket.on("socket_profileImageUpdated", function(dataString) {

      if (debugging_mode) {
        console.log("socket_profileImageUpdated ");
      }
      io.sockets.emit("socket_profileImageUpdated", {
        userId: dataString.userId || dataString.senderId,
        walletAddress: dataString.walletAddress || null,
        image: dataString.image || null
      });
    });
    /**
     * method to check if user is start typing
     */
    socket.on("socket_typing", function(data) {
      var user = users.getUser(data.recipientId);
      if (user != null) {
        socket.to(user.socketID).emit("socket_typing", {
          recipientId: data.recipientId,
          senderId: data.senderId
        });
      }
    });

    /**
     * method to check if user is stop typing
     */
    socket.on("socket_stop_typing", function(data) {

      var user = users.getUser(data.recipientId);
      if (user != null) {
        socket.to(user.socketID).emit("socket_stop_typing", {
          recipientId: data.recipientId,
          senderId: data.senderId
        });

      }
    });


    /*****************************************************************************************************************************************
     ********************************************* Users Call Methods  *****************************************************************
     *****************************************************************************************************************************************/

    socket.on("socket_get_ice_servers", function(data, callback) {
      getIceServers()
        .then(function(iceServers) {
          callback({ success: true, iceServers: iceServers });
        })
        .catch(function(err) {
          if (debugging_mode) {
            console.log('socket_get_ice_servers error:', err.message);
          }
          var fallbackCreds = getLocalTurnCredentials();
          callback({
            success: true,
            iceServers: [
              { urls: 'stun:stun.l.google.com:19302' },
              { urls: 'stun:stun1.l.google.com:19302' },
              { urls: 'stun:stun2.l.google.com:19302' },
              { urls: 'turn:' + TURN_HOST + ':' + TURN_PORT + '?transport=udp', username: fallbackCreds.username, credential: fallbackCreds.credential },
              { urls: 'turn:' + TURN_HOST + ':' + TURN_PORT + '?transport=tcp', username: fallbackCreds.username, credential: fallbackCreds.credential },
              { urls: 'turns:' + TURN_HOST + ':' + TURNS_PORT + '?transport=tcp', username: fallbackCreds.username, credential: fallbackCreds.credential }
            ]
          });
        });
    });

    /**
     * method to check if user is connected  before call him (do a ping and get a callback)
     */
    socket.on("socket_call_user_ping", function(data, callback) {
      if (debugging_mode)
        console.log("socket_call_user_ping called ");

      var user = users.getUser(data.recipientId);
      var pingedData;
      if (user != null) {
        console.log("socket id " + user.socketID);
        pingedData = {
          socketId: user.socketID,
          recipientId: data.recipientId,
          connected: true
        };
        callback(pingedData);
      } else {
        pingedData = {
          socketId: null,
          recipientId: data.recipientId,
          connected: false
        };
        callback(pingedData);
      }

    });

    /**
     * method to check if user is already on users array
     * Returns the requesting socket's actual ID in the ACK callback
     */
    socket.on("reset_socket_id", function(data, callback) {
      if (debugging_mode)
        console.log("reset_socket_id called, requesting socket.id = " + socket.id + ", data.userSocketId = " + data.userSocketId);
      var pingedData = {
        userSocketId: socket.id,
        previousSocketId: data.userSocketId || null
      };
      callback(pingedData);
    });

    /**
     * method make the connection between the two peers
     * Routes signaling messages by looking up the target user's socket ID
     * Falls back to direct socket ID routing if the target is a socket ID
     */
    socket.on("signaling_server", function(data) {
      if (debugging_mode)
        console.log("signaling_server called, to=" + data.to + ", from=" + socket.id + ", type=" + (data.type || 'unknown'));

      var targetSocketId = null;
      var targetId = data.to;

      // Try to look up the target as a user ID first
      if (targetId) {
        var targetUser = users.getUser(String(targetId));
        if (targetUser && targetUser.socketID) {
          targetSocketId = targetUser.socketID;
          if (debugging_mode)
            console.log("signaling_server: resolved user " + targetId + " -> socket " + targetSocketId);
        } else {
          // Fallback: treat the 'to' field as a direct socket ID
          targetSocketId = targetId;
          if (debugging_mode)
            console.log("signaling_server: using direct socket ID " + targetSocketId + " (user not found)");
        }

        if (targetSocketId && targetSocketId !== socket.id) {
          delete data.to;
          io.to(targetSocketId).emit("signaling_server", data);
        } else if (debugging_mode) {
          console.log("signaling_server: no valid target or target is self, skipping relay");
        }
      } else if (debugging_mode) {
        console.log("signaling_server: missing 'to' field in data");
      }
    });

    /**
     * Helper to route a call event to the target user's socket.
     * Looks up the user by ID first, falls back to direct socket ID routing.
     */
    function routeToCaller(data, eventName) {
      var targetSocketId = null;

      // First try to resolve by recipientId/callerId (user IDs)
      var lookupId = data.callerId || data.recipientId || data.to;
      if (lookupId) {
        var targetUser = users.getUser(String(lookupId));
        if (targetUser && targetUser.socketID) {
          targetSocketId = targetUser.socketID;
          if (debugging_mode)
            console.log(eventName + ": resolved user " + lookupId + " -> socket " + targetSocketId);
        }
      }

      // Fallback to direct callerSocketId
      if (!targetSocketId && data.callerSocketId) {
        targetSocketId = data.callerSocketId;
        if (debugging_mode)
          console.log(eventName + ": using callerSocketId " + targetSocketId);
      }

      // Last fallback: treat 'to' as socket ID directly
      if (!targetSocketId && data.to) {
        targetSocketId = data.to;
        if (debugging_mode)
          console.log(eventName + ": using 'to' field as socket ID " + targetSocketId);
      }

      if (targetSocketId) {
        io.to(targetSocketId).emit(eventName, data);
        if (debugging_mode)
          console.log(eventName + ": emitted to socket " + targetSocketId);
      } else if (debugging_mode) {
        console.log(eventName + ": no target socket found for routing");
      }
    }

    var makeCall = function(data) {
      if (debugging_mode)
        console.log("make_new_call: to=" + data.to + ", recipientId=" + data.recipientId + ", callerId=" + data.callerId);

      // Route via recipient user ID lookup first, then fall back to socket ID
      var targetSocketId = null;
      if (data.recipientId) {
        var recipientUser = users.getUser(String(data.recipientId));
        if (recipientUser && recipientUser.socketID) {
          targetSocketId = recipientUser.socketID;
          if (debugging_mode)
            console.log("make_new_call: resolved recipient " + data.recipientId + " -> socket " + targetSocketId);
        }
      }
      if (!targetSocketId && data.to) {
        // Fallback: treat data.to as direct socket ID
        targetSocketId = data.to;
        if (debugging_mode)
          console.log("make_new_call: using direct socket ID from 'to' field: " + targetSocketId);
      }

      if (targetSocketId) {
        io.to(targetSocketId).emit("receive_new_call", data);
        if (debugging_mode)
          console.log("make_new_call: receive_new_call emitted to socket " + targetSocketId);
      }

      // Send push notification if recipient is offline (no socket connection)
      if (data.recipientId) {
        var recipientUser = users.getUser(String(data.recipientId));
        if (recipientUser == null) {
          if (debugging_mode)
            console.log("make_new_call: recipient " + data.recipientId + " offline, sending push");
          sendCallPush(data.recipientId, data);
        } else if (debugging_mode) {
          console.log("make_new_call: recipient " + data.recipientId + " is online, skipping push");
        }
      }
    };
    /**
     * method to initialize the new call
     */
    socket.on("make_new_call", makeCall);

    /**
     * method to Reject a call
     */
    socket.on("reject_new_call", function(data) {
      if (debugging_mode)
        console.log("reject_new_call: callerSocketId=" + data.callerSocketId + ", callerId=" + data.callerId);
      routeToCaller(data, "reject_new_call");
    });


    /**
     * method to Accept a call
     */
    socket.on("accept_new_call", function(data) {
      if (debugging_mode)
        console.log("accept_new_call: callerSocketId=" + data.callerSocketId + ", callerId=" + data.callerId);
      routeToCaller(data, "accept_new_call");
    });
    /**
     * method to HangUp a call
     */
    socket.on("hang_up_call", function(data) {
      if (debugging_mode)
        console.log("hang_up_call: callerSocketId=" + data.callerSocketId + ", callerId=" + data.callerId);
      routeToCaller(data, "hang_up_call");
    });


  });
};
