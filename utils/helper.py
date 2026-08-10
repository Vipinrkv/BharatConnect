"""
BharatConnect UI Helpers & Color System (utils/helper.py)

Contains exact 8 color tokens, linear gradient texture generators,
Material icon wrappers, and pixel-perfect button/badge components matching Image 1 & Image 2.
"""

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard

from kivy.graphics.texture import Texture
from kivy.graphics import Rectangle, RoundedRectangle, Color

# 8 Core Palette Color Tokens (RGBA Lists for Kivy 0.0 - 1.0)
COLOR_6367FF = [0.388, 0.404, 1.000, 1.0]   # Primary Electric Indigo (#6367FF)
COLOR_8494FF = [0.518, 0.580, 1.000, 1.0]   # Soft Ice Blue Accent (#8494FF)
COLOR_C9BEFF = [0.788, 0.745, 1.000, 1.0]   # Light Frost Lavender (#C9BEFF)
COLOR_FFDBFD = [1.000, 0.859, 0.992, 1.0]   # Soft Pink Ice Highlight (#FFDBFD)
COLOR_2F2FE4 = [0.184, 0.184, 0.894, 1.0]   # Royal Winter Indigo (#2F2FE4)
COLOR_162E93 = [0.086, 0.180, 0.576, 1.0]   # Midnight Winter Blue (#162E93)
COLOR_1A1953 = [0.102, 0.098, 0.325, 1.0]   # Cold Navy Surface Card (#1A1953)
COLOR_080616 = [0.031, 0.024, 0.086, 1.0]   # Deep Frost Midnight Background (#080616)

# Text & Accent Tokens
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


def hex_to_rgba(hex_str, alpha=1.0):
    """Converts #RRGGBB string into Kivy RGBA list [0.0 - 1.0]."""
    clean_hex = hex_str.lstrip('#')
    if len(clean_hex) == 6:
        r = int(clean_hex[0:2], 16) / 255.0
        g = int(clean_hex[2:4], 16) / 255.0
        b = int(clean_hex[4:6], 16) / 255.0
        return [r, g, b, alpha]
    return [0.388, 0.404, 1.0, alpha]


def parse_kivy_radius(rad):
    """Parses card radius into float list compatible with Kivy RoundedRectangle."""
    if not rad:
        return [16.0, 16.0, 16.0, 16.0]
    if isinstance(rad, (int, float)):
        return [float(rad)] * 4
    if isinstance(rad, (list, tuple)):
        clean = []
        for item in rad:
            if isinstance(item, (list, tuple)):
                clean.append(float(item[0]))
            else:
                clean.append(float(item))
        if len(clean) == 1:
            return clean * 4
        return clean[:4]
    return [16.0, 16.0, 16.0, 16.0]


class GlassCard(MDCard):
    """Ultra-premium frosted glass translucent dark mode card widget."""
    def __init__(self, **kwargs):
        kwargs['theme_bg_color'] = 'Custom'
        kwargs['md_bg_color'] = kwargs.get('md_bg_color', [0.07, 0.06, 0.17, 0.75])
        kwargs['line_color'] = kwargs.get('line_color', [0.388, 0.404, 1.000, 0.35])
        kwargs['line_width'] = kwargs.get('line_width', 1.2)
        kwargs['elevation'] = kwargs.get('elevation', 4)
        super().__init__(**kwargs)


class GlassBox(MDBoxLayout):
    """Ultra-premium frosted glass translucent container layout."""
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        rad = kwargs.get('radius', [16, 16, 16, 16])
        with self.canvas.before:
            Color(0.07, 0.06, 0.17, 0.8)
            self.glass_rect = RoundedRectangle(pos=self.pos, size=self.size, radius=parse_kivy_radius(rad))
        self.bind(pos=self._update_glass, size=self._update_glass)

    def _update_glass(self, instance, value):
        self.glass_rect.pos = instance.pos
        self.glass_rect.size = instance.size


class GradientCard(MDCard):
    """Card widget with native Kivy linear gradient background."""
    def __init__(self, color1=COLOR_6367FF, color2=COLOR_2F2FE4, orientation="vertical", **kwargs):
        kwargs['theme_bg_color'] = 'Custom'
        kwargs['md_bg_color'] = color1
        kwargs['orientation'] = orientation
        super().__init__(**kwargs)
        self.orientation = orientation
        self.gradient_color1 = color1
        self.gradient_color2 = color2
        self.gradient_orientation = orientation
        self.elevation = kwargs.get('elevation', 0)
        self.radius = kwargs.get('radius', [16, 16, 16, 16])

        self.texture = create_gradient_texture(
            self.gradient_color1, self.gradient_color2,
            orientation=self.gradient_orientation
        )

        with self.canvas.before:
            Color(1, 1, 1, 1)
            self.rect = RoundedRectangle(
                texture=self.texture,
                pos=self.pos,
                size=self.size,
                radius=parse_kivy_radius(self.radius)
            )

        self.bind(pos=self._update_rect, size=self._update_rect, radius=self._update_rect)

    def _update_rect(self, instance, value):
        self.rect.pos = instance.pos
        self.rect.size = instance.size
        self.rect.radius = parse_kivy_radius(self.radius)


