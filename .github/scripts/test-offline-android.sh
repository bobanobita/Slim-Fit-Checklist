#!/bin/sh
set -eux

dump_ui() {
  adb shell uiautomator dump /sdcard/english-logic-window.xml >/dev/null
  adb pull /sdcard/english-logic-window.xml /tmp/english-logic-window.xml >/dev/null
}

adb uninstall vn.englishlogic.app >/dev/null 2>&1 || true
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell cmd uimode night no
adb shell svc wifi disable
adb shell svc data disable
adb shell am force-stop vn.englishlogic.app
adb shell am start -W -n vn.englishlogic.app/.MainActivity
sleep 10
adb shell dumpsys activity activities > /tmp/activities.txt
grep -q "vn.englishlogic.app/.MainActivity" /tmp/activities.txt
dump_ui
grep -q "English Logic" /tmp/english-logic-window.xml
grep -q "Bắt đầu Buổi 01" /tmp/english-logic-window.xml
grep -q "Bật giao diện tối" /tmp/english-logic-window.xml
if grep -Eiq "auth\.openai\.com|Tiếp tục với ChatGPT|Đăng nhập" /tmp/english-logic-window.xml; then exit 1; fi

# A process restart must still open the bundled Home while fully offline.
adb shell am force-stop vn.englishlogic.app
adb shell am start -W -n vn.englishlogic.app/.MainActivity
sleep 4
dump_ui
grep -q "English Logic" /tmp/english-logic-window.xml
grep -q "Bắt đầu Buổi 01" /tmp/english-logic-window.xml
grep -q "Bật giao diện tối" /tmp/english-logic-window.xml
if grep -Eiq "auth\.openai\.com|Tiếp tục với ChatGPT|Đăng nhập" /tmp/english-logic-window.xml; then exit 1; fi

# Repository v2, identity and device settings were committed locally.
adb shell am force-stop vn.englishlogic.app
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-local-progress-v2" app_webview'
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-local-identity-v1" app_webview'
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-device-settings-v1" app_webview'
