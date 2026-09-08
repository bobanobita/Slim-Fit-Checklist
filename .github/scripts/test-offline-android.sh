#!/bin/sh
set -eux

dump_ui() {
  adb shell uiautomator dump /sdcard/english-logic-window.xml >/dev/null
  adb pull /sdcard/english-logic-window.xml /tmp/english-logic-window.xml >/dev/null
}

wait_for_ui_text() {
  expected="$1"
  for _ in $(seq 1 20); do
    if dump_ui && grep -q "$expected" /tmp/english-logic-window.xml; then
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for UI text: $expected"
  cat /tmp/english-logic-window.xml || true
  adb logcat -d -t 300 || true
  return 1
}

adb uninstall vn.englishlogic.app >/dev/null 2>&1 || true
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell cmd uimode night no
adb shell cmd connectivity airplane-mode enable
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global http_proxy 127.0.0.1:9
adb shell am force-stop vn.englishlogic.app
adb shell am start -W -n vn.englishlogic.app/.MainActivity
adb shell dumpsys activity activities > /tmp/activities.txt
grep -q "vn.englishlogic.app/.MainActivity" /tmp/activities.txt
wait_for_ui_text "English Logic"
grep -q "Bắt đầu Buổi 01" /tmp/english-logic-window.xml
grep -q "Bật giao diện tối" /tmp/english-logic-window.xml
if grep -Eiq "auth\.openai\.com|Tiếp tục với ChatGPT|Đăng nhập" /tmp/english-logic-window.xml; then exit 1; fi

# Pairing is optional and its controls remain reachable while fully offline.
dump_ui
coords="$(python3 -c 'import re,sys,xml.etree.ElementTree as ET; root=ET.parse(sys.argv[1]).getroot(); node=next((n for n in root.iter("node") if n.attrib.get("content-desc")==sys.argv[2]), None); m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib["bounds"] if node is not None else ""); print(f"{(int(m[1])+int(m[3]))//2} {(int(m[2])+int(m[4]))//2}" if m else "")' /tmp/english-logic-window.xml "Quản lý dữ liệu")"
test -n "$coords"
adb shell input tap $coords
wait_for_ui_text "Đồng bộ với máy tính"
grep -q "Quét QR trên PC" /tmp/english-logic-window.xml
grep -q "Không email, mật khẩu hay ChatGPT OAuth" /tmp/english-logic-window.xml
adb shell input keyevent 4

# A process restart must still open the bundled Home while fully offline.
adb shell am force-stop vn.englishlogic.app
adb shell am start -W -n vn.englishlogic.app/.MainActivity
wait_for_ui_text "English Logic"
grep -q "Bắt đầu Buổi 01" /tmp/english-logic-window.xml
grep -q "Bật giao diện tối" /tmp/english-logic-window.xml
if grep -Eiq "auth\.openai\.com|Tiếp tục với ChatGPT|Đăng nhập" /tmp/english-logic-window.xml; then exit 1; fi

# Repository v2, identity and device settings were committed locally.
adb shell am force-stop vn.englishlogic.app
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-local-progress-v2" app_webview'
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-local-identity-v1" app_webview'
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-device-settings-v1" app_webview'
