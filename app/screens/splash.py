"""
BharatConnect Minimalist Vibrant Splash Screen
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout

from app.theme import (
    COLOR_BG_DARK, COLOR_CARD_DARK, COLOR_CARD_BORDER,
    COLOR_CYAN, COLOR_CYAN_GLOW, COLOR_PURPLE,
    COLOR_TEXT_MAIN, COLOR_TEXT_MUTED, create_pill_badge
)


class SplashScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "splash"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = MDBoxLayout(
            orientation="vertical",
            padding=["24dp", "24dp", "24dp", "24dp"],
            spacing="20dp",
            md_bg_color=COLOR_BG_DARK
        )

        scroll = ScrollView()
        content = MDBoxLayout(
            orientation="vertical",
            spacing="20dp",
            size_hint_y=None,
            padding=["0dp", "8dp", "0dp", "16dp"]
        )
        content.bind(minimum_height=content.setter("height"))

        # Modern Minimalist Hero Card with Cool Vibrant Styling
        hero_card = MDCard(
            orientation="vertical",
            padding="24dp",
            spacing="14dp",
            size_hint_y=None,
            height="210dp",
            radius=[16, 16, 16, 16],
            md_bg_color=[0.11, 0.16, 0.28, 1.0],
            line_color=COLOR_CARD_BORDER,
            elevation=0
        )

        badge = create_pill_badge(
            "🇮🇳 BHARATCONNECT v2.0 • 100% PYTHON",
            bg_color=[0.02, 0.71, 0.83, 0.18],
            text_color=COLOR_CYAN_GLOW,
            height="26dp"
        )
        hero_card.add_widget(badge)

        hero_card.add_widget(MDLabel(
            text="Modern Production Text Messaging Platform",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))

        hero_card.add_widget(MDLabel(
            text="Built with Python, Kivy & KivyMD for native sub-50ms text communication, real-time sync, developer communities, and local tech marketplace.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED
        ))

        content.add_widget(hero_card)

        # Features Section Heading
        feat_heading = MDLabel(
            text="⚡ Core Architecture & Features",
            font_style="Title",
            role="large",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
            size_hint_y=None,
            height="36dp"
        )
        content.add_widget(feat_heading)

        grid = GridLayout(cols=2, spacing="16dp", size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        features = [
            ("⚡ Sub-50ms Realtime Sync", "FastAPI WebSockets + O(1) Python indexed storage engine.", COLOR_CYAN),
            ("👥 Multi-User Switcher", "Instant identity switching between Vipin, Rahul, Priya & Ananya.", COLOR_PURPLE),
            ("🌐 Tech Communities 🇮🇳", "Connect with developers, AI builders & startup founders across India.", COLOR_CYAN),
            ("🛒 Tech Marketplace", "Buy, sell & offer developer services with direct instant chat inquiry.", COLOR_PURPLE)
        ]

        for feat_title, feat_desc, accent_col in features:
            card = MDCard(
                orientation="vertical",
                padding="18dp",
                spacing="8dp",
                size_hint_y=None,
                height="130dp",
                radius=[14, 14, 14, 14],
                md_bg_color=COLOR_CARD_DARK,
                line_color=COLOR_CARD_BORDER,
                elevation=0
            )

            top_row = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="24dp")
            top_row.add_widget(MDLabel(
                text=feat_title,
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN
            ))
            card.add_widget(top_row)

            card.add_widget(MDLabel(
                text=feat_desc,
                font_style="Body",
                role="small",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED
            ))

            grid.add_widget(card)

        content.add_widget(grid)
        scroll.add_widget(content)
        root.add_widget(scroll)

        # Minimalist Action Bar
        action_box = MDBoxLayout(orientation="horizontal", spacing="16dp", size_hint_y=None, height="52dp")

        btn_login = MDButton(
            style="outlined",
            size_hint_x=0.4,
            on_release=self.go_login
        )
        btn_login.add_widget(MDButtonText(text="Select Identity / Login"))

        btn_dashboard = MDButton(
            style="filled",
            size_hint_x=0.6,
            on_release=self.go_dashboard
        )
        btn_dashboard.add_widget(MDButtonText(text="Launch App 🚀"))

        action_box.add_widget(btn_login)
        action_box.add_widget(btn_dashboard)

        root.add_widget(action_box)
        self.add_widget(root)

    def go_login(self, *args):
        self.manager.current = "login"

    def go_dashboard(self, *args):
        self.manager.current = "dashboard"
