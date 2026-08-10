"""
BharatConnect Register Screen (app/screens/register.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard

from kivymd.uix.textfield import MDTextField, MDTextFieldHintText
from kivymd.uix.selectioncontrol import MDCheckbox

from database.db import db_engine
from utils.helper import (
    COLOR_080616,
    COLOR_1A1953,
    COLOR_6367FF,
    COLOR_2F2FE4,
    COLOR_8494FF,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    create_primary_button,
)


class RegisterScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "register"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = MDBoxLayout(
            orientation="vertical",
            padding=["20dp", "12dp", "20dp", "16dp"],
            spacing="12dp",
            md_bg_color=COLOR_080616,
        )

        # Top Bar
        top_bar = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="36dp")
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

        # Header Title
        header = MDBoxLayout(orientation="vertical", spacing="2dp", size_hint_y=None, height="50dp")
        header.add_widget(
            MDLabel(
                text="Create Account",
                font_style="Headline",
                role="medium",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        header.add_widget(
            MDLabel(
                text="Join us today!",
                font_style="Body",
                role="medium",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED,
            )
        )
        root.add_widget(header)

        # Form Card (Image 2 mockup 4)
        form_card = MDCard(
            orientation="vertical",
            padding="16dp",
            spacing="10dp",
            radius=[20, 20, 20, 20],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            elevation=4,
            size_hint_y=None,
            height="430dp",
        )

        self.input_name = MDTextField(mode="filled", size_hint_y=None, height="48dp")
        self.input_name.add_widget(MDTextFieldHintText(text="Full Name"))
        form_card.add_widget(self.input_name)

        self.input_username = MDTextField(mode="filled", size_hint_y=None, height="48dp")
        self.input_username.add_widget(MDTextFieldHintText(text="Username"))
        form_card.add_widget(self.input_username)

        self.input_email = MDTextField(mode="filled", size_hint_y=None, height="48dp")
        self.input_email.add_widget(MDTextFieldHintText(text="Email"))
        form_card.add_widget(self.input_email)

        self.input_pass1 = MDTextField(mode="filled", password=True, size_hint_y=None, height="48dp")
        self.input_pass1.add_widget(MDTextFieldHintText(text="Password"))
        form_card.add_widget(self.input_pass1)

        self.input_pass2 = MDTextField(mode="filled", password=True, size_hint_y=None, height="48dp")
        self.input_pass2.add_widget(MDTextFieldHintText(text="Confirm Password"))
        form_card.add_widget(self.input_pass2)

        # Terms Checkbox Row
        terms_row = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="28dp")
        self.chk_terms = MDCheckbox(active=True, size_hint=(None, None), size=("24dp", "24dp"))
        terms_row.add_widget(self.chk_terms)
        terms_lbl = MDLabel(
            text="I agree to the Terms & Conditions",
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED,
            valign="center",
        )
        terms_row.add_widget(terms_lbl)
        form_card.add_widget(terms_row)

        # Register Button
        reg_btn = create_primary_button("Register", on_release=self.do_register, size_hint_x=1.0, height="44dp")
        form_card.add_widget(reg_btn)

        root.add_widget(form_card)

        # Feedback & Login Link
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

        login_card = MDCard(
            size_hint_y=None,
            height="32dp",
            theme_bg_color="Custom",
            md_bg_color=[0, 0, 0, 0],
            pos_hint={"center_x": 0.5},
            ripple_behavior=True,
            on_release=self.go_login,
        )
        login_card.add_widget(
            MDLabel(
                text="Already have an account? Login",
                font_style="Body",
                role="small",
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_8494FF,
            )
        )
        root.add_widget(login_card)

        self.add_widget(root)

    def go_back(self, *args):
        self.manager.current = "login"

    def go_login(self, *args):
        self.manager.current = "login"

    def do_register(self, *args):
        if not self.chk_terms.active:
            self.lbl_status.text = "Please agree to the Terms & Conditions."
            return

        p1 = self.input_pass1.text.strip()
        p2 = self.input_pass2.text.strip()
        if p1 != p2:
            self.lbl_status.text = "Passwords do not match."
            return

        try:
            db_engine.register_user(
                full_name=self.input_name.text,
                email=self.input_email.text,
                username=self.input_username.text,
                password=p1,
            )
            if hasattr(self.manager, "dashboard_screen"):
                self.manager.dashboard_screen.reload_user_session()
            self.manager.current = "dashboard"
        except Exception as exc:
            self.lbl_status.text = str(exc)
