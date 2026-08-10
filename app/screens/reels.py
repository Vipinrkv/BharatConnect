"""
BharatConnect Reels & Short Video Showcase Screen (app/screens/reels.py)

Full-screen immersive 1-Reel-Per-Page swipe feed with floating Instagram social engagement bar,
audio track ticker, author verified badge, and vertical page snapping navigation.
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
    COLOR_FFDBFD,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    COLOR_TEXT_SUBTLE,
    GradientCard,
    GlassCard,
    create_story_avatar_widget,
    create_verified_badge,
    create_icon_button,
    create_pill_badge,
)


class ReelsView(MDBoxLayout):
    def __init__(self, navigation_callback=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "0dp"
        self.navigation_callback = navigation_callback
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        # Top Overlay Header
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["16dp", "8dp", "16dp", "8dp"],
            spacing="10dp",
            size_hint_y=None,
            height="52dp",
            md_bg_color=COLOR_162E93,
        )

        title_lbl = MDLabel(
            text="Reels",
            font_style="Headline",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        top_bar.add_widget(title_lbl)

        cam_btn = create_icon_button("camera-outline", size_dp=34, icon_size="22sp")
        top_bar.add_widget(cam_btn)
        self.add_widget(top_bar)

        # Full-Page Vertical Reel ScrollView
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        reels_container = MDBoxLayout(
            orientation="vertical",
            spacing="0dp",
            padding=["0dp", "0dp", "0dp", "0dp"],
            size_hint_y=None,
        )
        reels_container.bind(minimum_height=reels_container.setter("height"))

        reels_data = [
            {
                "id": "reel-1",
                "author": "Priya Sharma",
                "avatar": "PS",
                "avatar_color": "#E1306C",
                "caption": "Exploring the majestic Himalayas! 🏔️✨ #TravelIndia #BharatConnect",
                "song": "Original Audio - Priya Sharma",
                "likes": "48.2K",
                "comments": "2.1K",
                "shares": "980",
                "bg_gradient": (COLOR_6367FF, COLOR_2F2FE4),
                "icon": "terrain",
            },
            {
                "id": "reel-2",
                "author": "Rohan Verma",
                "avatar": "RV",
                "avatar_color": "#F77737",
                "caption": "Sub-50ms Realtime WebSocket Tech demo on Python Kivy! 🚀💻 #DeveloperLife",
                "song": "Coding Beats Vol. 4",
                "likes": "92.4K",
                "comments": "5.6K",
                "shares": "3.4K",
                "bg_gradient": (COLOR_162E93, COLOR_1A1953),
                "icon": "code-braces",
            },
            {
                "id": "reel-3",
                "author": "Ananya Patel",
                "avatar": "AP",
                "avatar_color": "#833AB4",
                "caption": "Street Food tour in Old Delhi! 🍲🌶️ #Foodie #Vlog",
                "song": "Desi Hits - Remix",
                "likes": "34.1K",
                "comments": "1.8K",
                "shares": "720",
                "bg_gradient": (COLOR_2F2FE4, COLOR_6367FF),
                "icon": "food",
            },
        ]

        for r in reels_data:
            # 1-Reel-Per-Page Full Screen Card
            reel_card = GradientCard(
                color1=r["bg_gradient"][0],
                color2=r["bg_gradient"][1],
                orientation="vertical",
                padding="16dp",
                spacing="12dp",
                radius=[0, 0, 0, 0],
                size_hint=(1, None),
                height="560dp",
                elevation=0,
            )

            # Center Video Preview Placeholder
            media_center = MDBoxLayout(orientation="vertical", pos_hint={"center_x": 0.5, "center_y": 0.5}, spacing="8dp")
            media_center.add_widget(
                MDIcon(
                    icon=r["icon"],
                    font_size="80sp",
                    halign="center",
                    pos_hint={"center_x": 0.5},
                    theme_text_color="Custom",
                    text_color=COLOR_FFDBFD,
                )
            )
            media_center.add_widget(
                MDLabel(
                    text="▶ REEL VIDEO PLAYING",
                    font_style="Title",
                    role="small",
                    bold=True,
                    halign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                )
            )
            reel_card.add_widget(media_center)

            # Bottom Controls & Metadata (Left Info + Right Dock)
            bottom_dock = MDBoxLayout(orientation="horizontal", spacing="10dp", size_hint_y=None, height="140dp")

            # Left Column (Author, Verified Badge, Follow Pill, Caption, Music Ticker)
            left_col = MDBoxLayout(orientation="vertical", spacing="6dp", size_hint_x=0.8)

            author_row = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="36dp")
            av = create_story_avatar_widget(initials=r["avatar"], bg_hex=r["avatar_color"], size_dp=32, has_unseen=True)
            author_row.add_widget(av)

            author_name = MDLabel(
                text=r["author"],
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                pos_hint={"center_y": 0.5},
                shorten=True,
            )
            author_row.add_widget(author_name)
            author_row.add_widget(create_verified_badge("16sp"))
            author_row.add_widget(create_pill_badge("Follow", bg_color=COLOR_6367FF, text_color=COLOR_TEXT_MAIN, height="24dp"))
            left_col.add_widget(author_row)

            cap_lbl = MDLabel(
                text=r["caption"],
                font_style="Body",
                role="small",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                shorten=True,
            )
            left_col.add_widget(cap_lbl)

            music_box = MDBoxLayout(orientation="horizontal", spacing="6dp", size_hint_y=None, height="20dp")
            music_box.add_widget(MDIcon(icon="music", font_size="16sp", theme_text_color="Custom", text_color=COLOR_FFDBFD))
            music_box.add_widget(
                MDLabel(
                    text=r["song"],
                    font_style="Label",
                    role="small",
                    theme_text_color="Custom",
                    text_color=COLOR_FFDBFD,
                    bold=True,
                    shorten=True,
                )
            )
            left_col.add_widget(music_box)
            bottom_dock.add_widget(left_col)

            # Right Vertical Engagement Dock (Like, Comment, Share)
            right_dock = MDBoxLayout(orientation="vertical", spacing="10dp", size_hint_x=0.2, pos_hint={"center_y": 0.5})

            # Like button + Counter
            like_box = MDBoxLayout(orientation="vertical", spacing="2dp")
            like_btn = create_icon_button("heart", icon_color=[0.95, 0.22, 0.38, 1.0], size_dp=34, icon_size="24sp")
            like_box.add_widget(like_btn)
            like_box.add_widget(
                MDLabel(
                    text=r["likes"],
                    font_style="Label",
                    role="small",
                    halign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                )
            )
            right_dock.add_widget(like_box)

            # Comment button + Counter
            cmt_box = MDBoxLayout(orientation="vertical", spacing="2dp")
            cmt_btn = create_icon_button("chat-outline", size_dp=34, icon_size="22sp")
            cmt_box.add_widget(cmt_btn)
            cmt_box.add_widget(
                MDLabel(
                    text=r["comments"],
                    font_style="Label",
                    role="small",
                    halign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                )
            )
            right_dock.add_widget(cmt_box)

            # Share button
            sh_btn = create_icon_button("share-variant-outline", size_dp=34, icon_size="22sp")
            right_dock.add_widget(sh_btn)

            bottom_dock.add_widget(right_dock)
            reel_card.add_widget(bottom_dock)

            reels_container.add_widget(reel_card)

        scroll.add_widget(reels_container)
        self.add_widget(scroll)
