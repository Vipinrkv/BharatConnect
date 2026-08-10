"""
BharatConnect Splash Screen (app/screens/splash.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard


from database.db import db_engine
from utils.helper import (
    COLOR_080616,
    COLOR_6367FF,
    COLOR_8494FF,
    COLOR_C9BEFF,
    COLOR_FFDBFD,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    COLOR_2F2FE4,
    COLOR_162E93,
    COLOR_1A1953,
    GradientCard,
    create_pill_badge,
    create_primary_button,
    create_outlined_button,
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
            spacing="16dp",
            md_bg_color=COLOR_080616,
        )

        # Top Pill Badge (Header)
        header = MDBoxLayout(
            orientation="horizontal",
            size_hint_y=None,
            height="36dp",
            pos_hint={"center_x": 0.5},
        )
        status_text = db_engine.get_status_text()
        header.add_widget(
            create_pill_badge(
                status_text,
                bg_color=[0.388, 0.404, 1.0, 0.25],
                text_color=COLOR_FFDBFD,
                height="28dp",
            )
        )
        root.add_widget(header)

        # Centered Brand Container (Logo, App Title, Subtitle & Pagination Dots)
        center_box = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            size_hint_y=1.0,
            pos_hint={"center_x": 0.5},
        )

        # Spacer top
        center_box.add_widget(MDBoxLayout(size_hint_y=1.0))

        # Squircle Logo Card with Material Design Forum/Chat Icon
        logo_card = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            size_hint=(None, None),
            size=("110dp", "110dp"),
            radius=[28, 28, 28, 28],
            pos_hint={"center_x": 0.5},
            elevation=4,
        )

        icon_w = MDIcon(
            icon="forum",
            font_size="56sp",
            pos_hint={"center_x": 0.5, "center_y": 0.5},
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        logo_card.add_widget(icon_w)
        center_box.add_widget(logo_card)

        # App Title & Tagline
        center_box.add_widget(
            MDLabel(
                text="BharatConnect",
                font_style="Headline",
                role="medium",
                bold=True,
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                size_hint_y=None,
                height="40dp",
            )
        )

        center_box.add_widget(
            MDLabel(
                text="Connect. Share. Grow.\nA community for everyone.",
                font_style="Title",
                role="small",
                bold=True,
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED,
                size_hint_y=None,
                height="44dp",
            )
        )

        # 3 Pagination Dots
        dots = MDBoxLayout(
            orientation="horizontal",
            spacing="8dp",
            size_hint=(None, None),
            size=("60dp", "10dp"),
            pos_hint={"center_x": 0.5},
        )
        dots.add_widget(MDCard(size_hint=(None, None), size=("10dp", "10dp"), radius=[5], theme_bg_color="Custom", md_bg_color=COLOR_6367FF, elevation=0))
        dots.add_widget(MDCard(size_hint=(None, None), size=("10dp", "10dp"), radius=[5], theme_bg_color="Custom", md_bg_color=COLOR_8494FF, elevation=0))
        dots.add_widget(MDCard(size_hint=(None, None), size=("10dp", "10dp"), radius=[5], theme_bg_color="Custom", md_bg_color=COLOR_C9BEFF, elevation=0))
        center_box.add_widget(dots)

        # Spacer bottom
        center_box.add_widget(MDBoxLayout(size_hint_y=1.0))
        root.add_widget(center_box)

        # Bottom Sleek Action Buttons
        bottom = MDBoxLayout(
            orientation="vertical",
            spacing="12dp",
            size_hint_y=None,
            height="115dp",
            pos_hint={"center_x": 0.5},
        )

        btn_get_started = create_primary_button("Get Started", on_release=self.go_login, size_hint_x=1.0, height="48dp")
        bottom.add_widget(btn_get_started)

        btn_demo = create_outlined_button("Continue as Demo", on_release=self.go_demo, size_hint_x=1.0, height="48dp")
        bottom.add_widget(btn_demo)

        root.add_widget(bottom)
        self.add_widget(root)

    def go_login(self, *args):
        self.manager.current = "login"

    def go_demo(self, *args):
        if hasattr(self.manager, "dashboard_screen"):
            self.manager.dashboard_screen.reload_user_session()
        self.manager.current = "dashboard"
