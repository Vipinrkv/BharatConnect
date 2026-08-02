/**
 * API REST Router for BharatConnect
 * Connects HTTP requests to Backend Business Services with speedup headers & standardized JSON responses.
 */
const express = require('express');
const router = express.Router();
const chatService = require('../backend/services/chatService');

// System Health & Telemetry Endpoint
router.get('/health', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'no-cache');
    const health = await chatService.getHealthStatus();
    res.json({ success: true, data: health });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Auth Endpoints
router.post('/auth/login', async (req, res) => {
  try {
    const { identifier, username } = req.body;
    const loginKey = identifier || username;
    const user = await chatService.authenticateUser(loginKey);
    res.json({
      success: true,
      token: `jwt_token_${user.user_id}_${Date.now()}`,
      user
    });
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

router.post('/auth/register', async (req, res) => {
  try {
    const user = await chatService.registerUser(req.body);
    res.json({
      success: true,
      token: `jwt_token_${user.user_id}_${Date.now()}`,
      user
    });
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

// User Endpoints
router.get('/users/me', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=5');
    const userId = req.headers['x-user-id'] || 'u-101';
    const user = await chatService.getCurrentUser(userId);
    res.json({ success: true, data: user, ...user });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.get('/users/search', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=3');
    const query = req.query.q || '';
    const results = await chatService.searchUsers(query);
    res.json(results);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Chat Endpoints
router.get('/chats', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=2');
    const userId = req.headers['x-user-id'] || 'u-101';
    const chats = await chatService.getUserChats(userId);
    res.json(chats);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/chats/direct', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const { target_user_id } = req.body;
    const chat = await chatService.createDirectChat(userId, target_user_id);
    res.json(chat);
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

router.post('/chats/group', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const { title, description, participant_ids } = req.body;
    const group = await chatService.createGroupChat(userId, title, description, participant_ids);
    res.json(group);
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

// Message History Endpoints
router.get('/chats/:chatId/messages', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=1');
    const { chatId } = req.params;
    const messages = await chatService.getMessages(chatId);
    res.json(messages);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Marketplace Endpoints
router.get('/marketplace', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=2');
    const category = req.query.category || 'ALL';
    const query = req.query.q || '';
    const listings = await chatService.getMarketplaceListings(category, query);
    res.json(listings);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/marketplace', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const listing = await chatService.createMarketplaceListing(userId, req.body);
    res.json(listing);
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

// Community Endpoints
router.get('/communities', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=3');
    const query = req.query.q || '';
    const communities = await chatService.getCommunities(query);
    res.json(communities);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/communities', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const community = await chatService.createCommunity(userId, req.body);
    res.json(community);
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

router.post('/communities/:id/join', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const community = await chatService.joinCommunity(userId, req.params.id);
    res.json(community);
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

// Nearby Endpoints
router.get('/nearby', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=2');
    const radius = req.query.radius || 5;
    const category = req.query.category || 'ALL';
    const query = req.query.q || '';
    const assets = await chatService.getNearbyAssets(radius, category, query);
    res.json(assets);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Posts Feed Endpoints
router.get('/posts', async (req, res) => {
  try {
    res.setHeader('Cache-Control', 'private, max-age=2');
    const category = req.query.category || 'ALL';
    const query = req.query.q || '';
    const posts = await chatService.getPosts(category, query);
    res.json(posts);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/posts', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const post = await chatService.createPost(userId, req.body);
    res.json(post);
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

router.post('/posts/:id/like', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] || 'u-101';
    const post = await chatService.likePost(userId, req.params.id);
    res.json(post);
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

module.exports = router;
