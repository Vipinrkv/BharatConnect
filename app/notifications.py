"""
BharatConnect Smart In-App Notification Engine (app/notifications.py)

Renders animated, non-blocking toast banners and notification alerts for
messages, post interactions, cloud connectivity updates, and security events.
"""

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel, MDIcon
from kivymd.uix.card import MDCard
from kivy.clock import Clock
from kivy.animation import Animation

from utils.helper import (
    COLOR_162E93,
    COLOR_6367FF,
    COLOR_8494FF,
    COLOR_TEXT_MAIN,
    COLOR_TEXT_MUTED,
    GradientCard,
)


class SmartNotificationToast(GradientCard):
    def __init__(self, title: str, message: str, icon_name: str = "bell", bg_color1=COLOR_6367FF, **kwargs):
        super().__init__(
            color1=bg_color1,
            color2=COLOR_162E93,
            orientation="horizontal",
            padding=["12dp", "8dp", "12dp", "8dp"],
            spacing="10dp",
            size_hint=(0.9, None),
            height="50dp",
            radius=[16, 16, 16, 16],
            pos_hint={"center_x": 0.5, "top": 1.05},
            elevation=4,
            **kwargs,
        )

        # Vector Icon
        ic = MDIcon(
            icon=icon_name,
            font_size="22sp",
            pos_hint={"center_y": 0.5},
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
        )
        self.add_widget(ic)

        # Text Content Box
        text_box = MDBoxLayout(orientation="vertical", spacing="1dp", pos_hint={"center_y": 0.5})
        t_lbl = MDLabel(
            text=title,
            font_style="Title",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN,
            shorten=True,
        )
        m_lbl = MDLabel(
            text=message,
            font_style="Label",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED,
            shorten=True,
        )
        text_box.add_widget(t_lbl)
        text_box.add_widget(m_lbl)
        self.add_widget(text_box)

    def show(self, parent_widget):
        """Animates toast sliding down from top."""
        parent_widget.add_widget(self)
        anim = Animation(pos_hint={"center_x": 0.5, "top": 0.96}, duration=0.3, t="out_quad")
        anim.start(self)
        Clock.schedule_once(lambda dt: self.dismiss(), 3.5)

    def dismiss(self):
        """Animates toast sliding back up and removes from parent."""
        anim = Animation(pos_hint={"center_x": 0.5, "top": 1.05}, duration=0.3, t="in_quad")
        anim.bind(on_complete=lambda a, w: self.parent.remove_widget(self) if self.parent else None)
        anim.start(self)


class NotificationManager:
    @staticmethod
    def notify(parent_widget, title: str, message: str, icon_name: str = "bell-outline", bg_color=COLOR_6367FF):
        """Dispatches a smart toast notification banner."""
        if not parent_widget:
            return
        toast = SmartNotificationToast(title=title, message=message, icon_name=icon_name, bg_color1=bg_color)
        toast.show(parent_widget)


notification_manager = NotificationManager()
