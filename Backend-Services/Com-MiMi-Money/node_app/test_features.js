/**
 * Comprehensive Test Script for MiMi Chat Features
 * Tests: Messaging (text, image, video, audio, PDF), Status updates, Voice/Video calls
 * 
 * Configure three disposable local test users through environment variables.
 */

const io = require('socket.io-client');
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const APP_KEY_SECRET = process.env.APP_KEY_SECRET || 'change-me-local-only';
const SOCKET_URL = process.env.SOCKET_URL || 'http://127.0.0.1:9001';
const API_URL = process.env.API_URL || 'http://127.0.0.1:8080';

// Disposable local test users; never point this test at production data.
const DEVICES = {
  samsung: { id: Number(process.env.TEST_USER_1_ID || 1), wallet: process.env.TEST_USER_1_WALLET || 'local-wallet-1', token: process.env.TEST_USER_1_TOKEN || 'local-token-1', name: 'User1' },
  techno: { id: Number(process.env.TEST_USER_2_ID || 2), wallet: process.env.TEST_USER_2_WALLET || 'local-wallet-2', token: process.env.TEST_USER_2_TOKEN || 'local-token-2', name: 'User2' },
  pixel: { id: Number(process.env.TEST_USER_3_ID || 3), wallet: process.env.TEST_USER_3_WALLET || 'local-wallet-3', token: process.env.TEST_USER_3_TOKEN || 'local-token-3', name: 'User3' }
};

let testResults = {
  messaging: { text: {}, image: {}, video: {}, audio: {}, pdf: {} },
  status: { online: {}, delivered: {}, seen: {}, downloaded: {} },
  calls: { voice: {}, video: {} },
  socket: {}
};

function log(section, test, status, message = '') {
  const timestamp = new Date().toISOString().split('T')[1].split('.')[0];
  const symbol = status === 'PASS' ? '✅' : status === 'FAIL' ? '❌' : '⚠️';
  console.log(`[${timestamp}] [${section}] ${symbol} ${test}: ${message}`);
  if (!testResults[section]) testResults[section] = {};
  testResults[section][test] = { status, message, timestamp };
}

function createSocketClient(device) {
  return new Promise((resolve, reject) => {
    const socket = io(SOCKET_URL, {
      query: { token: APP_KEY_SECRET },
      transports: ['websocket'],
      reconnection: false,
      timeout: 10000
    });

    socket.on('connect', () => {
      log('socket', `${device.name}_connect`, 'PASS', `Socket ID: ${socket.id}`);
      resolve(socket);
    });

    socket.on('connect_error', (err) => {
      log('socket', `${device.name}_connect`, 'FAIL', err.message);
      reject(err);
    });

    setTimeout(() => reject(new Error('Connection timeout')), 10000);
  });
}

