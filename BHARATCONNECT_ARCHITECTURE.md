# 🇮🇳 BharatConnect — Master System Architecture Specification
**Version:** 1.0.0  
**Target Platform:** Native Android (Kotlin + Jetpack Compose)  
**Backend Ecosystem:** Supabase (Postgres, Auth, Realtime, RLS) + Firebase (FCM) + Cloudinary (Media) + Google Sheets (Admin)

---

## 1. Executive Summary & Recommended Tech Stack

BharatConnect is designed as a high-performance, offline-first native Android social and messaging application. The architecture strictly decouples responsibilities across industry-standard cloud infrastructure while enforcing clean architecture principles on the mobile client.

### Core Stack Components

| Layer | Technology | Primary Responsibility |
| :--- | :--- | :--- |
| **Language** | Kotlin 1.9+ | Android application programming language |
| **UI Framework** | Jetpack Compose | Declarative UI components & state management |
| **Architecture** | MVVM + Clean Architecture | Strict separation of UI, Domain, and Data layers |
| **Build System** | Gradle (Kotlin DSL) | Dependency management & APK packaging |
| **Networking** | Ktor Client / Supabase Kotlin SDK | HTTP/REST communications & Supabase client calls |
| **Local Database** | Room Database | Offline persistence & local working cache |
| **Local Preferences** | DataStore (Preferences / Proto) | Encrypted session tokens & app preferences |
| **Image Loading** | Coil Compose | Asynchronous image caching and rendering |
| **Background Sync** | WorkManager | Guaranteed background sync & media retry queues |
| **Realtime Engine** | Supabase Realtime | Sub-second message & status change notifications |
| **Push Notifications** | Firebase Cloud Messaging (FCM) | Server-triggered device notifications |
| **Media Processing** | Cloudinary Android SDK | Image/video upload, compression, transformation |
| **Backend & DB** | Supabase (PostgreSQL + RLS) | Primary database, authentication, & authorization |
| **Admin & Reporting** | Google Sheets API | Out-of-band admin reporting, import/export only |

---

## 2. Service Responsibility Matrix

To prevent component conflict and maintain modularity, each service is assigned **ONE** strict responsibility:

```
                          ┌───────────────────────────┐
                          │    BharatConnect Client   │
                          │ (Android Kotlin Compose)  │
                          └─────────────┬─────────────┘
                                        │
           ┌────────────────────────────┼────────────────────────────┐
           ▼                            ▼                            ▼
┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
│       Supabase       │    │       Firebase       │    │      Cloudinary      │
├──────────────────────┤    ├──────────────────────┤    ├──────────────────────┤
│ • Auth Sessions      │    │ • FCM Push Tokens    │    │ • Media Processing   │
│ • PostgreSQL Data    │    │ • Android App Check  │    │ • CDN Media Delivery │
│ • Realtime Streams   │    │ • Hardware Safety    │    │ • Image Resizing     │
│ • RLS Security       │    └──────────────────────┘    │ • Video Encoding     │
│ • Edge Functions     │                                └──────────────────────┘
└──────────────────────┘

┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
│     Google Sheets    │    │        GitHub        │    │       Room DB        │
├──────────────────────┤    ├──────────────────────┤    ├──────────────────────┤
│ • Admin Reports      │    │ • Source Code        │    │ • Offline DB Copy    │
│ • Data Import/Export │    │ • Branch Strategy    │    │ • Pending Sync Queue │
│ • Manual Audits      │    │ • CI/CD Pipelines    │    │ • Draft Messages     │
└──────────────────────┘    └──────────────────────┘    └──────────────────────┘
```

### Strict Rules of Service Isolation
1. **Google Sheets IS NOT A PRODUCTION DATABASE.** Android clients must **NEVER** write application operational data (chats, users, posts) directly to Google Sheets. All runtime operations go to Supabase PostgreSQL.
2. **Firebase IS NOT THE PRIMARY DATABASE.** Firebase is restricted to FCM push notifications and device-level verification (App Check).
3. **Cloudinary STORES MEDIA, NOT DB RECORDS.** Cloudinary handles raw binary assets. Metadata (public IDs, URLs, dimensions, byte sizes) is stored in Supabase PostgreSQL.
4. **Supabase RLS ENFORCES AUTHORIZATION.** Client code must not bypass Row Level Security. Database credentials in the APK must always be public client keys (`anon` key).

