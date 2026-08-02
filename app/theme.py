"""
BharatConnect Vibrant Mobile Design System & Theme Engine
Gradient Palette:
  #6367FF (Primary Electric Indigo)
  #8494FF (Soft Ice Blue Accent)
  #C9BEFF (Light Frost Lavender)
  #FFDBFD (Soft Pink Ice Highlight)
  #2F2FE4 (Royal Winter Indigo)
  #162E93 (Midnight Winter Blue)
  #1A1953 (Cold Navy Surface Card)
  #080616 (Deep Frost Midnight Background)
"""

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivy.graphics.texture import Texture
from kivy.graphics import Rectangle, Color

# User-Specified 8 Color Tokens (RGBA Lists for Kivy 0.0 - 1.0)
COLOR_6367FF = [0.388, 0.404, 1.000, 1.0]   # Primary Electric Indigo
COLOR_8494FF = [0.518, 0.580, 1.000, 1.0]   # Soft Ice Blue Accent
COLOR_C9BEFF = [0.788, 0.745, 1.000, 1.0]   # Light Frost Lavender
COLOR_FFDBFD = [1.000, 0.859, 0.992, 1.0]   # Soft Pink Ice Highlight
COLOR_2F2FE4 = [0.184, 0.184, 0.894, 1.0]   # Royal Winter Indigo
COLOR_162E93 = [0.086, 0.180, 0.576, 1.0]   # Midnight Winter Blue
COLOR_1A1953 = [0.102, 0.098, 0.325, 1.0]   # Cold Navy Surface Card
COLOR_080616 = [0.031, 0.024, 0.086, 1.0]   # Deep Frost Midnight Background

# Vibrant Text & Accent Tokens
COLOR_TEXT_MAIN = [1.000, 1.000, 1.000, 1.0]
COLOR_TEXT_MUTED = [0.850, 0.880, 1.000, 1.0]
COLOR_TEXT_SUBTLE = [0.650, 0.700, 0.900, 1.0]
COLOR_EMERALD = [0.000, 0.900, 0.600, 1.0]
COLOR_AMBER = [1.000, 0.700, 0.200, 1.0]


def create_gradient_texture(color1_rgb, color2_rgb, width=128, height=128, orientation="horizontal"):
    """Creates a smooth 2-color linear gradient Texture."""
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


def create_vibrant_3stop_gradient(c1, c2, c3, width=128, height=128, orientation="horizontal"):
    """Creates a ultra-vibrant 3-color linear gradient Texture."""
    texture = Texture.create(size=(width, height), colorfmt='rgba')
    buf = bytearray()

    r1, g1, b1 = int(c1[0] * 255), int(c1[1] * 255), int(c1[2] * 255)
    r2, g2, b2 = int(c2[0] * 255), int(c2[1] * 255), int(c2[2] * 255)
    r3, g3, b3 = int(c3[0] * 255), int(c3[1] * 255), int(c3[2] * 255)

    for y in range(height):
        for x in range(width):
            t = (x / (width - 1)) if orientation == "horizontal" else (y / (height - 1))
            if t < 0.5:
                factor = t * 2.0
                r = int(r1 + (r2 - r1) * factor)
                g = int(g1 + (g2 - g1) * factor)
                b = int(b1 + (b2 - b1) * factor)
            else:
                factor = (t - 0.5) * 2.0
                r = int(r2 + (r3 - r2) * factor)
                g = int(g2 + (g3 - g2) * factor)
                b = int(b2 + (b3 - b2) * factor)
            buf.extend([r, g, b, 255])

    texture.blit_buffer(bytes(buf), colorfmt='rgba', bufferfmt='ubyte')
    return texture


