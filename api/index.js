/**
 * Central API Assembly & Connection Router for BharatConnect
 * Bridges Frontend HTTP/WS requests, Backend Services, and Database layer.
 */
const express = require('express');
const http = require('http');
const cors = require('cors');

const routes = require('./routes');
const eventDispatcher = require('./events');
const wsGateway = require('../backend/wsGateway');

function createApiServer(port = 5000) {
  const app = express();
  app.use(cors());
  app.use(express.json());

  // Mount API REST Routes
  app.use('/api/v1', routes);

  // Healthcheck Endpoint
  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: 'BharatConnect API Gateway', timestamp: new Date().toISOString() });
  });

  const server = http.createServer(app);

  // Initialize Realtime WebSocket Gateway & Event Dispatcher
  wsGateway.initialize(server);
  eventDispatcher.initialize();

  return { app, server, port };
}

module.exports = { createApiServer };
