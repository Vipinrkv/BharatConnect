# BharatConnect Frontend Web Application

The **`frontend/`** directory contains the client-side web application built with **React**, **Vite**, and **Vanilla CSS**.

---

## 🎨 UI Features & Design Aesthetics

- **Glassmorphism Theme**: Custom HSL dark and light mode themes with `backdrop-filter: blur(16px)` and subtle glowing accents.
- **Account Switcher**: Dropdown in the top-left sidebar header to switch active identity between test accounts (`Vipin`, `Rahul`, `Priya`, `Ananya`) for multi-user live testing.
- **Realtime Chat View**: Sequence-ordered feed, single/double/blue ticks (`SENT`, `DELIVERED`, `READ`), live typing indicators, replies, edits, deletions.
- **Filter Pills**: Quick list filters (`ALL`, `DIRECT`, `GROUPS`, `UNREAD`, `PINNED`).
- **Modals**: User Discovery (`UserDiscoveryModal`), Group Creation (`GroupModal`), Settings & Privacy (`SettingsModal`).

---

## 🔌 Connection to API Hub

The frontend connects directly to the **`api/`** layer:
- **REST Endpoints**: `http://localhost:5000/api/v1` (via `api/routes.js`)
- **WebSocket Gateway**: `ws://localhost:5000` (via `api/events.js` & `backend/wsGateway.js`)

---

## 🚀 Running Frontend Independently

While running `npm start` in the root folder launches the full system, you can also run the frontend independently:

```bash
cd frontend
npm install
npm run dev
```
- App URL: `http://localhost:5173`
