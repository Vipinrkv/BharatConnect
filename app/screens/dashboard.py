"""
BharatConnect Main Dashboard Screen (Focus on Core Messaging & Contact Matching)
Theme Palette: #6367FF, #8494FF, #C9BEFF, #FFDBFD
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText
from kivymd.uix.button import MDButton, MDButtonText
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
from kivy.clock import Clock

from database.db import db_engine
from app.theme import (
    get_theme_colors, create_presence_badge, create_pill_badge,
    COLOR_6367FF, COLOR_8494FF, COLOR_C9BEFF, COLOR_FFDBFD, COLOR_EMERALD, COLOR_AMBER
)


class DashboardScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "dashboard"
        self.active_tab = "chats"  # Focus on chat first!
        self.active_chat_id = "c-group-1"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()
        colors = get_theme_colors(self.theme_cls.theme_style)

        main_box = MDBoxLayout(
            orientation="vertical",
            spacing="0dp",
            md_bg_color=colors["bg"]
        )

        # ---------------------------------------------------------
        # TOP HEADER NAVBAR
        # ---------------------------------------------------------
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["16dp", "8dp", "16dp", "8dp"],
            spacing="12dp",
            size_hint_y=None,
            height="60dp",
            md_bg_color=colors["topbar"]
        )

        user = db_engine.get_current_user()

        logo_box = MDBoxLayout(
            orientation="horizontal",
            spacing="8dp",
            size_hint_x=0.55,
            pos_hint={"center_y": 0.5}
        )
        logo_box.add_widget(MDLabel(
            text="🇮🇳 BharatConnect",
            font_style="Title",
            role="medium",
            bold=True,
            adaptive_width=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))
        logo_box.add_widget(create_pill_badge("MESSAGING CORE", bg_color=[0.388, 0.404, 1.0, 0.2], text_color=COLOR_C9BEFF, height="22dp"))
        
        user_info_lbl = MDLabel(
            text=f"• User: [b]{user['display_name']}[/b] ({user['phone']})",
            font_style="Body",
            role="small",
            markup=True,
            theme_text_color="Custom",
            text_color=colors["text_muted"],
            pos_hint={"center_y": 0.5}
        )
        logo_box.add_widget(user_info_lbl)
        top_bar.add_widget(logo_box)

        btn_contacts = MDButton(
            style="outlined",
            size_hint_x=None,
            width="150dp",
            on_release=self.open_permissions_screen
        )
        btn_contacts.add_widget(MDButtonText(text="📇 Phone Sync"))
        top_bar.add_widget(btn_contacts)

        btn_switch = MDButton(
            style="outlined",
            size_hint_x=None,
            width="120dp",
            on_release=self.open_identity_dialog
        )
        btn_switch.add_widget(MDButtonText(text="🔄 Account"))
        top_bar.add_widget(btn_switch)

        main_box.add_widget(top_bar)

        # ---------------------------------------------------------
        # BODY LAYOUT: SIDEBAR + CONTENT VIEW
        # ---------------------------------------------------------
        self.body_layout = MDBoxLayout(orientation="horizontal", spacing="0dp")

        # Sidebar Navigation
        sidebar = MDBoxLayout(
            orientation="vertical",
            padding=["10dp", "14dp", "10dp", "14dp"],
            spacing="8dp",
            size_hint_x=None,
            width="210dp",
            md_bg_color=colors["sidebar"]
        )

        nav_items = [
            ("chats", "💬 Conversations", self.show_chats_tab),
            ("communities", "🌐 Communities", self.show_communities_tab),
            ("marketplace", "🛒 Marketplace", self.show_marketplace_tab),
            ("nearby", "📍 Nearby Devs", self.show_nearby_tab),
            ("settings", "⚙️ Settings", self.show_settings_tab)
        ]

        for tab_key, tab_label, callback in nav_items:
            is_active = (self.active_tab == tab_key)
            btn = MDButton(
                style="filled" if is_active else "text",
                size_hint_x=1,
                height="44dp",
                on_release=lambda instance, c=callback: c()
            )
            btn.add_widget(MDButtonText(text=tab_label))
            sidebar.add_widget(btn)

        sidebar.add_widget(MDBoxLayout(size_hint_y=1))

        # Mini Telemetry Widget
        telemetry = MDCard(
            orientation="vertical",
            padding="12dp",
            spacing="4dp",
            size_hint_y=None,
            height="85dp",
            radius=[12, 12, 12, 12],
            md_bg_color=colors["card"],
            line_color=colors["border"],
            elevation=0
        )
        telemetry.add_widget(MDLabel(
            text="⚡ #6367FF Engine",
            font_style="Label",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))
        telemetry.add_widget(MDLabel(
            text="Contact Matching Active",
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=colors["text_muted"]
        ))
        telemetry.add_widget(MDLabel(
            text="Sub-50ms Message Sync",
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_C9BEFF
        ))
        sidebar.add_widget(telemetry)

        self.body_layout.add_widget(sidebar)

        # Main Content Display Area
        self.content_area = MDBoxLayout(
            orientation="vertical",
            padding="16dp",
            spacing="16dp",
            md_bg_color=colors["bg"]
        )
        self.body_layout.add_widget(self.content_area)

        main_box.add_widget(self.body_layout)
        self.add_widget(main_box)

        self.render_active_tab()

    def reload_user_session(self):
        self.build_ui()

    def render_active_tab(self):
        self.content_area.clear_widgets()
        if self.active_tab == "chats":
            self.build_chats_view()
        elif self.active_tab == "communities":
            self.build_communities_view()
        elif self.active_tab == "marketplace":
            self.build_marketplace_view()
        elif self.active_tab == "nearby":
            self.build_nearby_view()
        elif self.active_tab == "settings":
            self.build_settings_view()

    def show_chats_tab(self):
        self.active_tab = "chats"
        self.build_ui()

    def show_communities_tab(self):
        self.active_tab = "communities"
        self.build_ui()

    def show_marketplace_tab(self):
        self.active_tab = "marketplace"
        self.build_ui()

    def show_nearby_tab(self):
        self.active_tab = "nearby"
        self.build_ui()

    def show_settings_tab(self):
        self.active_tab = "settings"
        self.build_ui()

    # ---------------------------------------------------------
    # CORE MESSAGE BUBBLE INTERFACE & CHATS VIEW
    # ---------------------------------------------------------
    def build_chats_view(self):
        colors = get_theme_colors(self.theme_cls.theme_style)
        chats_layout = MDBoxLayout(orientation="horizontal", spacing="14dp")

        # Sidebar Chat List
        chats_sidebar = MDBoxLayout(orientation="vertical", spacing="10dp", size_hint_x=0.35)

        chats_header = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="38dp")
        chats_header.add_widget(MDLabel(
            text="💬 Conversations",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))

        btn_new_chat = MDButton(style="filled", size_hint_x=None, width="85dp", on_release=self.open_new_chat_dialog)
        btn_new_chat.add_widget(MDButtonText(text="+ Direct"))
        chats_header.add_widget(btn_new_chat)

        btn_new_group = MDButton(style="outlined", size_hint_x=None, width="85dp", on_release=self.open_new_group_dialog)
        btn_new_group.add_widget(MDButtonText(text="+ Group"))
        chats_header.add_widget(btn_new_group)

        chats_sidebar.add_widget(chats_header)

        self.input_search_chats = MDTextField(mode="outlined", size_hint_y=None, height="44dp")
        self.input_search_chats.add_widget(MDTextFieldHintText(text="🔍 Search conversations & contacts..."))
        self.input_search_chats.bind(text=self.filter_chats_list)
        chats_sidebar.add_widget(self.input_search_chats)

        self.chat_scroll = ScrollView()
        self.chat_list_box = MDBoxLayout(orientation="vertical", spacing="8dp", size_hint_y=None)
        self.chat_list_box.bind(minimum_height=self.chat_list_box.setter("height"))

        self.populate_chats_list()
        self.chat_scroll.add_widget(self.chat_list_box)
        chats_sidebar.add_widget(self.chat_scroll)

        chats_layout.add_widget(chats_sidebar)

        # Core Message Pane
        self.chat_main_pane = MDBoxLayout(orientation="vertical", spacing="10dp", size_hint_x=0.65)
        self.populate_message_pane()

        chats_layout.add_widget(self.chat_main_pane)
        self.content_area.add_widget(chats_layout)

    def populate_chats_list(self, filter_query=""):
        colors = get_theme_colors(self.theme_cls.theme_style)
        self.chat_list_box.clear_widgets()
        user_chats = db_engine.get_user_chats()

        for chat in user_chats:
            title = chat.get("title", "Chat")
            if filter_query and filter_query.lower() not in title.lower():
                continue

            is_active = (chat["chat_id"] == self.active_chat_id)
            card = MDCard(
                orientation="vertical",
                padding="12dp",
                spacing="4dp",
                size_hint_y=None,
                height="72dp",
                radius=[12, 12, 12, 12],
                md_bg_color=colors["active_item"] if is_active else colors["card"],
                line_color=COLOR_6367FF if is_active else colors["border"],
                ripple_behavior=True,
                elevation=0,
                on_release=lambda inst, c_id=chat["chat_id"]: self.select_active_chat(c_id)
            )

            row1 = MDBoxLayout(orientation="horizontal", spacing="6dp", size_hint_y=None, height="24dp")
            row1.add_widget(MDLabel(
                text=title,
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=colors["text_main"]
            ))
            if chat.get("presence"):
                presence_badge = create_presence_badge(chat["presence"])
                row1.add_widget(presence_badge)
            card.add_widget(row1)

            msgs = db_engine.get_messages_for_chat(chat["chat_id"])
            last_msg = msgs[-1]["content"] if msgs else "No messages yet."
            card.add_widget(MDLabel(
                text=last_msg,
                font_style="Body",
                role="small",
                theme_text_color="Custom",
                text_color=colors["text_muted"],
                shorten=True
            ))

            self.chat_list_box.add_widget(card)

    def filter_chats_list(self, instance, text):
        self.populate_chats_list(text.strip())

    def select_active_chat(self, chat_id):
        self.active_chat_id = chat_id
        search_txt = self.input_search_chats.text if hasattr(self, 'input_search_chats') and self.input_search_chats else ""
        self.populate_chats_list(search_txt)
        self.populate_message_pane()

    def populate_message_pane(self):
        colors = get_theme_colors(self.theme_cls.theme_style)
        self.chat_main_pane.clear_widgets()

        chat = db_engine.chats.get(self.active_chat_id)
        if not chat:
            self.chat_main_pane.add_widget(MDLabel(
                text="Select a conversation to begin messaging.",
                theme_text_color="Custom",
                text_color=colors["text_muted"]
            ))
            return

        user_chats = db_engine.get_user_chats()
        active_chat_info = next((c for c in user_chats if c["chat_id"] == self.active_chat_id), chat)
        chat_title = active_chat_info.get("title") or "Chat"

        # Stream Header Card
        stream_header = MDCard(
            orientation="horizontal",
            padding=["16dp", "8dp", "16dp", "8dp"],
            spacing="8dp",
            size_hint_y=None,
            height="52dp",
            radius=[12, 12, 12, 12],
            md_bg_color=colors["card"],
            line_color=colors["border"],
            elevation=0
        )
        stream_header.add_widget(MDLabel(
            text=f"💬 {chat_title}",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))

        presence_str = active_chat_info.get("presence", "")
        if presence_str:
            p_badge = create_presence_badge(presence_str)
            p_badge.pos_hint = {"center_y": 0.5}
            stream_header.add_widget(p_badge)

        self.chat_main_pane.add_widget(stream_header)

        # Message Scroll Area
        self.msg_scroll = ScrollView()
        self.msg_box = MDBoxLayout(
            orientation="vertical",
            spacing="10dp",
            size_hint_y=None,
            padding=["0dp", "8dp", "0dp", "8dp"]
        )
        self.msg_box.bind(minimum_height=self.msg_box.setter("height"))

        self.render_messages()
        self.msg_scroll.add_widget(self.msg_box)
        self.chat_main_pane.add_widget(self.msg_scroll)

        self.typing_label = MDLabel(
            text="",
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_C9BEFF,
            size_hint_y=None,
            height="18dp"
        )
        self.chat_main_pane.add_widget(self.typing_label)

        # Input Bar
        input_bar = MDBoxLayout(orientation="vertical", spacing="6dp", size_hint_y=None, height="96dp")

        # Quick Emoji Bar with #FFDBFD highlights
        emoji_box = MDBoxLayout(orientation="horizontal", spacing="6dp", size_hint_y=None, height="30dp")
        for emoji in ["🇮🇳", "🚀", "❤️", "👍", "💻", "🔥"]:
            b = MDButton(style="outlined", size_hint_x=None, width="42dp", on_release=lambda inst, e=emoji: self.append_emoji(e))
            b.add_widget(MDButtonText(text=emoji))
            emoji_box.add_widget(b)
        input_bar.add_widget(emoji_box)

        row_send = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="52dp")
        self.msg_input = MDTextField(mode="outlined", size_hint_x=0.84)
        self.msg_input.add_widget(MDTextFieldHintText(text="Type a message..."))
        self.msg_input.bind(on_text_validate=self.send_text_message)

        btn_send = MDButton(style="filled", size_hint_x=0.16, on_release=self.send_text_message)
        btn_send.add_widget(MDButtonText(text="Send ✈️"))

        row_send.add_widget(self.msg_input)
        row_send.add_widget(btn_send)
        input_bar.add_widget(row_send)

        self.chat_main_pane.add_widget(input_bar)

    def render_messages(self):
        """Renders message bubbles using #6367FF, #8494FF, #C9BEFF, and #FFDBFD theme colors."""
        colors = get_theme_colors(self.theme_cls.theme_style)
        self.msg_box.clear_widgets()
        msgs = db_engine.get_messages_for_chat(self.active_chat_id)
        current_uid = db_engine.current_user_id

        for msg in msgs:
            is_me = (msg["sender_id"] == current_uid)
            sender_user = db_engine.users.get(msg["sender_id"], {})
            sender_name = "You" if is_me else sender_user.get("display_name", "User")

            bubble_wrapper = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="64dp", padding=["0dp", "2dp", "0dp", "2dp"])
            if is_me:
                bubble_wrapper.add_widget(MDBoxLayout(size_hint_x=0.25))

            # Sender Bubble (#6367FF) vs Recipient Bubble (#8494FF slate container)
            bubble_card = MDCard(
                orientation="vertical",
                padding="10dp",
                spacing="2dp",
                size_hint_x=0.75,
                size_hint_y=None,
                height="60dp",
                radius=[16, 16, 4, 16] if is_me else [16, 16, 16, 4],
                md_bg_color=COLOR_6367FF if is_me else [0.15, 0.18, 0.32, 0.95],
                line_color=COLOR_8494FF if not is_me else [0, 0, 0, 0],
                elevation=0
            )

            header_text = f"[b]{sender_name}[/b] • {msg['created_at']}"
            if msg.get("status") == "READ" and is_me:
                header_text += " ✓✓"

            bubble_card.add_widget(MDLabel(
                text=header_text,
                font_style="Label",
                role="small",
                markup=True,
                theme_text_color="Custom",
                text_color=[1, 1, 1, 0.9] if is_me else COLOR_C9BEFF
            ))

            bubble_card.add_widget(MDLabel(
                text=msg["content"],
                font_style="Body",
                role="medium",
                theme_text_color="Custom",
                text_color=[1, 1, 1, 1] if is_me else colors["text_main"]
            ))

            bubble_wrapper.add_widget(bubble_card)
            if not is_me:
                bubble_wrapper.add_widget(MDBoxLayout(size_hint_x=0.25))

            self.msg_box.add_widget(bubble_wrapper)

        Clock.schedule_once(lambda dt: setattr(self.msg_scroll, 'scroll_y', 0), 0.1)

    def append_emoji(self, emoji):
        if hasattr(self, 'msg_input') and self.msg_input:
            self.msg_input.text += emoji

    def send_text_message(self, *args):
        text = self.msg_input.text.strip()
        if not text:
            return

        db_engine.send_message(self.active_chat_id, text)
        self.msg_input.text = ""
        self.render_messages()
        search_txt = self.input_search_chats.text if hasattr(self, 'input_search_chats') and self.input_search_chats else ""
        self.populate_chats_list(search_txt)
        Clock.schedule_once(self.simulate_reply, 1.2)

    def simulate_reply(self, dt):
        chat = db_engine.chats.get(self.active_chat_id)
        if not chat:
            return
        other_ids = [p for p in chat["participants"] if p != db_engine.current_user_id]
        if other_ids:
            replier_id = other_ids[0]
            replier = db_engine.users.get(replier_id, {})
            self.typing_label.text = f"💬 {replier.get('display_name', 'User')} is typing..."
            Clock.schedule_once(lambda d: self.finish_simulate_reply(replier_id), 1.5)

    def finish_simulate_reply(self, replier_id):
        self.typing_label.text = ""
        replies = [
            "Got it! Thanks for sending the updates.",
            "Looks awesome! Core message bubble interface is super crisp.",
            "Confirmed. Address book phone numbers matched successfully!",
            "Great progress! Let's ship this update."
        ]
        import random
        reply_text = random.choice(replies)
        db_engine.send_message(self.active_chat_id, reply_text, sender_id=replier_id)
        self.render_messages()

    # ---------------------------------------------------------
    # COMMUNITIES VIEW
    # ---------------------------------------------------------
    def build_communities_view(self):
        colors = get_theme_colors(self.theme_cls.theme_style)
        root_comm = MDBoxLayout(orientation="vertical", spacing="14dp")

        h = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_y=None, height="48dp")
        h.add_widget(MDLabel(
            text="🌐 Indian Tech Communities Hub",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))
        h.add_widget(MDLabel(
            text="Discover and collaborate with developers, architects, and founders across India.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=colors["text_muted"]
        ))
        root_comm.add_widget(h)

        scroll = ScrollView()
        grid = GridLayout(cols=2, spacing="16dp", size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        for comm in db_engine.communities:
            card = MDCard(
                orientation="vertical",
                padding="16dp",
                spacing="10dp",
                size_hint_y=None,
                height="180dp",
                radius=[14, 14, 14, 14],
                md_bg_color=colors["card"],
                line_color=colors["border"],
                elevation=0
            )
            card.add_widget(MDLabel(
                text=f"🇮🇳 {comm['name']}",
                font_style="Title",
                role="medium",
                bold=True,
                theme_text_color="Custom",
                text_color=colors["text_main"]
            ))
            card.add_widget(MDLabel(
                text=comm["description"],
                font_style="Body",
                role="small",
                theme_text_color="Custom",
                text_color=colors["text_muted"]
            ))

            info_row = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="36dp")
            badge_cat = create_pill_badge(f"👥 {comm['members_count']} • {comm['category']}", bg_color=[0.388, 0.404, 1.0, 0.15], text_color=COLOR_C9BEFF)
            info_row.add_widget(badge_cat)
            info_row.add_widget(MDBoxLayout(size_hint_x=1))

            btn_join = MDButton(
                style="filled" if comm["is_joined"] else "outlined",
                size_hint_x=None,
                width="110dp",
                on_release=lambda inst, c_id=comm["community_id"]: self.toggle_join_community(c_id)
            )
            btn_join.add_widget(MDButtonText(text="Joined ✓" if comm["is_joined"] else "+ Join"))
            info_row.add_widget(btn_join)

            card.add_widget(info_row)
            grid.add_widget(card)

        scroll.add_widget(grid)
        root_comm.add_widget(scroll)
        self.content_area.add_widget(root_comm)

    def toggle_join_community(self, community_id):
        db_engine.toggle_community_join(community_id)
        self.render_active_tab()

    # ---------------------------------------------------------
    # MARKETPLACE VIEW
    # ---------------------------------------------------------
    def build_marketplace_view(self):
        colors = get_theme_colors(self.theme_cls.theme_style)
        root_mkt = MDBoxLayout(orientation="vertical", spacing="14dp")

        h = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_y=None, height="48dp")
        h.add_widget(MDLabel(
            text="🛒 Local Tech Marketplace & Services",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))
        h.add_widget(MDLabel(
            text="Buy, sell developer gear or offer technical services directly via BharatConnect chat.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=colors["text_muted"]
        ))
        root_mkt.add_widget(h)

        scroll = ScrollView()
        grid = GridLayout(cols=2, spacing="16dp", size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        for item in db_engine.marketplace:
            card = MDCard(
                orientation="vertical",
                padding="16dp",
                spacing="10dp",
                size_hint_y=None,
                height="200dp",
                radius=[14, 14, 14, 14],
                md_bg_color=colors["card"],
                line_color=colors["border"],
                elevation=0
            )
            row1 = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="28dp")
            row1.add_widget(MDLabel(
                text=item["title"],
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=colors["text_main"]
            ))

            price_badge = create_pill_badge(item["price"], bg_color=[0.06, 0.73, 0.51, 0.18], text_color=COLOR_EMERALD, height="26dp")
            row1.add_widget(price_badge)
            card.add_widget(row1)

            card.add_widget(MDLabel(
                text=item["description"],
                font_style="Body",
                role="small",
                theme_text_color="Custom",
                text_color=colors["text_muted"]
            ))

            card.add_widget(MDLabel(
                text=f"📍 {item['location']} • Seller: {item['seller_name']}",
                font_style="Label",
                role="small",
                theme_text_color="Custom",
                text_color=colors["text_subtle"]
            ))

            btn_contact = MDButton(
                style="filled",
                size_hint_y=None,
                height="38dp",
                on_release=lambda inst, s_id=item["seller_id"]: self.contact_seller(s_id)
            )
            btn_contact.add_widget(MDButtonText(text="💬 Contact Seller via Direct Chat"))
            card.add_widget(btn_contact)

            grid.add_widget(card)

        scroll.add_widget(grid)
        root_mkt.add_widget(scroll)
        self.content_area.add_widget(root_mkt)

    def contact_seller(self, seller_id):
        if seller_id == db_engine.current_user_id:
            return
        chat = db_engine.create_chat(seller_id)
        self.active_chat_id = chat["chat_id"]
        self.show_chats_tab()

    # ---------------------------------------------------------
    # NEARBY DEVELOPERS VIEW
    # ---------------------------------------------------------
    def build_nearby_view(self):
        colors = get_theme_colors(self.theme_cls.theme_style)
        root_near = MDBoxLayout(orientation="vertical", spacing="14dp")

        h = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_y=None, height="48dp")
        h.add_widget(MDLabel(
            text="📍 Nearby Developers & Tech Proximity",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))
        h.add_widget(MDLabel(
            text="Find tech professionals, engineers, and designers near your tech hub location.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=colors["text_muted"]
        ))
        root_near.add_widget(h)

        scroll = ScrollView()
        grid = GridLayout(cols=2, spacing="16dp", size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        for dev in db_engine.nearby:
            card = MDCard(
                orientation="vertical",
                padding="16dp",
                spacing="8dp",
                size_hint_y=None,
                height="175dp",
                radius=[14, 14, 14, 14],
                md_bg_color=colors["card"],
                line_color=colors["border"],
                elevation=0
            )
            r1 = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="28dp")
            r1.add_widget(MDLabel(
                text=dev["display_name"],
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=colors["text_main"]
            ))

            dist_badge = create_pill_badge(f"📍 {dev['distance_km']}", bg_color=[0.96, 0.62, 0.16, 0.18], text_color=COLOR_AMBER, height="24dp")
            r1.add_widget(dist_badge)
            card.add_widget(r1)

            card.add_widget(MDLabel(
                text=f"@{dev['username']} • {dev['role']}",
                font_style="Body",
                role="small",
                theme_text_color="Custom",
                text_color=colors["text_muted"]
            ))

            skills_str = ", ".join(dev['skills'])
            card.add_widget(MDLabel(
                text=f"🏢 {dev['city']} | Skills: {skills_str}",
                font_style="Label",
                role="small",
                theme_text_color="Custom",
                text_color=colors["text_subtle"]
            ))

            btn_conn = MDButton(
                style="filled",
                size_hint_y=None,
                height="38dp",
                on_release=lambda inst, u_id=dev["user_id"]: self.contact_seller(u_id)
            )
            btn_conn.add_widget(MDButtonText(text="💬 Connect & Chat"))
            card.add_widget(btn_conn)

            grid.add_widget(card)

        scroll.add_widget(grid)
        root_near.add_widget(scroll)
        self.content_area.add_widget(root_near)

    # ---------------------------------------------------------
    # SETTINGS VIEW
    # ---------------------------------------------------------
    def build_settings_view(self):
        colors = get_theme_colors(self.theme_cls.theme_style)
        root_set = MDBoxLayout(orientation="vertical", spacing="14dp")

        h = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_y=None, height="48dp")
        h.add_widget(MDLabel(
            text="⚙️ Profile & App Settings",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))
        h.add_widget(MDLabel(
            text="Manage your identity, status message, bio, and app options.",
            font_style="Body",
            role="medium",
            theme_text_color="Custom",
            text_color=colors["text_muted"]
        ))
        root_set.add_widget(h)

        scroll = ScrollView()
        set_box = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            size_hint_y=None,
            padding=["0dp", "8dp", "0dp", "16dp"]
        )
        set_box.bind(minimum_height=set_box.setter("height"))

        user = db_engine.get_current_user()

        card_prof = MDCard(
            orientation="vertical",
            padding="20dp",
            spacing="14dp",
            size_hint_y=None,
            height="320dp",
            radius=[14, 14, 14, 14],
            md_bg_color=colors["card"],
            line_color=colors["border"],
            elevation=0
        )
        card_prof.add_widget(MDLabel(
            text="👤 Edit Profile Information",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=colors["text_main"]
        ))

        self.set_name = MDTextField(mode="outlined", text=user["display_name"], size_hint_y=None, height="52dp")
        self.set_name.add_widget(MDTextFieldHintText(text="Display Name"))
        card_prof.add_widget(self.set_name)

        self.set_status = MDTextField(mode="outlined", text=user["status_message"], size_hint_y=None, height="52dp")
        self.set_status.add_widget(MDTextFieldHintText(text="Status Message"))
        card_prof.add_widget(self.set_status)

        self.set_bio = MDTextField(mode="outlined", text=user["bio"], size_hint_y=None, height="52dp")
        self.set_bio.add_widget(MDTextFieldHintText(text="Bio"))
        card_prof.add_widget(self.set_bio)

        btn_save = MDButton(
            style="filled",
            size_hint_y=None,
            height="44dp",
            on_release=self.save_profile
        )
        btn_save.add_widget(MDButtonText(text="Save Profile Changes"))
        card_prof.add_widget(btn_save)

        set_box.add_widget(card_prof)
        scroll.add_widget(set_box)
        root_set.add_widget(scroll)

        self.content_area.add_widget(root_set)

    def save_profile(self, *args):
        db_engine.update_user_profile(
            self.set_name.text.strip(),
            self.set_status.text.strip(),
            self.set_bio.text.strip(),
            "+91 98765 43210"
        )
        self.reload_user_session()

    def open_permissions_screen(self, *args):
        self.manager.current = "permissions"

    def open_identity_dialog(self, *args):
        self.manager.current = "auth"

    def open_new_chat_dialog(self, *args):
        other_users = [u for u in db_engine.users.values() if u["user_id"] != db_engine.current_user_id]
        if other_users:
            chat = db_engine.create_chat(other_users[0]["user_id"])
            self.select_active_chat(chat["chat_id"])

    def open_new_group_dialog(self, *args):
        new_group = db_engine.create_group_chat("New Project Group 🇮🇳", "Group discussion channel", ["u-102", "u-103"])
        self.select_active_chat(new_group["chat_id"])
