"""
BharatConnect E2EE Voice & Video Call View (app/screens/call.py)

WhatsApp/Messenger rival call screen featuring HD voice & video layout,
BharatShield E2EE encryption status pill, call timer, and interactive floating controls.
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard

from utils.helper import (
    COLOR_080616,
    COLOR_162E93,
    COLOR_1A1953,
    COLOR_6367FF,
    COLOR_2F2FE4,
    COLOR_FFDBFD,
    COLOR_EMERALD,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    GradientCard,
    GlassCard,
    create_avatar_widget,
    create_icon_button,
    create_pill_badge,
)


class EncryptedCallScreen(MDScreen):
    def __init__(self, callee_name="Alice Johnson", is_video=True, back_callback=None, **kwargs):
        super().__init__(**kwargs)
        self.name = "call"
        self.callee_name = callee_name
        self.is_video = is_video
        self.back_callback = back_callback
        self.is_muted = False
        self.is_cam_off = False
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = GradientCard(
            color1=COLOR_162E93,
            color2=COLOR_080616,
            orientation="vertical",
            padding="20dp",
            spacing="16dp",
            radius=[0, 0, 0, 0],
        )

        # 1. Top E2EE Status Pill Header
        top_bar = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="48dp")
        back_btn = create_icon_button("chevron-left", on_release=self.end_call, size_dp=36, icon_size="26sp")
        top_bar.add_widget(back_btn)

        e2ee_badge = create_pill_badge(
            "🔒 BharatShield™ E2EE Active",
            bg_color=[0.0, 0.9, 0.6, 0.25],
            text_color=COLOR_EMERALD,
            height="32dp",
        )
        top_bar.add_widget(e2ee_badge)

        dots_btn = create_icon_button("dots-vertical", size_dp=36, icon_size="22sp")
        top_bar.add_widget(dots_btn)
        root.add_widget(top_bar)

        # 2. Callee Avatar & Video Visual Container
        call_visual = GlassCard(
            orientation="vertical",
            padding="20dp",
            spacing="12dp",
            radius=[28, 28, 28, 28],
            size_hint_y=0.65,
            pos_hint={"center_x": 0.5},
        )

        inner_box = MDBoxLayout(orientation="vertical", spacing="14dp", pos_hint={"center_x": 0.5, "center_y": 0.5})
        
        av = create_avatar_widget(initials="AJ", bg_hex="#6367FF", size_dp=96, is_circle=True)
        av.pos_hint = {"center_x": 0.5}
        inner_box.add_widget(av)

        name_lbl = MDLabel(
            text=self.callee_name,
            font_style="Headline",
            role="medium",
            bold=True,
            halign="center",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        inner_box.add_widget(name_lbl)

        status_lbl = MDLabel(
            text="HD Video Call • 02:45" if self.is_video else "Voice Call • 02:45",
            font_style="Title",
            role="small",
            halign="center",
            theme_text_color="Custom",
            text_color=COLOR_FFDBFD,
        )
        inner_box.add_widget(status_lbl)
        call_visual.add_widget(inner_box)
        root.add_widget(call_visual)

        # 3. Bottom Call Controls Bar
        controls = MDCard(
            orientation="horizontal",
            padding=["20dp", "10dp", "20dp", "10dp"],
            spacing="16dp",
            size_hint_y=None,
            height="76dp",
            radius=[38, 38, 38, 38],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            pos_hint={"center_x": 0.5},
            elevation=4,
        )

        # Mute Toggle
        self.mute_btn = create_icon_button(
            "microphone" if not self.is_muted else "microphone-off",
            on_release=self.toggle_mute,
            size_dp=52,
            icon_size="26sp",
            icon_color=COLOR_TEXT_MAIN if not self.is_muted else [0.95, 0.22, 0.38, 1.0],
        )
        controls.add_widget(self.mute_btn)

        # Camera Flip
        self.cam_btn = create_icon_button(
            "camera-flip" if not self.is_cam_off else "camera-off",
            on_release=self.toggle_cam,
            size_dp=52,
            icon_size="26sp",
            icon_color=COLOR_TEXT_MAIN if not self.is_cam_off else [0.95, 0.22, 0.38, 1.0],
        )
        controls.add_widget(self.cam_btn)

        # Speaker
        spk_btn = create_icon_button("volume-high", size_dp=52, icon_size="26sp")
        controls.add_widget(spk_btn)

        # End Call Red Button
        end_btn = MDCard(
            size_hint=(None, None),
            size=("56dp", "56dp"),
            radius=[28, 28, 28, 28],
            theme_bg_color="Custom",
            md_bg_color=[0.95, 0.22, 0.38, 1.0],
            ripple_behavior=True,
            on_release=self.end_call,
            pos_hint={"center_y": 0.5},
        )
        end_btn.add_widget(
            MDIcon(
                icon="phone-hangup",
                font_size="28sp",
                pos_hint={"center_x": 0.5, "center_y": 0.5},
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        controls.add_widget(end_btn)

        root.add_widget(controls)
        self.add_widget(root)

    def toggle_mute(self, *args):
        self.is_muted = not self.is_muted
        self.build_ui()

    def toggle_cam(self, *args):
        self.is_cam_off = not self.is_cam_off
        self.build_ui()

    def end_call(self, *args):
        if self.back_callback:
            self.back_callback()
        elif self.manager:
            self.manager.current = "dashboard"