---

## 3. Native Android Clean Architecture

The Android application follows a 4-tier Clean Architecture hierarchy:

```
app/
├── core/                         # Cross-cutting foundational modules
│   ├── network/                  # Ktor engine configuration & Supabase client wrapper
│   ├── database/                 # Room DB instance & encryption setup
│   ├── security/                 # Encrypted DataStore & Keystore management
│   ├── storage/                  # Cloudinary initialization & upload helpers
│   ├── notifications/            # FCM Service & local NotificationManager
│   └── common/                   # Global constants, Result wrappers, Extensions
│
├── data/                         # Data layer (Repositories implementation)
│   ├── remote/                   # Server API clients
│   │   ├── supabase/             # Supabase Auth, DB, and Realtime DTOs & calls
│   │   ├── firebase/             # FCM Token sync service
│   │   └── cloudinary/           # Cloudinary upload service
│   ├── local/                    # Local storage handlers
│   │   ├── room/                 # DAOs, Entities, and Room database definition
│   │   └── datastore/            # Encrypted preference accessors
│   └── repository/               # Repository implementations (Combining local & remote)
│
├── domain/                       # Pure Kotlin Domain business logic (No Android dependencies)
│   ├── model/                    # Domain entities (User, Message, Post, Media)
│   ├── repository/               # Repository interfaces (Contracts)
│   └── usecase/                  # Single-responsibility business logic handlers
│       ├── auth/                 # LoginUseCase, RegisterUseCase, LogoutUseCase
│       ├── chat/                 # SendMessageUseCase, ObserveMessagesUseCase
│       └── media/                # UploadMediaUseCase, ProcessImageUseCase
│
└── presentation/                 # Presentation layer (Jetpack Compose UI)
    ├── auth/                     # LoginScreen, RegisterScreen, ForgotPasswordScreen
    ├── home/                     # HomeFeedScreen, PostCard, FeedViewModel
    ├── chat/                     # ConversationListScreen, ChatRoomScreen, ChatViewModel
    ├── profile/                  # UserProfileScreen, EditProfileScreen, ProfileViewModel
    ├── media/                    # MediaPickerScreen, VideoPlayerView, MediaViewModel
    ├── notifications/            # NotificationCenterScreen
    ├── settings/                 # AppSettingsScreen
    ├── components/               # Shared Compose UI elements (TopBar, Buttons, Dialogs)
    └── MainActivity.kt           # Navigation Host & Activity Entry Point
```

---

## 4. Database Schema & Supabase Row Level Security (RLS)

### Core PostgreSQL Table Architecture

```sql
-- 1. Profiles Table (Linked to Supabase Auth Users)
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    phone_number TEXT UNIQUE,
    is_online BOOLEAN DEFAULT false,
    last_seen TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Conversations Table
CREATE TABLE public.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_group BOOLEAN DEFAULT false,
    title TEXT,
    created_by UUID REFERENCES public.profiles(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Conversation Members Table
CREATE TABLE public.conversation_members (
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    role TEXT DEFAULT 'member', -- 'admin', 'member'
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);

-- 4. Messages Table
CREATE TABLE public.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT,
    media_url TEXT,
    media_type TEXT, -- 'image', 'video', 'document', 'audio'
    status TEXT DEFAULT 'sent', -- 'sending', 'sent', 'delivered', 'read'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Media Attachments Metadata
CREATE TABLE public.media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES public.conversations(id),
    cloudinary_public_id TEXT NOT NULL,
    media_type TEXT NOT NULL,
    width INT,
    height INT,
    duration INT,
    file_size BIGINT,
    secure_url TEXT NOT NULL,
    thumbnail_url TEXT,
    status TEXT DEFAULT 'ready',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. FCM Device Tokens Table
CREATE TABLE public.device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    fcm_token TEXT UNIQUE NOT NULL,
    device_model TEXT,
    last_updated TIMESTAMPTZ DEFAULT NOW()
);
```

