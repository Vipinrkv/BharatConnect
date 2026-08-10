"""
BharatConnect Profile Screen View (app/screens/profile.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard
from kivy.uix.scrollview import ScrollView

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
    create_avatar_widget,
    create_icon_button,
)


class ProfileView(MDBoxLayout):
    def __init__(self, open_settings_callback=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "10dp"
        self.open_settings_callback = open_settings_callback
        self.is_editing = False
        self.build_ui()

    def open_edit_mode(self, *args):
        self.is_editing = True
        self.build_ui()

    def close_edit_mode(self, *args):
        self.is_editing = False
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        if self.is_editing:
            from app.screens.edit_profile import EditProfileView

            edit_view = EditProfileView(
                back_callback=self.close_edit_mode,
                on_saved_callback=self.close_edit_mode,
            )
            self.add_widget(edit_view)
            return

        user = db_engine.get_current_user()

        # Top Bar (PROFILE)
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["16dp", "8dp", "16dp", "8dp"],
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )

        title_lbl = MDLabel(
            text="My Profile",
            font_style="Title",
            role="large",
            bold=True,
            halign="center",
            valign="center",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        top_bar.add_widget(title_lbl)

        gear_btn = create_icon_button("cog-outline", on_release=self.go_settings, size_dp=36, icon_size="24sp")
        top_bar.add_widget(gear_btn)
        self.add_widget(top_bar)

        # Scrollable Body
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        content = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            padding=["14dp", "10dp", "14dp", "16dp"],
            size_hint_y=None,
        )
        content.bind(minimum_height=content.setter("height"))

        # Profile Header Card (Avatar, Name, Handle, Stats, Bio, Edit Profile)
        header_card = GradientCard(
            color1=COLOR_1A1953,
            color2=COLOR_162E93,
            orientation="vertical",
            padding="16dp",
            spacing="10dp",
            radius=[22, 22, 22, 22],
            elevation=4,
            size_hint_y=None,
            height="320dp",
        )

        # Avatar with Camera Badge Overlay
        avatar_box = MDBoxLayout(size_hint_y=None, height="84dp", pos_hint={"center_x": 0.5})
        avatar_container = MDCard(
            size_hint=(None, None),
            size=("80dp", "84dp"),
            pos_hint={"center_x": 0.5},
            theme_bg_color="Custom",
            md_bg_color=[0, 0, 0, 0],
            elevation=0,
            ripple_behavior=True,
            on_release=self.open_edit_mode,
        )
        av_initials = user.get("avatar_initials") or user.get("profile_pic") or "AM"
        av_color = user.get("avatar_color", "#6367FF")

        av = create_avatar_widget(
            initials=av_initials,
            bg_hex=av_color,
            size_dp=76,
            is_circle=True,
        )
        av.pos_hint = {"center_x": 0.5, "center_y": 0.5}
        avatar_container.add_widget(av)

        # Camera Badge Pill
        cam_badge = MDCard(
            size_hint=(None, None),
            size=("26dp", "26dp"),
            radius=[13, 13, 13, 13],
            theme_bg_color="Custom",
            md_bg_color=COLOR_6367FF,
            pos_hint={"right": 1, "y": 0},
            elevation=2,
            ripple_behavior=True,
            on_release=self.open_edit_mode,
        )
        cam_badge.add_widget(
            MDIcon(
                icon="camera",
                font_size="14sp",
                pos_hint={"center_x": 0.5, "center_y": 0.5},
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        avatar_container.add_widget(cam_badge)
        avatar_box.add_widget(avatar_container)
        header_card.add_widget(avatar_box)

        # Name & Handle
        name_text = user.get("display_name") or user.get("name") or "Alex Morgan"
        user_text = user.get("username") or "alexmorgan"

        header_card.add_widget(
            MDLabel(
                text=name_text,
                font_style="Headline",
                role="small",
                bold=True,
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                size_hint_y=None,
                height="30dp",
                shorten=True,
            )
        )

        header_card.add_widget(
            MDLabel(
                text=f"@{user_text}",
                font_style="Label",
                role="medium",
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_C9BEFF,
                size_hint_y=None,
                height="22dp",
                shorten=True,
            )
        )

        # Stats Row (128 Posts | 1.2K Followers | 320 Following)
        stats_row = MDBoxLayout(
            orientation="horizontal",
            spacing="16dp",
            size_hint_y=None,
            height="46dp",
            pos_hint={"center_x": 0.5},
        )
        for num, label in [
            (str(user.get("posts_count", 128)), "Posts"),
            (str(user.get("followers_count", "1.2K")), "Followers"),
            (str(user.get("following_count", 320)), "Following"),
        ]:
            col = MDBoxLayout(orientation="vertical", spacing="2dp", size_hint_x=0.33)
            col.add_widget(
                MDLabel(
                    text=num,
                    font_style="Title",
                    role="small",
                    bold=True,
                    halign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                )
            )
            col.add_widget(
                MDLabel(
                    text=label,
                    font_style="Label",
                    role="small",
                    halign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MUTED,
                )
            )
            stats_row.add_widget(col)
        header_card.add_widget(stats_row)

        # Bio Paragraph
        header_card.add_widget(
            MDLabel(
                text=user.get("bio", "Passionate about technology, coffee and making a difference. 🚀"),
                font_style="Body",
                role="small",
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED,
                size_hint_y=None,
                height="38dp",
            )
        )

        # Edit Profile Button
        edit_btn = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            size_hint_y=None,
            height="44dp",
            radius=[14, 14, 14, 14],
            ripple_behavior=True,
            on_release=self.open_edit_mode,
        )
        edit_btn.add_widget(
            MDLabel(
                text="Edit Profile",
                font_style="Title",
                role="small",
                bold=True,
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        header_card.add_widget(edit_btn)
        content.add_widget(header_card)

        # Quick List Options (My Posts, Saved Items, My Jobs, My Communities)
        options_card = MDCard(
            orientation="vertical",
            padding="6dp",
            spacing="4dp",
            radius=[18, 18, 18, 18],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            elevation=2,
            size_hint_y=None,
        )
        options_card.bind(minimum_height=options_card.setter("height"))

        menu_items = [
            ("image-multiple-outline", "My Posts"),
            ("bookmark-outline", "Saved Items"),
            ("briefcase-outline", "My Jobs"),
            ("earth", "My Communities"),
        ]

        for icon_name, label in menu_items:
            row = MDBoxLayout(
                orientation="horizontal",
                padding=["12dp", "10dp", "12dp", "10dp"],
                spacing="12dp",
                size_hint_y=None,
                height="50dp",
            )
            row.add_widget(
                MDIcon(
                    icon=icon_name,
                    font_size="22sp",
                    size_hint_x=None,
                    width="28dp",
                    valign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_8494FF,
                )
            )
            row.add_widget(
                MDLabel(
                    text=label,
                    font_style="Title",
                    role="small",
                    bold=True,
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                    valign="center",
                )
            )
            row.add_widget(
                MDLabel(
                    text=">",
                    halign="right",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_SUBTLE,
                    size_hint_x=None,
                    width="20dp",
                    valign="center",
                )
            )
            options_card.add_widget(row)

        content.add_widget(options_card)
        scroll.add_widget(content)
        self.add_widget(scroll)

    def go_settings(self, *args):
        if self.open_settings_callback:
            self.open_settings_callback()


class ProfileScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "profile"
        self.add_widget(ProfileView())
