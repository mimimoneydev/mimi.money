var mysql = require('mysql2/promise');

var DB_CONFIG = {
  host: process.env.DB_HOST || '127.0.0.1',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'mimi_money',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'mimi_money',
  charset: 'utf8mb4',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
};

var pool = mysql.createPool(DB_CONFIG);

module.exports = {
  pool: pool,

  fetchMissedMessages: function(recipientId) {
    return pool.execute(
      'SELECT m.id as messageId, m.message as messageBody, m.image, m.video, ' +
      'm.thumbnail, m.audio, m.document, m.duration, m.fileSize, ' +
      'm.UserID as senderId, m.groupID, m.Date as date, ' +
      'm.ConversationID as conversationId, ' +
      'u.username as senderName, u.wallet_address, u.image as senderImage, ' +
      'mr.recipient_id, mr.id as receiptId, ' +
      'CASE WHEN m.groupID > 0 THEN 1 ELSE 0 END as isGroup ' +
      'FROM wa_message_receipts mr ' +
      'JOIN wa_messages m ON mr.message_id = m.id ' +
      'JOIN wa_users u ON m.UserID = u.id ' +
      'WHERE mr.recipient_id = ? AND mr.status = 0 ' +
      'ORDER BY m.Date ASC',
      [recipientId]
    ).then(function(results) {
      return results[0];
    });
  },

  markMessagesDelivered: function(recipientId, messageIds) {
    if (!messageIds || messageIds.length === 0) return Promise.resolve();
    var placeholders = messageIds.map(function() { return '?'; }).join(',');
    return pool.execute(
      'UPDATE wa_message_receipts SET status = 1 WHERE recipient_id = ? AND message_id IN (' + placeholders + ') AND status < 1',
      [recipientId].concat(messageIds)
    );
  },

  markMessagesDeliveredByReceiptIds: function(receiptIds) {
    if (!receiptIds || receiptIds.length === 0) return Promise.resolve();
    var placeholders = receiptIds.map(function() { return '?'; }).join(',');
    return pool.execute(
      'UPDATE wa_message_receipts SET status = 1 WHERE id IN (' + placeholders + ') AND status < 1',
      receiptIds
    );
  },

  markMessagesSeen: function(recipientId, senderId) {
    return pool.execute(
      'UPDATE wa_message_receipts mr ' +
      'JOIN wa_messages m ON mr.message_id = m.id ' +
      'SET mr.status = 2 ' +
      'WHERE mr.recipient_id = ? AND m.UserID = ? AND mr.status < 2',
      [recipientId, senderId]
    );
  },

  upsertMessageReceipt: function(messageId, recipientId, status) {
    return pool.execute(
      'INSERT INTO wa_message_receipts (message_id, recipient_id, status) VALUES (?, ?, ?) ' +
      'ON DUPLICATE KEY UPDATE status = GREATEST(status, ?)',
      [messageId, recipientId, status, status]
    ).then(function(results) {
      return results[0];
    }).catch(function(err) {
      return pool.execute(
        'UPDATE wa_message_receipts SET status = ? WHERE message_id = ? AND recipient_id = ? AND status < ?',
        [status, messageId, recipientId, status]
      );
    });
  },

  getSettings: function(names) {
    if (!Array.isArray(names)) names = [names];
    var placeholders = names.map(function() { return '?'; }).join(',');
    return pool.execute(
      'SELECT `name`, `value` FROM wa_settings WHERE `name` IN (' + placeholders + ')',
      names
    ).then(function(results) {
      var rows = results[0];
      var map = {};
      rows.forEach(function(row) { map[row.name] = row.value; });
      return map;
    });
  }
};
