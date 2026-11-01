# BharatConnect End-to-End Testing Suite with Maestro

This directory contains automated End-to-End (E2E) UI test flows for BharatConnect using [Maestro](https://maestro.mobile.dev/).

## Test Scenarios Included:
1. `chat_and_upi_flow.yaml`:
   - Validates cold start and main tab navigation.
   - Tests opening a contact conversation.
   - Tests Desi Quick Reply assist chip interaction with 400ms debounce safety.
   - Tests WhatsApp-style message bubble alignment and tick display.
   - Initiates in-chat P2P UPI payment drawer with NPCI deep linking.
   - Opens the Bharat Seva citizen utilities sheet (DigiLocker, Aadhaar, electricity services).

## How to Execute:
1. Install Maestro CLI:
   ```bash
   curl -Ls "https://get.maestro.mobile.dev" | bash
   ```
2. Start an Android emulator or connect a physical device via ADB.
3. Install the APK:
   ```bash
   adb install BharatConnect-Native.apk
   ```
4. Run the automated flow:
   ```bash
   maestro test .maestro/chat_and_upi_flow.yaml
   ```
