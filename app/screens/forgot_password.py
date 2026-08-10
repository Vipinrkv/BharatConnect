"""
BharatConnect Forgot Password Screen (app/screens/forgot_password.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText

from database.db import db_engine
from utils.helper import (
    COLOR_080616,
    COLOR_1A1953,
    COLOR_6367FF,
    COLOR_2F2FE4,
    COLOR_8494FF,
    COLOR_FFDBFD,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    GradientCard,
    create_icon_button,
)
from kivymd.uix.label import MDLabel, MDIcon


class ForgotPasswordScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "forgot"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = MDBoxLayout(
            orientation="vertical",
            padding=["20dp", "16dp", "20dp", "20dp"],
            spacing="16dp",
            md_bg_color=COLOR_080616,
        )

        # Top Bar with Vector Back Button
        top_bar = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="40dp")
        back_btn = create_icon_button("arrow-left", on_release=self.go_back, size_dp=36, icon_size="24sp")
        top_bar.add_widget(back_btn)
        root.add_widget(top_bar)

        # Header Title
        header = MDBoxLayout(orientation="vertical", spacing="6dp", size_hint_y=None, height="70dp")
        header.add_widget(
            MDLabel(
                text="Forgot Password?",
                font_style="Headline",
                role="medium",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        header.add_widget(
            MDLabel(
                text="No worries! Enter your email and we'll send you reset instructions.",
                font_style="Body",
                role="medium",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED,
            )
        )
        root.add_widget(header)

        # Main Card
        card = MDCard(
            orientation="vertical",
            padding="20dp",
            spacing="16dp",
            radius=[20, 20, 20, 20],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            elevation=4,
            size_hint_y=None,
            height="200dp",
        )

        self.input_email = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_email.add_widget(MDTextFieldHintText(text="Email Address"))
        self.input_email.text = "alex.morgan@bharatconnect.com"
        card.add_widget(self.input_email)

        send_btn = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            size_hint_y=None,
            height="46dp",
            radius=[14, 14, 14, 14],
            ripple_behavior=True,
            on_release=self.do_send_reset,
        )
        send_btn.add_widget(
            MDLabel(
                text="Send Reset Link",
                font_style="Title",
                role="medium",
                bold=True,
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        card.add_widget(send_btn)

        back_login_box = MDBoxLayout(size_hint_y=None, height="30dp")
        back_login_btn = MDButton(style="text", pos_hint={"center_x": 0.5}, on_release=self.go_back)
        back_login_btn.add_widget(MDButtonText(text="Back to Login"))
        back_login_box.add_widget(back_login_btn)
        card.add_widget(back_login_box)

        root.add_widget(card)

        # Vector Envelope Illustration Card (Image 1 mockup FORGOT PASSWORD)
        graphic_card = GradientCard(
            color1=COLOR_1A1953,
            color2=COLOR_6367FF,
            orientation="vertical",
            padding="16dp",
            spacing="8dp",
            radius=[24, 24, 24, 24],
            size_hint_y=0.4,
            pos_hint={"center_x": 0.5},
            elevation=4,
        )
        icon_row = MDBoxLayout(orientation="horizontal", spacing="12dp", pos_hint={"center_x": 0.5}, size_hint=(None, None), size=("90dp", "48dp"))
        icon_row.add_widget(MDIcon(icon="email-outline", font_size="36sp", theme_text_color="Custom", text_color=COLOR_FFDBFD))
        icon_row.add_widget(MDIcon(icon="lock-outline", font_size="36sp", theme_text_color="Custom", text_color=COLOR_FFDBFD))
        graphic_card.add_widget(icon_row)

        graphic_card.add_widget(
            MDLabel(
                text="Secure Password Recovery",
                font_style="Title",
                role="medium",
                bold=True,
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        root.add_widget(graphic_card)

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

        self.add_widget(root)

    def go_back(self, *args):
        self.manager.current = "login"

    def do_send_reset(self, *args):
        email = self.input_email.text.strip()
        success, msg = db_engine.reset_password(email)
        self.lbl_status.text = msg
