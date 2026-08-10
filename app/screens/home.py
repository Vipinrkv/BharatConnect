from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
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
    create_story_avatar_widget,
    create_icon_button,
    create_verified_badge,
    create_pill_badge,
)


class HomeScreenView(MDBoxLayout):
    def __init__(self, navigation_callback=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "0dp"
        self.navigation_callback = navigation_callback
        self.like_icons = {}
        self.likes_labels = {}
        self.posts_map = {}
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        # Top App Bar (Instagram/Threads Brand Header)
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["16dp", "8dp", "16dp", "8dp"],
            spacing="12dp",
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )

        title_lbl = MDLabel(
            text="BharatConnect",
            font_style="Headline",
            role="small",
            bold=True,
            valign="center",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        top_bar.add_widget(title_lbl)

        actions_box = MDBoxLayout(
            orientation="horizontal",
            spacing="8dp",
            size_hint_x=None,
            width="120dp",
        )

        actions_box.add_widget(
            create_icon_button(
                "plus-box-outline",
                on_release=lambda x: self.navigation_callback("add_post") if self.navigation_callback else None,
                size_dp=34,
                icon_size="22sp",
            )
        )
        actions_box.add_widget(
            create_icon_button(
                "heart-outline",
                on_release=lambda x: None,
                size_dp=34,
                icon_size="22sp",
            )
        )
        actions_box.add_widget(
            create_icon_button(
                "chat-processing-outline",
                on_release=lambda x: self.navigation_callback("chat") if self.navigation_callback else None,
                size_dp=34,
                icon_size="22sp",
            )
        )

        top_bar.add_widget(actions_box)
        self.add_widget(top_bar)

        # Scrollable Feed Container
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        content_list = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            padding=["12dp", "10dp", "12dp", "20dp"],
            size_hint_y=None,
        )
        content_list.bind(minimum_height=content_list.setter("height"))

        # Stories Scroll Section (Instagram-Style Gradient Ring Avatars)
        stories_scroll = ScrollView(size_hint_y=None, height="100dp", do_scroll_y=False)
        stories_row = MDBoxLayout(
            orientation="horizontal",
            spacing="14dp",
            padding=["4dp", "4dp", "4dp", "4dp"],
            size_hint_x=None,
        )
        stories_row.bind(minimum_width=stories_row.setter("width"))

        stories_data = db_engine.get_stories()
        for s in stories_data:
            s_box = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint=(None, None), size=("68dp", "90dp"))
            
            # Instagram-Style Story Ring
            sav = create_story_avatar_widget(
                initials=s["avatar"],
                bg_hex=s["color"],
                size_dp=54,
                is_user=s.get("is_user", False),
                has_unseen=not s.get("is_user", False),
            )
            s_box.add_widget(sav)

            name_lbl = MDLabel(
                text="Your Story" if s.get("is_user") else s["name"],
                font_style="Label",
                role="small",
                halign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN if s.get("is_user") else COLOR_TEXT_MUTED,
                shorten=True,
            )
            s_box.add_widget(name_lbl)
            stories_row.add_widget(s_box)

        stories_scroll.add_widget(stories_row)
        content_list.add_widget(stories_scroll)

        # Feed Posts List (Instagram Post Card Design)
        posts_data = db_engine.get_posts()
        if not posts_data:
            empty_card = MDCard(
                orientation="vertical",
                padding="24dp",
                spacing="10dp",
                radius=[20, 20, 20, 20],
                theme_bg_color="Custom",
                md_bg_color=COLOR_1A1953,
                elevation=2,
                size_hint_y=None,
                height="160dp",
            )
            empty_card.add_widget(
                MDIcon(
                    icon="post-outline",
                    font_size="44sp",
                    pos_hint={"center_x": 0.5},
                    theme_text_color="Custom",
                    text_color=COLOR_8494FF,
                )
            )
            empty_card.add_widget(
                MDLabel(
                    text="No posts in feed yet",
                    font_style="Title",
                    role="medium",
                    bold=True,
                    halign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                )
            )
            empty_card.add_widget(
                MDLabel(
                    text="Tap the + icon above to publish your first post!",
                    font_style="Body",
                    role="small",
                    halign="center",
                    theme_text_color="Custom",
                    text_color=COLOR_C9BEFF,
                )
            )
            content_list.add_widget(empty_card)

        for p in posts_data:
            pid = p["id"]
            self.posts_map[pid] = p

            post_card = MDCard(
                orientation="vertical",
                padding="14dp",
                spacing="12dp",
                radius=[20, 20, 20, 20],
                theme_bg_color="Custom",
                md_bg_color=COLOR_1A1953,
                elevation=3,
                size_hint_y=None,
            )
            post_card.bind(minimum_height=post_card.setter("height"))

            # 1. Post Header: Avatar with story ring + Author Name + Verified Badge + 3 Dots
            author_row = MDBoxLayout(orientation="horizontal", spacing="10dp", size_hint_y=None, height="46dp")
            
            author_av = create_story_avatar_widget(
                initials=p["user_avatar"],
                bg_hex=p.get("avatar_color", "#6367FF"),
                size_dp=40,
                has_unseen=True,
            )
            author_row.add_widget(author_av)

            info_box = MDBoxLayout(orientation="horizontal", spacing="6dp", pos_hint={"center_y": 0.5})
            author_name_lbl = MDLabel(
                text=p["author_name"],
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                size_hint_x=None,
            )
            author_name_lbl.bind(texture_size=lambda inst, val: setattr(inst, 'width', val[0]))
            info_box.add_widget(author_name_lbl)

            if p.get("author_name") in ["Alice Johnson", "Alex Morgan"]:
                info_box.add_widget(create_verified_badge("16sp"))

            time_lbl = MDLabel(
                text=f"• {p['time_ago']}",
                font_style="Label",
                role="small",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_SUBTLE,
                pos_hint={"center_y": 0.5},
            )
            info_box.add_widget(time_lbl)
            author_row.add_widget(info_box)

            dots_btn = create_icon_button("dots-horizontal", size_dp=32, icon_size="20sp", icon_color=COLOR_TEXT_SUBTLE)
            dots_btn.pos_hint = {"center_y": 0.5}
            author_row.add_widget(dots_btn)
            post_card.add_widget(author_row)

            # 2. Post Caption Content
            post_card.add_widget(
                MDLabel(
                    text=p["content"],
                    font_style="Body",
                    role="medium",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_MAIN,
                    size_hint_y=None,
                    height="46dp",
                )
            )

            # 3. Post Visual Media Banner
            if p.get("image"):
                img_card = GradientCard(
                    color1=COLOR_6367FF if p["id"] == "post-1" else COLOR_162E93,
                    color2=COLOR_2F2FE4,
                    orientation="vertical",
                    padding="16dp",
                    radius=[16, 16, 16, 16],
                    size_hint_y=None,
                    height="145dp",
                    elevation=2,
                )
                
                banner_inner = MDBoxLayout(orientation="vertical", spacing="6dp", pos_hint={"center_x": 0.5, "center_y": 0.5})
                banner_inner.add_widget(
                    MDIcon(
                        icon="weather-sunset" if "Sunset" in p.get("image", "") else "laptop",
                        font_size="42sp",
                        halign="center",
                        pos_hint={"center_x": 0.5},
                        theme_text_color="Custom",
                        text_color=COLOR_FFDBFD,
                    )
                )
                banner_inner.add_widget(
                    MDLabel(
                        text=p['image'],
                        font_style="Title",
                        role="medium",
                        bold=True,
                        halign="center",
                        theme_text_color="Custom",
                        text_color=COLOR_TEXT_MAIN,
                    )
                )
                img_card.add_widget(banner_inner)
                post_card.add_widget(img_card)

            # 4. Instagram Action Icons Bar (Like, Comment, Share, Bookmark)
            actions_bar = MDBoxLayout(orientation="horizontal", spacing="12dp", size_hint_y=None, height="38dp")

            is_liked = p.get("is_liked", False)
            like_icon = "heart" if is_liked else "heart-outline"
            like_color = [0.95, 0.22, 0.38, 1.0] if is_liked else COLOR_TEXT_MAIN

            like_btn = create_icon_button(
                like_icon,
                on_release=lambda instance, post_id=pid: self.on_like_post(post_id),
                icon_color=like_color,
                size_dp=34,
                icon_size="24sp",
            )
            # Reference MDIcon inside like_btn
            if hasattr(like_btn, "children") and like_btn.children:
                self.like_icons[pid] = like_btn.children[0]

            actions_bar.add_widget(like_btn)

            comment_btn = create_icon_button("chat-outline", size_dp=34, icon_size="23sp")
            actions_bar.add_widget(comment_btn)

            share_btn = create_icon_button("share-variant-outline", size_dp=34, icon_size="22sp")
            actions_bar.add_widget(share_btn)

            # Spacer right
            actions_bar.add_widget(MDBoxLayout(size_hint_x=1.0))

            bookmark_btn = create_icon_button("bookmark-outline", size_dp=34, icon_size="23sp")
            actions_bar.add_widget(bookmark_btn)
            post_card.add_widget(actions_bar)

            # 5. Likes Counter & Comments Summary
            likes_count = p.get("likes_count", 124)
            likes_text = f"Liked by alexmorgan and {likes_count:,} others"
            likes_label = MDLabel(
                text=likes_text,
                font_style="Title",
                role="small",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                size_hint_y=None,
                height="20dp",
            )
            self.likes_labels[pid] = likes_label
            post_card.add_widget(likes_label)

            comments_count = p.get("comments_count", 12)
            post_card.add_widget(
                MDLabel(
                    text=f"View all {comments_count} comments",
                    font_style="Label",
                    role="small",
                    theme_text_color="Custom",
                    text_color=COLOR_TEXT_SUBTLE,
                    size_hint_y=None,
                    height="18dp",
                )
            )

            content_list.add_widget(post_card)

        scroll.add_widget(content_list)
        self.add_widget(scroll)

    def on_like_post(self, post_id):
        db_engine.toggle_like_post(post_id)

        # In-place selective state update (Zero full page refresh!)
        p = self.posts_map.get(post_id)
        if p:
            is_liked = not p.get("is_liked", False)
            p["is_liked"] = is_liked
            p["likes_count"] = max(0, p.get("likes_count", 0) + (1 if is_liked else -1))

            if post_id in self.like_icons:
                ic = self.like_icons[post_id]
                ic.icon = "heart" if is_liked else "heart-outline"
                ic.text_color = [0.95, 0.22, 0.38, 1.0] if is_liked else COLOR_TEXT_MAIN

            if post_id in self.likes_labels:
                lbl = self.likes_labels[post_id]
                lbl.text = f"Liked by alexmorgan and {p['likes_count']:,} others"


class HomeScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "home"
        self.add_widget(HomeScreenView())
