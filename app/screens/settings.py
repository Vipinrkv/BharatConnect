"""
BharatConnect Settings Screen (app/screens/settings.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivy.uix.scrollview import ScrollView

from database.db import db_engine
from utils.helper import (
    COLOR_080616,
    COLOR_1A1953,
    COLOR_162E93,
    COLOR_6367FF,
    COLOR_2F2FE4,
    COLOR_8494FF,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    COLOR_TEXT_SUBTLE,
    GradientCard,
    create_icon_button,
)
from kivymd.uix.label import MDLabel, MDIcon


class SettingsScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "settings"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = MDBoxLayout(
            orientation="vertical",
            md_bg_color=COLOR_080616,
        )

        # Top Bar with Vector Back Button
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["8dp", "8dp", "16dp", "8dp"],
            spacing="10dp",
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )

        back_btn = create_icon_button("arrow-left", on_release=self.go_back, size_dp=36, icon_size="24sp")
        top_bar.add_widget(back_btn)

        title_lbl = MDLabel(
            text="Settings",
            font_style="Title",
            role="large",
            bold=True,
            valign="center",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        top_bar.add_widget(title_lbl)
        root.add_widget(top_bar)

        # Scrollable Settings Options
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        content = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            padding=["14dp", "12dp", "14dp", "20dp"],
            size_hint_y=None,
        )
        content.bind(minimum_height=content.setter("height"))

        settings_card = MDCard(
            orientation="vertical",
            padding="6dp",
            spacing="4dp",
            radius=[18, 18, 18, 18],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            elevation=2,
            size_hint_y=None,
        )
        settings_card.bind(minimum_height=settings_card.setter("height"))

        settings_items = [
            ("account-outline", "Account", "Manage your account"),
            ("lock-outline", "Privacy", "Control your privacy"),
            ("bell-outline", "Notifications", "Manage notifications"),
            ("theme-light-dark", "Theme", "Dark Mode"),
            ("translate", "Language", "English"),
            ("help-circle-outline", "Help & Support", "Get help"),
            ("information-outline", "About Us", "Learn more about us"),
        ]

        for icon_name, title, desc in settings_items:
            row = MDBoxLayout(
                orientation="horizontal",
                padding=["12dp", "10dp", "12dp", "10dp"],
                spacing="12dp",
                size_hint_y=None,
                height="56dp",
            )

            icon_w = MDIcon(
                icon=icon_name,
                font_size="22sp",
                size_hint_x=None,
                width="28dp",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_8494FF,
            )
            row.add_widget(icon_w)

            info_box = MDBoxLayout(orientation="vertical", spacing="2dp")
            info_box.add_widget(
                MDLabel(
                    text=title,
                    font_style="Title",
                    role="small",
                    bold=True,
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                )
            )
            info_box.add_widget(
                MDLabel(
                    text=desc,
                    font_style="Label",
                    role="small",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MUTED,
                )
            )
            row.add_widget(info_box)

            row.add_widget(
                MDLabel(
                    text=">",
                    halign="right",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_SUBTLE,
                    size_hint_x=None,
                    width="20dp",
                    valign="center",
                )
            )
            settings_card.add_widget(row)

        content.add_widget(settings_card)

        # Full-width Logout Button (Image 2 mockup 12)
        logout_btn = GradientCard(
            color1=COLOR_162E93,
            color2=COLOR_2F2FE4,
            size_hint_y=None,
            height="48dp",
            radius=[14, 14, 14, 14],
            ripple_behavior=True,
            on_release=self.do_logout,
        )
        logout_btn.add_widget(
            MDLabel(
                text="Logout",
                font_style="Title",
                role="medium",
                bold=True,
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        content.add_widget(logout_btn)

        scroll.add_widget(content)
        root.add_widget(scroll)

        self.add_widget(root)

    def go_back(self, *args):
        self.manager.current = "dashboard"

    def do_logout(self, *args):
        self.manager.current = "login"
