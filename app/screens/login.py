"""
BharatConnect Minimalist Vibrant Login Screen
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText
from kivymd.uix.button import MDButton, MDButtonText
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout

from database.db import db_engine
from app.theme import (
    COLOR_BG_DARK, COLOR_CARD_DARK, COLOR_CARD_BORDER,
    COLOR_CYAN, COLOR_TEXT_MAIN, COLOR_TEXT_MUTED, COLOR_TEXT_SUBTLE,
    create_presence_badge
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
            padding="24dp",
            spacing="16dp",
            md_bg_color=COLOR_BG_DARK
        )

        # Header Title
        header_box = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_y=None, height="54dp")
        header_box.add_widget(MDLabel(
            text="🔑 Select Identity or Sign In",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))
        header_box.add_widget(MDLabel(
            text="Select one of the pre-configured test accounts for instant multi-user session testing.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED
        ))
        root.add_widget(header_box)

        scroll = ScrollView()
        scroll_content = MDBoxLayout(
            orientation="vertical",
            spacing="20dp",
            size_hint_y=None,
            padding=["0dp", "8dp", "0dp", "16dp"]
        )
        scroll_content.bind(minimum_height=scroll_content.setter("height"))

        grid = GridLayout(cols=2, spacing="16dp", size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        for uid, user in db_engine.users.items():
            card = MDCard(
                orientation="vertical",
                padding="16dp",
                spacing="8dp",
                size_hint_y=None,
                height="150dp",
                radius=[14, 14, 14, 14],
                md_bg_color=COLOR_CARD_DARK,
                line_color=COLOR_CARD_BORDER,
                ripple_behavior=True,
                elevation=0,
                on_release=lambda instance, u_id=uid: self.select_account(u_id)
            )

            # Top Line: Display Name + Presence Dot
            top_line = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="26dp")
            top_line.add_widget(MDLabel(
                text=user["display_name"],
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN
            ))

            presence_widget = create_presence_badge(user["presence"])
            top_line.add_widget(presence_widget)
            card.add_widget(top_line)

            card.add_widget(MDLabel(
                text=f"@{user['username']} • {user['bio']}",
                font_style="Body",
                role="small",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED
            ))

            card.add_widget(MDLabel(
                text=f"💬 {user['status_message']}",
                font_style="Label",
                role="medium",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_SUBTLE
            ))

            grid.add_widget(card)

        scroll_content.add_widget(grid)

        # Custom Login Form Card
        form_card = MDCard(
            orientation="vertical",
            padding="20dp",
            spacing="14dp",
            size_hint_y=None,
            height="250dp",
            radius=[14, 14, 14, 14],
            md_bg_color=COLOR_CARD_DARK,
            line_color=COLOR_CARD_BORDER,
            elevation=0
        )
        form_card.add_widget(MDLabel(
            text="📝 Custom Login Credentials",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
            size_hint_y=None,
            height="26dp"
        ))

        self.input_user = MDTextField(mode="outlined", size_hint_y=None, height="52dp")
        self.input_user.add_widget(MDTextFieldHintText(text="Username or Email (@vipin_k)"))
        form_card.add_widget(self.input_user)

        self.input_pass = MDTextField(mode="outlined", password=True, size_hint_y=None, height="52dp")
        self.input_pass.add_widget(MDTextFieldHintText(text="Password"))
        form_card.add_widget(self.input_pass)

        btn_form_login = MDButton(
            style="filled",
            size_hint_y=None,
            height="44dp",
            on_release=self.perform_custom_login
        )
        btn_form_login.add_widget(MDButtonText(text="Sign In to BharatConnect"))
        form_card.add_widget(btn_form_login)

        scroll_content.add_widget(form_card)
        scroll.add_widget(scroll_content)
        root.add_widget(scroll)

        # Bottom Bar
        bottom_bar = MDBoxLayout(orientation="horizontal", spacing="16dp", size_hint_y=None, height="44dp")
        btn_back = MDButton(style="text", on_release=self.go_back)
        btn_back.add_widget(MDButtonText(text="← Back to Splash"))
        bottom_bar.add_widget(btn_back)

        root.add_widget(bottom_bar)
        self.add_widget(root)

    def select_account(self, user_id):
        user = db_engine.switch_user(user_id)
        if user and hasattr(self.manager, 'dashboard_screen'):
            self.manager.dashboard_screen.reload_user_session()
        self.manager.current = "dashboard"

    def perform_custom_login(self, *args):
        username = self.input_user.text.strip().lstrip('@')
        if not username:
            username = "vipin_k"
        
        found_id = None
        for uid, user in db_engine.users.items():
            if user["username"].lower() == username.lower() or user["email"].lower() == username.lower():
                found_id = uid
                break

        self.select_account(found_id if found_id else "u-101")

    def go_back(self, *args):
        self.manager.current = "splash"
