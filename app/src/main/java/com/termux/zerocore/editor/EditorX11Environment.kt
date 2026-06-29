package com.termux.zerocore.editor

import com.termux.shared.termux.TermuxConstants
import java.io.File

/**
 * 编辑器内置 GUI：独立 Xvfb + 截图 + xdotool，不依赖 termux-x11 / AVNC。
 * 终端只需 export DISPLAY=:99 并运行程序。
 */
object EditorX11Environment {

    const val DISPLAY = ":99"
    const val DISPLAY_NUM = 99
    const val SCREEN_GEOMETRY = "800x600x24"
    const val SCRIPT_X11_MARKER = "zt-gui-v8"
    const val X11_READY_MARKER = "[ZeroTermux Editor] GUI ready on"
    const val GUI_DIR = "\${HOME}/.zerotermux/gui"
    const val FRAME_FILE = "$GUI_DIR/frame.jpg"
    const val INPUT_DIR = "$GUI_DIR/input.d"
    const val LOG_FILE = "\${HOME}/.zerotermux/editor-gui.log"
    const val PID_FILE = "\${HOME}/.zerotermux/editor-gui.pid"

    fun isPackagesInstalled(): Boolean {
        val binDir = TermuxConstants.TERMUX_BIN_PREFIX_DIR
        return File(binDir, "Xvfb").canExecute()
            && File(binDir, "xdotool").canExecute()
            && (File(binDir, "scrot").canExecute() || File(binDir, "import").canExecute())
    }

