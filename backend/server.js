/**
 * BharatConnect Backend Master Runner Server
 * Imports API Gateway assembly, connects database & WebSocket engine.
 */
const { createApiServer } = require('../api');

const PORT = process.env.PORT || 5000;
const { server } = createApiServer(PORT);

server.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`🚀 BharatConnect Backend & API Engine Running`);
  console.log(`📡 REST API Base: http://localhost:${PORT}/api/v1`);
  console.log(`⚡ Realtime WebSocket Gateway: ws://localhost:${PORT}`);
  console.log(`=======================================================`);
});
