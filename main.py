"""
BharatConnect 🇮🇳 — Mobile Responsive WhatsApp-Style Python Messaging Platform
Palette: #6367FF, #8494FF, #C9BEFF, #FFDBFD, #2F2FE4, #162E93, #1A1953, #080616
Single command launcher: python main.py
"""

import sys
import os

# Ensure project root is in path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from kivy.config import Config
# Mobile Phone Screen Dimensions (Portrait Mode)
Config.set('graphics', 'width', '410')
Config.set('graphics', 'height', '730')
Config.set('graphics', 'minimum_width', '360')
Config.set('graphics', 'minimum_height', '600')

from kivy.core.window import Window
from kivy.uix.screenmanager import ScreenManager, FadeTransition
from kivymd.app import MDApp

# Set default window background color to Deep Frost Midnight #080616
Window.clearcolor = (0.031, 0.024, 0.086, 1.0)

from app.screens.splash import SplashScreen
from app.screens.auth import AuthScreen
from app.screens.dashboard import DashboardScreen


class BharatConnectApp(MDApp):
    def build(self):
        self.title = "BharatConnect 🇮🇳 — WhatsApp Mobile App"
        self.theme_cls.theme_style = "Dark"
        self.theme_cls.primary_palette = "Indigo"

        sm = ScreenManager(transition=FadeTransition())

        self.splash_screen = SplashScreen()
        self.auth_screen = AuthScreen()
        self.dashboard_screen = DashboardScreen()

        sm.add_widget(self.splash_screen)
        sm.add_widget(self.auth_screen)
        sm.add_widget(self.dashboard_screen)

        sm.dashboard_screen = self.dashboard_screen
        return sm


if __name__ == "__main__":
    print("🚀 Launching BharatConnect Mobile WhatsApp Messaging App...")
    BharatConnectApp().run()
