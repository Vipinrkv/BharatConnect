"""
BharatConnect Design System & Theme Engine
Custom Theme Palette:
  #6367FF (Primary Electric Indigo)
  #8494FF (Secondary Soft Blue)
  #C9BEFF (Light Lavender Accent)
  #FFDBFD (Soft Pastel Pink Highlight)
Includes Native Kivy Linear Gradient Canvas Engine.
"""

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivy.graphics.texture import Texture
from kivy.graphics import Rectangle, Color

# Required Theme Colors
COLOR_6367FF = [0.388, 0.404, 1.0, 1.0]   # Primary Electric Indigo #6367FF
COLOR_8494FF = [0.518, 0.580, 1.0, 1.0]   # Secondary Soft Blue #8494FF
COLOR_C9BEFF = [0.788, 0.745, 1.0, 1.0]   # Light Lavender Accent #C9BEFF
COLOR_FFDBFD = [1.0, 0.859, 0.992, 1.0]   # Soft Pastel Pink Highlight #FFDBFD

# Background & Dark Surfaces
COLOR_BG_DARK = [0.06, 0.08, 0.16, 1.0]       # Deep Slate Midnight
COLOR_TOPBAR_DARK = [0.08, 0.10, 0.20, 1.0]   # Deep Navbar
COLOR_SIDEBAR_DARK = [0.07, 0.09, 0.18, 1.0]  # Deep Sidebar
COLOR_CARD_DARK = [0.11, 0.14, 0.26, 1.0]     # Slate Surface Card
COLOR_CARD_BORDER = [0.25, 0.28, 0.45, 0.7]   # Minimalist Border

# Message Bubble Colors
COLOR_BUBBLE_ME = COLOR_6367FF                # Sender Bubble #6367FF
COLOR_BUBBLE_OTHER = [0.15, 0.18, 0.32, 0.95] # Recipient Bubble
COLOR_ACTIVE_ITEM = [0.388, 0.404, 1.0, 0.22] # Active Item Highlight

# Status & Badges
COLOR_EMERALD = [0.06, 0.73, 0.51, 1.0]       # Glowing Online #10B981
COLOR_AMBER = [0.96, 0.62, 0.16, 1.0]         # Idle #F59E0B
COLOR_CORAL = [0.96, 0.25, 0.37, 1.0]         # Alert #F43F5E

# Typography
COLOR_TEXT_MAIN = [0.97, 0.98, 1.0, 1.0]      # Pure Crisp White #F8FAFC
COLOR_TEXT_MUTED = [0.72, 0.75, 0.88, 1.0]    # Soft Blue-Gray
COLOR_TEXT_SUBTLE = [0.48, 0.52, 0.65, 1.0]   # Muted Hint
COLOR_TEXT_DARK = [0.06, 0.08, 0.16, 1.0]     # Dark Text for Light Accents

# Light Theme Fallbacks
COLOR_BG_LIGHT = [0.95, 0.96, 0.98, 1.0]
COLOR_TOPBAR_LIGHT = [1.0, 1.0, 1.0, 1.0]
COLOR_SIDEBAR_LIGHT = [0.92, 0.94, 0.97, 1.0]
COLOR_CARD_LIGHT = [1.0, 1.0, 1.0, 1.0]
COLOR_CARD_BORDER_LIGHT = [0.85, 0.88, 0.92, 1.0]


def create_gradient_texture(color1_rgb, color2_rgb, width=128, height=128, orientation="horizontal"):
    """
    Creates a smooth 2-color linear gradient Texture for Kivy canvas backgrounds.
    """
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


class GradientCard(MDCard):
    """Card widget with a native Kivy canvas linear gradient background."""
    def __init__(self, color1=COLOR_6367FF, color2=COLOR_8494FF, orientation="horizontal", **kwargs):
        super().__init__(**kwargs)
        self.gradient_color1 = color1
        self.gradient_color2 = color2
        self.gradient_orientation = orientation
        self.elevation = kwargs.get('elevation', 0)
        self.radius = kwargs.get('radius', [16, 16, 16, 16])
        self.md_bg_color = [0, 0, 0, 0]  # Translucent so canvas gradient shows
        self.bind(size=self._update_gradient, pos=self._update_gradient)
        self._update_gradient()

    def _update_gradient(self, *args):
        self.canvas.before.clear()
        with self.canvas.before:
            texture = create_gradient_texture(
                self.gradient_color1, self.gradient_color2,
                orientation=self.gradient_orientation
            )
            Color(1, 1, 1, 1)
            Rectangle(texture=texture, pos=self.pos, size=self.size)


def get_theme_colors(theme_mode="Dark"):
    """Return dictionary of theme colors based on active mode."""
    if theme_mode == "Dark":
        return {
            "bg": COLOR_BG_DARK,
            "topbar": COLOR_TOPBAR_DARK,
            "sidebar": COLOR_SIDEBAR_DARK,
            "card": COLOR_CARD_DARK,
            "border": COLOR_CARD_BORDER,
            "text_main": COLOR_TEXT_MAIN,
            "text_muted": COLOR_TEXT_MUTED,
            "text_subtle": COLOR_TEXT_SUBTLE,
            "c_6367ff": COLOR_6367FF,
            "c_8494ff": COLOR_8494FF,
            "c_c9beff": COLOR_C9BEFF,
            "c_ffdbfd": COLOR_FFDBFD,
            "emerald": COLOR_EMERALD,
            "amber": COLOR_AMBER,
            "bubble_me": COLOR_BUBBLE_ME,
            "bubble_other": COLOR_BUBBLE_OTHER,
            "active_item": COLOR_ACTIVE_ITEM,
        }
    else:
        return {
            "bg": COLOR_BG_LIGHT,
            "topbar": COLOR_TOPBAR_LIGHT,
            "sidebar": COLOR_SIDEBAR_LIGHT,
            "card": COLOR_CARD_LIGHT,
            "border": COLOR_CARD_BORDER_LIGHT,
            "text_main": COLOR_TEXT_DARK,
            "text_muted": [0.38, 0.44, 0.54, 1.0],
            "text_subtle": [0.55, 0.60, 0.70, 1.0],
            "c_6367ff": COLOR_6367FF,
            "c_8494ff": COLOR_8494FF,
            "c_c9beff": COLOR_C9BEFF,
            "c_ffdbfd": COLOR_FFDBFD,
            "emerald": COLOR_EMERALD,
            "amber": COLOR_AMBER,
            "bubble_me": COLOR_6367FF,
            "bubble_other": COLOR_CARD_LIGHT,
            "active_item": [0.388, 0.404, 1.0, 0.15],
        }


def create_pill_badge(text, bg_color=None, text_color=None, height="24dp"):
    """Utility to create a minimalist pill/tag badge widget."""
    if bg_color is None:
        bg_color = [0.388, 0.404, 1.0, 0.18]
    if text_color is None:
        text_color = COLOR_C9BEFF

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
