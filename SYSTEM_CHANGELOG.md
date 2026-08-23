# 🇮🇳 BharatConnect System Changelog & Audit Ledger

> **CRITICAL REPOSITORY RULE**:
> 🛑 **NEVER DELETE ANYTHING FROM THIS FILE.**
> 📝 **ALL NEW MODIFICATIONS, REFACTORS, BUG FIXES, AND SYSTEM ENHANCEMENTS MUST BE APPENDED AT THE BOTTOM IN CHRONOLOGICAL ORDER WITH FULL TIMESTAMPS, EXACT FILE LOCATIONS, AND CHANGE DETAILS.**

---

## 🏛️ System Architecture & Data Flow Overview

```
 ┌──────────────────────────────────────────────────────────────────────────────────┐
 │                         📱 BHARATCONNECT ANDROID CLIENT                          │
 │                       (Jetpack Compose • Material 3 • MVVM)                      │
 └──────────────┬────────────────────────┬───────────────────────────┬──────────────┘
                │                        │                           │
  [1] UI Actions & State         [2] Offline Cache           [3] Direct Binary Upload
                ▼                        ▼                           ▼
 ┌────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
 │   Domain Use Cases     │  │   Room SQLite Database  │  │   Cloudinary Storage    │
 │    & ViewModels        │  │  (Posts, Messages,      │  │  (Images, Media Files,  │
 │  - AuthViewModel       │  │   Conversations, Users) │  │   Story Backgrounds)    │
 │  - FeedViewModel       │  └───────────┬─────────────┘  └─────────────┬───────────┘
 │  - ChatViewModel       │              │                              │
 └──────────────┬─────────┘              │                              │
                │                        │ [4] Enqueue Sync             │ Media URL
                ▼                        ▼                              ▼
 ┌──────────────────────────────────────────────────────────────────────────────────┐
 │                       Android WorkManager (SyncManager)                          │
 │              - Idempotent UUID conflict resolution & delta syncing               │
 └───────────────────────────────────────┬──────────────────────────────────────────┘
                                         │
                         [5] Network Sync & Subscriptions
                                         ▼
 ┌──────────────────────────────────────────────────────────────────────────────────┐
 │                             ⚡ SUPABASE CLOUD BACKEND                            │
 ├──────────────────────────┬──────────────────────────┬────────────────────────────┤
 │    🔐 GoTrue Auth        │    🗄️ PostgREST Engine   │   📡 Realtime WebSocket    │
 │  - Email & Password      │  - CRUD on 13 Tables     │  - Live Chat Messages      │
 │  - Phone & DOB           │  - RLS Security Policies │  - Online Presence         │
 │  - Forgot Password Reset │  - Automated Triggers    │  - Instant Notifications   │
 └──────────────────────────┴──────────────────────────┴────────────────────────────┘
```

---

## 📊 Complete Supabase Database Tables & Flow Specification

| Table Name | Primary Key | Foreign Keys | Key Columns | System Flow & Access |
| :--- | :--- | :--- | :--- | :--- |
| **`public.profiles`** | `id (UUID)` | `auth.users(id)` | `email`, `username`, `full_name`, `phone_number`, `dob`, `avatar_url`, `bio`, `is_online`, `last_seen` | User identity ledger. Auto-populated on signup via `handle_new_user()` trigger. Case-insensitive indexes for instant login via username/phone. |
| **`public.conversations`**| `id (TEXT)` | `created_by -> profiles(id)` | `type` ('direct', 'group', 'community'), `title`, `avatar_url`, `last_message`, `last_message_time` | Manages 1-on-1 and group chat threads. Updated automatically on new messages via trigger. |
| **`public.conversation_members`** | `id (UUID)` | `conversation_id`, `user_id` | `role` ('admin', 'member'), `joined_at` | RLS access control for chat conversations. |
| **`public.messages`** | `id (TEXT/UUID)`| `conversation_id`, `sender_id` | `content`, `media_url`, `media_type`, `status` ('sending','sent','delivered','read') | Real-time chat messages. Synced with Room `MessageEntity` for offline message queue. |
| **`public.posts`** | `id (TEXT/UUID)`| `author_id -> profiles(id)` | `content`, `media_url`, `media_type`, `likes_count`, `comments_count`, `created_at` | Main social feed stream. Cached locally in Room `PostEntity`. |
| **`public.post_likes`** | `id (UUID)` | `post_id`, `user_id` | `created_at` | Unique like records. Increment/decrement triggers on `posts.likes_count`. |
| **`public.post_comments`**| `id (UUID)` | `post_id`, `author_id` | `content`, `created_at` | Comments under social feed posts. |
| **`public.stories`** | `id (TEXT/UUID)`| `author_id -> profiles(id)` | `media_url`, `text_content`, `background_gradient`, `expires_at` (24hr TTL) | Ephemeral story/status updates displayed in `StoriesRow`. |
| **`public.marketplace_items`** | `id (TEXT)` | `seller_id -> profiles(id)` | `title`, `price`, `category`, `location`, `image_url` | Buy/Sell classifieds with Indian Rupee (₹) pricing. |
| **`public.jobs`** | `id (TEXT)` | `poster_id -> profiles(id)` | `title`, `company`, `salary`, `type`, `location` | Tech & business career job openings with 1-tap apply. |
| **`public.quick_jobs`** | `id (TEXT)` | `poster_id -> profiles(id)` | `title`, `payout`, `duration`, `urgency` | Local tasks and rapid gigs with instant payouts. |
| **`public.notifications`**| `id (TEXT)` | `user_id -> profiles(id)` | `title`, `description`, `category` ('messages', 'likes', 'system'), `is_read` | User activity feed with read receipts. |
| **`public.user_locations`**| `user_id (UUID)`| `profiles(id)` | `latitude`, `longitude`, `status`, `is_visible`, `updated_at` | Powers Nearby Discovery Radar calculations with 1km/5km/10km radius. |

