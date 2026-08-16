"""
BharatConnect Chat (Individual) Screen View (Matching Social App UI Set)
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


class IndividualChatView(MDBoxLayout):
    """Chat (Individual) Component matching reference image."""
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "10dp"
        self.chat_id = "c-individual"
        self._last_msg_count = 0
        self._last_msg_id = ""
        self._poll_event = None
        self.build_ui()

    def on_parent(self, widget, parent):
        if parent is not None:
            db_engine.register_chat_listener(self.chat_id, self.on_instant_message)
            self.start_live_polling()
        else:
            db_engine.unregister_chat_listener(self.chat_id, self.on_instant_message)
            self.stop_live_polling()

    def on_instant_message(self, data=None):
        Clock.schedule_once(lambda dt: self.render_messages(), 0)

    def start_live_polling(self):
        if not self._poll_event:
            self._poll_event = Clock.schedule_interval(self.check_live_updates, 0.4)


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

        chats = db_engine.get_chats()
        if isinstance(chats, list):
            chat_info = next((c for c in chats if isinstance(c, dict) and c.get("id") == self.chat_id), {})
        elif isinstance(chats, dict):
            chat_info = chats.get(self.chat_id, {})
        else:
            chat_info = {}

        # Header Bar matching Individual Chat image: Back arrow, Avatar, Name ("Emma Watson"), "Online", Video call, Voice call
        header = GradientBox(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            orientation_grad="horizontal",
            orientation="horizontal",
            padding=["10dp", "6dp", "10dp", "6dp"],
            spacing="10dp",
            size_hint_y=None,
            height="56dp"
        )

        avatar = create_avatar_widget(chat_info.get("avatar_initials", "EW"), chat_info.get("avatar_color", "#8494FF"), size_dp=40)
        header.add_widget(avatar)

        name_box = MDBoxLayout(orientation="vertical", spacing="2dp")
        name_box.add_widget(MDLabel(
            text=chat_info.get("title", "Emma Watson"),
            font_style="Title",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))

        name_box.add_widget(MDLabel(
            text="Online",
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_FFDBFD
        ))
        header.add_widget(name_box)

        # Call Action Buttons (Video & Voice call icons)
        btn_vcall = MDButton(style="text", size_hint_x=None, width="36dp")
        btn_vcall.add_widget(MDButtonText(text="📹"))
        header.add_widget(btn_vcall)

        btn_acall = MDButton(style="text", size_hint_x=None, width="36dp")
        btn_acall.add_widget(MDButtonText(text="📞"))
        header.add_widget(btn_acall)

        self.add_widget(header)

        # Messages Bubble Stream matching reference image
        self.msg_scroll = ScrollView()
        self.msg_box = MDBoxLayout(
            orientation="vertical",
            spacing="12dp",
            size_hint_y=None,
            padding=["4dp", "8dp", "4dp", "8dp"]
        )
        self.msg_box.bind(minimum_height=self.msg_box.setter("height"))

        self.render_messages()
        self.msg_scroll.add_widget(self.msg_box)
        self.add_widget(self.msg_scroll)

        # Message Input Bar matching reference image: Emoji, Text field, clip, send
        input_bar = MDBoxLayout(orientation="horizontal", spacing="6dp", size_hint_y=None, height="48dp")

        btn_emoji = MDButton(style="text", size_hint_x=None, width="36dp", on_release=self.insert_quick_emoji)
        btn_emoji.add_widget(MDButtonText(text="😊"))
        input_bar.add_widget(btn_emoji)

        self.input_field = MDTextField(mode="outlined", size_hint_x=0.64)
        self.input_field.add_widget(MDTextFieldHintText(text="Message"))
        self.input_field.bind(on_text_validate=self.send_msg)
        input_bar.add_widget(self.input_field)

        btn_clip = MDButton(style="text", size_hint_x=None, width="36dp", on_release=self.attach_doc)
        btn_clip.add_widget(MDButtonText(text="📎"))
        input_bar.add_widget(btn_clip)

        btn_send = MDButton(style="filled", size_hint_x=0.20, on_release=self.send_msg)
        btn_send.add_widget(MDButtonText(text="✈️"))
        input_bar.add_widget(btn_send)

        self.add_widget(input_bar)


    def insert_quick_emoji(self, *args):
        emojis = ["😊", "😂", "❤️", "👍", "🔥", "🙏", "🎉", "🇮🇳"]
        import random
        selected = random.choice(emojis)
        self.input_field.text += selected

    def attach_doc(self, *args):
        text = "📄 Document: BharatConnect.apk (9.1 MB)"
        db_engine.send_chat_message(self.chat_id, text)
        self.render_messages()

    def attach_camera(self, *args):
        text = "📷 Photo Attachment"
        db_engine.send_chat_message(self.chat_id, text)
        self.render_messages()


    def render_messages(self):
        self.msg_box.clear_widgets()
        messages = db_engine.get_chat_messages(self.chat_id)

        for msg in messages:
            is_me = msg.get("is_me", False)

            row = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="56dp")
            if is_me:
                row.add_widget(MDBoxLayout(size_hint_x=0.2))

            # Bubble Card matching colors in reference image
            if is_me:
                bubble = GradientCard(
                    color1=COLOR_6367FF,
                    color2=COLOR_2F2FE4,
                    orientation="vertical",
                    padding="10dp",
                    spacing="2dp",
                    size_hint_x=0.8,
                    radius=[16, 16, 4, 16],
                    elevation=0
                )
            else:
                bubble = GradientCard(
                    color1=COLOR_1A1953,
                    color2=COLOR_162E93,
                    orientation="vertical",
                    padding="10dp",
                    spacing="2dp",
                    size_hint_x=0.8,
                    radius=[16, 16, 16, 4],
                    elevation=0
                )

            bubble.add_widget(MDLabel(
                text=msg["text"],
                font_style="Body",
                role="medium",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN
            ))

            bubble.add_widget(MDLabel(
                text=f"{msg['time']} {'✓✓' if is_me else ''}",
                font_style="Label",
                role="small",
                halign="right" if is_me else "left",
                theme_text_color="Custom",
                text_color=COLOR_FFDBFD if is_me else COLOR_TEXT_SUBTLE
            ))

            row.add_widget(bubble)
            if not is_me:
                row.add_widget(MDBoxLayout(size_hint_x=0.2))

            self.msg_box.add_widget(row)

        Clock.schedule_once(lambda dt: setattr(self.msg_scroll, 'scroll_y', 0), 0.1)

    def send_msg(self, *args):
        text = self.input_field.text.strip()
        if text:
            db_engine.send_chat_message(self.chat_id, text)
            self.input_field.text = ""
            self.render_messages()
