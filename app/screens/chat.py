"""
BharatConnect Chat Screen Views (app/screens/chat.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText
from kivy.uix.scrollview import ScrollView
from kivy.clock import Clock

from database.db import db_engine
from utils.helper import (
    COLOR_080616,
    COLOR_1A1953,
    COLOR_162E93,
    COLOR_6367FF,
    COLOR_2F2FE4,
    COLOR_8494FF,
    COLOR_C9BEFF,
    COLOR_FFDBFD,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    COLOR_TEXT_SUBTLE,
    GradientCard,
    GradientBox,
    create_avatar_widget,
    create_pill_badge,
    create_icon_button,
)


from utils.contact_sync import PhoneContactSyncEngine


class ChatListView(MDBoxLayout):
    def __init__(self, open_chat_callback=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "10dp"
        self.open_chat_callback = open_chat_callback
        self.active_tab = "INDIVIDUAL"
        self._last_summary_sig = ""
        self._poll_event = None
        self.build_ui()

    def on_parent(self, widget, parent):
        if parent is not None:
            self.start_live_polling()
        else:
            self.stop_live_polling()

    def start_live_polling(self):
        if not self._poll_event:
            self._poll_event = Clock.schedule_interval(self.check_live_updates, 2.5)

    def stop_live_polling(self):
        if self._poll_event:
            Clock.unschedule(self._poll_event)
            self._poll_event = None

    def check_live_updates(self, dt):
        all_chats = db_engine.get_chats()
        if isinstance(all_chats, dict):
            all_chats = list(all_chats.values())
        sig = str([(c.get("id"), c.get("message"), c.get("timestamp")) for c in all_chats if isinstance(c, dict)])
        if sig != self._last_summary_sig:
            self._last_summary_sig = sig
            self.build_ui()

    def build_ui(self):

        self.clear_widgets()

        # Top App Bar with Contact Sync Button
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["16dp", "8dp", "16dp", "8dp"],
            spacing="10dp",
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )
        top_bar.add_widget(
            MDLabel(
                text="Chats",
                font_style="Title",
                role="large",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                valign="center",
            )
        )
        sync_btn = create_icon_button("account-search-outline", on_release=self.show_contact_sync_modal, size_dp=36, icon_size="22sp")
        top_bar.add_widget(sync_btn)
        self.add_widget(top_bar)

        # High-Contrast Tab Segment Bar ([Individual] [Group] [Community])
        tab_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["12dp", "4dp", "12dp", "4dp"],
            spacing="8dp",
            size_hint_y=None,
            height="44dp",
        )
        tabs = [("INDIVIDUAL", "Individual"), ("GROUP", "Group"), ("COMMUNITY", "Community")]
        for tab_key, tab_label in tabs:
            is_active = (self.active_tab == tab_key)
            tab_card = MDCard(
                orientation="vertical",
                padding=["4dp", "4dp", "4dp", "4dp"],
                size_hint_x=0.33,
                radius=[12, 12, 12, 12],
                theme_bg_color="Custom",
                md_bg_color=COLOR_6367FF if is_active else COLOR_1A1953,
                ripple_behavior=True,
                elevation=2 if is_active else 0,
                on_release=lambda instance, k=tab_key: self.switch_chat_tab(k),
            )
            tab_lbl = MDLabel(
                text=tab_label,
                font_style="Title",
                role="small",
                bold=is_active,
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN if is_active else COLOR_C9BEFF,
                pos_hint={"center_x": 0.5, "center_y": 0.5},
            )
            tab_card.add_widget(tab_lbl)
            tab_bar.add_widget(tab_card)

        self.add_widget(tab_bar)

        # Scrollable Chat Threads List
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        threads_list = MDBoxLayout(
            orientation="vertical",
            spacing="10dp",
            padding=["12dp", "6dp", "12dp", "12dp"],
            size_hint_y=None,
        )
        threads_list.bind(minimum_height=threads_list.setter("height"))

        all_chats = db_engine.get_chats()
        if isinstance(all_chats, dict):
            all_chats = list(all_chats.values())

        filtered = [c for c in all_chats if isinstance(c, dict) and c.get("chat_type") == self.active_tab]
        if not filtered:
            filtered = [c for c in all_chats if isinstance(c, dict)]

        for chat in filtered:
            card = MDCard(
                orientation="horizontal",
                padding="12dp",
                spacing="12dp",
                radius=[16, 16, 16, 16],
                theme_bg_color="Custom",
                md_bg_color=COLOR_1A1953,
                elevation=2,
                size_hint_y=None,
                height="72dp",
                ripple_behavior=True,
                on_release=lambda instance, cid=chat["id"]: self.open_chat_callback(cid) if self.open_chat_callback else None,
            )

            # Avatar
            card.add_widget(
                create_avatar_widget(
                    initials=chat.get("avatar_initials", "BC"),
                    bg_hex=chat.get("avatar_color", "#6367FF"),
                    size_dp=46,
                )
            )

            # Title & Last Message
            info = MDBoxLayout(orientation="vertical", spacing="2dp")
            info.add_widget(
                MDLabel(
                    text=chat["title"],
                    font_style="Title",
                    role="small",
                    bold=True,
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                    shorten=True,
                )
            )
            info.add_widget(
                MDLabel(
                    text=chat.get("message") or chat.get("subtitle") or "Tap to chat...",
                    font_style="Body",
                    role="small",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MUTED,
                    shorten=True,
                )
            )
            card.add_widget(info)

            # Timestamp / Unread Counter
            meta = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_x=None, width="55dp")
            meta.add_widget(
                MDLabel(
                    text=chat.get("timestamp", "10:30 AM"),
                    font_style="Label",
                    role="small",
                    halign="right",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_SUBTLE,
                )
            )
            if chat.get("unread_count", 0) > 0:
                meta.add_widget(
                    create_pill_badge(
                        str(chat["unread_count"]),
                        bg_color=COLOR_6367FF,
                        text_color=COLOR_TEXT_MAIN,
                        height="20dp",
                    )
                )
            card.add_widget(meta)

            threads_list.add_widget(card)

        scroll.add_widget(threads_list)
        self.add_widget(scroll)

    def switch_chat_tab(self, tab_key):
        self.active_tab = tab_key
        self.build_ui()

    def show_contact_sync_modal(self, *args):
        registered, invite = PhoneContactSyncEngine.sync_and_match_contacts()
        self.clear_widgets()

        # Contact Sync Header Bar
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["12dp", "8dp", "12dp", "8dp"],
            spacing="10dp",
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )
        back_btn = create_icon_button("arrow-left", on_release=lambda x: self.build_ui(), size_dp=36, icon_size="24sp")
        top_bar.add_widget(back_btn)
        top_bar.add_widget(
            MDLabel(
                text="Phone Contacts Sync",
                font_style="Title",
                role="large",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                valign="center",
            )
        )
        self.add_widget(top_bar)

        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        box = MDBoxLayout(orientation="vertical", spacing="12dp", padding=["12dp", "10dp", "12dp", "16dp"], size_hint_y=None)
        box.bind(minimum_height=box.setter("height"))

        # Registered Section (Show Registered Users FIRST)
        box.add_widget(MDLabel(text=f"Registered on BharatConnect ({len(registered)})", font_style="Title", role="medium", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN))
        for reg in registered:
            reg_id = reg["id"]
            card = MDCard(
                orientation="horizontal",
                padding="12dp",
                spacing="12dp",
                radius=[16, 16, 16, 16],
                theme_bg_color="Custom",
                md_bg_color=COLOR_1A1953,
                elevation=2,
                size_hint_y=None,
                height="70dp",
                ripple_behavior=True,
                on_release=lambda instance, rid=reg_id: self._open_chat_for_user(rid),
            )
            card.add_widget(create_avatar_widget(initials=reg.get("avatar_initials", "BC"), bg_hex=reg.get("avatar_color", "#6367FF"), size_dp=44))

            info = MDBoxLayout(orientation="vertical", spacing="2dp")
            info.add_widget(MDLabel(text=reg["display_name"], font_style="Title", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN, shorten=True))
            info.add_widget(MDLabel(text=f"@{reg['username']} • {reg['phone']}", font_style="Label", role="small", theme_text_color="Custom", text_color=COLOR_C9BEFF, shorten=True))
            card.add_widget(info)

            msg_btn = create_pill_badge("Message", bg_color=COLOR_6367FF, text_color=COLOR_TEXT_MAIN, height="28dp")
            card.add_widget(msg_btn)
            box.add_widget(card)

        # Invite Section (Non-registered contacts: CANNOT CHAT, SMS INVITE ONLY)
        box.add_widget(MDLabel(text=f"Invite Phone Contacts ({len(invite)})", font_style="Title", role="medium", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN))
        for inv in invite:
            inv_phone = inv["phone"]
            inv_name = inv["name"]
            card = MDCard(
                orientation="horizontal",
                padding="12dp",
                spacing="12dp",
                radius=[16, 16, 16, 16],
                theme_bg_color="Custom",
                md_bg_color=COLOR_1A1953,
                elevation=1,
                size_hint_y=None,
                height="64dp",
            )
            card.add_widget(create_avatar_widget(initials=inv["name"][:2].upper(), bg_hex="#8494FF", size_dp=40))

            info = MDBoxLayout(orientation="vertical", spacing="2dp")
            info.add_widget(MDLabel(text=inv["name"], font_style="Title", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN, shorten=True))
            info.add_widget(MDLabel(text=inv["phone"], font_style="Label", role="small", theme_text_color="Custom", text_color=COLOR_TEXT_MUTED, shorten=True))
            card.add_widget(info)

            inv_btn = MDCard(
                orientation="vertical",
                padding=["8dp", "4dp", "8dp", "4dp"],
                radius=[12, 12, 12, 12],
                theme_bg_color="Custom",
                md_bg_color=COLOR_162E93,
                size_hint_x=None,
                width="90dp",
                size_hint_y=None,
                height="32dp",
                ripple_behavior=True,
                on_release=lambda instance, p=inv_phone, n=inv_name: PhoneContactSyncEngine.send_sms_invite(p, n),
            )
            inv_btn.add_widget(
                MDLabel(
                    text="SMS Invite",
                    font_style="Label",
                    role="small",
                    bold=True,
                    halign="center",
                    valign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                )
            )
            card.add_widget(inv_btn)
            box.add_widget(card)

        scroll.add_widget(box)
        self.add_widget(scroll)

    def _open_chat_for_user(self, target_user_id):
        chat_id = db_engine.get_or_create_individual_chat(target_user_id)
        if self.open_chat_callback:
            self.open_chat_callback(chat_id)


class ChatThreadView(MDBoxLayout):
    def __init__(self, chat_id="c-individual", back_callback=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.chat_id = chat_id
        self.back_callback = back_callback
        self._last_msg_count = 0
        self._last_msg_id = ""
        self._poll_event = None
        self.build_ui()

    def on_parent(self, widget, parent):
        if parent is not None:
            self.start_live_polling()
        else:
            self.stop_live_polling()

    def start_live_polling(self):
        if not self._poll_event:
            self._poll_event = Clock.schedule_interval(self.check_live_updates, 1.5)

    def stop_live_polling(self):
        if self._poll_event:
            Clock.unschedule(self._poll_event)
            self._poll_event = None

    def check_live_updates(self, dt):
        messages = db_engine.get_chat_messages(self.chat_id)
        current_count = len(messages) if isinstance(messages, list) else 0
        latest_id = messages[-1].get("id", "") if (isinstance(messages, list) and messages) else ""
        if current_count != self._last_msg_count or latest_id != self._last_msg_id:
            self._last_msg_count = current_count
            self._last_msg_id = latest_id
            self.build_ui()

    def build_ui(self):

        self.clear_widgets()

        # Load Chat Meta
        all_chats = db_engine.get_chats()
        if isinstance(all_chats, dict):
            chat_meta = all_chats.get(self.chat_id)
        elif isinstance(all_chats, list):
            chat_meta = next((c for c in all_chats if isinstance(c, dict) and c.get("id") == self.chat_id), None)
        else:
            chat_meta = None

        if not chat_meta:
            chat_meta = {"id": "c-individual", "title": "Emma Watson", "subtitle": "Online", "avatar_initials": "EW", "avatar_color": "#8494FF"}

        # Vector Icon Header Bar
        header_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["8dp", "6dp", "8dp", "6dp"],
            spacing="8dp",
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )

        back_btn = create_icon_button("arrow-left", on_release=self.go_back, size_dp=36, icon_size="24sp")
        header_bar.add_widget(back_btn)

        header_bar.add_widget(
            create_avatar_widget(
                initials=chat_meta.get("avatar_initials", "EW"),
                bg_hex=chat_meta.get("avatar_color", "#8494FF"),
                size_dp=38,
            )
        )

        title_box = MDBoxLayout(orientation="vertical", spacing="1dp")
        title_box.add_widget(
            MDLabel(
                text=chat_meta["title"],
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                shorten=True,
            )
        )
        title_box.add_widget(
            MDLabel(
                text=chat_meta.get("subtitle") or "Online",
                font_style="Label",
                role="small",
                theme_text_color="Custom",
                text_color=COLOR_C9BEFF,
                shorten=True,
            )
        )
        header_bar.add_widget(title_box)

        # Call, Video & More Vector Icon Buttons
        actions_box = MDBoxLayout(orientation="horizontal", spacing="4dp", size_hint_x=None, width="110dp")
        actions_box.add_widget(create_icon_button("phone-outline", size_dp=32, icon_size="20sp"))
        actions_box.add_widget(create_icon_button("video-outline", size_dp=32, icon_size="20sp"))
        actions_box.add_widget(create_icon_button("dots-vertical", size_dp=32, icon_size="20sp"))
        header_bar.add_widget(actions_box)
        self.add_widget(header_bar)

        # Pinned Message Banner
        if chat_meta.get("pinned_message"):
            pinned_card = GradientCard(
                color1=COLOR_1A1953,
                color2=COLOR_2F2FE4,
                orientation="horizontal",
                padding=["12dp", "6dp", "12dp", "6dp"],
                spacing="8dp",
                size_hint_y=None,
                height="38dp",
            )
            pinned_card.add_widget(
                MDLabel(
                    text=f"📌 {chat_meta['pinned_message']}",
                    font_style="Label",
                    role="small",
                    theme_text_color="Custom",
                    text_color=COLOR_FFDBFD,
                    shorten=True,
                )
            )
            self.add_widget(pinned_card)

        # Message Stream ScrollView
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        msg_list = MDBoxLayout(
            orientation="vertical",
            spacing="12dp",
            padding=["12dp", "10dp", "12dp", "10dp"],
            size_hint_y=None,
        )
        msg_list.bind(minimum_height=msg_list.setter("height"))

        # Group vs Community vs Individual Chat Stream
        if self.chat_id == "c-group":
            messages = db_engine.get_group_messages("g-team")
            for m in messages:
                bubble = MDCard(
                    orientation="vertical",
                    padding="12dp",
                    spacing="4dp",
                    radius=[16, 16, 16, 16],
                    theme_bg_color="Custom",
                    md_bg_color=COLOR_1A1953,
                    elevation=1,
                    size_hint_x=0.75,
                    size_hint_y=None,
                    pos_hint={"left": 1},
                )
                bubble.bind(minimum_height=bubble.setter("height"))
                bubble.add_widget(
                    MDLabel(
                        text=m["sender_name"],
                        font_style="Label",
                        role="small",
                        bold=True,
                        theme_text_color="Custom",
                        text_color=COLOR_8494FF,
                    )
                )
                bubble.add_widget(
                    MDLabel(
                        text=m["message"],
                        font_style="Body",
                        role="medium",
                        theme_text_color="Custom",
                        text_color=COLOR_TEXT_MAIN,
                    )
                )
                bubble.add_widget(
                    MDLabel(
                        text=m["timestamp"],
                        font_style="Label",
                        role="small",
                        halign="right",
                        theme_text_color="Custom",
                        text_color=COLOR_TEXT_SUBTLE,
                    )
                )
                msg_list.add_widget(bubble)

        elif self.chat_id == "c-community":
            # Admin Message
            m1 = MDCard(
                orientation="vertical",
                padding="12dp",
                spacing="6dp",
                radius=[16, 16, 16, 16],
                theme_bg_color="Custom",
                md_bg_color=COLOR_1A1953,
                elevation=1,
                size_hint_x=0.82,
                size_hint_y=None,
                pos_hint={"left": 1},
            )
            m1.bind(minimum_height=m1.setter("height"))
            m1.add_widget(MDLabel(text="Admin", font_style="Label", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_FFDBFD))
            m1.add_widget(MDLabel(text="Welcome everyone! Feel free to share your knowledge and resources.", font_style="Body", role="medium", theme_text_color="Custom", text_color=COLOR_TEXT_MAIN))
            m1.add_widget(create_pill_badge("👍 9   ❤️ 25", bg_color=COLOR_162E93, text_color=COLOR_TEXT_MAIN, height="24dp"))
            msg_list.add_widget(m1)

            # Member Message with Link Preview Card
            m2 = MDCard(
                orientation="vertical",
                padding="12dp",
                spacing="6dp",
                radius=[16, 16, 16, 16],
                theme_bg_color="Custom",
                md_bg_color=COLOR_1A1953,
                elevation=1,
                size_hint_x=0.82,
                size_hint_y=None,
                pos_hint={"left": 1},
            )
            m2.bind(minimum_height=m2.setter("height"))
            m2.add_widget(MDLabel(text="DevMaster", font_style="Label", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_8494FF))
            m2.add_widget(MDLabel(text="Check out this new AI tool I found. It's amazing!", font_style="Body", role="medium", theme_text_color="Custom", text_color=COLOR_TEXT_MAIN))

            link_preview = GradientCard(
                color1=COLOR_162E93,
                color2=COLOR_2F2FE4,
                orientation="vertical",
                padding="8dp",
                radius=[10, 10, 10, 10],
                size_hint_y=None,
                height="50dp",
            )
            link_preview.add_widget(MDLabel(text="🌐 AI Tool — https://aitool.com", font_style="Label", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN))
            link_preview.add_widget(MDLabel(text="Best AI tools for developers.", font_style="Label", role="small", theme_text_color="Custom", text_color=COLOR_TEXT_MUTED))
            m2.add_widget(link_preview)
            msg_list.add_widget(m2)

        else:
            # Individual Chat Stream (Dynamic DB Engine Messages with is_me Calculation)
            db_messages = db_engine.get_chat_messages(self.chat_id)
            if not db_messages:
                # Default seed fallback if new empty chat
                db_messages = [
                    {"text": "Hey there! Lets connect.", "time": "10:30 AM", "is_me": False, "sender_name": chat_meta["title"]}
                ]

            for msg in db_messages:
                txt = msg.get("text", "")
                tm = msg.get("time", "")
                is_me = msg.get("is_me", False)
                sender_name = msg.get("sender_name", "")

                bubble = MDCard(
                    orientation="vertical",
                    padding="12dp",
                    spacing="4dp",
                    radius=[18, 18, 18, 18],
                    theme_bg_color="Custom",
                    md_bg_color=COLOR_2F2FE4 if is_me else COLOR_1A1953,
                    elevation=1,
                    size_hint_x=0.72,
                    size_hint_y=None,
                    pos_hint={"right": 1} if is_me else {"left": 1},
                )
                bubble.bind(minimum_height=bubble.setter("height"))

                if not is_me and sender_name:
                    bubble.add_widget(
                        MDLabel(
                            text=sender_name,
                            font_style="Label",
                            role="small",
                            bold=True,
                            theme_text_color="Custom",
                            text_color=COLOR_8494FF,
                        )
                    )

                bubble.add_widget(
                    MDLabel(
                        text=txt,
                        font_style="Body",
                        role="medium",
                        theme_text_color="Custom",
                        text_color=COLOR_TEXT_MAIN,
                    )
                )

                time_row = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="16dp")
                time_row.add_widget(
                    MDLabel(
                        text=f"{tm} {'✓✓' if is_me else ''}",
                        font_style="Label",
                        role="small",
                        halign="right",
                        theme_text_color="Custom",
                        text_color=COLOR_TEXT_SUBTLE if is_me else COLOR_TEXT_MUTED,
                    )
                )
                bubble.add_widget(time_row)
                msg_list.add_widget(bubble)

        scroll.add_widget(msg_list)
        self.add_widget(scroll)

        # Vector Icon Bottom Input Bar
        input_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["8dp", "6dp", "8dp", "6dp"],
            spacing="6dp",
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )

        plus_btn = create_icon_button("plus", size_dp=36, icon_size="22sp")
        input_bar.add_widget(plus_btn)

        self.input_msg = MDTextField(mode="filled", size_hint_y=None, height="44dp")
        self.input_msg.add_widget(MDTextFieldHintText(text="Type a message..."))
        input_bar.add_widget(self.input_msg)

        clip_btn = create_icon_button("paperclip", size_dp=36, icon_size="20sp")
        input_bar.add_widget(clip_btn)

        send_btn = create_icon_button("send", on_release=self.send_msg, icon_color=COLOR_6367FF, size_dp=36, icon_size="22sp")
        input_bar.add_widget(send_btn)

        self.add_widget(input_bar)

    def go_back(self, *args):
        if self.back_callback:
            self.back_callback()

    def send_msg(self, *args):
        txt = self.input_msg.text.strip()
        if txt:
            db_engine.send_chat_message(self.chat_id, txt)
            self.input_msg.text = ""
            self.build_ui()



class ChatScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "chat"
        self.add_widget(ChatListView())
