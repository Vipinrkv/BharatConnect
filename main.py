"""
BharatConnect Mobile Social App
100% Free • Self Dependent • Offline • Cross-Platform
Launcher: python main.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from kivy.config import Config
from kivy.utils import platform

if platform not in ("android", "ios"):
    Config.set("graphics", "width", "410")
    Config.set("graphics", "height", "730")
    Config.set("graphics", "minimum_width", "360")
    Config.set("graphics", "minimum_height", "600")


from kivy.core.window import Window
from kivy.lang import Builder
from kivy.uix.screenmanager import FadeTransition, ScreenManager
from kivymd.app import MDApp

Window.clearcolor = (0.031, 0.024, 0.086, 1.0)

# Python screens in app/screens construct clean single-root view hierarchies dynamically.
# KV loading is bypassed to prevent double-layout conflicts.

from app.screens.splash import SplashScreen
from app.screens.login import LoginScreen
from app.screens.register import RegisterScreen
from app.screens.forgot_password import ForgotPasswordScreen
from app.screens.dashboard import DashboardScreen
from app.screens.settings import SettingsScreen
from app.screens.call import EncryptedCallScreen
from app.screens.edit_profile import EditProfileScreen


class BharatConnectApp(MDApp):
    def build(self):
        self.title = "BharatConnect"
        self.theme_cls.theme_style = "Dark"
        self.theme_cls.primary_palette = "Indigo"

        manager = ScreenManager(transition=FadeTransition())

        self.splash_screen = SplashScreen()
        self.login_screen = LoginScreen()
        self.register_screen = RegisterScreen()
        self.forgot_screen = ForgotPasswordScreen()
        self.dashboard_screen = DashboardScreen()
        self.settings_screen = SettingsScreen()
        self.call_screen = EncryptedCallScreen()
        self.edit_profile_screen = EditProfileScreen()

        manager.add_widget(self.splash_screen)
        manager.add_widget(self.login_screen)
        manager.add_widget(self.register_screen)
        manager.add_widget(self.forgot_screen)
        manager.add_widget(self.dashboard_screen)
        manager.add_widget(self.settings_screen)
        manager.add_widget(self.call_screen)
        manager.add_widget(self.edit_profile_screen)


        manager.dashboard_screen = self.dashboard_screen
        return manager


if __name__ == "__main__":
    print("Launching BharatConnect...")
    BharatConnectApp().run()
