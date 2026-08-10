# 📊 BharatConnect — Google Sheets Database Setup Guide

Use **Google Sheets** as a free, live cloud database for **BharatConnect**. All posts, users, chat messages, and marketplace items will automatically sync to and from your Google Sheet in real time.

---

## 🏗️ Architecture Overview

```text
┌───────────────────────────┐      HTTP REST      ┌───────────────────────────────┐      Google Webhook      ┌─────────────────────────────┐
│  BharatConnect App / Client│ ──────────────────> │ BharatConnect Backend Server  │ ───────────────────────> │    Google Apps Script App   │
│   (Kivy / Web / Mobile)   │                     │      (backend/server.py)      │                          │     (script.google.com)     │
└───────────────────────────┘                     └───────────────────────────────┘                          └──────────────┬──────────────┘
                                                                                                                            │ Reads / Writes
                                                                                                                            ▼
                                                                                                             ┌─────────────────────────────┐
                                                                                                             │    Google Sheets Database   │
                                                                                                             │    (users, posts, messages) │
                                                                                                             └─────────────────────────────┘
```

---

## 🚀 Step 1: Create your Google Sheet

1. Go to [Google Sheets](https://sheets.new) and create a **Blank Spreadsheet**.
2. Name the spreadsheet: **`BharatConnect Database`**.
3. Create 5 tabs (worksheets) at the bottom with the exact following names and header columns:

### Tab 1: `users`
Add header in row 1:
`id` | `username` | `display_name` | `email` | `bio` | `avatar_initials` | `avatar_color`

### Tab 2: `posts`
Add header in row 1:
`id` | `author_id` | `author_name` | `user_avatar` | `avatar_color` | `content` | `image_title` | `likes_count` | `comments_count` | `created_at`

### Tab 3: `messages`
Add header in row 1:
`id` | `chat_id` | `sender_id` | `sender_name` | `text` | `time` | `is_me`

### Tab 4: `chats`
Add header in row 1:
`id` | `chat_type` | `title` | `subtitle` | `avatar_initials` | `avatar_color` | `unread_count`

### Tab 5: `marketplace`
Add header in row 1:
`id` | `category` | `title` | `price_payout` | `type_tag` | `icon`

---

## ⚡ Step 2: Add Google Apps Script Backend Code

1. In your Google Sheet, click **`Extensions`** -> **`Apps Script`**.
2. Delete any existing text in the editor and paste the following script:

```javascript
function doGet(e) {
  var action = e.parameter.action;
  var sheetName = e.parameter.sheet || "posts";
  var doc = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = doc.getSheetByName(sheetName);
  
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: "Sheet not found" }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) {
    return ContentService.createTextOutput(JSON.stringify({ status: "success", rows: [] }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  var headers = data[0];
  var rows = [];
  for (var i = 1; i < data.length; i++) {
    var row = {};
    for (var j = 0; j < headers.length; j++) {
      row[headers[j]] = data[i][j];
    }
    rows.push(row);
  }
  
  return ContentService.createTextOutput(JSON.stringify({ status: "success", rows: rows }))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  try {
    var requestData = JSON.parse(e.postData.contents);
    var action = requestData.action;
    var payload = requestData.payload;
    var doc = SpreadsheetApp.getActiveSpreadsheet();
    
    if (action === "save_post") {
      var sheet = doc.getSheetByName("posts");
      sheet.appendRow([
        payload.id || "post-" + Date.now(),
        payload.author_id || "u-alex",
        payload.author_name || "Alex Morgan",
        payload.user_avatar || "AM",
        payload.avatar_color || "#6367FF",
        payload.content || "",
        payload.image_title || "",
        payload.likes_count || 0,
        payload.comments_count || 0,
        new Date().toISOString()
      ]);
    } else if (action === "save_message") {
      var sheet = doc.getSheetByName("messages");
      sheet.appendRow([
        payload.id || "msg-" + Date.now(),
        payload.chat_id || "c-individual",
        payload.sender_id || "u-alex",
        payload.sender_name || "Alex Morgan",
        payload.text || "",
        payload.time || "Just now",
        payload.is_me ? 1 : 0
      ]);
    } else if (action === "save_user") {
      var sheet = doc.getSheetByName("users");
      sheet.appendRow([
        payload.id || "u-" + Date.now(),
        payload.username || "",
        payload.display_name || "",
        payload.email || "",
        payload.bio || "",
        payload.avatar_initials || "US",
        payload.avatar_color || "#6367FF"
      ]);
    }
    
    return ContentService.createTextOutput(JSON.stringify({ status: "success" }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
```

---

## 🌐 Step 3: Deploy as Web App

1. In Apps Script, click the blue **`Deploy`** button at top right -> **`New deployment`**.
2. Click the gear icon next to *Select type* and select **`Web app`**.
3. Fill in settings:
   - **Description**: `BharatConnect API Endpoint`
   - **Execute as**: **`Me`** (`your.email@gmail.com`)
   - **Who has access**: **`Anyone`** *(Crucial for zero-auth API access)*
4. Click **`Deploy`**.
5. Click **`Authorize Access`** -> Choose your Google Account -> Click **`Advanced`** -> **`Go to Untitled project (unsafe)`** -> Click **`Allow`**.
6. Copy the **Web app URL**:
   `https://script.google.com/macros/s/AKfycbx.../exec`

---

## ⚙️ Step 4: Configure BharatConnect

1. Open [`backend/config.py`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/backend/config.py).
2. Set `GOOGLE_SHEETS_WEB_APP_URL`:

```python
GOOGLE_SHEETS_WEB_APP_URL = "https://script.google.com/macros/s/YOUR_DEPLOYED_SCRIPT_ID/exec"
```

3. Launch your server via **`start_backend.bat`** or **`start_all.bat`**.

---

## ✅ Verification

- Add a post in **BharatConnect App** -> Open your **Google Sheet** -> Check the **`posts`** tab! The post will appear immediately as a new row in your spreadsheet.
