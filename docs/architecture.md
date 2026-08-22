# 🏗️ System Architecture & Theme Design Tokens

## Architecture Overview

BharatConnect follows a modular UI-driven architecture built on top of **Kivy 2.3.1** and **KivyMD 2.0.1**. The core application manages screen switching via Kivy's `ScreenManager` with `FadeTransition`.

```
┌──────────────────────────────────────────────────────────┐
│                   BharatConnectApp                       │
│                     (MDApp Engine)                       │
└────────────────────────────┬─────────────────────────────┘
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   ScreenManager Engine                 HybridSyncEngine
(FadeTransition Controller)         (SQLite + Online REST/WS Sync)
            │
            ├──────► SplashScreen (Brand Intro, Connectivity Status Badge, Action Buttons)
            ├──────► LoginScreen (Email/User Input, Password Toggle, Social Login)
            ├──────► RegisterScreen (Account Registration, Terms Checkbox & Validation)
            ├──────► ForgotPasswordScreen (OTP Generation & Password Recovery)
            ├──────► DashboardScreen (Bottom Navigation Container)
            │         ├─► HomeScreenView (Brand Feed, Story Row, Post Likes/Comments)
            │         ├─► ReelsView (Full-Screen Short Videos, Engagement Bar)
            │         ├─► ChatListView & ChatThreadView (Individual/Group/Community Real-Time Streams)
            │         ├─► MarketplaceView (Items, Jobs, Quick Bounties with Filters)
            │         └─► ProfileView (Stats, Bio, Settings Action)
            ├──────► EditProfileScreen (Avatar Customization, Bio, Display Name Updates)
            ├──────► EncryptedCallScreen (E2EE Voice & Video Call Interface, In-Call Controls)
            └──────► SettingsScreen (Account, Privacy, Notification & Session Hardening)
```

---

## 🎨 Single-Root View Hierarchy & Canvas Rendering

To ensure pixel-perfect visual styling without double-rendering or layout bugs:
1. **Single-Root Python Views**: All screen views build clean, single-root layout trees in Python (`build_ui()`). Redundant `.kv` layout auto-loading is bypassed in `main.py` to prevent layout conflicts.
2. **KivyMD 2.0 Theme Colors (`theme_bg_color="Custom"`)**: Cards and buttons explicitly set `theme_bg_color="Custom"` alongside `md_bg_color` so KivyMD 2.0 respects custom palette tokens rather than defaulting to dark surface grey.
3. **Canvas Shader Texture Clipping (`RoundedRectangle`)**: `GradientCard` and `GradientBox` draw canvas gradient textures via Kivy's `RoundedRectangle` instruction mapped to `parse_kivy_radius()`, ensuring smooth rounded corners without protruding square artifacts.

---

## 🎨 Color Palette Tokens

```python
# Color Definitions in utils/helper.py
COLOR_6367FF = [0.388, 0.404, 1.000, 1.0]   # Electric Indigo Primary
COLOR_8494FF = [0.518, 0.580, 1.000, 1.0]   # Soft Ice Blue Accent
COLOR_C9BEFF = [0.788, 0.745, 1.000, 1.0]   # Light Frost Lavender
COLOR_FFDBFD = [1.000, 0.859, 0.992, 1.0]   # Soft Pink Ice Highlight
COLOR_2F2FE4 = [0.184, 0.184, 0.894, 1.0]   # Royal Winter Indigo
COLOR_162E93 = [0.086, 0.180, 0.576, 1.0]   # Midnight Winter Blue
COLOR_1A1953 = [0.102, 0.098, 0.325, 1.0]   # Cold Navy Card Surface
COLOR_080616 = [0.031, 0.024, 0.086, 1.0]   # Deep Frost Midnight Background
```

### Canvas Texture Gradient Generator
`GradientCard` dynamically attaches a linear gradient background texture on its Kivy canvas layer:

```python
def create_gradient_texture(color1_rgb, color2_rgb, width=128, height=128, orientation="horizontal"):
    texture = Texture.create(size=(width, height), colorfmt='rgba')
    buf = bytearray()
    r1, g1, b1 = int(color1_rgb[0] * 255), int(color1_rgb[1] * 255), int(color1_rgb[2] * 255)
    r2, g2, b2 = int(color2_rgb[0] * 255), int(color2_rgb[1] * 255), int(color2_rgb[2] * 255)

    for y in range(height):
        for x in range(width):
            t = (x / (width - 1)) if orientation == "horizontal" else (y / (height - 1))
            r = int(r1 + (r2 - r1) * t)
            g = int(g1 + (g2 - g1) * t)
            b = int(b1 + (b2 - b1) * t)
            buf.extend([r, g, b, 255])

    texture.blit_buffer(bytes(buf), colorfmt='rgba', bufferfmt='ubyte')
    return texture
```