### Row Level Security (RLS) Rules

1. **Profiles:**
   - Anyone can view public profiles: `CREATE POLICY "Public profiles read" ON public.profiles FOR SELECT USING (true);`
   - Users can update only their own profile: `CREATE POLICY "User update self" ON public.profiles FOR UPDATE USING (auth.uid() = id);`

2. **Messages:**
   - Members can read messages in their conversations:
     ```sql
     CREATE POLICY "Members read conversation messages" ON public.messages
     FOR SELECT USING (
         EXISTS (
             SELECT 1 FROM public.conversation_members
             WHERE conversation_id = messages.conversation_id AND user_id = auth.uid()
         )
     );
     ```
   - Members can insert messages into their conversations:
     ```sql
     CREATE POLICY "Members send messages" ON public.messages
     FOR INSERT WITH CHECK (
         auth.uid() = sender_id AND
         EXISTS (
             SELECT 1 FROM public.conversation_members
             WHERE conversation_id = messages.conversation_id AND user_id = auth.uid()
         )
     );
     ```

---

## 5. Media Architecture & Upload State Machine

To prevent orphaned media assets and inconsistent UI states, media uploads are processed through a strictly defined state machine handled by Kotlin Coroutines & Cloudinary SDK:

```
[ SELECTED ] ──► [ VALIDATING ] ──► [ COMPRESSING ] ──► [ UPLOADING ]
                                                              │
                                                              ▼
[ READY ] ◄── [ DB_RECORD_CREATED ] ◄── [ UPLOADED ] ◄── [ PROCESSING ]
   │
   └─► Failure at any point ──► [ RETRY QUEUE / FAILED ]
```

### Upload Execution Flow
1. **SELECTED:** User picks photo/video from Compose image picker.
2. **VALIDATING:** Check file size limits (< 50MB for video, < 10MB for image) and MIME types.
3. **COMPRESSING:** Compress image/video locally on device via Kotlin background worker before upload.
4. **UPLOADING:** Stream compressed file to Cloudinary storage via Cloudinary Android SDK.
5. **PROCESSING:** Cloudinary applies auto-format (`f_auto`), quality optimization (`q_auto`), and thumbnail generation.
6. **UPLOADED:** Cloudinary returns `public_id`, `secure_url`, and `thumbnail_url`.
7. **DB_RECORD_CREATED:** Client writes metadata record to Supabase `media` and `messages` tables.
8. **READY:** UI updates status indicator to "Sent".

---

## 6. Offline-First Synchronization Architecture

BharatConnect operates on an **Offline-First** model. The local Room Database serves as the single source of truth for the UI layer, while Supabase PostgreSQL serves as the remote server truth.

```
┌─────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                     │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observes Flow / StateFlow
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        Room Database                        │
│                 (Local Working Copy / Cache)                │
└──────────────┬──────────────────────────────▲───────────────┘
               │                              │
               │ Outgoing Operations          │ Sync Inward
               ▼                              │
┌──────────────────────────────┐              │
│       Sync Manager /         ├──────────────┼──────────────┐
│         WorkManager          │              │              │
└──────────────┬───────────────┘              │              │
               │ HTTPS                        │ Realtime     │ FCM Push
               ▼                              │              │
┌──────────────────────────────┐              │              │
│       Supabase Backend       ├──────────────┘              │
│       (PostgreSQL DB)        ├─────────────────────────────┘
└──────────────────────────────┘
```

### Message Delivery Guarantee
1. **Send Message:** UI writes message into Room DB with `status = 'sending'`.
2. **UI Update:** Room updates Flow immediately; user sees message rendered instantly.
3. **Network Dispatch:** Sync Engine sends HTTP POST / Supabase call.
4. **On Success:** Server assigns server `id` and timestamp; Room DB reconciled to `status = 'sent'`.
5. **On Offline / Error:** Message remains in Room DB with `status = 'pending_sync'`. `WorkManager` retries automatically when connection is restored.

