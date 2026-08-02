"""
BharatConnect Minimalist Vibrant Splash Screen (Gradient UI Edition)
Theme Palette: #6367FF, #8494FF, #C9BEFF, #FFDBFD
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
    COLOR_6367FF, COLOR_8494FF, COLOR_C9BEFF, COLOR_FFDBFD,
    COLOR_TEXT_MAIN, COLOR_TEXT_MUTED, create_pill_badge, GradientCard
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

        # Vibrant Hero Gradient Card (#6367FF -> #8494FF)
        hero_card = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_8494FF,
            orientation="horizontal",
            orientation_box="vertical",
            padding="24dp",
            spacing="14dp",
            size_hint_y=None,
            height="215dp",
            radius=[18, 18, 18, 18],
            elevation=0
        )

        badge = create_pill_badge(
            "🇮🇳 BHARATCONNECT v2.0 • GRADIENT EDITION",
            bg_color=[1, 1, 1, 0.25],
            text_color=COLOR_TEXT_MAIN,
            height="26dp"
        )
        hero_card.add_widget(badge)

        hero_card.add_widget(MDLabel(
            text="Ultra-Fast Text Messaging & Contact Matching",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))

        hero_card.add_widget(MDLabel(
            text="Built with Python & Kivy for sub-50ms message delivery, address book phone number matching, developer communities, and local tech marketplace.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=[1, 1, 1, 0.9]
        ))

        content.add_widget(hero_card)

        # Features Section Heading
        feat_heading = MDLabel(
            text="⚡ Core Features & Gradient UI System",
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
            ("💬 Message Bubble Interface", "Electric Indigo #6367FF bubbles with status ticks & reaction pills.", COLOR_6367FF, COLOR_8494FF),
            ("📇 Phone Contact Sync", "Match phone numbers from contacts with registered BharatConnect users.", COLOR_8494FF, COLOR_C9BEFF),
            ("🔑 Flexible Sign In & Auth", "Sign in via email, phone (+91), username, or register with DOB.", COLOR_C9BEFF, COLOR_FFDBFD),
            ("🔒 Password Verification", "Email OTP code verification for instant password reset.", COLOR_FFDBFD, COLOR_6367FF)
        ]

        for feat_title, feat_desc, col1, col2 in features:
            card = GradientCard(
                color1=[0.11, 0.14, 0.28, 1.0],
                color2=[0.16, 0.19, 0.38, 1.0],
                orientation="horizontal",
                orientation_box="vertical",
                padding="18dp",
                spacing="8dp",
                size_hint_y=None,
                height="135dp",
                radius=[16, 16, 16, 16],
                line_color=col1,
                elevation=0
            )

            card.add_widget(MDLabel(
                text=feat_title,
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN
            ))

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

        # Action Bar: Get Started / Sign In
        action_box = MDBoxLayout(orientation="horizontal", spacing="16dp", size_hint_y=None, height="52dp")

        btn_auth = MDButton(
            style="filled",
            size_hint_x=0.6,
            on_release=self.go_auth
        )
        btn_auth.add_widget(MDButtonText(text="Get Started / Sign In 🚀"))

        btn_dash = MDButton(
            style="outlined",
            size_hint_x=0.4,
            on_release=self.go_dashboard
        )
        btn_dash.add_widget(MDButtonText(text="Demo Dashboard"))

        action_box.add_widget(btn_auth)
        action_box.add_widget(btn_dash)

        root.add_widget(action_box)
        self.add_widget(root)

    def go_auth(self, *args):
        self.manager.current = "auth"

    def go_dashboard(self, *args):
        self.manager.current = "dashboard"
