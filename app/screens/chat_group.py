"""
BharatConnect Chat (Group) Screen View (Matching Social App UI Set)
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
    COLOR_TEXT_MAIN, COLOR_TEXT_MUTED, COLOR_TEXT_SUBTLE, create_avatar_widget, GradientCard, GradientBox
)


class GroupChatView(MDBoxLayout):
    """Chat (Group) Component matching reference image."""
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "10dp"
        self.chat_id = "c-group"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        chats_data = db_engine.get_chats() if hasattr(db_engine, 'get_chats') else getattr(db_engine, 'chats', {})
        if isinstance(chats_data, list):
            chat_info = next((c for c in chats_data if isinstance(c, dict) and c.get("id") == self.chat_id), {})
        elif isinstance(chats_data, dict):
            chat_info = chats_data.get(self.chat_id, {})
        else:
            chat_info = {}

        # Header Bar matching Group Chat image: Avatar, "Project Team", "8 members, 3 online", Call & Info icons
        header = GradientBox(
            color1=COLOR_162E93,
            color2=COLOR_1A1953,
            orientation_grad="horizontal",
            orientation="horizontal",
            padding=["10dp", "6dp", "10dp", "6dp"],
            spacing="10dp",
            size_hint_y=None,
            height="56dp"
        )

        avatar = create_avatar_widget("PT", "#6367FF", size_dp=40)
        header.add_widget(avatar)

        name_box = MDBoxLayout(orientation="vertical", spacing="2dp")
        name_box.add_widget(MDLabel(
            text=chat_info.get("title", "Project Team"),
            font_style="Title",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))
        name_box.add_widget(MDLabel(
            text=chat_info.get("subtitle", "8 members, 3 online"),
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED
        ))
        header.add_widget(name_box)

        btn_call = MDButton(style="text", size_hint_x=None, width="36dp")
        btn_call.add_widget(MDButtonText(text="📞"))
        header.add_widget(btn_call)

        btn_info = MDButton(style="text", size_hint_x=None, width="36dp")
        btn_info.add_widget(MDButtonText(text="ℹ️"))
        header.add_widget(btn_info)

        self.add_widget(header)

        # Messages Stream with Sender Avatars & Names matching image
        self.msg_scroll = ScrollView()
        self.msg_box = MDBoxLayout(
            orientation="vertical",
            spacing="14dp",
            size_hint_y=None,
            padding=["4dp", "8dp", "4dp", "8dp"]
        )
        self.msg_box.bind(minimum_height=self.msg_box.setter("height"))

        self.render_messages()
        self.msg_scroll.add_widget(self.msg_box)
        self.add_widget(self.msg_scroll)

        # Message Input Bar
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

            row = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="64dp")
            if not is_me:
                # Sender Avatar
                av_col = msg.get("avatar_color", "#8494FF")
                initials = "".join([p[0].upper() for p in msg["sender_name"].split()[:2]])
                av = create_avatar_widget(initials, av_col, size_dp=36)
                row.add_widget(av)

            if is_me:
                row.add_widget(MDBoxLayout(size_hint_x=0.25))
                bubble = GradientCard(
                    color1=COLOR_6367FF,
                    color2=COLOR_2F2FE4,
                    orientation="vertical",
                    padding="10dp",
                    spacing="2dp",
                    size_hint_x=0.75,
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
                    size_hint_x=0.75,
                    radius=[16, 16, 16, 4],
                    elevation=0
                )

            if not is_me:
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

            bubble.add_widget(MDLabel(
                text=msg["time"],
                font_style="Label",
                role="small",
                halign="right" if is_me else "left",
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
