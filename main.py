"""
BharatConnect 🇮🇳 — Modern Production Text Messaging Platform (Python Edition)
Single command launcher: python main.py
"""

import sys
import os

# Ensure project root is in path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from kivy.config import Config
Config.set('graphics', 'width', '1100')
Config.set('graphics', 'height', '720')
Config.set('graphics', 'minimum_width', '800')
Config.set('graphics', 'minimum_height', '600')

from kivy.core.window import Window
from kivy.uix.screenmanager import ScreenManager, FadeTransition
from kivymd.app import MDApp

# Set default window background to Deep Slate Midnight
Window.clearcolor = (0.06, 0.09, 0.16, 1.0)

from app.screens.splash import SplashScreen
from app.screens.login import LoginScreen
from app.screens.dashboard import DashboardScreen


class BharatConnectApp(MDApp):
    def build(self):
        self.title = "BharatConnect 🇮🇳 — Modern Production Text Messaging Platform (Python)"
        self.theme_cls.theme_style = "Dark"
        self.theme_cls.primary_palette = "Cyan"

        sm = ScreenManager(transition=FadeTransition())

        self.splash_screen = SplashScreen()
        self.login_screen = LoginScreen()
        self.dashboard_screen = DashboardScreen()

        sm.add_widget(self.splash_screen)
        sm.add_widget(self.login_screen)
        sm.add_widget(self.dashboard_screen)

        sm.dashboard_screen = self.dashboard_screen
        return sm


if __name__ == "__main__":
    print("🚀 Launching BharatConnect Python Kivy + KivyMD Platform...")
    BharatConnectApp().run()