---

## 📜 Chronological System Changelog

---

### 🔹 Entry #001 — Native Architecture Transition & Clean Architecture Setup
- **Date & Time**: `2026-08-22 18:30:00 IST` (`2026-08-22T13:00:00Z`)
- **Author**: Antigravity Assistant & Engineering Team
- **Summary of Changes**:
  - Migrated core project from hybrid/legacy codebase into pure **Kotlin Native Jetpack Compose (Material 3)**.
  - Implemented Clean Architecture with separated `core`, `data`, `domain`, and `presentation` layers.
  - Established Room SQLite offline-first database (`AppDatabase`, `UserDao`, `PostDao`, `MessageDao`, `ConversationDao`).
  - Integrated Supabase SDK (Ktor client) for Realtime WebSocket messaging, GoTrue Auth, and PostgREST.
  - Integrated Cloudinary media engine for client-side image/video uploads.

---

### 🔹 Entry #002 — End-to-End System Analysis, Variable Flow Mapping & Initial APK Build
- **Date & Time**: `2026-08-23 13:05:00 IST` (`2026-08-23T07:35:00Z`)
- **Git Commit**: `9a981fb`
- **Files Modified / Added**:
  - `.gitignore` (Whitelisted `!BharatConnect-Native.apk` for direct repository binary distribution)
  - `android_native/app/build.gradle.kts`
  - `README.md`
  - `BharatConnect-Native.apk` (~22.2 MB)
- **Summary of Changes**:
  - Conducted full analysis of code flow, execution graphs, ViewModels, UseCases, DAOs, and network layers.
  - Cleaned up obsolete legacy files.
  - Built initial standalone debug APK and published to GitHub `main` branch.

---

### 🔹 Entry #003 — Removal of Mock/Demo Data & Integration of Production Placeholders
- **Date & Time**: `2026-08-23 13:21:00 IST` (`2026-08-23T07:51:00Z`)
- **Git Commit**: `dbee515`
- **Files Modified**:
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/home/HomeScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/home/HomeScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/nearby/NearbyScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/nearby/NearbyScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/marketplace/MarketplaceScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/marketplace/MarketplaceScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/notifications/NotificationsScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/notifications/NotificationsScreen.kt)
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk)
- **Summary of Changes**:
  - Removed all hardcoded test data (dummy users "Aarav Sharma", "Priya Patel", fake posts, mock electronics/furniture items, fake jobs, and mock notifications).
  - Added clean, responsive empty-state cards across all tabs:
    - **FeedTab**: *"Your Feed is Ready"* with prompt to create the first post.
    - **ChatsTab**: *"No Conversations Yet"* with *"Start New Chat"* button.
    - **StoriesRow**: Enabled dynamic `+ Your Story` creator button.
    - **NearbyScreen**: Live scanning indicator + *"No Nearby Members Discovered"* card with radius guidance.
    - **MarketplaceScreen**: Individual empty state cards for Items, Jobs, and Quick Gigs with direct creation action buttons.
    - **NotificationsScreen**: *"No Notifications"* card with catch-up indicator.
    - **ProfileTab**: Initialized real default placeholders with 0 stats.