function connectDevice(device) {
  return new Promise((resolve, reject) => {
    createSocketClient(device).then(socket => {
      const handlers = {
        onMessage: null,
        onDelivered: null,
        onSeen: null,
        onDownloaded: null,
        onCall: null,
        onIceServers: null,
        onSignaling: null
      };

      socket.on('socket_new_message_server', (data) => {
        if (handlers.onMessage) handlers.onMessage(data);
      });

      socket.on('socket_delivered', (data) => {
        if (handlers.onDelivered) handlers.onDelivered(data);
      });

      socket.on('socket_seen', (data) => {
        if (handlers.onSeen) handlers.onSeen(data);
      });

      socket.on('socket_downloaded', (data) => {
        if (handlers.onDownloaded) handlers.onDownloaded(data);
      });

      socket.on('receive_new_call', (data) => {
        if (handlers.onCall) handlers.onCall(data);
      });

      socket.on('socket_get_ice_servers', (data, callback) => {
        if (handlers.onIceServers) handlers.onIceServers(data, callback);
      });

      socket.on('signaling_server', (data) => {
        if (handlers.onSignaling) handlers.onSignaling(data);
      });

      // Send connection event
      socket.emit('socket_user_connect', {
        connectedId: device.id,
        connected: true,
        userToken: device.token,
        socketId: socket.id
      });

      resolve({
        device,
        socket,
        handlers,
        messageIdCounter: 1
      });

    }).catch(reject);
  });
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function testTextMessaging(sender, receiver) {
  const testName = `text_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('messaging', testName, 'FAIL', 'Timeout waiting for message');
      resolve(false);
    }, 15000);

    receiver.handlers.onMessage = (data) => {
      clearTimeout(timeout);
      if (data.messageBody === 'Test text message' && data.senderId === sender.device.id) {
        log('messaging', testName, 'PASS', `Message received: "${data.messageBody}"`);
        resolve(true);
      } else {
        log('messaging', testName, 'FAIL', 'Message content mismatch');
        resolve(false);
      }
    };

    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: 'Test text message',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: 'null',
      video: 'null',
      audio: 'null',
      document: 'null',
      thumbnail: 'null'
    };

    sender.socket.emit('socket_save_new_message', messageData);
  });
}

async function testImageMessaging(sender, receiver) {
  const testName = `image_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  const imageHash = 'test_image_' + Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('messaging', testName, 'FAIL', 'Timeout waiting for image');
      resolve(false);
    }, 15000);

    receiver.handlers.onMessage = (data) => {
      clearTimeout(timeout);
      if (data.image && data.image !== 'null' && data.senderId === sender.device.id) {
        log('messaging', testName, 'PASS', `Image received: ${data.image}`);
        resolve(true);
      } else {
        log('messaging', testName, 'FAIL', 'Image not received properly');
        resolve(false);
      }
    };

    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: '',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: imageHash + '.jpg',
      video: 'null',
      audio: 'null',
      document: 'null',
      thumbnail: 'null',
      fileSize: '1024'
    };

    sender.socket.emit('socket_save_new_message', messageData);
  });
}

async function testVideoMessaging(sender, receiver) {
  const testName = `video_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  const videoHash = 'test_video_' + Date.now();
  const thumbnailHash = 'test_thumb_' + Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('messaging', testName, 'FAIL', 'Timeout waiting for video');
      resolve(false);
    }, 15000);

    receiver.handlers.onMessage = (data) => {
      clearTimeout(timeout);
      if (data.video && data.video !== 'null' && data.thumbnail && data.thumbnail !== 'null') {
        log('messaging', testName, 'PASS', `Video: ${data.video}, Thumbnail: ${data.thumbnail}`);
        resolve(true);
      } else {
        log('messaging', testName, 'FAIL', 'Video/thumbnail not received');
        resolve(false);
      }
    };

    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: '',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: 'null',
      video: videoHash + '.mp4',
      audio: 'null',
      document: 'null',
      thumbnail: thumbnailHash + '.jpg',
      duration: '30',
      fileSize: '5120'
    };

    sender.socket.emit('socket_save_new_message', messageData);
  });
}

async function testAudioMessaging(sender, receiver) {
  const testName = `audio_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  const audioHash = 'test_audio_' + Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('messaging', testName, 'FAIL', 'Timeout waiting for audio');
      resolve(false);
    }, 15000);

    receiver.handlers.onMessage = (data) => {
      clearTimeout(timeout);
      if (data.audio && data.audio !== 'null' && data.duration) {
        log('messaging', testName, 'PASS', `Audio: ${data.audio}, Duration: ${data.duration}s`);
        resolve(true);
      } else {
        log('messaging', testName, 'FAIL', 'Audio not received');
        resolve(false);
      }
    };

    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: '',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: 'null',
      video: 'null',
      audio: audioHash + '.mp3',
      document: 'null',
      thumbnail: 'null',
      duration: '15',
      fileSize: '500'
    };

    sender.socket.emit('socket_save_new_message', messageData);
  });
}

