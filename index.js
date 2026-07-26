/**
 * BharatConnect Root System Master Initiator
 * Single main file to initiate everything: Database, Backend, API Gateway & Realtime Engine.
 */
const { createApiServer } = require('./api');
const db = require('./database/db');

const PORT = process.env.PORT || 5000;

async function bootstrap() {
  console.log(`\n-------------------------------------------------------`);
  console.log(`🇮🇳 Initiating BharatConnect System Environment...`);
  console.log(`-------------------------------------------------------`);

  // Initialize Database Engine
  const users = await db.searchUsers('');
  console.log(`✅ [Database Engine] Initialized (${users.length} pre-configured active accounts)`);

  // Start Master API & WebSocket Gateway Server
  const { server } = createApiServer(PORT);

  server.listen(PORT, () => {
    console.log(`✅ [Backend & API Router] Server active on port ${PORT}`);
    console.log(`   ➜ REST API Base:        http://localhost:${PORT}/api/v1`);
    console.log(`   ➜ WebSocket Gateway:    ws://localhost:${PORT}`);
    console.log(`   ➜ API Healthcheck:      http://localhost:${PORT}/health`);
    console.log(`-------------------------------------------------------`);
    console.log(`💡 To launch the React frontend web interface:`);
    console.log(`   cd frontend && npm run dev`);
    console.log(`-------------------------------------------------------\n`);
  });
}

bootstrap().catch(err => {
  console.error('❌ Failed to initiate BharatConnect system:', err);
  process.exit(1);
});