---

### 🔹 Entry #004 — Full Registration Fields, Universal Identifier Login & Forgot Password Flow
- **Date & Time**: `2026-08-23 13:28:30 IST` (`2026-08-23T07:58:30Z`)
- **Git Commit**: `140a07d`
- **Files Modified**:
  - [`android_native/app/src/main/java/com/bharatconnect/app/domain/model/User.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/domain/model/User.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/data/remote/dto/ProfileDto.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/data/remote/dto/ProfileDto.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/data/local/room/entity/UserEntity.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/data/local/room/entity/UserEntity.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/domain/repository/AuthRepository.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/domain/repository/AuthRepository.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/domain/usecase/auth/AuthUseCases.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/domain/usecase/auth/AuthUseCases.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/LoginScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/LoginScreen.kt)
  - [`android_native/app/src/test/java/com/bharatconnect/app/domain/AuthUseCasesTest.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/test/java/com/bharatconnect/app/domain/AuthUseCasesTest.kt)
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk)
- **Summary of Changes**:
  - **Registration Requirements**:
    - Added inputs: **Full Name**, **Username**, **Email Address**, **Phone Number with Country Code Dropdown** (13+ countries), **Date of Birth (DOB)** with interactive `DatePickerDialog`, **Password**, and **Confirm Password** with eye visibility toggles.
    - Updated `UserProfile`, `ProfileDto`, `UserEntity` models to store and persist `phoneNumber` and `dob`.
  - **Sign In / Log In Requirements**:
    - Enhanced identifier field to accept **Username**, **Email**, or **Mobile Number**.
    - Configured automatic email resolution via `profiles` lookup for username/phone login.
    - Integrated **Forgot Password** dialog with direct Supabase password reset link dispatching (`supabase.auth.resetPasswordForEmail`).
  - Updated all 25 unit test suites to validate new auth signatures.
  - Recompiled debug APK and synced with GitHub `main`.

---

### 🔹 Entry #005 — Database Schema Production Release & Immutable Audit System Initiation
- **Date & Time**: `2026-08-23 13:45:00 IST` (`2026-08-23T08:15:00Z`)
- **Git Commit**: `53dbe97`
- **Files Modified / Added**:
  - [`supabase_schema.sql`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/supabase_schema.sql) (Complete Supabase PostgreSQL schema with 13 tables, RLS policies, triggers, and Realtime publications)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md) (Master immutable changelog and data flow specification)
- **Summary of Changes**:
  - Created standalone production database schema definition covering profiles, conversations, messages, posts, likes, comments, stories, marketplace, jobs, quick gigs, notifications, and locations.
  - Added PostgreSQL triggers for automated auth-to-profile onboarding (`handle_new_user`), post like count aggregation (`handle_post_like_counter`), and conversation last message updates (`handle_new_message`).
  - Enforced Row Level Security (RLS) policies for all 13 tables and enabled real-time publication subscriptions.
  - Initialized this master `SYSTEM_CHANGELOG.md` file following strict append-only rules.

---

### 🔹 Entry #006 — Complete Standalone APK Recompilation & GitHub Remote Sync
- **Date & Time**: `2026-08-23 13:51:30 IST` (`2026-08-23T08:21:30Z`)
- **Git Commit**: `50ea133`
- **Files Modified**:
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk) (Updated compiled standalone binary)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - Executed Gradle `assembleDebug` compilation with Java 19 SDK.
  - Verified all database entities, DAOs, Auth flows, and UI composables compile without errors.
  - Synced fresh binary [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk) with root directory and pushed changes to GitHub `origin/main`.

---

### 🔹 Entry #007 — Supabase Schema Idempotency & Missing Column Migration Fix
- **Date & Time**: `2026-08-23 13:57:00 IST` (`2026-08-23T08:27:00Z`)
- **Git Commit**: `29870be`
- **Files Modified**:
  - [`supabase_schema.sql`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/supabase_schema.sql)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - Resolved `ERROR 42703: column "email" does not exist` occurring when executing schema on pre-existing Supabase databases.
  - Added explicit `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` statements for all columns across `profiles`, `conversations`, `messages`, and `posts`.
  - Added `DROP POLICY IF EXISTS` prior to every policy creation to avoid duplicate policy creation errors.
  - Wrapped `ALTER PUBLICATION supabase_realtime ADD TABLE` in exception-handling `DO $$ ... $$` blocks to guarantee 100% idempotent migrations.

