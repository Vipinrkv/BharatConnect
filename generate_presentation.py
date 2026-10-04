import os
import sys
import win32com.client

def rgb(r, g, b):
    """Converts RGB (0-255) to PowerPoint COM integer color."""
    return r + (g << 8) + (b << 16)

# Design System Palette (Bharat Modern)
NAVY_DEEP = rgb(10, 17, 40)        # #0A1128
NAVY_CARD = rgb(18, 30, 66)        # #121E42
NAVY_LIGHT = rgb(28, 45, 96)       # #1C2D60
WHITE = rgb(255, 255, 255)         # #FFFFFF
OFF_WHITE = rgb(240, 244, 248)     # #F0F4F8
SAFFRON = rgb(255, 127, 17)        # #FF7F11 (Indian Saffron)
GOLD = rgb(255, 195, 0)            # #FFC300
EMERALD = rgb(0, 196, 159)         # #00C49F (Verified/Success Green)
CYAN = rgb(0, 180, 216)            # #00B4D8 (Tech Cyan)
TEXT_MUTED = rgb(160, 174, 192)    # #A0AEC0
BORDER_COLOR = rgb(45, 65, 125)    # #2D417D

FONT_TITLE = "Segoe UI"
FONT_BODY = "Segoe UI"

def set_shape_flat(shape, fill_color, line_color=None, line_width=1):
    shape.Fill.Solid()
    shape.Fill.ForeColor.RGB = fill_color
    if line_color:
        shape.Line.Visible = True
        shape.Line.ForeColor.RGB = line_color
        shape.Line.Weight = line_width
    else:
        shape.Line.Visible = False

def add_header(slide, title, category, slide_num, total_slides=16):
    # Category badge
    cat_shape = slide.Shapes.AddTextbox(1, 50, 25, 600, 20)
    tf = cat_shape.TextFrame
    tf.MarginLeft = tf.MarginRight = tf.MarginTop = tf.MarginBottom = 0
    tr = tf.TextRange
    tr.Text = category.upper()
    tr.Font.Name = FONT_TITLE
    tr.Font.Size = 10
    tr.Font.Bold = True
    tr.Font.Color.RGB = SAFFRON

    # Main Title
    title_shape = slide.Shapes.AddTextbox(1, 50, 45, 750, 45)
    tf2 = title_shape.TextFrame
    tf2.MarginLeft = tf2.MarginRight = tf2.MarginTop = tf2.MarginBottom = 0
    tr2 = tf2.TextRange
    tr2.Text = title
    tr2.Font.Name = FONT_TITLE
    tr2.Font.Size = 22
    tr2.Font.Bold = True
    tr2.Font.Color.RGB = WHITE

    # Top right slide number badge
    num_shape = slide.Shapes.AddTextbox(1, 840, 25, 70, 25)
    tfn = num_shape.TextFrame
    tfn.MarginLeft = tfn.MarginRight = tfn.MarginTop = tfn.MarginBottom = 0
    trn = tfn.TextRange
    trn.Text = f"{slide_num:02d} / {total_slides:02d}"
    trn.Font.Name = FONT_TITLE
    trn.Font.Size = 11
    trn.Font.Bold = True
    trn.Font.Color.RGB = TEXT_MUTED

    # Subtle accent divider
    divider = slide.Shapes.AddShape(1, 50, 92, 860, 2)
    set_shape_flat(divider, BORDER_COLOR)

    # Footer note
    footer = slide.Shapes.AddTextbox(1, 50, 515, 860, 20)
    tff = footer.TextFrame
    tff.MarginLeft = tff.MarginRight = tff.MarginTop = tff.MarginBottom = 0
    trf = tff.TextRange
    trf.Text = "🇮🇳 BharatConnect Confidential • Project Master Presentation & Support Proposal • 2026"
    trf.Font.Name = FONT_BODY
    trf.Font.Size = 9
    trf.Font.Color.RGB = rgb(90, 110, 145)

