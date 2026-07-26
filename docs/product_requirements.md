# Product Requirements Document (PRD) - BharatSphere

BharatSphere is a unified social, communication, and local utility application that combines features from multiple major platforms into a single, cohesive experience.

---

## 1. Core Feature Specification

### 1.1 User Authentication & Profile
- **Registration Inputs**: Username, Mobile Number, Email, Password, and Location Coordinates (GPS/Browser geolocation).
- **Unique ID Generation**:
  - Automatically generated upon registration.
  - Exactly **10 characters** long.
  - Alphanumeric combination (e.g., `BS9K8J2L4A`).
  - Must be guaranteed unique across the platform.
- **Password Strength Rules**:
  - Minimum **8 characters** long.
  - Must contain at least one uppercase letter (`A-Z`).
  - Must contain at least one lowercase letter (`a-z`).
  - Must contain at least one digit (`0-9`).
- **Date, Time & Day**:
  - Displayed permanently at the top of the application home screen (e.g., *Monday, July 20, 2026, 12:45 AM*), updated in real-time.

### 1.2 Direct Messaging & Contact Sync (WhatsApp Style)
- **Mutual Friendship System**:
  - Users cannot message each other out-of-the-box.
  - Chatting can only be initiated once a mutual friendship request is accepted (`A` requests `B`, `B` accepts `A`).
- **Initiating Chat**:
  - Users can look up potential friends/contacts by **Mobile Number** or **Username**.
- **Contact Syncing**:
  - Upon app load, the client requests permission to sync contacts.
  - In our web sandbox, this is simulated using a **Mock Phone Book** importer (or loading pre-populated simulated contacts) that matches contact numbers against registered user phone numbers to identify who is already on BharatSphere.
- **Chat Experience**:
  - Real-time text messages.
  - Media sharing (images, videos) with smart compression.

### 1.3 Groups & Communities (Telegram/WhatsApp Style)
- **Groups**:
  - Multi-user chat rooms.
  - **Public Groups**: Discoverable via global and nearby search. Anyone can join.
  - **Private Groups**: Hidden from search. Accessible only via invitation link or admin add.
- **Communities**:
  - Organizational umbrellas that contain multiple sub-groups (e.g., a "Neighborhood" community containing "General Chat", "Security Notifications", and "Buy/Sell" subgroups).
  - Admin controls for announcements.

### 1.4 Hyper-local Marketplace
- **Listing Types**:
  - **Gigs**: Short-term freelance services (e.g., dog walking, plumbing, coding).
  - **Jobs**: Full/part-time employment listings.
  - **Products**: Items for sale or rent.
  - **Emergency Requests**: Urgent calls for help (e.g., blood donation, mechanical breakdown, natural disaster assistance).
- **Metadata**:
  - Title, description, photos, price/compensation, category, user ID, contact info, and precise geocoordinates (latitude & longitude).

### 1.5 Geolocation & Nearby Explorer
- **Nearby Search Radius**:
  - Users can view a feed of nearby items sorted by distance.
  - Query items: public users, active communities, public groups, emergency requests, jobs, gigs, and products.
- **Visual Radar**:
  - An interactive map or list view indicating items within a configurable radius (e.g., 1 km, 5 km, 10 km, 50 km).
- **Proximity calculation**: Calculated dynamically using coordinate math (Haversine formula).

### 1.6 Unified Social Feed (Instagram/Facebook/X Style)
- **Post Composition**: Text, image uploads, and video uploads.
- **Home Feed**:
  - Aggregated feed showing posts from friends, public accounts, and local announcements.
  - Chronological or engagement-weighted sorting.

---

## 2. Advanced Technical Algorithms

### 2.1 Smart Storage Algorithm
To prevent server storage exhaustion from large images/videos:
1. **Client-Side Image Optimization**:
   - Before upload, client-side JavaScript reads image files via the HTML5 FileReader and draws them onto an offscreen `<canvas>`.
   - The canvas resizes images above 1920px width/height and compresses them into `image/webp` (target quality 0.75), reducing size by up to 80-90% before sending any bytes over the network.
2. **Backend Deduplication (Content-Addressable Storage)**:
   - When a media file is uploaded to the backend, the server computes its cryptographic hash (SHA-256) before writing it to disk.
   - The database maps the file hash to the storage path.
   - If another user uploads an identical file (e.g., forwarding a popular video/meme in a group chat), the backend detects the duplicate hash, deletes the newly uploaded file, and points the database record to the existing file path.
3. **Lazy Archiving**:
   - Chat histories and attachments older than 30 days are flagged as cold data.
   - Text is indexed locally, but large media files are loaded on demand and cached locally with a limited maximum cache size.

### 2.2 Geolocation Proximity Algorithm (Haversine Bounding Box)
To find items within \(R\) kilometers of latitude \(\phi_0\) and longitude \(\lambda_0\) efficiently without checking every database row:
1. **Bounding Box Pre-filtering**:
   - Compute coordinate boundaries (delta latitude and delta longitude):
     \[
     \Delta \phi = \frac{R}{111.045}
     \]
     \[
     \Delta \lambda = \frac{R}{111.045 \times \cos(\phi_0)}
     \]
   - Execute a highly efficient database indexed range query:
     ```sql
     SELECT * FROM items 
     WHERE latitude BETWEEN (\phi_0 - \Delta \phi) AND (\phi_0 + \Delta \phi)
       AND longitude BETWEEN (\lambda_0 - \Delta \lambda) AND (\lambda_0 + \Delta \lambda);
     ```
2. **Haversine Distance Sorting**:
   - Calculate exact distances in code or SQLite custom SQL for the filtered subset:
     \[
     d = 2r \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)
     \]
   - Return sorted results.
