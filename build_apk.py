import os
import sys
import shutil
import subprocess

def build_android_apk():
    print("=======================================================================")
    print("        BHARATCONNECT - Standalone Windows APK Builder                 ")
    print("=======================================================================")

    # 1. Pre-flight Asset & File Integrity Verification
    required_assets = [
        os.path.join('android_app', 'app', 'src', 'main', 'assets', 'www', 'index.html'),
        os.path.join('android_app', 'app', 'src', 'main', 'assets', 'www', 'config.js'),
        os.path.join('android_app', 'app', 'src', 'main', 'assets', 'www', 'database.js'),
        os.path.join('android_app', 'app', 'src', 'main', 'assets', 'www', 'app.js'),
        os.path.join('android_app', 'app', 'src', 'main', 'assets', 'www', 'style.css')
    ]
    
    missing_assets = [asset for asset in required_assets if not os.path.exists(asset)]
    if missing_assets:
        print("[!] ERROR: Essential web assets are missing from android_app assets:")
        for ma in missing_assets:
            print(f"    - {ma}")
        return False
    print("[*] Web assets verification passed.")

    # 2. Environment Auto-Detection
    android_sdk_candidates = [
        os.environ.get('ANDROID_HOME', ''),
        r'C:\Users\Vipin\AppData\Local\Android\Sdk',
        r'C:\Android\Sdk'
    ]
    android_sdk = next((p for p in android_sdk_candidates if p and os.path.exists(p)), None)

    java_candidates = [
        r'C:\Program Files\Java\jdk-19',
        r'C:\Program Files\Android\Android Studio\jbr',
        r'C:\Program Files\Java\jdk-17',
        r'C:\Program Files\Java\jdk-11',
        os.environ.get('JAVA_HOME', '')
    ]
    java_home = next((p for p in java_candidates if p and os.path.exists(p)), None)

    # Common Gradle candidate paths
    gradle_candidates = [
        r'C:\Users\Vipin\OneDrive\Desktop\WebAplications\V-Billings\gradle-8.2\bin\gradle.bat',
        r'C:\Gradle\gradle-8.2\bin\gradle.bat',
        r'C:\Program Files\Gradle\gradle-8.2\bin\gradle.bat'
    ]
    gradle_bin = next((g for g in gradle_candidates if os.path.exists(g)), gradle_candidates[0])

    if android_sdk:
        os.environ['ANDROID_HOME'] = android_sdk
        os.environ['ANDROID_SDK_ROOT'] = android_sdk
        print(f"[*] Using Android SDK: {android_sdk}")
    else:
        print(f"[!] Warning: Android SDK directory not found in candidates.")

    if java_home:
        os.environ['JAVA_HOME'] = java_home
        print(f"[*] Using Java JDK: {java_home}")
    else:
        print(f"[!] Error: No valid Java JDK installation found.")
        return False

    if not os.path.exists(gradle_bin):
        print(f"[!] Error: Gradle binary not found at path: {gradle_bin}")
        print("[!] Please specify a valid Gradle installation path.")
        return False

    print(f"[*] Using Gradle binary: {gradle_bin}")

    # 3. Clean parameter handling
    if "--clean" in sys.argv:
        print("[*] Cleaning previous Android build artifacts...")
        subprocess.run([gradle_bin, 'clean'], cwd='android_app')

    print("\n[*] Compiling Standalone Android APK with Gradle (assembleDebug)...")
    res = subprocess.run([gradle_bin, 'assembleDebug', '--no-daemon'], cwd='android_app')

    if res.returncode != 0:
        print(f"\n[!] Gradle exit code failure: {res.returncode}. Check compilation log above.")
        return False

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
        print("\n[!] Gradle compilation completed but app-debug.apk was not found.")
        return False

if __name__ == "__main__":
    success = build_android_apk()
    sys.exit(0 if success else 1)

