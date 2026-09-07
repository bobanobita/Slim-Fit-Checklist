#!/bin/sh
set -eu

cat > /tmp/tap-ui.py <<'PY'
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

query = sys.argv[1]
subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/window.xml"], check=True, stdout=subprocess.DEVNULL)
subprocess.run(["adb", "pull", "/sdcard/window.xml", "/tmp/window.xml"], check=True, stdout=subprocess.DEVNULL)
matches = []
for node in ET.parse("/tmp/window.xml").iter("node"):
    label = " ".join((node.attrib.get("text", ""), node.attrib.get("content-desc", "")))
    if query not in label or node.attrib.get("enabled") == "false":
        continue
    bounds = [int(value) for value in re.findall(r"\d+", node.attrib.get("bounds", ""))]
    if len(bounds) != 4:
        continue
    left, top, right, bottom = bounds
    center_x, center_y = (left + right) // 2, (top + bottom) // 2
    if right > left and bottom > top and 80 <= center_y <= 2050:
        matches.append((top, center_x, center_y, label))
if not matches:
    raise SystemExit(1)
_, x, y, _ = sorted(matches)[0]
subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
PY

dump_ui() {
  adb shell uiautomator dump /sdcard/english-logic-window.xml >/dev/null
  adb pull /sdcard/english-logic-window.xml /tmp/english-logic-window.xml >/dev/null
}

tap_visible() {
  python3 /tmp/tap-ui.py "$1" || return 1
  sleep 1
}

tap_after_scroll() {
  label="$1"
  for _ in $(seq 1 12); do
    if tap_visible "$label"; then return 0; fi
    adb shell input swipe 540 1750 540 650 350
    sleep 1
  done
  echo "Could not find enabled UI node: $label"
  return 1
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
if grep -Eiq "auth\.openai\.com|Tiếp tục với ChatGPT|Đăng nhập" /tmp/english-logic-window.xml; then exit 1; fi

# Dark mode is device-local and must survive a process restart.
tap_visible "Bật giao diện tối"
dump_ui
grep -q "Bật giao diện sáng" /tmp/english-logic-window.xml
adb shell am force-stop vn.englishlogic.app
adb shell am start -W -n vn.englishlogic.app/.MainActivity
sleep 4
dump_ui
grep -q "Bật giao diện sáng" /tmp/english-logic-window.xml

# Answer all eight foundation questions to complete Session 01.
tap_visible "Bắt đầu Buổi 01"
tap_after_scroll "Tôi chắc"
tap_after_scroll "She isn't available today."
tap_after_scroll "Tôi chắc"
tap_after_scroll "Do they work on Saturdays?"
tap_after_scroll "Tôi chắc"
tap_after_scroll "Does đã mang dấu hiệu ngôi thứ ba"
tap_after_scroll "Tôi chắc"
tap_after_scroll "Is Lan waiting outside?"
tap_after_scroll "Tôi chắc"
tap_after_scroll "Do you have a receipt?"
tap_after_scroll "Tôi chắc"
tap_after_scroll "Làm trợ động từ tạo Present Perfect"
tap_after_scroll "Tôi chắc"
tap_after_scroll "She isn't busy."
tap_after_scroll "Tôi chắc"
tap_after_scroll "He works → He doesn't work"

adb shell am force-stop vn.englishlogic.app
adb shell am start -W -n vn.englishlogic.app/.MainActivity
sleep 5
dump_ui
grep -q "1/60 buổi" /tmp/english-logic-window.xml
grep -q "Bật giao diện sáng" /tmp/english-logic-window.xml

# Repository v2, identity and device settings were committed locally.
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-local-progress-v2" app_webview'
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-local-identity-v1" app_webview'
adb exec-out run-as vn.englishlogic.app sh -c 'grep -R -a -q "english-logic-device-settings-v1" app_webview'
