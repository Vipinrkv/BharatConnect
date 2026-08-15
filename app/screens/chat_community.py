"""
BharatConnect Chat (Community) Screen View (Matching Social App UI Set)
Palette: #6367FF, #8494FF, #C9BEFF, #FFDBFD, #2F2FE4, #162E93, #1A1953, #080616
"""

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText
from kivy.uix.scrollview import ScrollView
from kivy.clock import Clock

from database.db import db_engine
from app.theme import (
    COLOR_080616, COLOR_1A1953, COLOR_162E93,
    COLOR_6367FF, COLOR_8494FF, COLOR_C9BEFF, COLOR_FFDBFD, COLOR_2F2FE4,
    COLOR_TEXT_MAIN, COLOR_TEXT_MUTED, COLOR_TEXT_SUBTLE, create_pill_badge, create_avatar_widget, GradientCard, GradientBox
)


class CommunityChatView(MDBoxLayout):
    """Chat (Community) Component matching reference image."""
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "10dp"
        self.chat_id = "c-community"
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
            self.render_messages()

    def build_ui(self):

        self.clear_widgets()

        chats_data = db_engine.get_chats() if hasattr(db_engine, 'get_chats') else getattr(db_engine, 'chats', {})
        if isinstance(chats_data, list):
            chat_info = next((c for c in chats_data if isinstance(c, dict) and c.get("id") == self.chat_id), {})
        elif isinstance(chats_data, dict):
            chat_info = chats_data.get(self.chat_id, {})
        else:
            chat_info = {}

        # Header Bar matching Community Chat image: Avatar, "Tech Community", "1.2K members, 120 online", Options
        header = GradientBox(
            color1=COLOR_2F2FE4,
            color2=COLOR_162E93,
            orientation_grad="horizontal",
            orientation="horizontal",
            padding=["10dp", "6dp", "10dp", "6dp"],
            spacing="10dp",
            size_hint_y=None,
            height="56dp"
        )

        avatar = create_avatar_widget("TC", "#8494FF", size_dp=40)
        header.add_widget(avatar)

        name_box = MDBoxLayout(orientation="vertical", spacing="2dp")
        name_box.add_widget(MDLabel(
            text=chat_info.get("title", "Tech Community"),
            font_style="Title",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))
        name_box.add_widget(MDLabel(
            text=chat_info.get("subtitle", "1.2K members, 120 online"),
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_FFDBFD
        ))
        header.add_widget(name_box)

        btn_opts = MDButton(style="text", size_hint_x=None, width="36dp")
        btn_opts.add_widget(MDButtonText(text="•••"))
        header.add_widget(btn_opts)

        self.add_widget(header)

        # Pinned Message Banner matching reference image
        pinned_card = GradientCard(
            color1=COLOR_1A1953,
            color2=COLOR_162E93,
            orientation="horizontal",
            padding="10dp",
            spacing="8dp",
            size_hint_y=None,
            height="46dp",
            radius=[10, 10, 10, 10],
            elevation=0
        )
        pinned_box = MDBoxLayout(orientation="vertical", spacing="2dp")
        pinned_box.add_widget(MDLabel(
            text="📌 Pinned Message",
            font_style="Label",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_FFDBFD
        ))
        pinned_box.add_widget(MDLabel(
            text=chat_info.get("pinned_message", "Welcome to Tech Community! 🚀"),
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED,
            shorten=True
        ))
        pinned_card.add_widget(pinned_box)

        btn_close_pin = MDButton(style="text", size_hint_x=None, width="30dp")
        btn_close_pin.add_widget(MDButtonText(text="✕"))
        pinned_card.add_widget(btn_close_pin)

        self.add_widget(pinned_card)

        # Messages Stream with Reaction Badges & Link Card Preview matching image
        self.msg_scroll = ScrollView()
        self.msg_box = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            size_hint_y=None,
            padding=["4dp", "8dp", "4dp", "8dp"]
        )
        self.msg_box.bind(minimum_height=self.msg_box.setter("height"))

        self.render_messages()
        self.msg_scroll.add_widget(self.msg_box)
        self.add_widget(self.msg_scroll)

        # Input Bar
        input_bar = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="48dp")

        btn_add = MDButton(style="outlined", size_hint_x=None, width="40dp")
        btn_add.add_widget(MDButtonText(text="+"))
        input_bar.add_widget(btn_add)

        self.input_field = MDTextField(mode="outlined", size_hint_x=0.65)
        self.input_field.add_widget(MDTextFieldHintText(text="Type a message..."))
        self.input_field.bind(on_text_validate=self.send_msg)
        input_bar.add_widget(self.input_field)

        btn_clip = MDButton(style="text", size_hint_x=None, width="36dp")
        btn_clip.add_widget(MDButtonText(text="📎"))
        input_bar.add_widget(btn_clip)

        btn_send = MDButton(style="filled", size_hint_x=0.25, on_release=self.send_msg)
        btn_send.add_widget(MDButtonText(text="✈️"))
        input_bar.add_widget(btn_send)

        self.add_widget(input_bar)

    def render_messages(self):
        self.msg_box.clear_widgets()
        messages = db_engine.get_chat_messages(self.chat_id)

        for msg in messages:
            is_me = msg.get("is_me", False)

            row = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None)
            
            # Sender Avatar
            av_col = msg.get("avatar_color", "#8494FF")
            initials = "".join([p[0].upper() for p in msg["sender_name"].split()[:2]])
            av = create_avatar_widget(initials, av_col, size_dp=36)
            row.add_widget(av)

            # Bubble Container
            bubble_h = "140dp" if msg.get("link_preview") else "85dp"
            row.height = bubble_h

            bubble = GradientCard(
                color1=COLOR_1A1953,
                color2=COLOR_162E93,
                orientation="vertical",
                padding="10dp",
                spacing="4dp",
                size_hint_x=0.85,
                radius=[16, 16, 16, 4],
                elevation=0
            )

            bubble.add_widget(MDLabel(
                text=msg["sender_name"],
                font_style="Label",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_FFDBFD
            ))

            bubble.add_widget(MDLabel(
                text=msg["text"],
                font_style="Body",
                role="medium",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN
            ))

            # Rich Link Card Preview matching reference image
            if msg.get("link_preview"):
                preview = msg["link_preview"]
                link_card = MDCard(
                    orientation="vertical",
                    padding="8dp",
                    spacing="2dp",
                    size_hint_y=None,
                    height="55dp",
                    radius=[8, 8, 8, 8],
                    md_bg_color=COLOR_080616,
                    line_color=COLOR_6367FF,
                    elevation=0
                )
                link_card.add_widget(MDLabel(
                    text=f"🔗 {preview['url']}",
                    font_style="Label",
                    role="small",
                    theme_text_color="Custom",
                    text_color=COLOR_8494FF
                ))
                link_card.add_widget(MDLabel(
                    text=f"[b]{preview['title']}[/b] - {preview['desc']}",
                    font_style="Label",
                    role="small",
                    markup=True,
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MUTED
                ))
                bubble.add_widget(link_card)

            # Reaction Pills Row matching reference image: 👍 9, ❤️ 25
            if msg.get("reactions"):
                react_row = MDBoxLayout(orientation="horizontal", spacing="6dp", size_hint_y=None, height="22dp")
                for emoji, count in msg["reactions"]:
                    r_badge = create_pill_badge(f"{emoji} {count}", bg_color=[0.388, 0.404, 1.0, 0.25], text_color=COLOR_FFDBFD, height="20dp")
                    react_row.add_widget(r_badge)
                react_row.add_widget(MDLabel(
                    text=msg["time"],
                    font_style="Label",
                    role="small",
                    halign="right",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_SUBTLE
                ))
                bubble.add_widget(react_row)
            else:
                bubble.add_widget(MDLabel(
                    text=msg["time"],
                    font_style="Label",
                    role="small",
                    halign="right",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_SUBTLE
                ))

            row.add_widget(bubble)
            self.msg_box.add_widget(row)

        Clock.schedule_once(lambda dt: setattr(self.msg_scroll, 'scroll_y', 0), 0.1)

    def send_msg(self, *args):
        text = self.input_field.text.strip()
        if text:
            db_engine.send_chat_message(self.chat_id, text)
            self.input_field.text = ""
            self.render_messages()