class GradientBox(MDBoxLayout):
    """Layout widget with native Kivy linear gradient background."""
    def __init__(self, color1=COLOR_162E93, color2=COLOR_1A1953, orientation_grad="horizontal", **kwargs):
        super().__init__(**kwargs)
        self.gradient_color1 = color1
        self.gradient_color2 = color2
        self.gradient_orientation = orientation_grad
        rad = kwargs.get('radius', [0, 0, 0, 0])

        self.texture = create_gradient_texture(
            self.gradient_color1, self.gradient_color2,
            orientation=self.gradient_orientation
        )

        with self.canvas.before:
            Color(1, 1, 1, 1)
            self.rect = RoundedRectangle(
                texture=self.texture,
                pos=self.pos,
                size=self.size,
                radius=parse_kivy_radius(rad)
            )

        self.bind(pos=self._update_rect, size=self._update_rect)

    def _update_rect(self, instance, value):
        self.rect.pos = instance.pos
        self.rect.size = instance.size


def create_pill_badge(text, bg_color=None, text_color=None, height="28dp"):
    """Utility to create a minimalist vibrant pill badge with non-wrapping width."""
    if bg_color is None:
        bg_color = [0.388, 0.404, 1.000, 0.35]
    if text_color is None:
        text_color = COLOR_FFDBFD

    text_str = str(text) if text is not None else ""
    calc_width = min(160, max(80, len(text_str) * 8 + 24))
    badge_card = MDCard(
        orientation="horizontal",
        padding=["8dp", "2dp", "8dp", "2dp"],
        size_hint=(None, None),
        size=(f"{calc_width}dp", height),
        radius=[14, 14, 14, 14],
        theme_bg_color="Custom",
        md_bg_color=bg_color,
        line_color=COLOR_6367FF,
        line_width=1,
        elevation=0,
        pos_hint={"center_x": 0.5}
    )
    lbl = MDLabel(
        text=text_str,
        font_style="Label",
        role="small",
        bold=True,
        theme_text_color="Custom",
        text_color=text_color,
        halign="center",
        valign="center",
        shorten=True,
    )
    badge_card.add_widget(lbl)
    return badge_card


def create_primary_button(text, on_release=None, size_hint_x=1.0, height="50dp", color1=COLOR_6367FF, color2=COLOR_2F2FE4):
    """Creates a sleek full-width rounded primary button matching Image 1 & 2 mockups."""
    btn_card = MDCard(
        size_hint_x=size_hint_x,
        size_hint_y=None,
        height=height,
        radius=[25, 25, 25, 25],
        pos_hint={"center_x": 0.5},
        theme_bg_color="Custom",
        md_bg_color=color1,
        ripple_behavior=True,
        on_release=on_release,
    )

    texture = create_gradient_texture(color1, color2, orientation="horizontal")
    with btn_card.canvas.before:
        Color(1, 1, 1, 1)
        btn_card._rect = RoundedRectangle(
            texture=texture,
            pos=btn_card.pos,
            size=btn_card.size,
            radius=parse_kivy_radius([25, 25, 25, 25])
        )
    btn_card.bind(
        pos=lambda inst, val: setattr(inst._rect, 'pos', val),
        size=lambda inst, val: setattr(inst._rect, 'size', val)
    )

    lbl = MDLabel(
        text=text,
        font_style="Title",
        role="medium",
        bold=True,
        halign="center",
        valign="center",
        theme_text_color="Custom",
        text_color=COLOR_TEXT_MAIN,
    )
    btn_card.add_widget(lbl)
    return btn_card


def create_outlined_button(text, on_release=None, size_hint_x=1.0, height="50dp", border_color=COLOR_8494FF):
    """Creates a sleek outlined pill button matching Image 1 & 2 mockups."""
    btn_card = MDCard(
        size_hint_x=size_hint_x,
        size_hint_y=None,
        height=height,
        radius=[25, 25, 25, 25],
        pos_hint={"center_x": 0.5},
        theme_bg_color="Custom",
        md_bg_color=[0.102, 0.098, 0.325, 0.7],
        line_color=border_color,
        line_width=1.5,
        ripple_behavior=True,
        on_release=on_release,
    )
    lbl = MDLabel(
        text=text,
        font_style="Title",
        role="medium",
        bold=True,
        halign="center",
        valign="center",
        theme_text_color="Custom",
        text_color=COLOR_TEXT_MAIN,
    )
    btn_card.add_widget(lbl)
    return btn_card


