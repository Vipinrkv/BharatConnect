"""
BharatConnect Marketplace Screen View (app/screens/marketplace.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.button import MDButton, MDButtonText
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText
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
    create_pill_badge,
)


from kivymd.uix.label import MDLabel, MDIcon
from utils.helper import create_icon_button


class MarketplaceView(MDBoxLayout):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "10dp"
        self.active_tab = "ALL"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        # Top Bar with Search Field & Bell Icon (Image 1 mockup MARKETPLACE)
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["12dp", "8dp", "12dp", "8dp"],
            spacing="10dp",
            size_hint_y=None,
            height="60dp",
            md_bg_color=COLOR_162E93,
        )

        search_field = MDTextField(mode="filled", size_hint_y=None, height="44dp")
        search_field.add_widget(MDTextFieldHintText(text="Search in marketplace..."))
        top_bar.add_widget(search_field)

        bell_btn = create_icon_button("bell-outline", size_dp=36, icon_size="22sp")
        top_bar.add_widget(bell_btn)
        self.add_widget(top_bar)

        # High-Contrast Filter Tabs Row ([Items] [Jobs] [Quick Jobs])
        tab_row = MDBoxLayout(
            orientation="horizontal",
            padding=["12dp", "4dp", "12dp", "4dp"],
            spacing="8dp",
            size_hint_y=None,
            height="44dp",
        )
        tabs = [("ALL", "All"), ("popular_items", "Items"), ("jobs", "Jobs"), ("quick_jobs", "Quick Jobs")]
        for tab_key, tab_label in tabs:
            is_active = (self.active_tab == tab_key)
            tab_card = MDCard(
                orientation="vertical",
                padding=["4dp", "4dp", "4dp", "4dp"],
                size_hint_x=0.25,
                radius=[12, 12, 12, 12],
                theme_bg_color="Custom",
                md_bg_color=COLOR_6367FF if is_active else COLOR_1A1953,
                ripple_behavior=True,
                elevation=2 if is_active else 0,
                on_release=lambda instance, k=tab_key: self.switch_filter(k),
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
            tab_row.add_widget(tab_card)

        self.add_widget(tab_row)

        # Scrollable Sections Area
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        content_box = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            padding=["12dp", "8dp", "12dp", "16dp"],
            size_hint_y=None,
        )
        content_box.bind(minimum_height=content_box.setter("height"))

        m_data = db_engine.get_marketplace_data()

        # Section 1: Popular Items (Image 1 mockup MARKETPLACE)
        if self.active_tab in ["ALL", "popular_items"]:
            items_header = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="28dp")
            items_header.add_widget(
                MDLabel(text="Popular Items", font_style="Title", role="medium", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN)
            )
            items_header.add_widget(
                MDLabel(text="View all", font_style="Label", role="small", halign="right", theme_text_color="Custom", text_color=COLOR_8494FF)
            )
            content_box.add_widget(items_header)

            items_grid = MDBoxLayout(orientation="horizontal", spacing="12dp", size_hint_y=None, height="145dp")
            for item in m_data.get("popular_items", []):
                card = GradientCard(
                    color1=COLOR_1A1953,
                    color2=COLOR_162E93,
                    orientation="vertical",
                    padding="12dp",
                    spacing="6dp",
                    radius=[16, 16, 16, 16],
                    size_hint_x=0.5,
                    elevation=2,
                    ripple_behavior=True,
                )
                title_text = item.get("title") or "Item"
                price_text = item.get("price") or item.get("price_payout") or "$899"

                card.add_widget(
                    MDIcon(
                        icon="cellphone" if "iPhone" in title_text else "laptop",
                        font_size="32sp",
                        pos_hint={"center_x": 0.5},
                        theme_text_color="Custom",
                        text_color=COLOR_8494FF,
                    )
                )
                card.add_widget(MDLabel(text=title_text, font_style="Title", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN, shorten=True))
                card.add_widget(create_pill_badge(price_text, bg_color=COLOR_6367FF, text_color=COLOR_TEXT_MAIN, height="22dp"))
                items_grid.add_widget(card)
            content_box.add_widget(items_grid)

        # Section 2: Jobs
        if self.active_tab in ["ALL", "jobs"]:
            jobs_header = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="28dp")
            jobs_header.add_widget(
                MDLabel(text="Jobs", font_style="Title", role="medium", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN)
            )
            jobs_header.add_widget(
                MDLabel(text="View all", font_style="Label", role="small", halign="right", theme_text_color="Custom", text_color=COLOR_8494FF)
            )
            content_box.add_widget(jobs_header)

            jobs_row = MDBoxLayout(orientation="horizontal", spacing="12dp", size_hint_y=None, height="115dp")
            for job in m_data.get("jobs", []):
                card = GradientCard(
                    color1=COLOR_162E93,
                    color2=COLOR_2F2FE4,
                    orientation="vertical",
                    padding="12dp",
                    spacing="4dp",
                    radius=[16, 16, 16, 16],
                    size_hint_x=0.5,
                    elevation=2,
                    ripple_behavior=True,
                )
                j_title = job.get("title") or "Job"
                j_desc = job.get("description") or job.get("type_tag") or "Full Time"

                card.add_widget(
                    MDIcon(
                        icon="palette-outline" if "UI/UX" in j_title else "bullhorn-outline",
                        font_size="26sp",
                        theme_text_color="Custom",
                        text_color=COLOR_FFDBFD,
                    )
                )
                card.add_widget(MDLabel(text=j_title, font_style="Title", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN, shorten=True))
                card.add_widget(MDLabel(text=j_desc, font_style="Label", role="small", theme_text_color="Custom", text_color=COLOR_C9BEFF, shorten=True))
                jobs_row.add_widget(card)
            content_box.add_widget(jobs_row)

        # Section 3: Quick Jobs
        if self.active_tab in ["ALL", "quick_jobs"]:
            qj_header = MDBoxLayout(orientation="horizontal", size_hint_y=None, height="28dp")
            qj_header.add_widget(
                MDLabel(text="Quick Jobs", font_style="Title", role="medium", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN)
            )
            qj_header.add_widget(
                MDLabel(text="View all", font_style="Label", role="small", halign="right", theme_text_color="Custom", text_color=COLOR_8494FF)
            )
            content_box.add_widget(qj_header)

            qj_row = MDBoxLayout(orientation="horizontal", spacing="12dp", size_hint_y=None, height="115dp")
            for qj in m_data.get("quick_jobs", []):
                card = GradientCard(
                    color1=COLOR_1A1953,
                    color2=COLOR_6367FF,
                    orientation="vertical",
                    padding="12dp",
                    spacing="4dp",
                    radius=[16, 16, 16, 16],
                    size_hint_x=0.5,
                    elevation=2,
                    ripple_behavior=True,
                )
                q_title = qj.get("title") or "Quick Job"
                q_price = qj.get("price") or qj.get("price_payout") or "$50"

                card.add_widget(MDLabel(text=q_title, font_style="Title", role="small", bold=True, theme_text_color="Custom", text_color=COLOR_TEXT_MAIN, shorten=True))
                card.add_widget(MDLabel(text=q_price, font_style="Label", role="medium", bold=True, theme_text_color="Custom", text_color=COLOR_FFDBFD, shorten=True))
                qj_row.add_widget(card)
            content_box.add_widget(qj_row)

        scroll.add_widget(content_box)
        self.add_widget(scroll)

    def switch_filter(self, filter_key):
        self.active_tab = filter_key
        self.build_ui()


class MarketplaceScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "marketplace"
        self.add_widget(MarketplaceView())
