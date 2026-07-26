/**
 * API REST Router for BharatConnect
 * Connects HTTP requests to Backend Business Services.
 */
const express = require('express');
const router = express.Router();
const chatService = require('../backend/services/chatService');

// Auth Endpoints
router.post('/auth/login', async (req, res) => {
  try {
    const { username } = req.body;
    const user = await chatService.authenticateUser(username);
    res.json({
      token: `jwt_token_${user.user_id}_${Date.now()}`,
      user
    });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// User Endpoints
router.get('/users/me', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const user = await chatService.getCurrentUser(userId);
    res.json(user);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/users/search', async (req, res) => {
  try {
    const query = req.query.q || '';
    const results = await chatService.searchUsers(query);
    res.json(results);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Chat Endpoints
router.get('/chats', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const chats = await chatService.getUserChats(userId);
    res.json(chats);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post('/chats/direct', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const { target_user_id } = req.body;
    const chat = await chatService.createDirectChat(userId, target_user_id);
    res.json(chat);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.post('/chats/group', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const { title, description, participant_ids } = req.body;
    const group = await chatService.createGroupChat(userId, title, description, participant_ids);
    res.json(group);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Message History Endpoints
router.get('/chats/:chatId/messages', async (req, res) => {
  try {
    const { chatId } = req.params;
    const messages = await chatService.getMessages(chatId);
    res.json(messages);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
