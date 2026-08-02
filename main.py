"""
BharatConnect 🇮🇳 — Core Messaging & Contact Matching Platform (Python Edition)
Theme Palette: #6367FF, #8494FF, #C9BEFF, #FFDBFD
Single command launcher: python main.py
"""

import sys
import os

# Ensure project root is in path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from kivy.config import Config
Config.set('graphics', 'width', '1150')
Config.set('graphics', 'height', '740')
Config.set('graphics', 'minimum_width', '850')
Config.set('graphics', 'minimum_height', '620')

from kivy.core.window import Window
from kivy.uix.screenmanager import ScreenManager, FadeTransition
from kivymd.app import MDApp

# Set default window background color to Deep Slate Midnight
Window.clearcolor = (0.06, 0.08, 0.16, 1.0)

from app.screens.splash import SplashScreen
from app.screens.auth import AuthScreen
from app.screens.permissions import PermissionScreen
from app.screens.dashboard import DashboardScreen


class BharatConnectApp(MDApp):
    def build(self):
        self.title = "BharatConnect 🇮🇳 — Core Text Messaging & Contact Matching Platform"
        self.theme_cls.theme_style = "Dark"
        self.theme_cls.primary_palette = "Indigo"

        sm = ScreenManager(transition=FadeTransition())

        self.splash_screen = SplashScreen()
        self.auth_screen = AuthScreen()
        self.permissions_screen = PermissionScreen()
        self.dashboard_screen = DashboardScreen()

        sm.add_widget(self.splash_screen)
        sm.add_widget(self.auth_screen)
        sm.add_widget(self.permissions_screen)
        sm.add_widget(self.dashboard_screen)

        sm.dashboard_screen = self.dashboard_screen
        return sm


if __name__ == "__main__":
    print("🚀 Launching BharatConnect Core Messaging & Contact Matching Platform...")
    BharatConnectApp().run()