async function testPDFMessaging(sender, receiver) {
  const testName = `pdf_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  const docHash = 'test_doc_' + Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('messaging', testName, 'FAIL', 'Timeout waiting for PDF');
      resolve(false);
    }, 15000);

    receiver.handlers.onMessage = (data) => {
      clearTimeout(timeout);
      if (data.document && data.document !== 'null') {
        log('messaging', testName, 'PASS', `Document: ${data.document}`);
        resolve(true);
      } else {
        log('messaging', testName, 'FAIL', 'Document not received');
        resolve(false);
      }
    };

    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: '',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: 'null',
      video: 'null',
      audio: 'null',
      document: docHash + '.pdf',
      thumbnail: 'null',
      fileSize: '2048'
    };

    sender.socket.emit('socket_save_new_message', messageData);
  });
}

async function testMessageDelivered(sender, receiver) {
  const testName = `delivered_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('status', testName, 'FAIL', 'Timeout waiting for delivered status');
      resolve(false);
    }, 10000);

    sender.handlers.onDelivered = (data) => {
      clearTimeout(timeout);
      if (data.messageId == messageId) {
        log('status', testName, 'PASS', `Message ${messageId} marked as delivered`);
        resolve(true);
      }
    };

    // First send a message
    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: 'Test delivered status',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: 'null',
      video: 'null',
      audio: 'null',
      document: 'null',
      thumbnail: 'null'
    };

    sender.socket.emit('socket_save_new_message', messageData);

    // Receiver marks as delivered
    // Note: recipientId in socket_delivered should be the SENDER's ID (who gets notified)
    // senderId should be the RECEIVER's ID (who is marking as delivered)
    receiver.handlers.onMessage = (data) => {
      setTimeout(() => {
        receiver.socket.emit('socket_delivered', {
          messageId: data.messageId,
          senderId: receiver.device.id,  // The one marking as delivered
          recipientId: sender.device.id   // The one to notify (original sender)
        });
      }, 100);
    };
  });
}