class GradientCard(MDCard):
    """Card widget with native Kivy linear gradient texture background."""
    def __init__(self, color1=COLOR_6367FF, color2=COLOR_2F2FE4, color3=None, orientation="horizontal", **kwargs):
        super().__init__(**kwargs)
        self.gradient_color1 = color1
        self.gradient_color2 = color2
        self.gradient_color3 = color3
        self.gradient_orientation = orientation
        self.elevation = kwargs.get('elevation', 0)
        self.radius = kwargs.get('radius', [16, 16, 16, 16])
        self.md_bg_color = [0, 0, 0, 0]  # Translucent canvas layer

        if self.gradient_color3:
            self.texture = create_vibrant_3stop_gradient(
                self.gradient_color1, self.gradient_color2, self.gradient_color3,
                orientation=self.gradient_orientation
            )
        else:
            self.texture = create_gradient_texture(
                self.gradient_color1, self.gradient_color2,
                orientation=self.gradient_orientation
            )

        with self.canvas.before:
            Color(1, 1, 1, 1)
            self.rect = Rectangle(texture=self.texture, pos=self.pos, size=self.size)

        self.bind(pos=self._update_rect, size=self._update_rect)

    def _update_rect(self, instance, value):
        self.rect.pos = instance.pos
        self.rect.size = instance.size


class GradientBox(MDBoxLayout):
    """Layout widget with native Kivy linear gradient background."""
    def __init__(self, color1=COLOR_162E93, color2=COLOR_1A1953, color3=None, orientation_grad="horizontal", **kwargs):
        super().__init__(**kwargs)
        self.gradient_color1 = color1
        self.gradient_color2 = color2
        self.gradient_color3 = color3
        self.gradient_orientation = orientation_grad

        if self.gradient_color3:
            self.texture = create_vibrant_3stop_gradient(
                self.gradient_color1, self.gradient_color2, self.gradient_color3,
                orientation=self.gradient_orientation
            )
        else:
            self.texture = create_gradient_texture(
                self.gradient_color1, self.gradient_color2,
                orientation=self.gradient_orientation
            )

        with self.canvas.before:
            Color(1, 1, 1, 1)
            self.rect = Rectangle(texture=self.texture, pos=self.pos, size=self.size)

        self.bind(pos=self._update_rect, size=self._update_rect)

    def _update_rect(self, instance, value):
        self.rect.pos = instance.pos
        self.rect.size = instance.size


def get_theme_colors(theme_mode="Dark"):
    """Returns vibrant color dictionary."""
    return {
        "bg": COLOR_080616,
        "topbar": COLOR_162E93,
        "sidebar": COLOR_1A1953,
        "card": COLOR_1A1953,
        "text_main": COLOR_TEXT_MAIN,
        "text_muted": COLOR_TEXT_MUTED,
        "text_subtle": COLOR_TEXT_SUBTLE,
        "c_6367ff": COLOR_6367FF,
        "c_8494ff": COLOR_8494FF,
        "c_c9beff": COLOR_C9BEFF,
        "c_ffdbfd": COLOR_FFDBFD,
        "c_2f2fe4": COLOR_2F2FE4,
        "c_162e93": COLOR_162E93,
        "c_1a1953": COLOR_1A1953,
        "c_080616": COLOR_080616,
        "emerald": COLOR_EMERALD,
        "amber": COLOR_AMBER,
    }


def create_pill_badge(text, bg_color=None, text_color=None, height="24dp"):
    """Utility to create a minimalist vibrant pill/tag badge widget."""
    if bg_color is None:
        bg_color = [0.388, 0.404, 1.000, 0.35]
    if text_color is None:
        text_color = COLOR_FFDBFD

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
    """Utility to create a sleek status indicator dot + text badge using exact palette tokens."""
    p_upper = (presence or "OFFLINE").upper()
    if p_upper == "ONLINE":
        dot_color = COLOR_6367FF
        text_color = COLOR_FFDBFD
    elif p_upper == "IDLE":
        dot_color = COLOR_8494FF
        text_color = COLOR_C9BEFF
    else:
        dot_color = COLOR_162E93
        text_color = COLOR_TEXT_MUTED

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
