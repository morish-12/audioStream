# Audio Stream (Audio Relay-style)
Streams your PC's audio to your Android phone over Wi-Fi.

## Get the APK (no Android Studio)
1. Create a GitHub repo, upload all these files (keep the .github folder), commit.
2. Open the Actions tab -> "Build APK" -> download artifact AudioStream-apk (unzip -> app-debug.apk).
3. Install on the phone (allow "install unknown apps").
(Or open the folder in Android Studio -> Build -> Build APK.)

## Use
PC: pip install soundcard numpy ; python server/server.py
Phone + PC on same Wi-Fi. Enter the IP/port printed by the server, tap Connect & Play.
Allow port 5555 in the PC firewall if it can't connect.