def create_deck():
    print("Initializing Microsoft PowerPoint via COM Automation...")
    ppt = win32com.client.Dispatch("PowerPoint.Application")
    ppt.Visible = True  # Avoid COM lockups
    pres = ppt.Presentations.Add(WithWindow=True)
    
    # 16:9 Widescreen: 960 x 540 points
    pres.PageSetup.SlideWidth = 960
    pres.PageSetup.SlideHeight = 540

    total_slides = 16

    # ==========================================
    # SLIDE 1: TITLE SLIDE (HERO)
    # ==========================================
    s1 = pres.Slides.Add(1, 12) # ppLayoutBlank = 12
    bg1 = s1.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg1, NAVY_DEEP)

    # Saffron & Green Accent Bands
    band_saffron = s1.Shapes.AddShape(1, 0, 0, 960, 6)
    set_shape_flat(band_saffron, SAFFRON)
    band_green = s1.Shapes.AddShape(1, 0, 534, 960, 6)
    set_shape_flat(band_green, EMERALD)

    # Hero Pill Badge
    pill = s1.Shapes.AddShape(5, 50, 55, 340, 28) # 5 = rounded rectangle
    set_shape_flat(pill, NAVY_CARD, SAFFRON, 1)
    ptf = pill.TextFrame
    ptf.MarginLeft = ptf.MarginRight = ptf.MarginTop = ptf.MarginBottom = 0
    ptr = ptf.TextRange
    ptr.Text = "🇮🇳  ATMANIRBHAR BHARAT • INDEPENDENT DIGITAL INFRASTRUCTURE"
    ptr.Font.Name = FONT_TITLE
    ptr.Font.Size = 8.5
    ptr.Font.Bold = True
    ptr.Font.Color.RGB = SAFFRON

    # Main Title
    t_box = s1.Shapes.AddTextbox(1, 50, 95, 860, 80)
    ttf = t_box.TextFrame
    ttf.MarginLeft = ttf.MarginRight = ttf.MarginTop = ttf.MarginBottom = 0
    ttr = ttf.TextRange
    ttr.Text = "BHARATCONNECT"
    ttr.Font.Name = FONT_TITLE
    ttr.Font.Size = 46
    ttr.Font.Bold = True
    ttr.Font.Color.RGB = WHITE

    # Subtitle
    sub_box = s1.Shapes.AddTextbox(1, 50, 180, 860, 60)
    stf = sub_box.TextFrame
    stf.MarginLeft = stf.MarginRight = stf.MarginTop = stf.MarginBottom = 0
    str_ = stf.TextRange
    str_.Text = "India's Sovereign Hyper-Local Social Mesh, Real-Time Encrypted Messaging\n& Decentralized Community Commerce Engine"
    str_.Font.Name = FONT_TITLE
    str_.Font.Size = 17
    str_.Font.Color.RGB = CYAN

    # 4 Highlight Metric Cards
    cards_data = [
        ("📱 NATIVE PERFORMANCE", "Jetpack Compose M3\n60/120 FPS Fluid UI\nUltra-Light (< 25 MB)"),
        ("🛡️ MILITARY-GRADE E2EE", "Sentinel 7-Layer\nAES-GCM-256 Client E2EE\nHardware Keystore Ready"),
        ("⚡ OFFLINE-FIRST ROOM", "Room SQLite Database\nZero Data Loss Sync\nDual WebSocket Engine"),
        ("🛍️ 3-TIER COMMERCE", "Pre-owned Goods (₹)\n1-Tap Career Jobs\nInstant-Payout Quick Gigs")
    ]
    for i, (heading, desc) in enumerate(cards_data):
        cx = 50 + i * 218
        cy = 265
        c_shape = s1.Shapes.AddShape(5, cx, cy, 208, 140)
        set_shape_flat(c_shape, NAVY_CARD, BORDER_COLOR, 1)
        ctf = c_shape.TextFrame
        ctf.MarginLeft = ctf.MarginRight = ctf.MarginTop = ctf.MarginBottom = 12
        ctr = ctf.TextRange
        ctr.Text = f"{heading}\n\n{desc}"
        ctr.Font.Name = FONT_BODY
        ctr.Font.Size = 10
        ctr.Font.Color.RGB = TEXT_MUTED
        h_line = ctr.Lines(1)
        h_line.Font.Bold = True
        h_line.Font.Size = 10.5
        h_line.Font.Color.RGB = GOLD if i % 2 == 0 else EMERALD

    # Presentation Metadata Footer Card
    meta_card = s1.Shapes.AddShape(5, 50, 425, 860, 75)
    set_shape_flat(meta_card, NAVY_CARD, BORDER_COLOR, 1)
    mtf = meta_card.TextFrame
    mtf.MarginLeft = mtf.MarginRight = mtf.MarginTop = mtf.MarginBottom = 14
    mtr = mtf.TextRange
    mtr.Text = "PROJECT OVERVIEW & SUPPORT SUBMISSION\n" \
               "Engineering Core: Clean MVVM Architecture • Kotlin 1.9 • Supabase PostgreSQL • Cloudinary CDN\n" \
               "Purpose: Project Evaluation, Grant Submission & National Scale-Up Strategy"
    mtr.Font.Name = FONT_BODY
    mtr.Font.Size = 10
    mtr.Font.Color.RGB = TEXT_MUTED
    mtr.Lines(1).Font.Bold = True
    mtr.Lines(1).Font.Color.RGB = WHITE

    # ==========================================
    # SLIDE 2: THE PROBLEM STATEMENT
    # ==========================================
    s2 = pres.Slides.Add(2, 12)
    bg2 = s2.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg2, NAVY_DEEP)
    add_header(s2, "The Crisis & Opportunity: Why Bharat Needs an Independent Social Mesh", "The Problem Statement", 2)

    problems = [
        ("RURAL & TIER 2/3 CONNECTIVITY VOID", 
         "• Spotty 3G/4G connectivity causes Western social apps to hang or fail completely.\n"
         "• Bloated apps (>100MB) consume excessive storage and expensive cellular bandwidth.\n"
         "• Zero offline usability: Messages are lost, feeds refuse to render, and drafts disappear.\n"
         "• Need: Lightweight (<25MB), offline-first architecture functioning seamlessly on weak networks.",
         SAFFRON),
        ("CENTRALIZED DATA EXPLOITATION", 
         "• Citizen communication patterns, location traces, and social graphs harvested by foreign tech giants.\n"
         "• Closed proprietary algorithms censor local community voices and prioritize rage-bait ads.\n"
         "• High vulnerability to single-point cloud outages and international policy shifts.\n"
         "• Need: Sovereign, encrypted, locally controlled communications with true data privacy.",
         CYAN),
        ("THE DISCONNECTED INFORMAL ECONOMY", 
         "• Over 85% of India's 500M+ workforce belongs to the unorganized/informal sector.\n"
         "• Local daily-wage workers, plumbers, artisans, and dukandaars have no digital discovery.\n"
         "• Traditional gig aggregators levy punitive 20%–30% commissions on low-income workers.\n"
         "• Need: Zero-commission, hyper-local discovery radar and direct-payout community marketplace.",
         EMERALD)
    ]

    for i, (title, body, accent) in enumerate(problems):
        x = 50 + i * 293
        box = s2.Shapes.AddShape(5, x, 110, 280, 290)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"⚠️  {title}\n\n{body}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 10.5
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.5
        btr.Lines(1).Font.Color.RGB = accent

    banner2 = s2.Shapes.AddShape(5, 50, 415, 860, 80)
    set_shape_flat(banner2, NAVY_LIGHT, SAFFRON, 1)
    bntf = banner2.TextFrame
    bntf.MarginLeft = bntf.MarginRight = bntf.MarginTop = bntf.MarginBottom = 14
    bntr = bntf.TextRange
    bntr.Text = "THE MARKET REALITY: Over 500 Million 'Next Billion Users' across Tier 2, 3, 4 towns and rural villages are forced into platforms designed for Silicon Valley. BharatConnect is architected from the ground up for the Indian reality: resilient, private, hyper-local, and economically empowering."
    bntr.Font.Name = FONT_BODY
    bntr.Font.Size = 11
    bntr.Font.Bold = True
    bntr.Font.Color.RGB = WHITE

    # ==========================================
    # SLIDE 3: THE SOLUTION & PHILOSOPHY
    # ==========================================
    s3 = pres.Slides.Add(3, 12)
    bg3 = s3.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg3, NAVY_DEEP)
    add_header(s3, "The BharatConnect Solution: Sovereign, Resilient & Hyper-Local", "Product & Architectural Vision", 3)

    pillars = [
        ("OFFLINE-FIRST RESILIENCE", 
         "Local Room SQLite database operates as an authoritative client ledger. Users can browse feeds, compose posts, and send messages without an active connection. Android WorkManager intelligently synchronizes transactions when internet resumes.",
         GOLD),
        ("HYPER-LOCAL RADAR", 
         "Live animated pulse radar dynamically discovers nearby members and services within 1km, 5km, and 10km radii. Real-time distance calculations with 1-tap instant messaging foster authentic neighborhood collaboration.",
         CYAN),
        ("WHATSAPP-TIER MESSAGING", 
         "Real-time dual sync engine utilizing Supabase WebSocket channels with 2.0s polling fallback. Multi-state delivery ticks (⏳ Sending, ✓ Sent, ✓✓ Delivered, ✓✓ Blue Read) with rich multimedia attachment drawers.",
         EMERALD),
        ("TRI-TIER COMMUNITY COMMERCE", 
         "Built-in marketplace removing middlemen: Pre-owned classified items with transparent Rupee (₹) pricing, verified career job listings, and instant-payout Quick Gigs for localized neighborhood tasks.",
         SAFFRON)
    ]

    for i, (title, body, color) in enumerate(pillars):
        x = 50 + (i % 2) * 440
        y = 110 + (i // 2) * 190
        box = s3.Shapes.AddShape(5, x, y, 420, 175)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"✦  {title}\n\n{body}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 11
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 12.5
        btr.Lines(1).Font.Color.RGB = color

    # ==========================================
    # SLIDE 4: CURRENT PROGRESS & PRODUCTION MILESTONES
    # ==========================================
    s4 = pres.Slides.Add(4, 12)
    bg4 = s4.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg4, NAVY_DEEP)
    add_header(s4, "Current Progress: Production Milestones & Validated Deliverables", "Engineering Status", 4)

    kpis = [
        ("24.7 MB", "Native Binary Size", "Zero bloat, instant install", EMERALD),
        ("60 / 120 FPS", "Fluid Animation Rate", "Jetpack Compose Material 3", CYAN),
        ("100% PASS", "27 Unit Test Suites", "Clean architecture test coverage", GOLD),
        ("< 150 ms", "WebSocket Delivery", "Supabase Realtime channels", SAFFRON)
    ]
    for i, (val, label, sub, col) in enumerate(kpis):
        x = 50 + i * 218
        box = s4.Shapes.AddShape(5, x, 110, 208, 90)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 8
        btr = btf.TextRange
        btr.Text = f"{val}\n{label}\n{sub}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 9
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 19
        btr.Lines(1).Font.Color.RGB = col
        btr.Lines(2).Font.Bold = True
        btr.Lines(2).Font.Size = 10.5
        btr.Lines(2).Font.Color.RGB = WHITE

    progress_details = [
        ("CLIENT APPLICATION", 
         "• 100% Kotlin Native Jetpack Compose.\n"
         "• Shimmer skeleton loading & zero UI lockups.\n"
         "• Full Room SQLite persistence layer (`PostDao`, `MessageDao`, `UserDao`, `ConversationDao`).\n"
         "• Cloudinary unsigned direct binary upload engine.\n"
         "• Coil media engine with smart memory caching.\n"
         "• Standalone signed APK: `BharatConnect-Native.apk`."),
        ("IDENTITY & REAL-TIME SYNC", 
         "• Multi-tier session resolver: Supabase token + SharedPreferences + Room local fallback.\n"
         "• Universal login: Username, Email, or Mobile.\n"
         "• Dual Real-Time Sync: WebSocket channel subscription + 2.0s background polling fallback.\n"
         "• Idempotent UUID conflict resolution.\n"
         "• Ephemeral 24hr TTL status & stories creator.\n"
         "• Full dark/light Material 3 theme engine."),
        ("BACKEND & INFRASTRUCTURE", 
         "• Supabase PostgreSQL 15 cloud database.\n"
         "• 13 production schema tables with RLS policies.\n"
         "• Database triggers for likes, unread counts & chats.\n"
         "• FastAPI microservice (`server.py`) for webhook dispatching and health monitoring.\n"
         "• Web distribution landing page with direct download.\n"
         "• 1-Click build automation (`build_native_apk.bat`).")
    ]

    for i, (title, body) in enumerate(progress_details):
        x = 50 + i * 293
        box = s4.Shapes.AddShape(5, x, 215, 280, 275)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 14
        btr = btf.TextRange
        btr.Text = f"✅  {title}\n\n{body}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 9.5
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11
        btr.Lines(1).Font.Color.RGB = WHITE

    # ==========================================
    # SLIDE 5: TECHNICAL ARCHITECTURE & DATA FLOW
    # ==========================================
    s5 = pres.Slides.Add(5, 12)
    bg5 = s5.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg5, NAVY_DEEP)
    add_header(s5, "System Architecture: Clean Architecture & Dual Sync Engine", "Technical Design", 5)

    arch_layers = [
        ("LAYER 1: PRESENTATION (JETPACK COMPOSE)",
         "Declarative UI built with Material 3. Single-Activity architecture using StateFlow/SharedFlow. Screens: Home Feed, Real-Time Chat Hub, Nearby Radar, Marketplace Hub, Notifications, and Profile.",
         CYAN),
        ("LAYER 2: DOMAIN & USE CASES (CLEAN ARCHITECTURE)",
         "Pure Kotlin business logic decoupled from Android SDK. Dedicated use cases for Auth, Messaging, Post Creation, Geolocation Filtering, and Offline Queue Enqueueing.",
         WHITE),
        ("LAYER 3: DATA REPOSITORY & LOCAL ROOM SQLITE",
         "Repository pattern providing single source of truth. Offline-first local database caching all posts, messages, and profiles. Android WorkManager handles background synchronization.",
         EMERALD),
        ("LAYER 4: REMOTE NETWORK, REALTIME & MEDIA CDN",
         "Ktor HTTP/WebSocket Client connecting to Supabase PostgREST & Realtime channels. Cloudinary unsigned direct client upload for media files with on-device thumbnail compression.",
         GOLD),
        ("LAYER 5: CLOUD DATABASE & EVENT DISPATCHER",
         "Supabase PostgreSQL 15 with Row-Level Security (RLS) policies. Automated SQL triggers for message receipts, activity notifications, and FastAPI cloud webhook integration.",
         SAFFRON)
    ]

    for i, (title, desc, accent) in enumerate(arch_layers):
        y = 110 + i * 76
        box = s5.Shapes.AddShape(5, 50, y, 860, 66)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 10
        btr = btf.TextRange
        btr.Text = f"⚙️  {title}\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 9.5
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11
        btr.Lines(1).Font.Color.RGB = accent

    # ==========================================
    # SLIDE 6: SECURITY, PRIVACY & ENCRYPTION
    # ==========================================
    s6 = pres.Slides.Add(6, 12)
    bg6 = s6.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg6, NAVY_DEEP)
    add_header(s6, "Security & Privacy: Sentinel Multi-Tier Defense Architecture", "Data Security", 6)

    sec_cards = [
        ("AES-GCM-256 CLIENT ENCRYPTION",
         "• Chat message payloads are encrypted client-side using authenticated AES-GCM-256.\n"
         "• Ephemeral session keys derived on-device.\n"
         "• No unencrypted message content reaches public server logs.\n"
         "• Cryptographic integrity check prevents tampering.",
         CYAN),
        ("ROW LEVEL SECURITY (RLS)",
         "• Strict PostgreSQL Row-Level Security policies active across all 13 tables.\n"
         "• Users can only query their own private messages and conversation memberships.\n"
         "• Zero data leakage across tenants or users.\n"
         "• Enforced directly at the database engine level.",
         EMERALD),
        ("HARDENED IDENTITY RESOLUTION",
         "• GoTrue JWT tokens with automated background silent token refresh.\n"
         "• Passwords secured via industry-standard Argon2 / Bcrypt hashing.\n"
         "• Multi-tier fallback prevents authentication deadlocks during network blips.\n"
         "• Encrypted local SharedPreferences storage.",
         GOLD),
        ("PRIVACY BY DESIGN & ZERO TRACKING",
         "• Nearby Discovery Radar utilizes fuzzy location offsets to prevent exact tracking.\n"
         "• Ephemeral status stories automatically expire after 24 hours (strict TTL).\n"
         "• Zero background behavioral profiling, ad tracking SDKs, or foreign analytics trackers.",
         SAFFRON)
    ]

    for i, (title, desc, col) in enumerate(sec_cards):
        x = 50 + (i % 2) * 440
        y = 110 + (i // 2) * 190
        box = s6.Shapes.AddShape(5, x, y, 420, 175)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"🛡️  {title}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 10
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.5
        btr.Lines(1).Font.Color.RGB = col

    # ==========================================
    # SLIDE 7: KEY FEATURE SPOTLIGHT
    # ==========================================
    s7 = pres.Slides.Add(7, 12)
    bg7 = s7.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg7, NAVY_DEEP)
    add_header(s7, "Feature Deep-Dive: Real-Time Chat, Radar & Marketplace", "Feature Spotlight", 7)

    features = [
        ("ENCRYPTED CHAT HUB",
         "• Individual, Group, and Community tabs.\n"
         "• WhatsApp-Style Multi-State Ticks:\n"
         "   ⏳ Pending | ✓ Sent | ✓✓ Delivered | ✓✓ Blue Read\n"
         "• Rich Attachment Sheet:\n"
         "   Camera, Photo Gallery, PDF Docs, GPS Location, Contacts.\n"
         "• Built-in categorized emoji picker drawer.\n"
         "• Outbound bubble alignment with profile resolution.\n"
         "• Instant notification auto-dismiss on chat open.",
         CYAN),
        ("NEARBY DISCOVERY RADAR",
         "• Real-Time Geolocation Pulse Radar:\n"
         "   Animated circular radar beam with live distance calculations.\n"
         "• Dynamic Radius Filtering:\n"
         "   Switch between 1 km, 5 km, and 10 km boundaries.\n"
         "• Privacy Masking:\n"
         "   1-Tap toggle to hide location or make profile discoverable.\n"
         "• 1-Tap Direct Messaging:\n"
         "   Initiate direct conversation with nearby users instantly.",
         EMERALD),
        ("3-IN-1 MARKETPLACE HUB",
         "• Pre-Owned Classified Items:\n"
         "   Buy & sell electronics, furniture, vehicles in Indian Rupees (₹).\n"
         "• Local Career Jobs:\n"
         "   Salary ranges, full-time/part-time filters & 1-tap direct apply.\n"
         "• Quick Gigs (Micro-Tasks):\n"
         "   Instant neighborhood tasks (deliveries, repairs, errands) with transparent immediate payouts.\n"
         "• Middleman-Free Direct Contact.",
         SAFFRON)
    ]

    for i, (title, desc, col) in enumerate(features):
        x = 50 + i * 293
        box = s7.Shapes.AddShape(5, x, 110, 280, 385)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"✨  {title}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 9.8
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.5
        btr.Lines(1).Font.Color.RGB = col

    # ==========================================
    # SLIDE 8: CURRENT TOOLS, METHODS & ENGINEERING STACK
    # ==========================================
    s8 = pres.Slides.Add(8, 12)
    bg8 = s8.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg8, NAVY_DEEP)
    add_header(s8, "Engineering Stack: Current Tools, Protocols & Methods", "Technology Stack", 8)

    stacks = [
        ("CLIENT & UI TOOLING",
         "• Kotlin 1.9.23 (JVM 19 target)\n"
         "• Jetpack Compose 1.5 (Material 3)\n"
         "• AndroidX Lifecycle & Coroutines Flow\n"
         "• AndroidX Navigation Compose\n"
         "• Coil 2.6 for asynchronous image rendering\n"
         "• Shimmer effect skeleton loading",
         CYAN),
        ("PERSISTENCE & SYNC",
         "• Room SQLite 2.6 (`AppDatabase`)\n"
         "• TypeConverters for custom entities\n"
         "• AndroidX WorkManager 2.9 (`SyncWorker`)\n"
         "• Optimistic Local UI State Mutation\n"
         "• Dual Sync: WebSocket + 2.0s Polling\n"
         "• Multi-tier Session Resolution engine",
         EMERALD),
        ("CLOUD BACKEND & NETWORK",
         "• Supabase SDK (Ktor HTTP Client)\n"
         "• Supabase Realtime Channels (WebSockets)\n"
         "• PostgreSQL 15 with Row-Level Security\n"
         "• GoTrue Auth (OTP / Password / Reset)\n"
         "• Cloudinary CDN for direct binary uploads\n"
         "• FastAPI Python 3.11 webhook microservice",
         GOLD),
        ("BUILD & QUALITY PIPELINE",
         "• Gradle 8.9 with Kotlin DSL\n"
         "• ProGuard/R8 byte-code optimization\n"
         "• JUnit 4 & MockK test harnesses\n"
         "• 27 passing automated test suites\n"
         "• Windows 1-Click build launcher (`.bat`)\n"
         "• Strict immutable changelog auditing",
         SAFFRON)
    ]

    for i, (title, desc, col) in enumerate(stacks):
        x = 50 + i * 218
        box = s8.Shapes.AddShape(5, x, 110, 208, 385)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 14
        btr = btf.TextRange
        btr.Text = f"🛠️  {title}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 9.5
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 10.5
        btr.Lines(1).Font.Color.RGB = col

    # ==========================================
    # SLIDE 9: CURRENT HARDWARE & INFRASTRUCTURE (EQUIPMENTS)
    # ==========================================
    s9 = pres.Slides.Add(9, 12)
    bg9 = s9.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg9, NAVY_DEEP)
    add_header(s9, "Current Infrastructure: Cloud Assets, Workstations & Equipments", "Hardware & Infrastructure", 9)

    equipments = [
        ("CLOUD DATABASE & COMPUTE NODES",
         "• Supabase Managed PostgreSQL Cluster:\n"
         "   PostgreSQL 15 running with PgBouncer connection pooling.\n"
         "• Realtime Webhook & WebSocket Gateway:\n"
         "   Zero-delay pub/sub event broadcasting.\n"
         "• Cloud Security Isolation:\n"
         "   Automated SSL/TLS termination and DDoS filtering.",
         CYAN),
        ("GLOBAL MEDIA STORAGE & CDN",
         "• Cloudinary Multi-Region CDN Cluster:\n"
         "   Automated image/video format transcoding (WebP, AVIF).\n"
         "• Edge-Cached Binary Storage:\n"
         "   Sub-50ms media delivery across Indian internet providers.\n"
         "• Client-Side Direct Upload Endpoints:\n"
         "   Zero server proxy bandwidth overhead.",
         EMERALD),
        ("BUILD & COMPILATION WORKSTATIONS",
         "• High-Performance Engineering Workstations:\n"
         "   Multi-core AMD/Intel development rigs with 32GB RAM.\n"
         "• Android SDK Build Environment:\n"
         "   Targeting API 34 (Android 14) with minSdk 24 (96%+ device reach).\n"
         "• Java Development Kit 19 Toolchain:\n"
         "   Automated Gradle daemon parallel compilation.",
         GOLD),
        ("PHYSICAL & EMULATED TEST HARNESSES",
         "• Physical Android Test Devices:\n"
         "   Testing across varied screen densities (FHD+, HD) and RAM tiers (2GB - 8GB).\n"
         "• Android Virtual Devices (AVD):\n"
         "   Automated headless test execution via ADB.\n"
         "• Network Throttling Simulators:\n"
         "   Simulating 2G/3G packet loss, high latency, and airplane mode.",
         SAFFRON)
    ]

    for i, (title, desc, col) in enumerate(equipments):
        x = 50 + (i % 2) * 440
        y = 110 + (i // 2) * 190
        box = s9.Shapes.AddShape(5, x, y, 420, 175)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"🖥️  {title}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 10
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.5
        btr.Lines(1).Font.Color.RGB = col

    # ==========================================
    # SLIDE 10: DEVELOPMENT PLAN & MULTI-PHASE ROADMAP
    # ==========================================
    s10 = pres.Slides.Add(10, 12)
    bg10 = s10.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg10, NAVY_DEEP)
    add_header(s10, "Development Plan: From Foundation to Pan-India Scale", "Strategic Roadmap", 10)

    phases = [
        ("PHASE 1: FOUNDATION & HARDENING (COMPLETED)",
         "Native Jetpack Compose architecture • Room SQLite offline engine • Dual WebSocket sync • AES-GCM-256 E2EE • Standalone APK release.",
         EMERALD),
        ("PHASE 2: OFF-GRID DECENTRALIZED MESH (Q4 2026)",
         "Wi-Fi Direct & Bluetooth Low Energy (BLE) multi-hop relay • True off-grid communication during network blackouts or disaster scenarios.",
         CYAN),
        ("PHASE 3: BHARAT VERNACULAR AI & VOICE (Q1 2027)",
         "Government Bhashini AI integration • Voice-first input and real-time translation across 22 scheduled Indian languages.",
         GOLD),
        ("PHASE 4: UPI ESCROW & ONDC COMMERCE (Q2 2027)",
         "Direct UPI 2.0 deep linking • Automated micro-escrow for Quick Gig payouts • ONDC protocol integration for neighborhood commerce.",
         SAFFRON),
        ("PHASE 5: NATIONAL EDGE SCALE & CALLING (Q3 2027)",
         "Low-latency WebRTC encrypted voice & video calling • Sovereign edge clusters across Mumbai, Hyderabad, and Delhi NCR.",
         WHITE)
    ]

    for i, (title, desc, col) in enumerate(phases):
        y = 110 + i * 76
        box = s10.Shapes.AddShape(5, 50, y, 860, 66)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 10
        btr = btf.TextRange
        btr.Text = f"🚀  {title}\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 9.8
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.2
        btr.Lines(1).Font.Color.RGB = col

    # ==========================================
    # SLIDE 11: FUTURE TOOLS, METHODS & EMERGING TECH
    # ==========================================
    s11 = pres.Slides.Add(11, 12)
    bg11 = s11.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg11, NAVY_DEEP)
    add_header(s11, "Future Tools & Methods: Next-Gen Technology Integration", "Future Technologies", 11)

    future_tools = [
        ("BHASHINI AI / AI4BHARAT VOICE ENGINE",
         "• National Language Translation Mission (NLTM) API integration.\n"
         "• Real-time speech-to-text and text-to-speech in Hindi, Tamil, Telugu, Bengali, Marathi, etc.\n"
         "• Enables semi-literate and rural citizens to interact via natural voice prompts.\n"
         "• Dialect-aware localized audio posts and stories.",
         SAFFRON),
        ("ANDROID NEARBY CONNECTIONS (BLE / NAN)",
         "• Wi-Fi Aware (Neighbor Awareness Networking) and BLE multi-hop mesh.\n"
         "• Zero-internet device-to-device peer relay.\n"
         "• Emergency citizen broadcasting during floods, cyclones, or cellular tower failures.\n"
         "• Automatic packet forwarding with cryptographic TTL.",
         CYAN),
        ("WEBRTC & LIVEKIT P2P CALLING",
         "• Peer-to-peer end-to-end encrypted audio and video calling engine.\n"
         "• Adaptive bitrate codecs (Opus & AV1) optimized for high packet-loss 2G/3G connections.\n"
         "• Direct peer connection with minimum cloud relay dependency.\n"
         "• Group community audio huddles and town halls.",
         EMERALD),
        ("PGVECTOR & HYPER-LOCAL SEMANTIC SEARCH",
         "• Vector embeddings on PostgreSQL for contextual semantic matching.\n"
         "• Smart matching between Quick Gig seekers and local employers based on skills.\n"
         "• Localized recommendation engine without privacy-invasive ad tracking.\n"
         "• On-device lightweight embeddings.",
         GOLD)
    ]

    for i, (title, desc, col) in enumerate(future_tools):
        x = 50 + (i % 2) * 440
        y = 110 + (i // 2) * 190
        box = s11.Shapes.AddShape(5, x, y, 420, 175)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"🔮  {title}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 10
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.5
        btr.Lines(1).Font.Color.RGB = col

    # ==========================================
    # SLIDE 12: FUTURE INFRASTRUCTURE & EQUIPMENTS NEEDED
    # ==========================================
    s12 = pres.Slides.Add(12, 12)
    bg12 = s12.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg12, NAVY_DEEP)
    add_header(s12, "Future Infrastructure: Sovereign Data Centers & Hardware Assets", "Infrastructure Expansion", 12)

    future_infra = [
        ("SOVEREIGN INDIAN DATA CENTER CLUSTERS",
         "• Geo-distributed bare-metal and Kubernetes nodes in Mumbai, Hyderabad, and Noida.\n"
         "• Partnering with MeitY-empaneled Indian cloud providers (Yotta / ESDS).\n"
         "• 100% data residency within Indian sovereign borders.\n"
         "• Low-latency sub-15ms edge round-trip time across all Indian telecom circles.",
         CYAN),
        ("DEDICATED STUN/TURN RELAY NODES",
         "• Distributed high-throughput STUN/TURN servers to traverse strict carrier-grade NATs.\n"
         "• Critical for establishing peer-to-peer WebRTC calls over Indian mobile 4G/5G carriers.\n"
         "• Automated geographical DNS routing to the nearest regional relay server.",
         EMERALD),
        ("HARDWARE SECURITY MODULES (HSM)",
         "• Enterprise-grade HSMs for cryptographic key management and identity attestation.\n"
         "• Integration with Android StrongBox and Keystore hardware security chips.\n"
         "• Digital Public Infrastructure (DPI) compliance: Aadhaar e-Sign & DigiLocker readiness.",
         GOLD),
        ("HYPER-LOCAL IOT BLUETOOTH BEACONS",
         "• Low-cost, solar-assisted Bluetooth Low Energy (BLE) micro-beacons.\n"
         "• Piloted across local grain mandis, bus terminals, and panchayat halls.\n"
         "• Broadcasts hyper-local civic notices, mandi commodity rates, and emergency bulletins.",
         SAFFRON)
    ]

    for i, (title, desc, col) in enumerate(future_infra):
        x = 50 + (i % 2) * 440
        y = 110 + (i // 2) * 190
        box = s12.Shapes.AddShape(5, x, y, 420, 175)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"🏗️  {title}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 10
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.5
        btr.Lines(1).Font.Color.RGB = col

    # ==========================================
    # SLIDE 13: MARKET OPPORTUNITY & NATIONAL PROSPECTS
    # ==========================================
    s13 = pres.Slides.Add(13, 12)
    bg13 = s13.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg13, NAVY_DEEP)
    add_header(s13, "Market Opportunity: The Next Billion Users & National Impact", "Market Potential", 13)

    mkt_cards = [
        ("TAM: 850M+ USERS", 
         "Total Addressable Market\nIndia has 850M+ active smartphone users.\n$35B+ hyper-local commerce & digital services ecosystem by 2028.",
         CYAN),
        ("SAM: 500M+ VERNACULAR", 
         "Serviceable Addressable Market\nCitizens in Tier 2, 3, 4 towns & rural areas who need lightweight, vernacular, offline-first tools.",
         GOLD),
        ("SOM: 25M CITIZENS (24 MO)", 
         "Serviceable Obtainable Market\nTargeting 25 Million active users across 15 concentrated regional business & agrarian clusters.",
         EMERALD)
    ]

    for i, (title, desc, col) in enumerate(mkt_cards):
        x = 50 + i * 293
        box = s13.Shapes.AddShape(5, x, 110, 280, 115)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 12
        btr = btf.TextRange
        btr.Text = f"{title}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 10
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 13
        btr.Lines(1).Font.Color.RGB = col

    strat_box = s13.Shapes.AddShape(5, 50, 240, 860, 250)
    set_shape_flat(strat_box, NAVY_CARD, BORDER_COLOR, 1)
    sbtf = strat_box.TextFrame
    sbtf.MarginLeft = sbtf.MarginRight = sbtf.MarginTop = sbtf.MarginBottom = 18
    sbtr = sbtf.TextRange
    sbtr.Text = "🇮🇳  NATIONAL STRATEGIC ALIGNMENT & SOCIETAL IMPACT\n\n" \
              "• ATMANIRBHAR BHARAT (DIGITAL SOVEREIGNTY): Fosters domestic technological self-reliance, eliminating dangerous dependencies on foreign social network monopolies that manipulate data and compromise digital sovereignty.\n\n" \
              "• DISASTER RESILIENCE & CIVIC SAFETY: Peer-to-peer mesh networking ensures uninterrupted community communication during natural catastrophes (floods, cyclones, earthquakes) when cellular infrastructure collapses.\n\n" \
              "• INCLUSIVE ECONOMIC MULTIPLIER: Directly connects rural artisans, informal micro-workers, repairmen, and local merchants with neighborhood consumers without crippling commission middleman fees.\n\n" \
              "• DIGITAL PUBLIC INFRASTRUCTURE (DPI) INTEGRATION: Architected for seamless interoperability with India Stack: UPI 2.0 micro-payments, ONDC commerce, DigiLocker verified identity, and Bhashini AI translation."
    sbtr.Font.Name = FONT_BODY
    sbtr.Font.Size = 10.5
    sbtr.Font.Color.RGB = TEXT_MUTED
    sbtr.Lines(1).Font.Bold = True
    sbtr.Lines(1).Font.Size = 12.5
    sbtr.Lines(1).Font.Color.RGB = SAFFRON

    # ==========================================
    # SLIDE 14: THREE DISTINCT BUSINESS MODELS (MONETIZATION)
    # ==========================================
    s14 = pres.Slides.Add(14, 12)
    bg14 = s14.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg14, NAVY_DEEP)
    add_header(s14, "Monetization Engine: Three Distinct, Sustainable Business Models", "Business Models", 14)

    biz_models = [
        ("MODEL 1: HYPER-LOCAL MERCHANT ENGINE",
         "B2B Subscription & Promoted Discovery",
         "• Target: Neighborhood shops, tuition teachers, clinics, local services.\n"
         "• Promoted Radar Listings: Priority highlight in 1km/5km/10km radar scans.\n"
         "• Verified Merchant Gold Badge: Builds consumer trust and authenticity.\n"
         "• Sponsored Story Highlights: Local story broadcast to neighborhood radius.\n"
         "• Pricing: Affordable tiered subscription:\n"
         "   - Basic: Free (Listing only)\n"
         "   - Silver: ₹299 / month (Radar Boost)\n"
         "   - Gold: ₹699 / month (Stories & Analytics)\n"
         "• High margin, zero inventory holding.",
         CYAN),
        ("MODEL 2: QUICK-GIG ESCROW ENGINE",
         "Platform Take-Rate on Micro-Transactions",
         "• Target: Informal workers, plumbers, electricians, drivers, daily helpers.\n"
         "• Automated UPI Escrow: Employer locks payment; released instantly upon completion.\n"
         "• Low Platform Take-Rate: 2%–3% commission (vs. 20%–30% on traditional aggregators).\n"
         "• Premium Recruiter Listings: Local businesses pay ₹99–₹249 to feature urgent job openings.\n"
         "• Transparent Dispute Resolution: Instant refunds and verified completion proof.\n"
         "• Massive transaction volume velocity.",
         EMERALD),
        ("MODEL 3: ENTERPRISE & CIVIC SAAS",
         "B2G & B2B Gated Mesh Subscriptions",
         "• Target: Gated societies (RWAs), colleges, factories, Gram Panchayats.\n"
         "• Private Community Hubs: Dedicated admin panels, gatekeeper approvals, visitor passes.\n"
         "• Civic Emergency Broadcasts: Municipal alerts & disaster broadcast sirens.\n"
         "• White-Label Enterprise Mesh: Secure intranet communications for off-grid industrial sites.\n"
         "• Pricing: Predictable recurring SaaS:\n"
         "   - RWAs/Societies: ₹2,500 - ₹5,000 / mo\n"
         "   - Institutions/Panchayats: ₹10,000+ / mo.",
         GOLD)
    ]

    for i, (title, sub, desc, col) in enumerate(biz_models):
        x = 50 + i * 293
        box = s14.Shapes.AddShape(5, x, 110, 280, 385)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 16
        btr = btf.TextRange
        btr.Text = f"💼  {title}\n{sub}\n\n{desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 9.2
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.2
        btr.Lines(1).Font.Color.RGB = col
        btr.Lines(2).Font.Bold = True
        btr.Lines(2).Font.Size = 9.8
        btr.Lines(2).Font.Color.RGB = WHITE

    # ==========================================
    # SLIDE 15: FINANCIAL PROJECTIONS & UNIT ECONOMICS
    # ==========================================
    s15 = pres.Slides.Add(15, 12)
    bg15 = s15.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg15, NAVY_DEEP)
    add_header(s15, "Financial Projections: 3-Year Trajectory & Robust Unit Economics", "Financial Projections", 15)

    years = [
        ("YEAR 1 (PILOT CLUSTERS)", 
         "500,000 Active Users\n12,000 Paid Merchants\n₹1.8 Crore ARR ($220K)\nGross Margin: 72%\nFocus: 5 High-Density Tier-2 Districts",
         CYAN),
        ("YEAR 2 (REGIONAL SCALE)", 
         "3,500,000 Active Users\n85,000 Paid Merchants\n₹14.5 Crore ARR ($1.75M)\nGross Margin: 78%\nFocus: Escrow Scaling & Vernacular AI",
         GOLD),
        ("YEAR 3 (PAN-INDIA EXPANSION)", 
         "15,000,000 Active Users\n350,000 Paid Merchants\n₹68.0 Crore ARR ($8.2M)\nGross Margin: 82%\nFocus: Civic SaaS & National Mesh Network",
         EMERALD)
    ]

    for i, (y_title, y_desc, col) in enumerate(years):
        x = 50 + i * 293
        box = s15.Shapes.AddShape(5, x, 110, 280, 130)
        set_shape_flat(box, NAVY_CARD, BORDER_COLOR, 1)
        btf = box.TextFrame
        btf.MarginLeft = btf.MarginRight = btf.MarginTop = btf.MarginBottom = 14
        btr = btf.TextRange
        btr.Text = f"📈  {y_title}\n\n{y_desc}"
        btr.Font.Name = FONT_BODY
        btr.Font.Size = 10.2
        btr.Font.Color.RGB = TEXT_MUTED
        btr.Lines(1).Font.Bold = True
        btr.Lines(1).Font.Size = 11.8
        btr.Lines(1).Font.Color.RGB = col

    ue_box = s15.Shapes.AddShape(5, 50, 255, 860, 235)
    set_shape_flat(ue_box, NAVY_CARD, BORDER_COLOR, 1)
    uetf = ue_box.TextFrame
    uetf.MarginLeft = uetf.MarginRight = uetf.MarginTop = uetf.MarginBottom = 18
    uetr = uetf.TextRange
    uetr.Text = "📊  UNIT ECONOMICS & CAPITAL EFFICIENCY METRICS\n\n" \
               "• CUSTOMER ACQUISITION COST (CAC): ~₹16 - ₹22 ($0.20 - $0.27)\n" \
               "   Driven by organic viral loop of Nearby Discovery Radar and hyper-local merchant QR standees in neighborhood shops.\n\n" \
               "• AVERAGE REVENUE PER USER (ARPU / BLENDED): ~₹145 / Year\n" \
               "   Generated through blended merchant subscriptions, micro-gig transaction fees, and sponsored neighborhood discovery.\n\n" \
               "• LIFETIME VALUE (LTV) TO CAC RATIO: ~7.5x - 9.0x\n" \
               "   Significantly outperforming typical consumer internet benchmarks due to zero user acquisition ad burn and community retention.\n\n" \
               "• PATH TO BREAK-EVEN: Expected by Month 18 at 2.2M monthly active users with cash-flow positive operations across active clusters."
    uetr.Font.Name = FONT_BODY
    uetr.Font.Size = 10.5
    uetr.Font.Color.RGB = TEXT_MUTED
    uetr.Lines(1).Font.Bold = True
    uetr.Lines(1).Font.Size = 12.5
    uetr.Lines(1).Font.Color.RGB = SAFFRON

    # ==========================================
    # SLIDE 16: SUPPORT ASK, CAPITAL ALLOCATION & CONCLUSION
    # ==========================================
    s16 = pres.Slides.Add(16, 12)
    bg16 = s16.Shapes.AddShape(1, 0, 0, 960, 540)
    set_shape_flat(bg16, NAVY_DEEP)
    add_header(s16, "Support & Investment Proposition: Powering the Bharat Revolution", "Support & Investment Proposal", 16)

    ask_box = s16.Shapes.AddShape(5, 50, 110, 420, 280)
    set_shape_flat(ask_box, NAVY_CARD, BORDER_COLOR, 1)
    atf = ask_box.TextFrame
    atf.MarginLeft = atf.MarginRight = atf.MarginTop = atf.MarginBottom = 16
    atr = atf.TextRange
    atr.Text = "💰  THE ASK: ₹2.50 CRORE (~$300,000 USD)\n" \
              "Seed Capital / Innovation Grant for 18-Month Scale\n\n" \
              "• 40% — ENGINEERING & PROTOCOL R&D:\n" \
              "   Decentralized BLE/Wi-Fi mesh routing, Bhashini AI voice integration, WebRTC calling engines.\n\n" \
              "• 25% — SOVEREIGN INDIAN EDGE INFRASTRUCTURE:\n" \
              "   High-availability cloud clusters, STUN/TURN relays, and cryptographic HSM attestation.\n\n" \
              "• 20% — PILOT DEPLOYMENTS & MERCHANT ONBOARDING:\n" \
              "   Ground-level rollout in 5 pilot districts with local merchant associations and RWAs.\n\n" \
              "• 15% — SECURITY AUDITING & LEGAL COMPLIANCE:\n" \
              "   CERT-In security audit, DPI interoperability, and patent filings."
    atr.Font.Name = FONT_BODY
    atr.Font.Size = 9.8
    atr.Font.Color.RGB = TEXT_MUTED
    atr.Lines(1).Font.Bold = True
    atr.Lines(1).Font.Size = 12
    atr.Lines(1).Font.Color.RGB = GOLD
    atr.Lines(2).Font.Bold = True
    atr.Lines(2).Font.Size = 10
    atr.Lines(2).Font.Color.RGB = WHITE

    del_box = s16.Shapes.AddShape(5, 490, 110, 420, 280)
    set_shape_flat(del_box, NAVY_CARD, BORDER_COLOR, 1)
    dtf = del_box.TextFrame
    dtf.MarginLeft = dtf.MarginRight = dtf.MarginTop = dtf.MarginBottom = 16
    dtr = dtf.TextRange
    dtr.Text = "🎯  PROJECTED 18-MONTH IMPACT & DELIVERABLES\n" \
              "Key Milestone Outcomes from Funding\n\n" \
              "• 2,500,000+ Verified Registered Citizens across target Tier 2/3 clusters.\n\n" \
              "• 60,000+ Hyper-Local Merchants active on the Radar & Marketplace.\n\n" \
              "• 1,200,000+ Completed Quick Gigs with guaranteed instant UPI escrow payouts.\n\n" \
              "• Certified Zero-Internet Disaster Mesh Protocol validated in disaster drills.\n\n" \
              "• 22 Scheduled Indian Languages supported with real-time voice and text."
    dtr.Font.Name = FONT_BODY
    dtr.Font.Size = 10
    dtr.Font.Color.RGB = TEXT_MUTED
    dtr.Lines(1).Font.Bold = True
    dtr.Lines(1).Font.Size = 12
    dtr.Lines(1).Font.Color.RGB = EMERALD
    dtr.Lines(2).Font.Bold = True
    dtr.Lines(2).Font.Size = 10
    dtr.Lines(2).Font.Color.RGB = WHITE

    final_banner = s16.Shapes.AddShape(5, 50, 405, 860, 85)
    set_shape_flat(final_banner, NAVY_LIGHT, SAFFRON, 1.5)
    fbtf = final_banner.TextFrame
    fbtf.MarginLeft = fbtf.MarginRight = fbtf.MarginTop = fbtf.MarginBottom = 14
    fbtr = fbtf.TextRange
    fbtr.Text = "JOIN US IN BUILDING INDIA'S SOVEREIGN DIGITAL FUTURE\n" \
               "BharatConnect transforms how Indian communities communicate, trade, and empower one another. With your support, we will bridge the digital divide and build an unshakeable, self-reliant communication backbone for the nation.\n" \
               "Contact & Repository: github.com/Vipinrkv/BharatConnect • Standalone APK: BharatConnect-Native.apk"
    fbtr.Font.Name = FONT_BODY
    fbtr.Font.Size = 10
    fbtr.Font.Color.RGB = WHITE
    fbtr.Lines(1).Font.Bold = True
    fbtr.Lines(1).Font.Size = 12
    fbtr.Lines(1).Font.Color.RGB = SAFFRON

    # Save PPTX and PDF
    base_dir = os.path.dirname(os.path.abspath(__file__))
    pptx_out = os.path.join(base_dir, "BharatConnect_Master_Presentation.pptx")
    pdf_out = os.path.join(base_dir, "BharatConnect_Master_Presentation.pdf")

    print(f"Saving PPTX to: {pptx_out}")
    pres.SaveAs(pptx_out)

    print(f"Exporting PDF to: {pdf_out}")
    # 32 = ppSaveAsPDF
    pres.SaveAs(pdf_out, 32)

    pres.Close()
    ppt.Quit()
    print("SUCCESS: Both PPTX and PDF successfully generated and exported!")

if __name__ == "__main__":
    create_deck()
