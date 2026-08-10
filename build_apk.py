import os
import sys
import shutil
import subprocess

def build_android_apk():
    print("=======================================================================")
    print("        BHARATCONNECT - Standalone Windows APK Builder                 ")
    print("=======================================================================")

    android_sdk = r'C:\Users\Vipin\AppData\Local\Android\Sdk'
    java_home = r'C:\Program Files\Java\jdk-19'
    gradle_bin = r'C:\Users\Vipin\OneDrive\Desktop\WebAplications\V-Billings\gradle-8.2\bin\gradle.bat'

    if os.path.exists(android_sdk):
        os.environ['ANDROID_HOME'] = android_sdk
        os.environ['ANDROID_SDK_ROOT'] = android_sdk
        print(f"[*] Using Android SDK: {android_sdk}")

    if os.path.exists(java_home):
        os.environ['JAVA_HOME'] = java_home
        print(f"[*] Using Java JDK: {java_home}")

    if not os.path.exists(gradle_bin):
        print("[!] Error: Gradle binary not found at specified path.")
        return False

    print("\n[*] Compiling Standalone Android APK with Gradle (assembleDebug)...")
    res = subprocess.run([gradle_bin, 'assembleDebug', '--no-daemon'], cwd='android_app')

    apk_src = os.path.join('android_app', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
    output_apk = 'BharatConnect.apk'

    if os.path.exists(apk_src):
        shutil.copy(apk_src, output_apk)
        size_mb = os.path.getsize(output_apk) / (1024 * 1024)
        print("\n=======================================================================")
        print(f"[+] SUCCESS! Final Standalone APK Created: {os.path.abspath(output_apk)}")
        print(f"[*] APK Binary Size: {size_mb:.2f} MB")
        print("[*] 100% Offline & Fully Self-Contained")
        print("=======================================================================")
        return True
    else:
        print("\n[!] Gradle compilation failed. Check build logs above.")
        return False

if __name__ == "__main__":
    success = build_android_apk()
    sys.exit(0 if success else 1)