    fun coreX11ShellFunctions(): String {
        return buildString {
            append("editor_gui_frame_fresh() {\n")
            append("  [ -f \"${FRAME_FILE}\" ] && [ -s \"${FRAME_FILE}\" ] && return 0\n")
            append("  [ -f \"${GUI_DIR}/frame.ppm\" ] && [ -s \"${GUI_DIR}/frame.ppm\" ]\n")
            append("}\n")
            append("editor_gpu_env() {\n")
            append("  unset LIBGL_ALWAYS_SOFTWARE 2>/dev/null || true\n")
            append("}\n")
            append("stop_editor_gui_bridge() {\n")
            append("  if [ -f \"${PID_FILE}\" ]; then\n")
            append("    while read -r _pid; do\n")
            append("      [ -n \"\$_pid\" ] && kill \"\$_pid\" 2>/dev/null || true\n")
            append("    done < \"${PID_FILE}\"\n")
            append("    : > \"${PID_FILE}\"\n")
            append("  fi\n")
            append("}\n")
            append("ensure_editor_xvfb() {\n")
            append("  if pgrep -f \"Xvfb :${DISPLAY_NUM}\" >/dev/null 2>&1; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  pkill -f \"Xvfb :${DISPLAY_NUM}\" 2>/dev/null || true\n")
            append("  mkdir -p \"\${HOME}/.zerotermux\"\n")
            append("  Xvfb :${DISPLAY_NUM} -screen 0 ${SCREEN_GEOMETRY} -ac +extension GLX +render -noreset >>${LOG_FILE} 2>&1 &\n")
            append("  sleep 1\n")
            append("  pgrep -f \"Xvfb :${DISPLAY_NUM}\" >/dev/null 2>&1\n")
            append("}\n")
            append("editor_gui_capture_once() {\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("  mkdir -p \"${GUI_DIR}\"\n")
            append("  if command -v scrot >/dev/null 2>&1; then\n")
            append("    scrot -o \"${GUI_DIR}/frame.tmp.jpg\" 2>/dev/null \\\n")
            append("      && mv -f \"${GUI_DIR}/frame.tmp.jpg\" \"${FRAME_FILE}\"\n")
            append("    return\n")
            append("  fi\n")
            append("  if command -v import >/dev/null 2>&1; then\n")
            append("    import -depth 8 -window root -display ${DISPLAY} \\\n")
            append("      \"jpeg:${GUI_DIR}/frame.tmp.jpg\" 2>/dev/null \\\n")
            append("      && mv -f \"${GUI_DIR}/frame.tmp.jpg\" \"${FRAME_FILE}\"\n")
            append("  fi\n")
            append("}\n")
            append("ensure_editor_gui_bridge() {\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("  mkdir -p \"${GUI_DIR}\" \"${INPUT_DIR}\"\n")
            append("  stop_editor_gui_bridge\n")
            append("  (\n")
            append("    export DISPLAY=${DISPLAY}\n")
            append("    _input_dir=\"${INPUT_DIR}\"\n")
            append("    while true; do\n")
            append("      shopt -s nullglob 2>/dev/null || setopt nullglob 2>/dev/null || true\n")
            append("      for _cmd in \"\$_input_dir\"/*.cmd; do\n")
            append("        [ -f \"\$_cmd\" ] || continue\n")
            append("        _line=\$(head -n 1 \"\$_cmd\" 2>/dev/null)\n")
            append("        rm -f \"\$_cmd\"\n")
            append("        [ -n \"\$_line\" ] || continue\n")
            append("        case \"\$_line\" in\n")
            append("          click:*)\n")
            append("            _xy=\"\${_line#click:}\"; _x=\"\${_xy%%,*}\"; _y=\"\${_xy#*,}\"\n")
            append("            xdotool mousemove \"\$_x\" \"\$_y\" click 1 2>/dev/null || true\n")
            append("            ;;\n")
            append("        esac\n")
            append("      done\n")
            append("      sleep 0.01\n")
            append("    done\n")
            append("  ) >>${LOG_FILE} 2>&1 &\n")
            append("  echo \$! >> \"${PID_FILE}\"\n")
            append("  (\n")
            append("    export DISPLAY=${DISPLAY}\n")
            append("    while true; do\n")
            append("      editor_gui_capture_once\n")
            append("      sleep 0.08\n")
            append("    done\n")
            append("  ) >>${LOG_FILE} 2>&1 &\n")
            append("  echo \$! >> \"${PID_FILE}\"\n")
            append("  sleep 0.2\n")
            append("  editor_gui_capture_once\n")
            append("}\n")
            append("start_editor_gui() {\n")
            append("  ensure_editor_xvfb || return 1\n")
            append("  ensure_editor_gui_bridge\n")
            append("}\n")
            append("ensure_editor_gui_stack() {\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("  editor_gpu_env\n")
            append("  start_editor_gui\n")
            append("}\n")
            append("refresh_editor_gui_display() {\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("  ensure_editor_gui_stack || return 1\n")
            append("  command -v xrefresh >/dev/null 2>&1 && xrefresh -display \"${DISPLAY}\" || true\n")
            append("}\n")
            append("wait_for_editor_gui() {\n")
            append("  for _ in \$(seq 1 80); do\n")
            append("    if editor_gui_frame_fresh; then\n")
            append("      return 0\n")
            append("    fi\n")
            append("    if [ \$(( _ % 4 )) -eq 0 ]; then\n")
            append("      editor_gui_capture_once 2>/dev/null || true\n")
            append("    fi\n")
            append("    sleep 0.25\n")
            append("  done\n")
            append("  return 1\n")
            append("}\n")
        }
    }

    fun ensureAndStartScript(
        installRepoEcho: String,
        installPackagesEcho: String
    ): String {
        return buildString {
            append("mkdir -p \"\${HOME}/.zerotermux\"\n")
            append("ensure_editor_gui_pkg() {\n")
            append("  if command -v Xvfb >/dev/null 2>&1 && command -v xdotool >/dev/null 2>&1; then\n")
            append("    if command -v scrot >/dev/null 2>&1 || command -v import >/dev/null 2>&1; then\n")
            append("      return 0\n")
            append("    fi\n")
            append("  fi\n")
            append("  echo ")
            append(shellQuote(installRepoEcho))
            append("\n")
            append("  pkg install -y x11-repo\n")
            append("  echo ")
            append(shellQuote(installPackagesEcho))
            append("\n")
            append("  pkg install -y xorg-server-xvfb scrot xdotool imagemagick\n")
            append("  pkg install -y ttf-dejavu 2>/dev/null || true\n")
            append("  command -v Xvfb >/dev/null 2>&1 && command -v xdotool >/dev/null 2>&1 \\\n")
            append("    && { command -v scrot >/dev/null 2>&1 || command -v import >/dev/null 2>&1; }\n")
            append("}\n")
            append(coreX11ShellFunctions())
            append(bootstrapTail())
        }
    }

    fun startServerOnlyScript(): String {
        return buildString {
            append("mkdir -p \"\${HOME}/.zerotermux\"\n")
            append(coreX11ShellFunctions())
            append(startServerTail())
        }
    }

    fun exportDisplayScript(): String = "export DISPLAY=${DISPLAY}\n"

    private fun bootstrapTail(): String {
        return buildString {
            append("if ensure_editor_gui_pkg && start_editor_gui && wait_for_editor_gui; then\n")
            append("  echo '$X11_READY_MARKER ${DISPLAY}'\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("else\n")
            append("  echo '[ZeroTermux Editor] GUI bootstrap failed (tap GUI Retry)'\n")
            append("  tail -n 8 ${LOG_FILE} 2>/dev/null || true\n")
            append("fi\n")
        }
    }

    private fun startServerTail(): String {
        return buildString {
            append("if start_editor_gui && wait_for_editor_gui; then\n")
            append("  echo '$X11_READY_MARKER ${DISPLAY}'\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("else\n")
            append("  echo '[ZeroTermux Editor] GUI start failed (tap GUI Retry)'\n")
            append("  tail -n 8 ${LOG_FILE} 2>/dev/null || true\n")
            append("fi\n")
        }
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
