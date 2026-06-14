#!/usr/bin/env bash
# =============================================================================
# Xime 输入法 · 模拟器端到端自动检收脚本
# =============================================================================
# 用途：在无界面(headless) x86_64 模拟器上自动完成
#   构建 → 启动模拟器 → 安装 → 启用输入法 → 唤起键盘 → 打字 → 截图取证
# 设计给「AI / CI 自主验证」用：每步都有可读输出，验证结果靠 acceptance-shots/ 下的截图。
#
# 运行环境：Windows + git-bash（adb / emulator 来自 Android SDK）
# 用法：
#   bash scripts/acceptance.sh accept        # 一键全流程（最常用）
#   bash scripts/acceptance.sh boot          # 仅确保模拟器已启动
#   bash scripts/acceptance.sh build         # 仅构建 x86_64 debug 包
#   bash scripts/acceptance.sh install       # 安装到运行中的模拟器
#   bash scripts/acceptance.sh ime           # 启用并设为默认输入法
#   bash scripts/acceptance.sh kbd <名字>     # 唤起键盘并截图 acceptance-shots/<名字>.png
#   bash scripts/acceptance.sh type <字母串>  # 在默认布局上按坐标点击字母键(如 wgg)后截图
#   bash scripts/acceptance.sh shot <名字>    # 仅截当前屏
#   bash scripts/acceptance.sh stop          # 关闭模拟器
#
# 可用环境变量覆盖：
#   SDK   Android SDK 路径（默认读 local.properties 的 sdk.dir，失败则用 ANDROID_HOME）
#   AVD   模拟器名（默认 xime_x86_64）
# =============================================================================
set -uo pipefail
cd "$(dirname "$0")/.."                 # 切到仓库根目录

# ---- 配置（可被环境变量覆盖）-------------------------------------------------
AVD="${AVD:-xime_x86_64}"
IME="com.kingzcheung.xime/.service.XimeInputMethodService"
PKG="com.kingzcheung.xime"
SHOTS="acceptance-shots"
SYS_IMG="system-images;android-35;google_apis;x86_64"

# 解析 SDK 路径：优先 local.properties，其次 ANDROID_SDK_ROOT/ANDROID_HOME
detect_sdk() {
  if [ -n "${SDK:-}" ] && [ -d "$SDK" ]; then echo "$SDK"; return; fi
  local d
  d=$(grep -E '^sdk.dir=' local.properties 2>/dev/null | head -1 | cut -d= -f2-)
  if [ -n "$d" ]; then
    # local.properties 里是 Java 转义的 Windows 路径，如 C\:\\Users\\...
    # 先反转义(\: -> :, \\ -> /, 残留 \ -> /)，再把盘符 C: 转成 git-bash 的 /c
    echo "$d" | sed -E 's#\\:#:#g; s#\\\\#/#g; s#\\#/#g; s#^([A-Za-z]):#/\L\1#'
    return
  fi
  echo "${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
}
SDK="$(detect_sdk)"
EMU="$SDK/emulator/emulator.exe"
export ANDROID_SDK_ROOT ANDROID_HOME
ANDROID_SDK_ROOT="$(echo "$SDK" | sed -E 's#^/([a-z])/#\U\1:/#')"   # 还原成 Windows 风格给 emulator.exe
ANDROID_HOME="$ANDROID_SDK_ROOT"

mkdir -p "$SHOTS"
log() { echo -e "\033[36m[检收]\033[0m $*"; }
err() { echo -e "\033[31m[失败]\033[0m $*" >&2; }

