# ☁️ BharatConnect — Cloudinary Media Configuration Guide

**Cloud Name:** `twiesyqj`  
**API Key (Public):** `446197212112895`  
**Status:** Active & Integrated in Native Android Client

---

## 1. Unsigned Upload Preset Setup (Required for APK Uploads)

To allow the Android client to safely upload images and videos without exposing your secret API key:

1. Log into your [Cloudinary Management Console](https://cloudinary.com/console).
2. Go to **Settings (Gear Icon)** ➔ **Upload**.
3. Scroll down to **Upload Presets** and click **Add Upload Preset**.
4. Set the following fields:
   * **Upload preset name:** `bharatconnect_unsigned`
   * **Signing Mode:** `Unsigned`
   * **Folder:** `bharatconnect`
   * **Unique filename:** `True`
   * **Delivery type:** `Upload`
   * **Access mode:** `Public`
5. Click **Save**.

---

## 2. On-Device Media Processing Pipeline

The Android Kotlin app executes the following 8-state pipeline before uploading:

```
[ SELECTED ] 
    │ (User selects image/video from gallery)
    ▼
[ VALIDATING ] 
    │ (Check file bounds: < 50MB for video, < 15MB for image)
    ▼
[ COMPRESSING ] 
    │ (Android Bitmap compression at 80% JPEG quality)
    ▼
[ UPLOADING ] 
    │ (Stream bytes to Cloudinary REST endpoint)
    ▼
[ PROCESSING ] 
    │ (Cloudinary generates thumbnails & WebP/AVIF variants)
    ▼
[ UPLOADED ] 
    │ (Obtain secure_url and public_id)
    ▼
[ DB_RECORD_CREATED ] 
    │ (Save metadata to Supabase 'media' and 'messages' tables)
    ▼
[ READY ] 
    │ (UI updates message status to Delivered)
```

---

## 3. Dynamic Transformation URLs

Coil Compose renders images dynamically using Cloudinary URL parameters:
* **Avatar Thumbnail:** `https://res.cloudinary.com/twiesyqj/image/upload/c_thumb,w_200,g_face/v1/bharatconnect/{public_id}.jpg`
* **Feed Post Media:** `https://res.cloudinary.com/twiesyqj/image/upload/w_1080,q_auto,f_auto/v1/bharatconnect/{public_id}.jpg`