---

## 7. Firebase FCM Notification Architecture

Push notifications are delivered securely via FCM triggered by server-side events, keeping FCM secret keys off mobile devices:

```
[ User A Sends Message ]
         │
         ▼
[ Supabase DB Insert ]
         │
         ▼
[ Supabase Database Trigger / Edge Function ]
         │
         ▼ (Calls FCM HTTP v1 API with Secret Service Account Token)
[ Firebase Cloud Messaging (FCM) Server ]
         │
         ▼
[ User B Mobile Device (FCM Token) ]
         │
         ▼
[ Android FirebaseMessagingService Handler ] ──► [ Local Device Notification ]
```

---

## 8. Security & Secret Management Rules

1. **NO SECRET KEYS IN APK:**
   - `SUPABASE_SERVICE_ROLE_KEY` -> **NEVER** put inside Android project.
   - `CLOUDINARY_API_SECRET` -> **NEVER** put inside Android project.
   - `FIREBASE_SERVICE_ACCOUNT_JSON` -> Keep strictly on server / Edge Functions.
2. **ALLOWED IN APK:**
   - `SUPABASE_URL` & `SUPABASE_ANON_KEY` (Public Client Configuration).
   - `CLOUDINARY_CLOUD_NAME` & Signed Upload Preset configuration.
3. **ON-DEVICE SECURITY:**
   - Store access tokens in Encrypted DataStore using Android Keystore System.
   - Enforce HTTPS for all backend communications.

---

## 9. Controlled 14-Phase Implementation Roadmap

To avoid chaos, development strictly progresses in controlled, buildable, and testable phases:

```
Phase  1: Development Environment & Dependency Verification
Phase  2: Android Native Skeleton & Jetpack Compose Navigation Setup
Phase  3: Supabase Authentication Integration (Email, Password, Session Management)
Phase  4: Database Schema Deployment & Room Local Persistence Integration
Phase  5: User Profile & Account Settings Module
Phase  6: Social Feed & Posts Interactive Engine
Phase  7: Realtime Chat Engine (1-on-1 & Group Messaging)
Phase  8: Cloudinary Media Upload & Optimization Engine
Phase  9: Firebase FCM Push Notifications Integration
Phase 10: WorkManager Offline Synchronization & Retry Engine
Phase 11: Security Audit & Row Level Security (RLS) Hardening
Phase 12: Automated Unit & Integration Testing Suite
Phase 13: UI/UX Animation, Theme Polish & Performance Tuning
Phase 14: Release Build, APK Optimization & Infrastructure Scaling Prep
```

---

## 10. "DO NOT BREAK EXISTING SYSTEM" Constraints

1. **Preserve Current Codebase:** The existing Python/Kivy codebase (`main.py`, `app/`, `backend/`) remains fully functional and accessible during native Android Kotlin development.
2. **Non-Destructive Refactoring:** Do not delete or overwrite legacy backend or database structures until the new Kotlin native implementation is fully verified against live test cases.
3. **Controlled Incremental Steps:** Antigravity AI agent will execute task steps incrementally, executing builds and tests after every single feature phase.

---

## 11. Command Line Android Build & Hardware Testing Guide

Since development operates in a lightweight environment without Android Studio IDE:

### Build Commands (Terminal / Powershell)
```bash
# 1. Check Gradle wrapper version and dependencies
./gradlew --version

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Build Release APK
./gradlew assembleRelease
```

### Physical Android Device Testing (ADB)
```bash
# 1. List connected Android hardware devices
adb devices

# 2. Install compiled Debug APK to connected phone
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Stream real-time app logs from physical device
adb logcat -s "BharatConnect" "Supabase" "Cloudinary" "FCM"
```

---
*Specification standard established for BharatConnect Native Android Ecosystem.*