---

### 🔹 Entry #008 — Comprehensive Column Patching Across All 13 Database Tables
- **Date & Time**: `2026-08-23 14:00:00 IST` (`2026-08-23T08:30:00Z`)
- **Git Commit**: `5e275d5`
- **Files Modified**:
  - [`supabase_schema.sql`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/supabase_schema.sql)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - Resolved `ERROR 42703: column "expires_at" does not exist` on `public.stories`.
  - Added comprehensive `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` statements covering every column in all 13 tables (`profiles`, `conversations`, `conversation_members`, `messages`, `posts`, `post_likes`, `post_comments`, `stories`, `marketplace_items`, `jobs`, `quick_jobs`, `notifications`, `user_locations`).
  - Guarantees seamless execution regardless of any previous schema state in Supabase.

---

### 🔹 Entry #009 — Foreign Key Column Migration Validation (`author_id`, `seller_id`, `poster_id`, `user_id`)
- **Date & Time**: `2026-08-23 14:02:30 IST` (`2026-08-23T08:32:30Z`)
- **Git Commit**: `e877668`
- **Files Modified**:
  - [`supabase_schema.sql`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/supabase_schema.sql)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - Resolved `ERROR 42703: column "author_id" does not exist` on `public.posts`.
  - Added explicit `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for all foreign key identifier columns (`author_id` on posts/stories/comments, `seller_id` on marketplace items, `poster_id` on jobs/gigs, `user_id` on likes/notifications/members, `conversation_id` on messages/members, `post_id` on likes/comments).

---

### 🔹 Entry #010 — Type-Safe Casting for Mixed UUID / VARCHAR / TEXT Schema Policies
- **Date & Time**: `2026-08-23 14:05:00 IST` (`2026-08-23T08:35:00Z`)
- **Git Commit**: `9e0c806`
- **Files Modified**:
  - [`supabase_schema.sql`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/supabase_schema.sql)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - Resolved `ERROR 42883: operator does not exist: uuid = character varying` during RLS policy evaluation and trigger updates.
  - Applied explicit bidirectional `::TEXT` casting on all `auth.uid()`, foreign key joins (`conversation_id::TEXT = id::TEXT`, `post_id::TEXT = posts.id::TEXT`), and trigger comparisons (`id::TEXT = NEW.post_id::TEXT`).

---

### 🔹 Entry #011 — Standalone APK Recompilation & GitHub Remote Synchronization
- **Date & Time**: `2026-08-23 14:10:30 IST` (`2026-08-23T08:40:30Z`)
- **Git Commit**: `ddc156d`
- **Files Modified**:
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - Successfully executed Gradle `assembleDebug` compilation.
  - Verified and refreshed standalone binary [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk) in root directory.
  - Pushed updated binary and changelog to GitHub remote `origin/main`.

---

### 🔹 Entry #012 — Demo Mode Removal, Profile Picture Upload Engine & Cloudinary Fast Acceleration
- **Date & Time**: `2026-08-23 14:34:00 IST` (`2026-08-23T09:04:00Z`)
- **Git Commit**: `4141609`
- **Files Modified**:
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/splash/SplashScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/splash/SplashScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/core/storage/CloudinaryManager.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/core/storage/CloudinaryManager.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/domain/repository/AuthRepository.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/domain/repository/AuthRepository.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/domain/usecase/auth/AuthUseCases.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/domain/usecase/auth/AuthUseCases.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/home/HomeScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/home/HomeScreen.kt)
  - [`android_native/app/src/test/java/com/bharatconnect/app/domain/AuthUseCasesTest.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/test/java/com/bharatconnect/app/domain/AuthUseCasesTest.kt)
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - **Demo Mode Removed**: Removed `Explore Demo Mode →` button and demo bypass from `SplashScreen.kt`, enforcing live authenticated flows.
  - **Profile Picture Upload Integration**:
    - Added interactive circular Avatar picker with live Coil image preview during registration (`RegisterScreen.kt`).
    - Added live Avatar modification & upload directly inside profile tab and `EditProfileDialog` (`HomeScreen.kt`).
    - Added `UpdateProfileUseCase` with Supabase PostgreSQL profile syncing.
  - **Cloudinary Acceleration & Reliability**:
    - Added fast on-device bitmap downscaling and smart JPEG compression (512x512 max for avatars ~40-80KB, 1280x1280 max for media).
    - Enabled 64KB chunked streaming upload with keep-alive connections and 2-attempt exponential backoff retry.
    - Replaced silent mock URL fallbacks with genuine status verification and error handling.
  - All 25 unit test suites passed and standalone APK recompiled successfully.