async function testMessageSeen(sender, receiver) {
  const testName = `seen_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('status', testName, 'FAIL', 'Timeout waiting for seen status');
      resolve(false);
    }, 10000);

    sender.handlers.onSeen = (data) => {
      clearTimeout(timeout);
      if (data.senderId === receiver.device.id) {
        log('status', testName, 'PASS', `Message marked as seen`);
        resolve(true);
      }
    };

    // First send a message
    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: 'Test seen status',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: 'null',
      video: 'null',
      audio: 'null',
      document: 'null',
      thumbnail: 'null'
    };

    sender.socket.emit('socket_save_new_message', messageData);

    // Receiver marks as seen
    // Note: recipientId should be the SENDER's ID (who gets notified)
    // senderId should be the RECEIVER's ID (who is marking as seen)
    receiver.handlers.onMessage = (data) => {
      setTimeout(() => {
        receiver.socket.emit('socket_seen', {
          messageId: data.messageId,
          senderId: receiver.device.id,  // The one marking as seen
          recipientId: sender.device.id   // The one to notify (original sender)
        });
      }, 100);
    };
  });
}

async function testMessageDownloaded(sender, receiver) {
  const testName = `downloaded_${sender.device.name}_to_${receiver.device.name}`;
  const messageId = Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('status', testName, 'FAIL', 'Timeout waiting for downloaded status');
      resolve(false);
    }, 10000);

    sender.handlers.onDownloaded = (data) => {
      clearTimeout(timeout);
      if (data.senderId === receiver.device.id) {
        log('status', testName, 'PASS', `Message marked as downloaded`);
        resolve(true);
      }
    };

    // Send a file message
    const messageData = {
      recipientId: receiver.device.id,
      messageId: messageId,
      messageBody: '',
      senderId: sender.device.id,
      senderName: sender.device.name,
      date: new Date().toISOString(),
      isGroup: false,
      image: 'test_image.jpg',
      video: 'null',
      audio: 'null',
      document: 'null',
      thumbnail: 'null',
      fileSize: '1024'
    };

    sender.socket.emit('socket_save_new_message', messageData);

    // Receiver marks as downloaded
    receiver.handlers.onMessage = (data) => {
      setTimeout(() => {
        receiver.socket.emit('socket_downloaded', {
          messageId: data.messageId,
          senderId: receiver.device.id,
          recipientId: sender.device.id
        });
      }, 100);
    };
  });
}

async function testOnlineStatus(sender, receiver) {
  const testName = `online_${receiver.device.name}_status`;
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('status', testName, 'FAIL', 'Timeout waiting for online status');
      resolve(false);
    }, 10000);

    sender.socket.emit('socket_is_online', { userId: receiver.device.id });

    sender.socket.on('socket_is_online', function handler(data) {
      if (data.userId === receiver.device.id) {
        clearTimeout(timeout);
        sender.socket.off('socket_is_online', handler);
        if (data.connected) {
          log('status', testName, 'PASS', `${receiver.device.name} is online`);
          resolve(true);
        } else {
          log('status', testName, 'FAIL', `${receiver.device.name} is offline`);
          resolve(false);
        }
      }
    });
  });
}

async function testVoiceCall(caller, receiver) {
  const testName = `voice_call_${caller.device.name}_to_${receiver.device.name}`;
  const callId = 'call_' + Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('calls', testName, 'FAIL', 'Timeout waiting for call signal');
      resolve(false);
    }, 15000);

    let steps = { callReceived: false, iceReceived: false };

    receiver.handlers.onCall = (data) => {
      if (data.callerID === caller.device.id && !data.isVideoCall) {
        steps.callReceived = true;
        log('calls', `${testName}_signal`, 'PASS', 'Voice call signal received');
        
        // Simulate accepting call
        receiver.socket.emit('accept_new_call', {
          callerSocketId: caller.socket.id,
          userSocketId: receiver.socket.id
        });

        // Simulate signaling
        setTimeout(() => {
          receiver.socket.emit('signaling_server', {
            to: caller.socket.id,
            from: receiver.socket.id,
            type: 'init',
            payload: {}
          });
        }, 200);
      }
    };

    caller.handlers.onSignaling = (data) => {
      if (data.type === 'init') {
        steps.iceReceived = true;
        clearTimeout(timeout);
        log('calls', `${testName}_signaling`, 'PASS', 'Signaling exchange successful');
        log('calls', testName, 'PASS', 'Voice call flow completed');
        resolve(true);
      }
    };

    // Initiate call
    caller.socket.emit('make_new_call', {
      to: receiver.socket.id,
      callerPhone: caller.device.wallet,
      callerImage: 'null',
      from: caller.device.wallet,
      callerID: caller.device.id,
      isVideoCall: false,
      callId: callId
    });
  });
}

async function testVideoCall(caller, receiver) {
  const testName = `video_call_${caller.device.name}_to_${receiver.device.name}`;
  const callId = 'video_call_' + Date.now();
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('calls', testName, 'FAIL', 'Timeout waiting for video call signal');
      resolve(false);
    }, 15000);

    let steps = { callReceived: false, signalingReceived: false };

    receiver.handlers.onCall = (data) => {
      if (data.callerID === caller.device.id && data.isVideoCall) {
        steps.callReceived = true;
        log('calls', `${testName}_signal`, 'PASS', 'Video call signal received');
        
        // Simulate accepting call
        receiver.socket.emit('accept_new_call', {
          callerSocketId: caller.socket.id,
          userSocketId: receiver.socket.id
        });

        // Simulate signaling
        setTimeout(() => {
          receiver.socket.emit('signaling_server', {
            to: caller.socket.id,
            from: receiver.socket.id,
            type: 'init',
            payload: {}
          });
        }, 200);
      }
    };

    caller.handlers.onSignaling = (data) => {
      if (data.type === 'init') {
        steps.signalingReceived = true;
        clearTimeout(timeout);
        log('calls', `${testName}_signaling`, 'PASS', 'Video signaling exchange successful');
        log('calls', testName, 'PASS', 'Video call flow completed');
        resolve(true);
      }
    };

    // Initiate video call
    caller.socket.emit('make_new_call', {
      to: receiver.socket.id,
      callerPhone: caller.device.wallet,
      callerImage: 'null',
      from: caller.device.wallet,
      callerID: caller.device.id,
      isVideoCall: true,
      callId: callId
    });
  });
}

async function testIceServers(device) {
  const testName = `ice_servers_${device.device.name}`;
  
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      log('calls', testName, 'FAIL', 'Timeout getting ICE servers');
      resolve(false);
    }, 10000);

    device.socket.emit('socket_get_ice_servers', {}, (response) => {
      clearTimeout(timeout);
      if (response && response.success && response.iceServers && response.iceServers.length > 0) {
        const hasTurn = response.iceServers.some(s => s.urls && s.urls.includes('turn:'));
        const hasStun = response.iceServers.some(s => s.urls && s.urls.includes('stun:'));
        log('calls', testName, 'PASS', 
          `Got ${response.iceServers.length} ICE servers (STUN: ${hasStun}, TURN: ${hasTurn})`);
        resolve(true);
      } else {
        log('calls', testName, 'FAIL', 'No ICE servers returned');
        resolve(false);
      }
    });
  });
}

function printSummary() {
  console.log('\n' + '='.repeat(80));
  console.log('TEST SUMMARY');
  console.log('='.repeat(80));

  const categories = ['socket', 'messaging', 'status', 'calls'];
  let total = { pass: 0, fail: 0, warn: 0 };

  categories.forEach(cat => {
    if (!testResults[cat]) return;
    console.log(`\n[${cat.toUpperCase()}]`);
    Object.entries(testResults[cat]).forEach(([test, result]) => {
      if (result.message === undefined) return; // Skip placeholder entries
      const symbol = result.status === 'PASS' ? '✅' : result.status === 'FAIL' ? '❌' : '⚠️';
      console.log(`  ${symbol} ${test}: ${result.message || result.status}`);
      if (result.status === 'PASS') total.pass++;
      else if (result.status === 'FAIL') total.fail++;
      else total.warn++;
    });
  });

  console.log('\n' + '-'.repeat(80));
  console.log(`TOTAL: ✅ ${total.pass} passed | ❌ ${total.fail} failed | ⚠️ ${total.warn} warnings`);
  console.log('='.repeat(80));
}

async function testOfflineMessageDelivery(sender, receiver, receiverClient) {
  const testName = `offline_msg_${sender.device.name}_to_${receiver.device.name}`;
  const mysql = require('mysql2/promise');
  
  return new Promise(async (resolve) => {
    const timeout = setTimeout(() => {
      log('messaging', testName, 'FAIL', 'Timeout waiting for offline message');
      resolve(false);
    }, 15000);

    try {
      // Create a test message directly in the database
      const pool = mysql.createPool({
        host: process.env.DB_HOST || '127.0.0.1',
        port: Number(process.env.DB_PORT || 3306),
        user: process.env.DB_USER || 'mimi_money',
        password: process.env.DB_PASSWORD || '',
        database: process.env.DB_NAME || 'mimi_money',
        charset: 'utf8mb4'
      });

      // Insert test message
      const [msgResult] = await pool.execute(
        'INSERT INTO wa_messages (message, UserID, groupID, Date, ConversationID, status, image, video, audio, document, thumbnail) VALUES (?, ?, 0, NOW(), 0, 1, "null", "null", "null", "null", "null")',
        ['Offline test message ' + Date.now(), sender.device.id]
      );
      const messageId = msgResult.insertId;

      // Create receipt with status=0 (pending)
      await pool.execute(
        'INSERT INTO wa_message_receipts (message_id, recipient_id, status) VALUES (?, ?, 0)',
        [messageId, receiver.device.id]
      );

      await pool.end();

      // Handler for missed messages when receiver reconnects
      const missedHandler = (data) => {
        clearTimeout(timeout);
        if (data.messages && data.messages.length > 0) {
          const found = data.messages.find(m => m.messageId === messageId);
          if (found) {
            log('messaging', testName, 'PASS', `Offline message delivered: "${found.messageBody}"`);
            // Acknowledge receipt
            receiverClient.socket.emit('socket_missed_messages_received', {
              messageIds: data.messages.map(m => m.messageId)
            });
            resolve(true);
            return;
          }
        }
        log('messaging', testName, 'FAIL', 'Offline message not found in missed messages');
        resolve(false);
      };

      receiverClient.socket.on('socket_missed_messages', missedHandler);

      // Simulate user reconnecting - emit connect event to trigger missed message fetch
      receiverClient.socket.emit('socket_user_connect', {
        connectedId: receiver.device.id,
        connected: true,
        userToken: receiver.device.token,
        socketId: receiverClient.socket.id
      });

    } catch (err) {
      clearTimeout(timeout);
      log('messaging', testName, 'FAIL', `Database error: ${err.message}`);
      resolve(false);
    }
  });
}

async function runTests() {
  console.log('='.repeat(80));
  console.log('MiMi Chat Feature Test Suite');
  console.log('='.repeat(80));
  console.log(`Started: ${new Date().toISOString()}`);
  console.log(`Devices: Samsung(ID:37), Techno(ID:38), Pixel(ID:39)`);
  console.log('='.repeat(80) + '\n');

  let samsung, techno, pixel;

  try {
    // Connect all devices
    console.log('[CONNECTION] Connecting devices...\n');
    [samsung, techno, pixel] = await Promise.all([
      connectDevice(DEVICES.samsung),
      connectDevice(DEVICES.techno),
      connectDevice(DEVICES.pixel)
    ]);
    await sleep(1000);

    // Test Socket Connections
    console.log('\n[SOCKET] Testing connections...\n');
    await Promise.all([
      testOnlineStatus(samsung, techno),
      testOnlineStatus(techno, pixel),
      testOnlineStatus(pixel, samsung)
    ]);

    // Test Messaging
    console.log('\n[MESSAGING] Testing message types...\n');
    
    // Text messages
    await testTextMessaging(samsung, techno);
    await testTextMessaging(techno, pixel);
    await testTextMessaging(pixel, samsung);
    await sleep(500);

    // Image messages
    await testImageMessaging(samsung, pixel);
    await testImageMessaging(pixel, techno);
    await sleep(500);

    // Video messages
    await testVideoMessaging(techno, samsung);
    await testVideoMessaging(samsung, pixel);
    await sleep(500);

    // Audio messages
    await testAudioMessaging(pixel, techno);
    await testAudioMessaging(techno, samsung);
    await sleep(500);

    // PDF messages
    await testPDFMessaging(samsung, techno);
    await testPDFMessaging(pixel, samsung);

    // Test Offline Message Delivery
    console.log('\n[MESSAGING] Testing offline message delivery...\n');
    await testOfflineMessageDelivery(samsung, techno, techno);
    await testOfflineMessageDelivery(pixel, samsung, samsung);

    // Test Status Updates
    console.log('\n[STATUS] Testing message status updates...\n');
    await testMessageDelivered(samsung, techno);
    await testMessageSeen(techno, pixel);
    await testMessageDownloaded(pixel, samsung);

    // Test ICE Servers
    console.log('\n[CALLS] Testing ICE server configuration...\n');
    await testIceServers(samsung);
    await testIceServers(techno);
    await testIceServers(pixel);

    // Test Voice Calls
    console.log('\n[CALLS] Testing voice calls...\n');
    await testVoiceCall(samsung, techno);
    await testVoiceCall(pixel, samsung);

    // Test Video Calls  
    console.log('\n[CALLS] Testing video calls...\n');
    await testVideoCall(techno, pixel);
    await testVideoCall(samsung, pixel);

  } catch (error) {
    console.error('\n[FATAL] Test execution error:', error.message);
  } finally {
    // Cleanup
    if (samsung) samsung.socket.disconnect();
    if (techno) techno.socket.disconnect();
    if (pixel) pixel.socket.disconnect();
  }

  printSummary();
}

runTests().catch(console.error);
