"""
BharatConnect Edit Profile Screen (app/screens/edit_profile.py)
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
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
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    GradientCard,
    create_avatar_widget,
    create_icon_button,
)


class EditProfileView(MDBoxLayout):
    def __init__(self, back_callback=None, on_saved_callback=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "vertical"
        self.spacing = "0dp"
        self.back_callback = back_callback
        self.on_saved_callback = on_saved_callback
        self.selected_color = "#6367FF"
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        user = db_engine.get_current_user()
        self.user_id = user.get("id")
        self.selected_color = user.get("avatar_color", "#6367FF")

        # Top Bar with Back Arrow & Title
        top_bar = MDBoxLayout(
            orientation="horizontal",
            padding=["8dp", "8dp", "16dp", "8dp"],
            spacing="10dp",
            size_hint_y=None,
            height="56dp",
            md_bg_color=COLOR_162E93,
        )

        back_btn = create_icon_button("arrow-left", on_release=self.go_back, size_dp=36, icon_size="24sp")
        top_bar.add_widget(back_btn)

        title_lbl = MDLabel(
            text="Edit Profile",
            font_style="Title",
            role="large",
            bold=True,
            valign="center",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        top_bar.add_widget(title_lbl)

        # Save checkmark button on top bar
        save_icon_btn = create_icon_button("check", on_release=self.save_profile, size_dp=36, icon_size="24sp")
        top_bar.add_widget(save_icon_btn)
        self.add_widget(top_bar)

        # Scrollable Body Form
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        content = MDBoxLayout(
            orientation="vertical",
            spacing="16dp",
            padding=["16dp", "12dp", "16dp", "24dp"],
            size_hint_y=None,
        )
        content.bind(minimum_height=content.setter("height"))

        # Avatar Customization Card
        avatar_card = GradientCard(
            color1=COLOR_1A1953,
            color2=COLOR_162E93,
            orientation="vertical",
            padding="16dp",
            spacing="12dp",
            radius=[20, 20, 20, 20],
            elevation=4,
            size_hint_y=None,
            height="180dp",
        )

        # Avatar Preview
        self.avatar_preview_box = MDBoxLayout(
            size_hint_y=None,
            height="76dp",
            pos_hint={"center_x": 0.5},
        )
        self.refresh_avatar_preview(user.get("avatar_initials", "AM"), self.selected_color)
        avatar_card.add_widget(self.avatar_preview_box)

        # Color Swatches Row
        color_swatches = [
            "#6367FF", "#2F2FE4", "#162E93", "#8494FF", "#C9BEFF", "#FFDBFD", "#E91E63", "#4CAF50"
        ]
        colors_box = MDBoxLayout(
            orientation="horizontal",
            spacing="8dp",
            size_hint=(None, None),
            height="32dp",
            pos_hint={"center_x": 0.5},
        )
        colors_box.width = f"{len(color_swatches) * 38}dp"

        for hex_color in color_swatches:
            swatch = MDCard(
                size_hint=(None, None),
                size=("30dp", "30dp"),
                radius=[15, 15, 15, 15],
                theme_bg_color="Custom",
                md_bg_color=hex_color,
                elevation=2 if hex_color == self.selected_color else 0,
                ripple_behavior=True,
                on_release=lambda inst, c=hex_color: self.select_color(c),
            )
            colors_box.add_widget(swatch)

        avatar_card.add_widget(colors_box)
        content.add_widget(avatar_card)

        # Main Details Input Form Card
        form_card = MDCard(
            orientation="vertical",
            padding="18dp",
            spacing="14dp",
            radius=[20, 20, 20, 20],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            elevation=3,
            size_hint_y=None,
        )
        form_card.bind(minimum_height=form_card.setter("height"))

        # Section Title
        form_card.add_widget(
            MDLabel(
                text="Personal Details",
                font_style="Title",
                role="medium",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
                size_hint_y=None,
                height="28dp",
            )
        )

        # Display Name
        self.input_name = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_name.add_widget(MDTextFieldHintText(text="Display Name"))
        self.input_name.text = user.get("display_name") or user.get("name") or ""
        form_card.add_widget(self.input_name)

        # Username
        self.input_username = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_username.add_widget(MDTextFieldHintText(text="Username"))
        self.input_username.text = user.get("username") or ""
        form_card.add_widget(self.input_username)

        # Bio
        self.input_bio = MDTextField(mode="filled", multiline=True, size_hint_y=None, height="80dp")
        self.input_bio.add_widget(MDTextFieldHintText(text="Bio / Status"))
        self.input_bio.text = user.get("bio") or ""
        form_card.add_widget(self.input_bio)

        # Phone Number
        self.input_phone = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_phone.add_widget(MDTextFieldHintText(text="Phone Number"))
        self.input_phone.text = user.get("phone") or ""
        form_card.add_widget(self.input_phone)

        # Status Message
        self.input_status = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_status.add_widget(MDTextFieldHintText(text="Status Message"))
        self.input_status.text = user.get("status_message") or ""
        form_card.add_widget(self.input_status)

        # Country / Location
        self.input_country = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_country.add_widget(MDTextFieldHintText(text="Country / Location"))
        self.input_country.text = user.get("country") or "India"
        form_card.add_widget(self.input_country)

        # Avatar Initials
        self.input_initials = MDTextField(mode="filled", size_hint_y=None, height="54dp")
        self.input_initials.add_widget(MDTextFieldHintText(text="Avatar Initials (e.g. AM)"))
        self.input_initials.text = user.get("avatar_initials") or "AM"
        self.input_initials.bind(text=self.on_initials_changed)
        form_card.add_widget(self.input_initials)

        content.add_widget(form_card)

        # Status / Feedback label
        self.lbl_status = MDLabel(
            text="",
            font_style="Body",
            role="small",
            halign="center",
            theme_text_color="Custom",
            text_color=COLOR_8494FF,
            size_hint_y=None,
            height="26dp",
        )
        content.add_widget(self.lbl_status)

        # Action Buttons Row (Save & Cancel)
        actions_box = MDBoxLayout(
            orientation="horizontal",
            spacing="12dp",
            size_hint_y=None,
            height="50dp",
        )

        cancel_btn = MDCard(
            size_hint_x=0.4,
            height="46dp",
            radius=[14, 14, 14, 14],
            theme_bg_color="Custom",
            md_bg_color=COLOR_1A1953,
            ripple_behavior=True,
            on_release=self.go_back,
        )
        cancel_btn.add_widget(
            MDLabel(
                text="Cancel",
                font_style="Title",
                role="small",
                bold=True,
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MUTED,
            )
        )
        actions_box.add_widget(cancel_btn)

        save_btn = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            size_hint_x=0.6,
            height="46dp",
            radius=[14, 14, 14, 14],
            ripple_behavior=True,
            on_release=self.save_profile,
        )
        save_btn.add_widget(
            MDLabel(
                text="Save Changes",
                font_style="Title",
                role="small",
                bold=True,
                halign="center",
                valign="center",
                theme_text_color="Custom",
                text_color=COLOR_TEXT_MAIN,
            )
        )
        actions_box.add_widget(save_btn)

        content.add_widget(actions_box)

        scroll.add_widget(content)
        self.add_widget(scroll)

    def refresh_avatar_preview(self, initials, color_hex):
        self.avatar_preview_box.clear_widgets()
        av = create_avatar_widget(
            initials=initials,
            bg_hex=color_hex,
            size_dp=72,
            is_circle=True,
        )
        av.pos_hint = {"center_x": 0.5, "center_y": 0.5}
        self.avatar_preview_box.add_widget(av)

    def select_color(self, hex_color):
        self.selected_color = hex_color
        initials = self.input_initials.text.strip().upper() or "AM"
        self.refresh_avatar_preview(initials, self.selected_color)

    def on_initials_changed(self, instance, text):
        initials = text.strip().upper()[:3] or "AM"
        self.refresh_avatar_preview(initials, self.selected_color)

    def go_back(self, *args):
        if self.back_callback:
            self.back_callback()
        elif hasattr(self, "manager") and self.manager and self.manager.has_screen("dashboard"):
            self.manager.current = "dashboard"

    def save_profile(self, *args):
        display_name = self.input_name.text.strip()
        username = self.input_username.text.strip()
        bio = self.input_bio.text.strip()
        phone = self.input_phone.text.strip()
        status_message = self.input_status.text.strip()
        country = self.input_country.text.strip()
        avatar_initials = self.input_initials.text.strip().upper()[:3]

        if not display_name:
            self.lbl_status.text_color = [1, 0.4, 0.4, 1]
            self.lbl_status.text = "Display Name cannot be empty."
            return

        if not username:
            self.lbl_status.text_color = [1, 0.4, 0.4, 1]
            self.lbl_status.text = "Username cannot be empty."
            return

        success, msg = db_engine.update_user_profile(
            user_id=self.user_id,
            display_name=display_name,
            username=username,
            bio=bio,
            phone=phone,
            status_message=status_message,
            country=country,
            avatar_initials=avatar_initials if avatar_initials else None,
            avatar_color=self.selected_color,
        )

        if success:
            self.lbl_status.text_color = [0.4, 1, 0.5, 1]
            self.lbl_status.text = "Profile updated successfully!"
            if self.on_saved_callback:
                self.on_saved_callback()
            elif self.back_callback:
                self.back_callback()
            elif hasattr(self, "manager") and self.manager and self.manager.has_screen("dashboard"):
                self.manager.current = "dashboard"
        else:
            self.lbl_status.text_color = [1, 0.4, 0.4, 1]
            self.lbl_status.text = str(msg)


class EditProfileScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "edit_profile"
        self.add_widget(EditProfileView(back_callback=self.go_back))

    def go_back(self):
        if self.manager:
            self.manager.current = "dashboard"
