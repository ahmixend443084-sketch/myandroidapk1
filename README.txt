YALLA QUICK SEND - prototype Android Studio source project

This is source code, not a ready-to-install APK.

Build:
1. Open this folder in Android Studio and sync Gradle.
2. Build > Build Bundle(s) / APK(s) > Build APK(s).
3. Install the debug APK.
4. Open app > Enable keyboard, choose keyboard, grant overlay permission, and enable Accessibility service yourself.

Prototype features:
- Custom keyboard with manual SEND key.
- MOD overlay panel with number/alphabet mode and a visible SEND control.
- Accessibility service attempts to click an on-screen accessible control whose label is exactly "Send".
- Number/alphabet auto sequence prototype and delay setting.

Important limitations:
- Yalla may not expose its Send button as an accessible node, so the SEND action may fail. It must be tested on the actual Yalla screen.
- Overlay quick keys currently copy characters to clipboard; they do not type directly into Yalla. Use the keyboard for direct text entry.
- The auto sequence overlay is a demonstration prototype, not a verified Yalla automation.
- Accessibility permission gives the service ability to inspect screen content while enabled. Enable only if you understand and accept this.
- No guarantee of compatibility with Yalla or compliance with its game rules.
- Keyboard text is not transmitted to any server.
