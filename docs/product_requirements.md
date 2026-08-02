# Product Requirements Document (PRD) — BharatConnect (Python Edition)

BharatConnect is a modern production text messaging platform and developer hub built from first principles in **Python** using **Kivy**, **KivyMD**, and **FastAPI**.

---

## 1. Core Feature Specification

### 1.1 Multi-User Identity & Profile Management
- **Instant Identity Switcher**:
  - Test multi-user real-time interaction across pre-configured developer identities:
    - `@vipin_k`: Vipin Kumar (Senior Architect & Core Developer) — `ONLINE`
    - `@rahul_dev`: Rahul Sharma (Fullstack Engineer) — `ONLINE`
    - `@priya_design`: Priya Patel (Lead Product Designer) — `IDLE`
    - `@ananya_pm`: Ananya Verma (Product Manager) — `OFFLINE`
- **Profile Customization**:
  - Editable display name, bio, status message, phone number, and location.

### 1.2 Sub-50ms Realtime Text Messaging & Chat Stream
- **Direct & Group Chats**:
  - Direct 1-on-1 chats and group team channels.
- **Rich Messaging Experience**:
  - Speech bubble timeline (sent vs received styling, timestamps, `✓✓` read checkmarks).
  - Quick emoji reactions (`🇮🇳`, `🚀`, `👍`, `❤️`, `💻`, `🔥`).
  - Real-time typing indicators.
  - Message search filter and instant auto-reply simulation for testing.

### 1.3 Tech Communities Hub 🇮🇳
- **Indian Developer & Startup Hubs**:
  - Tech Innovators India 🇮🇳, Python & Kivy Developers, Startup Founders Bharat, UI/UX Design Systems India.
- **Interactive Membership**:
  - 1-click Join/Leave actions with live member counts.

### 1.4 Local Tech Marketplace & Proximity Explorer
- **Marketplace Listings**:
  - Developer hardware gear and freelance technical services.
  - 1-click "Contact Seller via Direct Chat" trigger.
- **Nearby Developers Proximity**:
  - Discover tech professionals across major Indian tech hubs (Bengaluru, Mumbai, Gurgaon, Hyderabad) with distance indicators (e.g. `1.2 km`).

---

## 2. Non-Functional Requirements

- **Technology Stack**: 100% Python (Python 3.11+, Kivy 2.3+, KivyMD 2.0+, FastAPI 0.100+).
- **Performance**: Sub-50ms message dispatches using in-memory $O(1)$ Hash Map indexed storage engine.
- **Cross-Platform**: Runs natively on Windows, macOS, Linux, Android, and iOS via Kivy.
- **One-Click Launch**: Single-click launch via `start.bat` or `python main.py`.
