"""
BharatConnect Permission Request & Contact Number Matching Screen
Theme Palette: #6367FF, #8494FF, #C9BEFF, #FFDBFD
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout

from database.db import db_engine
from app.theme import (
    COLOR_BG_DARK, COLOR_CARD_DARK, COLOR_CARD_BORDER,
    COLOR_6367FF, COLOR_8494FF, COLOR_C9BEFF, COLOR_FFDBFD, COLOR_EMERALD,
    COLOR_TEXT_MAIN, COLOR_TEXT_MUTED, create_pill_badge
)


class PermissionScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "permissions"
        self.permissions_granted = False
        self.matched_contacts = []
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
        header_box = MDBoxLayout(orientation="vertical", spacing="6dp", size_hint_y=None, height="60dp")
        row_title = MDBoxLayout(orientation="horizontal", spacing="10dp", size_hint_y=None, height="30dp")
        row_title.add_widget(MDLabel(
            text="🛡️ App Permissions & Contact Sync",
            font_style="Headline",
            role="small",
            bold=True,
            adaptive_width=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))
        row_title.add_widget(create_pill_badge("PRIVACY PROTECTED", bg_color=[0.388, 0.404, 1.0, 0.2], text_color=COLOR_C9BEFF))
        header_box.add_widget(row_title)

        header_box.add_widget(MDLabel(
            text="BharatConnect requires permissions to match your contacts with registered users and support messaging.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED
        ))
        root.add_widget(header_box)

        scroll = ScrollView()
        content = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            size_hint_y=None,
            padding=["0dp", "8dp", "0dp", "16dp"]
        )
        content.bind(minimum_height=content.setter("height"))

        # Permissions List Card
        perm_card = MDCard(
            orientation="vertical",
            padding="18dp",
            spacing="12dp",
            size_hint_y=None,
            height="260dp",
            radius=[16, 16, 16, 16],
            md_bg_color=COLOR_CARD_DARK,
            line_color=COLOR_CARD_BORDER,
            elevation=0
        )
        perm_card.add_widget(MDLabel(
            text="📱 Required System Permissions:",
            font_style="Title",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))

        permissions = [
            ("📇 Contacts Permission", "Fetch address book numbers to match with registered BharatConnect users."),
            ("💬 SMS & Phone Calls", "Receive instant verification codes and enable direct cell calling."),
            ("📷 Photo & Camera", "Send photos, profile avatars, and scan QR codes."),
            ("🎥 Video & Audio", "Record video messages and high-clarity voice notes.")
        ]

        for title, desc in permissions:
            r = MDBoxLayout(orientation="horizontal", spacing="10dp", size_hint_y=None, height="38dp")
            r.add_widget(MDLabel(
                text=title,
                font_style="Title",
                role="small",
                bold=True,
                size_hint_x=0.4,
                theme_text_color="Custom",
                text_color=COLOR_C9BEFF
            ))
            r.add_widget(MDLabel(
                text=desc,
                font_style="Body",
                role="small",
                size_hint_x=0.6,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED
            ))
            perm_card.add_widget(r)

        content.add_widget(perm_card)

        # Grant Permission & Sync Action Card
        action_card = MDCard(
            orientation="vertical",
            padding="18dp",
            spacing="12dp",
            size_hint_y=None,
            height="110dp",
            radius=[16, 16, 16, 16],
            md_bg_color=COLOR_CARD_DARK,
            line_color=COLOR_6367FF,
            elevation=0
        )

        btn_grant = MDButton(
            style="filled",
            size_hint_y=None,
            height="44dp",
            on_release=self.grant_and_sync_contacts
        )
        btn_grant.add_widget(MDButtonText(text="Grant Permissions & Match Phone Contacts 📇"))
        action_card.add_widget(btn_grant)

        action_card.add_widget(MDLabel(
            text="🔐 Your contacts are hashed locally and never stored in plain text.",
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_FFDBFD
        ))

        content.add_widget(action_card)

        # Matched Contacts Display Area
        if self.permissions_granted:
            matched_card = MDCard(
                orientation="vertical",
                padding="18dp",
                spacing="12dp",
                size_hint_y=None,
                height="280dp",
                radius=[16, 16, 16, 16],
                md_bg_color=COLOR_CARD_DARK,
                line_color=COLOR_CARD_BORDER,
                elevation=0
            )

            r_header = MDBoxLayout(orientation="horizontal", spacing="10dp", size_hint_y=None, height="30dp")
            r_header.add_widget(MDLabel(
                text=f"🎉 {len([m for m in self.matched_contacts if m['is_registered']])} Contacts Matched on BharatConnect!",
                font_style="Title",
                role="medium",
                bold=True,
                adaptive_width=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN
            ))
            r_header.add_widget(create_pill_badge("SYNC COMPLETE", bg_color=[0.06, 0.73, 0.51, 0.2], text_color=COLOR_EMERALD))
            matched_card.add_widget(r_header)

            grid_contacts = GridLayout(cols=1, spacing="8dp", size_hint_y=None)
            grid_contacts.bind(minimum_height=grid_contacts.setter("height"))

            for contact in self.matched_contacts:
                row_c = MDBoxLayout(orientation="horizontal", spacing="10dp", size_hint_y=None, height="38dp")
                row_c.add_widget(MDLabel(
                    text=f"👤 {contact['name']} ({contact['phone']})",
                    font_style="Body",
                    role="medium",
                    bold=True,
                    size_hint_x=0.6,
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN if contact["is_registered"] else COLOR_TEXT_MUTED
                ))

                if contact["is_registered"]:
                    btn_c = MDButton(
                        style="filled",
                        size_hint_x=0.4,
                        on_release=lambda inst, u_id=contact["matched_user_id"]: self.start_chat_with_contact(u_id)
                    )
                    btn_c.add_widget(MDButtonText(text="Message 💬"))
                else:
                    btn_c = MDButton(style="outlined", size_hint_x=0.4)
                    btn_c.add_widget(MDButtonText(text="Invite +"))

                row_c.add_widget(btn_c)
                grid_contacts.add_widget(row_c)

            matched_card.add_widget(grid_contacts)
            content.add_widget(matched_card)

        scroll.add_widget(content)
        root.add_widget(scroll)

        # Bottom Proceed Bar
        bottom_bar = MDBoxLayout(orientation="horizontal", spacing="16dp", size_hint_y=None, height="48dp")
        btn_proceed = MDButton(
            style="filled",
            size_hint_x=1,
            on_release=self.go_to_dashboard
        )
        btn_proceed.add_widget(MDButtonText(text="Proceed to Home Dashboard 🚀"))
        bottom_bar.add_widget(btn_proceed)

        root.add_widget(bottom_bar)
        self.add_widget(root)

    def grant_and_sync_contacts(self, *args):
        self.permissions_granted = True
        self.matched_contacts = db_engine.match_device_contacts()
        self.build_ui()

    def start_chat_with_contact(self, user_id):
        chat = db_engine.create_chat(user_id)
        if hasattr(self.manager, 'dashboard_screen'):
            self.manager.dashboard_screen.active_chat_id = chat["chat_id"]
            self.manager.dashboard_screen.active_tab = "chats"
            self.manager.dashboard_screen.reload_user_session()
        self.manager.current = "dashboard"

    def go_to_dashboard(self, *args):
        if hasattr(self.manager, 'dashboard_screen'):
            self.manager.dashboard_screen.reload_user_session()
        self.manager.current = "dashboard"
