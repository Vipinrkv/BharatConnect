"""
BharatConnect Mobile Authentication Screen (Vibrant Gradient Edition)
Palette: #6367FF, #8494FF, #C9BEFF, #FFDBFD, #2F2FE4, #162E93, #1A1953, #080616
"""

from kivymd.uix.screen import MDScreen
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.card import MDCard
from kivymd.uix.textfield import MDTextField, MDTextFieldHintText
from kivymd.uix.button import MDButton, MDButtonText
from kivy.uix.scrollview import ScrollView

from database.db import db_engine
from app.theme import (
    COLOR_080616, COLOR_1A1953, COLOR_162E93,
    COLOR_6367FF, COLOR_8494FF, COLOR_C9BEFF, COLOR_FFDBFD, COLOR_2F2FE4,
    COLOR_TEXT_MAIN, COLOR_TEXT_MUTED, create_pill_badge, GradientCard
)


class AuthScreen(MDScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = "auth"
        self.active_mode = "signin"  # 'signin', 'register', 'forgot'
        self.status_msg = ""
        self.build_ui()

    def build_ui(self):
        self.clear_widgets()

        root = MDBoxLayout(
            orientation="vertical",
            padding="16dp",
            spacing="12dp",
            md_bg_color=COLOR_080616
        )

        # Header Title with Gradient Pill Banner
        header_box = MDBoxLayout(orientation="vertical", spacing="4dp", size_hint_y=None, height="60dp")
        
        row_title = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="30dp")
        row_title.add_widget(MDLabel(
            text="🇮🇳 BharatConnect Auth",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))
        row_title.add_widget(create_pill_badge("SECURE AUTH", bg_color=[0.388, 0.404, 1.0, 0.35], text_color=COLOR_FFDBFD))
        header_box.add_widget(row_title)

        header_box.add_widget(MDLabel(
            text="Sign in using email, mobile number, or username — or register a new account.",
            font_style="Body",
            role="small",
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MUTED
        ))
        root.add_widget(header_box)

        # Tab Navigation (Sign In / Register / Forgot Password)
        nav_tab_box = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="40dp")
        
        btn_tab_signin = MDButton(
            style="filled" if self.active_mode == "signin" else "outlined",
            size_hint_x=0.33,
            on_release=lambda inst: self.switch_mode("signin")
        )
        btn_tab_signin.add_widget(MDButtonText(text="🔑 Sign In"))
        nav_tab_box.add_widget(btn_tab_signin)

        btn_tab_reg = MDButton(
            style="filled" if self.active_mode == "register" else "outlined",
            size_hint_x=0.33,
            on_release=lambda inst: self.switch_mode("register")
        )
        btn_tab_reg.add_widget(MDButtonText(text="📝 Register"))
        nav_tab_box.add_widget(btn_tab_reg)

        btn_tab_forgot = MDButton(
            style="filled" if self.active_mode == "forgot" else "outlined",
            size_hint_x=0.34,
            on_release=lambda inst: self.switch_mode("forgot")
        )
        btn_tab_forgot.add_widget(MDButtonText(text="🔒 Forgot"))
        nav_tab_box.add_widget(btn_tab_forgot)

        root.add_widget(nav_tab_box)

        # Status Message Label (if any)
        if self.status_msg:
            lbl_status = MDLabel(
                text=self.status_msg,
                font_style="Label",
                role="medium",
                bold=True,
                theme_text_color="Custom",
                text_color=COLOR_FFDBFD,
                size_hint_y=None,
                height="22dp"
            )
            root.add_widget(lbl_status)

        # Scrollable Form Content
        scroll = ScrollView()
        self.form_box = MDBoxLayout(
            orientation="vertical",
            spacing="12dp",
            size_hint_y=None,
            padding=["0dp", "4dp", "0dp", "12dp"]
        )
        self.form_box.bind(minimum_height=self.form_box.setter("height"))

        if self.active_mode == "signin":
            self.build_signin_form()
        elif self.active_mode == "register":
            self.build_register_form()
        elif self.active_mode == "forgot":
            self.build_forgot_form()

        scroll.add_widget(self.form_box)
        root.add_widget(scroll)

        # Bottom Bar
        bottom_bar = MDBoxLayout(orientation="horizontal", spacing="16dp", size_hint_y=None, height="40dp")
        btn_back = MDButton(style="text", on_release=self.go_back)
        btn_back.add_widget(MDButtonText(text="← Back to Splash"))
        bottom_bar.add_widget(btn_back)

        root.add_widget(bottom_bar)
        self.add_widget(root)

    def switch_mode(self, mode):
        self.active_mode = mode
        self.status_msg = ""
        self.build_ui()

    # ---------------------------------------------------------
    # SIGN IN FORM
    # ---------------------------------------------------------
    def build_signin_form(self):
        card = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            color3=COLOR_162E93,
            orientation="horizontal",
            padding="16dp",
            spacing="12dp",
            size_hint_y=None,
            height="315dp",
            radius=[18, 18, 18, 18],
            elevation=0
        )
        card.add_widget(MDLabel(
            text="🔑 Sign In to BharatConnect",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))

        self.input_login_id = MDTextField(mode="outlined", size_hint_y=None, height="48dp")
        self.input_login_id.add_widget(MDTextFieldHintText(text="Email, Mobile Number (+91), or Username"))
        card.add_widget(self.input_login_id)

        self.input_login_pass = MDTextField(mode="outlined", password=True, size_hint_y=None, height="48dp")
        self.input_login_pass.add_widget(MDTextFieldHintText(text="Password"))
        card.add_widget(self.input_login_pass)

        btn_submit = MDButton(
            style="filled",
            size_hint_y=None,
            height="44dp",
            on_release=self.do_signin
        )
        btn_submit.add_widget(MDButtonText(text="Verify & Open WhatsApp Home 🚀"))
        card.add_widget(btn_submit)

        self.form_box.add_widget(card)

        # Quick Test Accounts Selector Card
        quick_card = GradientCard(
            color1=COLOR_1A1953,
            color2=COLOR_162E93,
            orientation="horizontal",
            padding="14dp",
            spacing="8dp",
            size_hint_y=None,
            height="170dp",
            radius=[16, 16, 16, 16],
            elevation=0
        )
        quick_card.add_widget(MDLabel(
            text="⚡ Or Select a Test Account:",
            font_style="Title",
            role="small",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_FFDBFD
        ))

        row_quick = MDBoxLayout(orientation="horizontal", spacing="8dp", size_hint_y=None, height="42dp")
        for uid, user in list(db_engine.users.items())[:3]:
            btn_u = MDButton(
                style="outlined",
                size_hint_x=0.33,
                on_release=lambda inst, u_id=uid: self.select_quick_user(u_id)
            )
            btn_u.add_widget(MDButtonText(text=user["display_name"].split()[0]))
            row_quick.add_widget(btn_u)

        quick_card.add_widget(row_quick)
        self.form_box.add_widget(quick_card)

    def do_signin(self, *args):
        login_id = self.input_login_id.text.strip()
        password = self.input_login_pass.text.strip()
        if not login_id:
            login_id = "vipin_k"

        success, user = db_engine.authenticate_user(login_id, password)
        if hasattr(self.manager, 'dashboard_screen'):
            self.manager.dashboard_screen.reload_user_session()
        self.manager.current = "dashboard"

    def select_quick_user(self, user_id):
        db_engine.switch_user(user_id)
        if hasattr(self.manager, 'dashboard_screen'):
            self.manager.dashboard_screen.reload_user_session()
        self.manager.current = "dashboard"

    # ---------------------------------------------------------
    # REGISTER FORM
    # ---------------------------------------------------------
    def build_register_form(self):
        card = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            color3=COLOR_162E93,
            orientation="horizontal",
            padding="16dp",
            spacing="10dp",
            size_hint_y=None,
            height="505dp",
            radius=[18, 18, 18, 18],
            elevation=0
        )
        card.add_widget(MDLabel(
            text="📝 Register New Account",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))

        self.reg_fullname = MDTextField(mode="outlined", size_hint_y=None, height="46dp")
        self.reg_fullname.add_widget(MDTextFieldHintText(text="Full Name (e.g. Vikramaditya Singh)"))
        card.add_widget(self.reg_fullname)

        self.reg_email = MDTextField(mode="outlined", size_hint_y=None, height="46dp")
        self.reg_email.add_widget(MDTextFieldHintText(text="Email Address (e.g. vikram@domain.com)"))
        card.add_widget(self.reg_email)

        self.reg_phone = MDTextField(mode="outlined", size_hint_y=None, height="46dp", text="+91 ")
        self.reg_phone.add_widget(MDTextFieldHintText(text="Phone (+91 98765 11223)"))
        card.add_widget(self.reg_phone)

        self.reg_username = MDTextField(mode="outlined", size_hint_y=None, height="46dp")
        self.reg_username.add_widget(MDTextFieldHintText(text="Username (e.g. vikram_dev)"))
        card.add_widget(self.reg_username)

        self.reg_dob = MDTextField(mode="outlined", size_hint_y=None, height="46dp", text="2000-01-15")
        self.reg_dob.add_widget(MDTextFieldHintText(text="Date of Birth (YYYY-MM-DD)"))
        card.add_widget(self.reg_dob)

        self.reg_pass = MDTextField(mode="outlined", password=True, size_hint_y=None, height="46dp")
        self.reg_pass.add_widget(MDTextFieldHintText(text="Password"))
        card.add_widget(self.reg_pass)

        self.reg_confirm = MDTextField(mode="outlined", password=True, size_hint_y=None, height="46dp")
        self.reg_confirm.add_widget(MDTextFieldHintText(text="Confirm Password"))
        card.add_widget(self.reg_confirm)

        btn_register = MDButton(
            style="filled",
            size_hint_y=None,
            height="44dp",
            on_release=self.do_register
        )
        btn_register.add_widget(MDButtonText(text="Complete Registration & Open Dashboard 🚀"))
        card.add_widget(btn_register)

        self.form_box.add_widget(card)

    def do_register(self, *args):
        fullname = self.reg_fullname.text.strip() or "New Developer"
        email = self.reg_email.text.strip() or "new@bharatconnect.com"
        phone = self.reg_phone.text.strip() or "+91 98989 89898"
        username = self.reg_username.text.strip() or "new_dev"
        dob = self.reg_dob.text.strip() or "2000-01-01"
        password = self.reg_pass.text.strip()

        new_user = db_engine.register_user(fullname, email, phone, username, dob, password)
        self.status_msg = f"Account created for {new_user['display_name']}!"
        if hasattr(self.manager, 'dashboard_screen'):
            self.manager.dashboard_screen.reload_user_session()
        self.manager.current = "dashboard"

    # ---------------------------------------------------------
    # FORGOT PASSWORD FORM
    # ---------------------------------------------------------
    def build_forgot_form(self):
        card = GradientCard(
            color1=COLOR_6367FF,
            color2=COLOR_2F2FE4,
            color3=COLOR_162E93,
            orientation="horizontal",
            padding="16dp",
            spacing="12dp",
            size_hint_y=None,
            height="325dp",
            radius=[18, 18, 18, 18],
            elevation=0
        )
        card.add_widget(MDLabel(
            text="🔒 Reset Password via Email OTP",
            font_style="Title",
            role="medium",
            bold=True,
            theme_text_color="Custom",
            text_color=COLOR_TEXT_MAIN
        ))

        self.forgot_email = MDTextField(mode="outlined", size_hint_y=None, height="48dp")
        self.forgot_email.add_widget(MDTextFieldHintText(text="Enter Registered Email (vipin@bharatconnect.com)"))
        card.add_widget(self.forgot_email)

        self.forgot_otp = MDTextField(mode="outlined", size_hint_y=None, height="48dp", text="849201")
        self.forgot_otp.add_widget(MDTextFieldHintText(text="Email Verification Code (OTP)"))
        card.add_widget(self.forgot_otp)

        self.forgot_newpass = MDTextField(mode="outlined", password=True, size_hint_y=None, height="48dp")
        self.forgot_newpass.add_widget(MDTextFieldHintText(text="New Password"))
        card.add_widget(self.forgot_newpass)

        btn_reset = MDButton(
            style="filled",
            size_hint_y=None,
            height="44dp",
            on_release=self.do_reset_password
        )
        btn_reset.add_widget(MDButtonText(text="Verify Code & Reset Password"))
        card.add_widget(btn_reset)

        self.form_box.add_widget(card)

    def do_reset_password(self, *args):
        email = self.forgot_email.text.strip() or "vipin@bharatconnect.com"
        otp = self.forgot_otp.text.strip()
        new_pass = self.forgot_newpass.text.strip() or "newpassword123"

        success, msg = db_engine.reset_password_with_email(email, otp, new_pass)
        self.status_msg = msg
        if success:
            self.switch_mode("signin")
        else:
            self.build_ui()

    def go_back(self, *args):
        self.manager.current = "splash"
