"""
BharatConnect Login Screen (app/screens/login.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard

from kivymd.uix.textfield import MDTextField, MDTextFieldHintText

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
    create_primary_button,
)


class LoginScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "login"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = MDBoxLayout(
            orientation="vertical",
            padding=["20dp", "16dp", "20dp", "20dp"],
            spacing="16dp",
            md_bg_color=COLOR_080616,
        )

        # Top Bar with Back Arrow
        top_bar = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="40dp")
        back_card = MDCard(
            size_hint=(None, None),
            size=("36dp", "36dp"),
            radius=[18],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            ripple_behavior=True,
            on_release=self.go_back,
        )
        back_card.add_widget(MDIcon(icon="arrow-left", theme_text_color="Custom", text_color=COLOR_TEXT_MAIN, pos_hint={"center_x": 0.5, "center_y": 0.5}))
        top_bar.add_widget(back_card)
        root.add_widget(top_bar)

        # Welcome Back Title & Subtitle
        header = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_y=None, height="65dp")
        header.add_widget(
            MDLabel(
                text="Welcome Back",
                font_style="Headline",
                role="medium",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        header.add_widget(
            MDLabel(
                text="Login to continue your journey",
                font_style="Body",
                role="medium",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED,
            )
        )
        root.add_widget(header)

        # Login Form Card (Image 2 mockup 3)
        form_card = MDCard(
            orientation="vertical",
            padding="20dp",
            spacing="14dp",
            radius=[20, 20, 20, 20],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            elevation=4,
            size_hint_y=None,
            height="360dp",
            pos_hint={"center_x": 0.5},
        )

        self.input_identifier = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_identifier.add_widget(MDTextFieldHintText(text="Email or Username"))
        self.input_identifier.text = "alex.morgan@bharatconnect.com"
        form_card.add_widget(self.input_identifier)

        self.input_password = MDTextField(mode="filled", password=True, size_hint_y=None, height="54dp")
        self.input_password.add_widget(MDTextFieldHintText(text="Password"))
        self.input_password.text = "password123"
        form_card.add_widget(self.input_password)

        # Forgot Password Link
        forgot_card = MDCard(
            size_hint_y=None,
            height="26dp",
            theme_bg_color="Custom",
            md_bg_color=[0, 0, 0, 0],
            ripple_behavior=True,
            on_release=self.go_forgot_password,
        )
        forgot_card.add_widget(
            MDLabel(
                text="Forgot Password?",
                font_style="Label",
                role="medium",
                halign="right",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_8494FF,
            )
        )
        form_card.add_widget(forgot_card)

        # Primary Login Button
        login_btn = create_primary_button("Login", on_release=self.do_login, size_hint_x=1.0, height="46dp")
        form_card.add_widget(login_btn)

        # Divider "or continue with"
        form_card.add_widget(
            MDLabel(
                text="— or continue with —",
                font_style="Label",
                role="small",
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_SUBTLE,
                size_hint_y=None,
                height="20dp",
            )
        )

        # Social Login Buttons (Google, FB, Apple)
        social_box = MDBoxLayout(
            orientation="horizontal",
            spacing="16dp",
            size_hint=(None, None),
            size=("180dp", "44dp"),
            pos_hint={"center_x": 0.5},
        )
        for icon_name in ["google", "facebook", "apple"]:
            sc = MDCard(
                size_hint=(None, None),
                size=("44dp", "44dp"),
                radius=[22, 22, 22, 22],
                theme_bg_color="Custom",
                md_bg_color=COLOR_162E93,
                elevation=0,
                ripple_behavior=True,
                on_release=self.do_login,
            )
            sc.add_widget(MDIcon(icon=icon_name, pos_hint={"center_x": 0.5, "center_y": 0.5}, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN))
            social_box.add_widget(sc)
        form_card.add_widget(social_box)

        root.add_widget(form_card)

        # Feedback & Register Link
        self.lbl_status = MDLabel(
            text="",
            font_style="Body",
            role="small",
            halign="center",
            theme_text_color="Custom",
            text_color=COLOR_8494FF,
            size_hint_y=None,
            height="24dp",
        )
        root.add_widget(self.lbl_status)

        reg_card = MDCard(
            size_hint_y=None,
            height="32dp",
            theme_bg_color="Custom",
            md_bg_color=[0, 0, 0, 0],
            pos_hint={"center_x": 0.5},
            ripple_behavior=True,
            on_release=self.go_register,
        )
        reg_card.add_widget(
            MDLabel(
                text="Don't have an account? Register",
                font_style="Body",
                role="small",
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_8494FF,
            )
        )
        root.add_widget(reg_card)

        self.add_widget(root)

    def go_back(self, *args):
        self.manager.current = "splash"

    def go_forgot_password(self, *args):
        self.manager.current = "forgot"

    def go_register(self, *args):
        self.manager.current = "register"

    def do_login(self, *args):
        identifier = self.input_identifier.text
        password = self.input_password.text
        success, res = db_engine.authenticate_user(identifier, password)
        if success:
            if hasattr(self.manager, "dashboard_screen"):
                self.manager.dashboard_screen.reload_user_session()
            self.manager.current = "dashboard"
        else:
            self.lbl_status.text = str(res)
