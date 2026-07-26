# BharatConnect Frontend Web Application

The **`frontend/`** directory contains the client-side single page web application for BharatConnect built with **React**, **Vite**, and **Vanilla CSS**.

---

## 🔌 Connection to API Gateway

The frontend connects directly to the **`api/`** layer:
- **REST Endpoints**: `http://localhost:5000/api/v1/...` (routed via `api/routes.js`)
- **WebSocket Gateway**: `ws://localhost:5000` (routed via `api/events.js` & `backend/wsGateway.js`)

---

## 🎨 Design System & Features

- **Glassmorphism Theme**: Custom HSL dark and light mode themes with `backdrop-filter: blur(16px)` and subtle glowing accents.
- **Identity Tester Switcher**: Dropdown in the sidebar to toggle active account between mock users (`Vipin`, `Rahul`, `Priya`, `Ananya`) to test multi-user real-time chats locally.
- **Realtime Messaging**: Sequence-ordered feed, single/double/blue ticks (`SENT`, `DELIVERED`, `READ`), live typing indicators, replies, edits, deletions.
- **Filter Pills**: Quick list filters (`ALL`, `DIRECT`, `GROUPS`, `UNREAD`, `PINNED`).
- **Modals**: User Discovery (`UserDiscoveryModal`), Group Creation (`GroupModal`), Settings & Privacy (`SettingsModal`).

---

## 🚀 Commands

```bash
# Install packages
npm install

# Start Vite dev server
npm run dev

# Build production bundle
npm run build
```
- App URL: `http://localhost:5173`