# ---- 找 x86_64 debug APK ----------------------------------------------------
find_apk() {
  # injected.build.abi 把单 ABI 包放在 intermediates/ 下；常规 split 包在 outputs/ 下。取最新者。
  ls -t app/build/intermediates/apk/debug/*x86_64*.apk app/build/outputs/apk/debug/*x86_64*.apk 2>/dev/null | head -1
}

# ---- 子命令 -----------------------------------------------------------------
emu_running() { adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device'; }

cmd_boot() {
  if emu_running; then log "模拟器已在运行"; return 0; fi
  log "启动 headless 模拟器: $AVD"
  "$EMU" -avd "$AVD" -no-window -no-audio -no-boot-anim -no-snapshot \
         -gpu swiftshader_indirect -netdelay none -netspeed full -no-metrics \
         >/dev/null 2>&1 &
  adb start-server >/dev/null 2>&1
  adb wait-for-device
  log "等待 sys.boot_completed ..."
  for i in $(seq 1 90); do
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && { log "开机完成 (~$((i*4))s)"; break; }
    sleep 4
  done
  # 关动画，稳定截图
  adb shell settings put global window_animation_scale 0 >/dev/null 2>&1
  adb shell settings put global transition_animation_scale 0 >/dev/null 2>&1
  adb shell settings put global animator_duration_scale 0 >/dev/null 2>&1
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
  adb shell wm dismiss-keyguard >/dev/null 2>&1
}

cmd_build() {
  log "构建 x86_64 debug APK（仅 x86_64 ABI，跳过 sherpa 语音 native）"
  git submodule update --init app/src/main/jni/librime app/src/main/jni/librime-lua \
      app/src/main/jni/librime-lua-deps app/src/main/jni/snappy >/dev/null 2>&1
  ./gradlew :app:assembleDebug -Pandroid.injected.build.abi=x86_64 -x buildSherpaOnnx --console=plain
}

cmd_install() {
  local apk; apk="$(find_apk)"
  [ -z "$apk" ] && { err "找不到 x86_64 debug APK，请先 build"; return 1; }
  log "安装: $apk"
  adb install -r -t "$apk" | tail -2
}

cmd_ime() {
  log "启用并设为默认输入法"
  adb shell ime enable "$IME"
  adb shell ime set "$IME"
  echo "default_input_method = $(adb shell settings get secure default_input_method | tr -d '\r')"
}

# 唤起键盘：开系统设置 → 点搜索栏（真 EditText）→ 截图
cmd_kbd() {
  local name="${1:-keyboard}"
  adb shell am start -a android.settings.SETTINGS >/dev/null 2>&1; sleep 3
  adb shell input tap 540 660 >/dev/null 2>&1; sleep 3      # 设置首页搜索栏
  cmd_shot "$name"
}

cmd_shot() {
  local name="${1:-shot}"
  adb exec-out screencap -p > "$SHOTS/$name.png" 2>/dev/null
  log "截图 -> $SHOTS/$name.png"
}

# 按「默认 QWERTY 布局」的键位坐标点击字母（注意：改了键几何后坐标会变，需重新标定）
# 仅作默认布局基线验证用；布局编辑器改动后请以截图为准。
cmd_type() {
  local seq="${1:-}"; [ -z "$seq" ] && { err "用法: type wgg"; return 1; }
  declare -A KX=( [q]=57 [w]=165 [e]=270 [r]=378 [t]=486 [y]=592 [u]=700 [i]=808 [o]=912 [p]=1018 \
                  [a]=100 [s]=210 [d]=322 [f]=432 [g]=540 [h]=650 [j]=760 [k]=868 [l]=975 \
                  [z]=205 [x]=315 [c]=425 [v]=540 [b]=650 [n]=762 [m]=872 )
  declare -A KY=( [q]=1805 [w]=1805 [e]=1805 [r]=1805 [t]=1805 [y]=1805 [u]=1805 [i]=1805 [o]=1805 [p]=1805 \
                  [a]=1955 [s]=1955 [d]=1955 [f]=1955 [g]=1955 [h]=1955 [j]=1955 [k]=1955 [l]=1955 \
                  [z]=2100 [x]=2100 [c]=2100 [v]=2100 [b]=2100 [n]=2100 [m]=2100 )
  local i ch
  for (( i=0; i<${#seq}; i++ )); do
    ch="${seq:$i:1}"; ch="${ch,,}"
    [ -n "${KX[$ch]:-}" ] && { adb shell input tap "${KX[$ch]}" "${KY[$ch]}" >/dev/null 2>&1; sleep 0.5; }
  done
  sleep 0.8
  cmd_shot "type_${seq}"
}

cmd_stop() {
  log "关闭模拟器"
  adb emu kill >/dev/null 2>&1 || adb -s "$(adb devices | grep emulator | head -1 | cut -f1)" emu kill >/dev/null 2>&1
}

cmd_accept() {
  cmd_boot || return 1
  cmd_build || return 1
  cmd_install || return 1
  cmd_ime
  # 触发 RIME 数据初始化
  adb shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1; sleep 15
  cmd_shot "00_app"
  cmd_kbd "10_keyboard"
  cmd_type "wgg"
  log "全流程完成。证据见 $SHOTS/ ，请逐张核验键盘渲染与候选词/编码注释。"
}

# ---- 入口 -------------------------------------------------------------------
case "${1:-accept}" in
  boot) cmd_boot ;;
  build) cmd_build ;;
  install) cmd_install ;;
  ime) cmd_ime ;;
  kbd) cmd_kbd "${2:-keyboard}" ;;
  type) cmd_type "${2:-}" ;;
  shot) cmd_shot "${2:-shot}" ;;
  stop) cmd_stop ;;
  accept) cmd_accept ;;
  *) err "未知命令: $1"; grep -E '^#   bash' "$0" | sed 's/^# //'; exit 1 ;;
esac
