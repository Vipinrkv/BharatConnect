"""
BharatConnect Main Dashboard Shell (app/screens/dashboard.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText

from database.db import db_engine
from utils.helper import (
    COLOR_080616,
    COLOR_1A1953,
    COLOR_162E93,
    COLOR_6367FF,
    COLOR_2F2FE4,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    GradientCard,
    GradientBox,
)

from app.screens.home import HomeScreenView
from app.screens.chat import ChatListView, ChatThreadView
from app.screens.marketplace import MarketplaceView
from app.screens.profile import ProfileView
from app.screens.reels import ReelsView
from app.screens.call import EncryptedCallScreen


class DashboardScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "dashboard"
        self.active_tab = "home"
        self.active_chat_id = None
        self.nav_cards = {}
        self.nav_icons = {}
        self.nav_labels = {}
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = MDBoxLayout(orientation="vertical", spacing="0dp", md_bg_color=COLOR_080616)

        self.content_area = MDBoxLayout(
            orientation="vertical",
            padding=["0dp", "0dp", "0dp", "0dp"],
            spacing="0dp",
            md_bg_color=COLOR_080616,
        )
        root.add_widget(self.content_area)

        root.add_widget(self._build_bottom_nav())
        self.add_widget(root)
        self.render_active_tab()

    def _build_bottom_nav(self):
        bottom_nav = GradientBox(
            color1=COLOR_162E93,
            color2=COLOR_1A1953,
            orientation_grad="horizontal",
            orientation="horizontal",
            padding=["6dp", "4dp", "6dp", "4dp"],
            spacing="4dp",
            size_hint_y=None,
            height="60dp",
        )

        nav_items = [
            ("home", "home-variant", "home-variant-outline", "Feed", self.show_home_tab),
            ("reels", "play-box-multiple", "play-box-multiple-outline", "Reels", self.show_reels_tab),
            ("chat", "chat-processing", "chat-processing-outline", "Chats", self.show_chat_tab),
            ("add_post", "plus-box", "plus-box-outline", "Create", self.show_add_post_tab),
            ("marketplace", "storefront", "storefront-outline", "Market", self.show_marketplace_tab),
            ("profile", "account-circle", "account-circle-outline", "Profile", self.show_profile_tab),
        ]

        self.nav_items_meta = nav_items

        for tab_key, active_icon, inactive_icon, tab_label, callback in nav_items:
            is_active = (self.active_tab == tab_key)
            item_box = MDCard(
                orientation="vertical",
                padding=["2dp", "4dp", "2dp", "4dp"],
                spacing="2dp",
                size_hint_x=0.2,
                radius=[14, 14, 14, 14],
                theme_bg_color="Custom",
                md_bg_color=COLOR_6367FF if is_active else [0, 0, 0, 0],
                ripple_behavior=True,
                elevation=2 if is_active else 0,
                on_release=lambda instance, cb=callback: cb(),
            )

            ic = MDIcon(
                icon=active_icon if is_active else inactive_icon,
                font_size="22sp",
                pos_hint={"center_x": 0.5},
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN if is_active else COLOR_TEXT_MUTED,
            )
            item_box.add_widget(ic)

            lbl = MDLabel(
                text=tab_label,
                font_style="Label",
                role="small",
                bold=is_active,
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN if is_active else COLOR_TEXT_MUTED,
            )
            item_box.add_widget(lbl)
            bottom_nav.add_widget(item_box)

            self.nav_cards[tab_key] = item_box
            self.nav_icons[tab_key] = (ic, active_icon, inactive_icon)
            self.nav_labels[tab_key] = lbl

        return bottom_nav

    def reload_user_session(self):
        self.active_tab = "home"
        self.active_chat_id = None
        self.switch_tab("home")

    def update_bottom_nav_ui(self):
        for tab_key, card in self.nav_cards.items():
            is_active = (self.active_tab == tab_key)
            card.md_bg_color = COLOR_6367FF if is_active else [0, 0, 0, 0]
            card.elevation = 2 if is_active else 0

            ic, active_icon, inactive_icon = self.nav_icons[tab_key]
            ic.icon = active_icon if is_active else inactive_icon
            ic.text_color = COLOR_TEXT_MAIN if is_active else COLOR_TEXT_MUTED

            lbl = self.nav_labels[tab_key]
            lbl.bold = is_active
            lbl.text_color = COLOR_TEXT_MAIN if is_active else COLOR_TEXT_MUTED

    def render_active_tab(self):
        self.content_area.clear_widgets()

        if self.active_tab == "chat":
            if self.active_chat_id:
                self.content_area.add_widget(
                    ChatThreadView(chat_id=self.active_chat_id, back_callback=self.show_chat_list)
                )
            else:
                self.content_area.add_widget(ChatListView(open_chat_callback=self.open_chat_thread))
        elif self.active_tab == "home":
            self.content_area.add_widget(HomeScreenView(navigation_callback=self.switch_tab))
        elif self.active_tab == "reels":
            self.content_area.add_widget(ReelsView(navigation_callback=self.switch_tab))
        elif self.active_tab == "add_post":
            self.render_add_post_view()
        elif self.active_tab == "marketplace":
            self.content_area.add_widget(MarketplaceView())
        elif self.active_tab == "profile":
            self.content_area.add_widget(ProfileView(open_settings_callback=self.open_settings))

    def switch_tab(self, tab_key):
        self.active_tab = tab_key
        if tab_key != "chat":
            self.active_chat_id = None
        self.update_bottom_nav_ui()
        self.render_active_tab()

    def show_home_tab(self, *args):
        self.switch_tab("home")

    def show_reels_tab(self, *args):
        self.switch_tab("reels")

    def show_chat_tab(self, *args):
        self.switch_tab("chat")

    def show_chat_list(self, *args):
        self.active_chat_id = None
        self.render_active_tab()

    def open_chat_thread(self, chat_id):
        self.active_chat_id = chat_id
        self.render_active_tab()

    def show_add_post_tab(self, *args):
        self.switch_tab("add_post")

    def show_marketplace_tab(self, *args):
        self.switch_tab("marketplace")

    def show_profile_tab(self, *args):
        self.switch_tab("profile")

    def open_settings(self):
        self.manager.current = "settings"

    def render_add_post_view(self):
        box = MDBoxLayout(orientation="vertical", spacing="14dp", padding=["16dp", "16dp", "16dp", "16dp"])

        box.add_widget(
            MDLabel(
                text="Create New Post",
                font_style="Headline",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                size_hint_y=None,
                height="42dp",
            )
        )

        card = GradientCard(
            color1=COLOR_1A1953,
            color2=COLOR_162E93,
            orientation="vertical",
            padding="16dp",
            spacing="14dp",
            size_hint_y=None,
            height="280dp",
            radius=[18, 18, 18, 18],
            elevation=2,
        )

        self.input_post_text = MDTextField(mode="filled", size_hint_y=None, height="100dp", multiline=True)
        self.input_post_text.add_widget(MDTextFieldHintText(text="What's on your mind?"))
        card.add_widget(self.input_post_text)

        self.input_post_image = MDTextField(mode="filled", size_hint_y=None, height="50dp")
        self.input_post_image.add_widget(MDTextFieldHintText(text="Photo title / topic"))
        card.add_widget(self.input_post_image)

        publish = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            orientation="vertical",
            padding="10dp",
            size_hint_y=None,
            height="46dp",
            radius=[12, 12, 12, 12],
            ripple_behavior=True,
            on_release=self.do_publish_post,
        )
        publish.add_widget(
            MDLabel(
                text="Publish Post",
                font_style="Title",
                role="small",
                bold=True,
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        card.add_widget(publish)

        hint = MDLabel(
            text="Posts are saved 100% locally and appear instantly in your feed.",
            font_style="Body",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED,
            size_hint_y=None,
            height="38dp",
        )
        box.add_widget(card)
        box.add_widget(hint)
        self.content_area.add_widget(box)

    def do_publish_post(self, *args):
        text = self.input_post_text.text.strip() or "Excited to connect with everyone on BharatConnect."
        image_title = self.input_post_image.text.strip() or "Community Photo"
        db_engine.add_post(text, image_title)
        self.show_home_tab()