def create_avatar_widget(initials="AM", bg_hex="#6367FF", size_dp=44, is_circle=True):
    """Creates a circular/rounded avatar card with initials and gradient background."""
    rgba = hex_to_rgba(bg_hex)
    size_val = size_dp if isinstance(size_dp, int) else int(str(size_dp).replace("dp", ""))
    half_size = size_val // 2
    radius_val = [half_size] * 4 if is_circle else [12, 12, 12, 12]

    avatar_card = MDCard(
        size_hint=(None, None),
        size=(f"{size_val}dp", f"{size_val}dp"),
        radius=radius_val,
        theme_bg_color="Custom",
        md_bg_color=rgba,
        elevation=0
    )

    lbl = MDLabel(
        text=initials,
        font_style="Title",
        role="medium" if half_size > 20 else "small",
        bold=True,
        halign="center",
        valign="center",
        theme_text_color="Custom",
        text_color=COLOR_TEXT_MAIN
    )
    avatar_card.add_widget(lbl)
    return avatar_card


def create_story_avatar_widget(initials="AM", bg_hex="#6367FF", size_dp=56, is_user=False, has_unseen=True):
    """Creates an Instagram-style story avatar with a vibrant gradient ring."""
    size_val = size_dp if isinstance(size_dp, int) else int(str(size_dp).replace("dp", ""))
    outer_size = size_val + 6
    outer_radius = outer_size // 2

    outer_card = MDCard(
        size_hint=(None, None),
        size=(f"{outer_size}dp", f"{outer_size}dp"),
        radius=[outer_radius] * 4,
        theme_bg_color="Custom",
        md_bg_color=[0.88, 0.18, 0.42, 1.0] if has_unseen else [0.2, 0.2, 0.4, 0.6],
        line_color=COLOR_6367FF if has_unseen else COLOR_1A1953,
        line_width=2.0 if has_unseen else 1.0,
        elevation=0,
        pos_hint={"center_x": 0.5}
    )

    inner_avatar = create_avatar_widget(initials=initials, bg_hex=bg_hex, size_dp=size_dp, is_circle=True)
    inner_avatar.pos_hint = {"center_x": 0.5, "center_y": 0.5}
    outer_card.add_widget(inner_avatar)
    return outer_card


def create_icon_button(icon_name, on_release=None, icon_color=COLOR_TEXT_MAIN, size_dp=38, icon_size="22sp"):
    """Creates a sleek, touch-responsive icon button using MDIcon vector glyphs."""
    card_kwargs = {
        "size_hint": (None, None),
        "size": (f"{size_dp}dp", f"{size_dp}dp"),
        "radius": [size_dp // 2] * 4,
        "theme_bg_color": "Custom",
        "md_bg_color": [0, 0, 0, 0],
        "ripple_behavior": True,
    }
    if on_release is not None:
        card_kwargs["on_release"] = on_release

    card_btn = MDCard(**card_kwargs)
    icon_w = MDIcon(
        icon=icon_name,
        font_size=icon_size,
        pos_hint={"center_x": 0.5, "center_y": 0.5},
        theme_text_color="Custom",
        text_color=icon_color,
    )
    card_btn.add_widget(icon_w)
    return card_btn


def create_verified_badge(size_sp="16sp"):
    """Creates an Instagram-style blue checkmark verified badge."""
    return MDIcon(
        icon="check-decagram",
        font_size=size_sp,
        theme_text_color="Custom",
        text_color=COLOR_6367FF,
        pos_hint={"center_y": 0.5},
    )


def compress_image_base64(base64_data, max_size=(128, 128), quality=75):
    """
    Compresses a Base64 image string to a lightweight WebP/JPEG thumbnail (< 15 KB).
    Prevents storage inflation and database cell limits.
    """
    if not base64_data or not isinstance(base64_data, str) or not base64_data.startswith("data:image"):
        return base64_data

    try:
        import base64
        import io
        from PIL import Image

        header, encoded = base64_data.split(",", 1)
        image_bytes = base64.b64decode(encoded)
        img = Image.open(io.BytesIO(image_bytes))

        # Convert palette/RGBA images to RGB for JPG/WebP compression
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")

        img.thumbnail(max_size, Image.Resampling.LANCZOS)

        out_buffer = io.BytesIO()
        img.save(out_buffer, format="JPEG", quality=quality, optimize=True)
        compressed_bytes = out_buffer.getvalue()
        compressed_b64 = base64.b64encode(compressed_bytes).decode("ascii")

        return f"data:image/jpeg;base64,{compressed_b64}"
    except Exception:
        # Fallback to original string if Pillow is not available or error occurs
        return base64_data