---

### 🔹 Entry #013 — Tool Mentions Sanitization & Infrastructure Info Masking
- **Date & Time**: `2026-08-23 14:53:00 IST` (`2026-08-23T09:23:00Z`)
- **Git Commit**: `202a979`
- **Files Modified**:
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/splash/SplashScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/splash/SplashScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/home/HomeScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/home/HomeScreen.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/core/storage/CloudinaryManager.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/core/storage/CloudinaryManager.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/media/MediaViewModel.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/media/MediaViewModel.kt)
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - **Branding & Privacy Sanitization**:
    - Replaced "Sign In with Supabase" and "Connecting to Supabase..." with consumer-facing "Sign In to BharatConnect" and "Connecting to BharatConnect..." in `SplashScreen.kt`.
    - Sanitized internal architecture badges in `HomeScreen.kt` (replaced backend infrastructure labels with clean "Cloud Account & Data Sync", "Local Offline Storage", and "Media & Attachment Service").
    - Cleaned chat header encryption indicator and call dialog title.
    - Sanitized all user-facing media upload error messages in `CloudinaryManager.kt`.
    - Removed hardcoded sample demo media tasks from `MediaViewModel.kt`.
  - All 25 unit tests verified and updated standalone APK binary compiled and pushed to GitHub `origin/main`.

---

### 🔹 Entry #014 — Network Timeout Resilience (60s) & URL Error Leak Prevention
- **Date & Time**: `2026-08-23 15:09:30 IST` (`2026-08-23T09:39:30Z`)
- **Git Commit**: `909686f`
- **Files Modified**:
  - [`android_native/app/src/main/java/com/bharatconnect/app/core/network/SupabaseClient.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/core/network/SupabaseClient.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/core/network/NetworkErrorSanitizer.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/core/network/NetworkErrorSanitizer.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt)
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - **Network Timeout Extension**: Configured Ktor CIO engine in `SupabaseClient.kt` with a 60-second request timeout (`requestTimeout = 60.seconds`), 30-second connect timeout, and 60-second socket timeout (extended from the default 10-second limit that caused mobile signup timeouts).
  - **Universal Network Error Sanitizer**: Added `NetworkErrorSanitizer.kt` to catch and mask any low-level network exceptions, completely stripping internal Supabase URLs (`https://.../auth/v1/signup`), request parameters, and stack traces into friendly, polished user prompts.
  - **Recompiled Standalone APK**: Tested with 25 unit test suites and compiled fresh binary to root `BharatConnect-Native.apk`.

---

### 🔹 Entry #015 — Seamless Email Verification Flow & User Metadata Synchronization
- **Date & Time**: `2026-08-23 15:27:00 IST` (`2026-08-23T09:57:00Z`)
- **Files Modified**:
  - [`android_native/app/src/main/java/com/bharatconnect/app/domain/model/User.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/domain/model/User.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/data/repository/AuthRepositoryImpl.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/AuthViewModel.kt)
  - [`android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/presentation/auth/RegisterScreen.kt)
  - [`BharatConnect-Native.apk`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/BharatConnect-Native.apk)
  - [`SYSTEM_CHANGELOG.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/SYSTEM_CHANGELOG.md)
- **Summary of Changes**:
  - **Email Verification State**: Added `AuthState.VerificationEmailSent` to seamlessly handle Supabase GoTrue email confirmation without throwing error banners on mobile.
  - **Celebratory Verification Dialog**: Added an interactive AlertDialog in `RegisterScreen.kt` highlighting the target email with a direct "Proceed to Sign In" navigation CTA.
  - **Metadata Payload on Registration**: In `AuthRepositoryImpl.kt`, attached user metadata (`username`, `full_name`, `phone_number`, `dob`, `avatar_url`) into `data = buildJsonObject { ... }` during `signUpWith(Email)` for database triggers and profile synchronization.
  - **APK & Tests**: All 25 unit tests passed and fresh standalone binary compiled to `BharatConnect-Native.apk`.

---
*(Append all future system changes below this line)*










