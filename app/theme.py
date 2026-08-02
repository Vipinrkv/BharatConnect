"""
BharatConnect Design System & Theme Engine
Vibrant & Cool Minimalist Theme (Dark Slate + Electric Cyan + Purple + Emerald)
"""

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard


# Color Palette Definitions (RGBA Lists for Kivy Compatibility 0.0 - 1.0)
COLOR_BG_DARK = [0.06, 0.09, 0.16, 1.0]       # Deep Slate Midnight #0F172A
COLOR_TOPBAR_DARK = [0.07, 0.11, 0.20, 1.0]   # Deep Navbar #121C33
COLOR_SIDEBAR_DARK = [0.08, 0.12, 0.22, 1.0]  # Deep Sidebar #141E38
COLOR_CARD_DARK = [0.12, 0.17, 0.29, 1.0]     # Cool Slate Card #1E2B4A
COLOR_CARD_ALT_DARK = [0.15, 0.21, 0.35, 1.0] # Secondary Slate Card #263559
COLOR_CARD_BORDER = [0.22, 0.30, 0.46, 0.7]   # Minimalist Border #384C75

# Accents
COLOR_CYAN = [0.02, 0.71, 0.83, 1.0]          # Vibrant Cyan #06B6D4
COLOR_CYAN_GLOW = [0.22, 0.94, 1.0, 1.0]      # Electric Cyan #38BDF8
COLOR_PURPLE = [0.55, 0.36, 0.96, 1.0]        # Vibrant Purple/Violet #8B5CF6
COLOR_BLUE_ME = [0.15, 0.42, 0.82, 0.95]      # Electric Blue Bubble
COLOR_BUBBLE_OTHER = [0.14, 0.19, 0.31, 0.95] # Slate Dark Bubble
COLOR_ACTIVE_ITEM = [0.02, 0.71, 0.83, 0.2]  # Active Nav Highlight

# Status & Badges
COLOR_EMERALD = [0.06, 0.73, 0.51, 1.0]       # Glowing Online / Success #10B981
COLOR_AMBER = [0.96, 0.62, 0.16, 1.0]         # Idle / Warning #F59E0B
COLOR_CORAL = [0.96, 0.25, 0.37, 1.0]         # Offline / Alert #F43F5E

# Typography / Text Colors
COLOR_TEXT_MAIN = [0.97, 0.98, 1.0, 1.0]      # Pure Crisp White #F8FAFC
COLOR_TEXT_MUTED = [0.58, 0.64, 0.73, 1.0]    # Slate Silver #94A3B8
COLOR_TEXT_SUBTLE = [0.39, 0.45, 0.55, 1.0]   # Slate Hint #64748B

# Light Theme Fallbacks
COLOR_BG_LIGHT = [0.95, 0.96, 0.98, 1.0]
COLOR_TOPBAR_LIGHT = [1.0, 1.0, 1.0, 1.0]
COLOR_SIDEBAR_LIGHT = [0.92, 0.94, 0.97, 1.0]
COLOR_CARD_LIGHT = [1.0, 1.0, 1.0, 1.0]
COLOR_CARD_BORDER_LIGHT = [0.85, 0.88, 0.92, 1.0]
COLOR_TEXT_MAIN_LIGHT = [0.06, 0.09, 0.16, 1.0]
COLOR_TEXT_MUTED_LIGHT = [0.38, 0.44, 0.54, 1.0]


def get_theme_colors(theme_mode="Dark"):
    """Return dictionary of theme colors based on active mode."""
    if theme_mode == "Dark":
        return {
            "bg": COLOR_BG_DARK,
            "topbar": COLOR_TOPBAR_DARK,
            "sidebar": COLOR_SIDEBAR_DARK,
            "card": COLOR_CARD_DARK,
            "card_alt": COLOR_CARD_ALT_DARK,
            "border": COLOR_CARD_BORDER,
            "text_main": COLOR_TEXT_MAIN,
            "text_muted": COLOR_TEXT_MUTED,
            "text_subtle": COLOR_TEXT_SUBTLE,
            "cyan": COLOR_CYAN,
            "purple": COLOR_PURPLE,
            "emerald": COLOR_EMERALD,
            "amber": COLOR_AMBER,
            "bubble_me": COLOR_BLUE_ME,
            "bubble_other": COLOR_BUBBLE_OTHER,
            "active_item": COLOR_ACTIVE_ITEM,
        }
    else:
        return {
            "bg": COLOR_BG_LIGHT,
            "topbar": COLOR_TOPBAR_LIGHT,
            "sidebar": COLOR_SIDEBAR_LIGHT,
            "card": COLOR_CARD_LIGHT,
            "card_alt": COLOR_CARD_LIGHT,
            "border": COLOR_CARD_BORDER_LIGHT,
            "text_main": COLOR_TEXT_MAIN_LIGHT,
            "text_muted": COLOR_TEXT_MUTED_LIGHT,
            "text_subtle": COLOR_TEXT_MUTED_LIGHT,
            "cyan": COLOR_CYAN,
            "purple": COLOR_PURPLE,
            "emerald": COLOR_EMERALD,
            "amber": COLOR_AMBER,
            "bubble_me": COLOR_BLUE_ME,
            "bubble_other": COLOR_CARD_LIGHT,
            "active_item": [0.02, 0.71, 0.83, 0.15],
        }


def create_pill_badge(text, bg_color=None, text_color=None, height="24dp"):
    """Utility to create a minimalist pill/tag badge widget."""
    if bg_color is None:
        bg_color = [0.02, 0.71, 0.83, 0.15]
    if text_color is None:
        text_color = COLOR_CYAN_GLOW

    badge_card = MDCard(
        orientation="horizontal",
        padding=["10dp", "2dp", "10dp", "2dp"],
        size_hint=(None, None),
        height=height,
        radius=[12, 12, 12, 12],
        md_bg_color=bg_color,
        elevation=0
    )
    lbl = MDLabel(
        text=text,
        font_style="Label",
        role="small",
        bold=True,
        theme_text_color="Custom",
        text_color=text_color,
        adaptive_width=True,
        halign="center",
        valign="center"
    )
    badge_card.add_widget(lbl)
    return badge_card


def create_presence_badge(presence):
    """Utility to create a sleek status indicator dot + text badge."""
    p_upper = (presence or "OFFLINE").upper()
    if p_upper == "ONLINE":
        dot_color = COLOR_EMERALD
        text_color = COLOR_EMERALD
    elif p_upper == "IDLE":
        dot_color = COLOR_AMBER
        text_color = COLOR_AMBER
    else:
        dot_color = COLOR_TEXT_SUBTLE
        text_color = COLOR_TEXT_SUBTLE

    box = MDBoxLayout(
        orientation="horizontal",
        spacing="6dp",
        size_hint=(None, None),
        height="24dp",
        adaptive_width=True
    )

    dot = MDCard(
        size_hint=(None, None),
        size=("8dp", "8dp"),
        radius=[4, 4, 4, 4],
        md_bg_color=dot_color,
        elevation=0,
        pos_hint={"center_y": 0.5}
    )
    box.add_widget(dot)

    lbl = MDLabel(
        text=p_upper,
        font_style="Label",
        role="small",
        bold=True,
        theme_text_color="Custom",
        text_color=text_color,
        adaptive_width=True,
        pos_hint={"center_y": 0.5}
    )
    box.add_widget(lbl)
    return box
