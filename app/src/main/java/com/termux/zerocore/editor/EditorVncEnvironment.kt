package com.termux.zerocore.editor

object EditorVncEnvironment {

    const val DISPLAY = ":99"
    const val DISPLAY_NUM = 99
    const val VNC_HOST = "127.0.0.1"
    const val VNC_PORT = 15901
    const val SCREEN_GEOMETRY = "800x600x24"
    const val PID_FILE = "\${HOME}/.zerotermux/editor-vnc.pid"
    const val LOG_FILE = "\${HOME}/.zerotermux/x11vnc.log"
    const val SCRIPT_VNC_MARKER = "vnc-gui-v5"

    /** Shared bash helpers for boot script and build.sh */
    fun coreVncShellFunctions(): String {
        return buildString {
            append("editor_vnc_port_listening() {\n")
            append("  if (echo >/dev/tcp/127.0.0.1/${VNC_PORT}) 2>/dev/null; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  if command -v nc >/dev/null 2>&1 && nc -z 127.0.0.1 ${VNC_PORT} 2>/dev/null; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  if command -v ss >/dev/null 2>&1 && ss -tln 2>/dev/null | grep -qE ':${VNC_PORT}([^0-9]|$)'; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  if command -v netstat >/dev/null 2>&1 && netstat -tln 2>/dev/null | grep -qE ':${VNC_PORT}([^0-9]|$)'; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  return 1\n")
            append("}\n")
            append("ensure_editor_xvfb() {\n")
            append("  if pgrep -f \"Xvfb :${DISPLAY_NUM}\" >/dev/null 2>&1; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  pkill -f \"Xvfb :${DISPLAY_NUM}\" 2>/dev/null || true\n")
            append("  mkdir -p \"\${HOME}/.zerotermux\"\n")
            append("  : > ${PID_FILE}\n")
            append("  Xvfb :${DISPLAY_NUM} -screen 0 ${SCREEN_GEOMETRY} -ac +extension GLX +render -noreset >/dev/null 2>&1 &\n")
            append("  echo \$! > ${PID_FILE}\n")
            append("  sleep 1\n")
            append("  pgrep -f \"Xvfb :${DISPLAY_NUM}\" >/dev/null 2>&1\n")
            append("}\n")
            append("ensure_editor_x11vnc() {\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("  mkdir -p \"\${HOME}/.zerotermux\"\n")
            append("  if editor_vnc_port_listening; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  if pgrep -f \"x11vnc.*${VNC_PORT}\" >/dev/null 2>&1; then\n")
            append("    for _ in \$(seq 1 16); do\n")
            append("      if editor_vnc_port_listening; then\n")
            append("        return 0\n")
            append("      fi\n")
            append("      sleep 0.25\n")
            append("    done\n")
            append("  fi\n")
            append("  pkill -f \"x11vnc.*${VNC_PORT}\" 2>/dev/null || true\n")
            append("  pkill -f \"x11vnc.*-rfbport ${VNC_PORT}\" 2>/dev/null || true\n")
            append("  sleep 0.3\n")
            append("  x11vnc -display ${DISPLAY} -forever -shared -rfbport ${VNC_PORT} -localhost -nopw -noshm -noxinerama -noxdamage >>${LOG_FILE} 2>&1 &\n")
            append("  echo \$! >> ${PID_FILE}\n")
            append("  for _ in \$(seq 1 24); do\n")
            append("    if editor_vnc_port_listening; then\n")
            append("      return 0\n")
            append("    fi\n")
            append("    sleep 0.25\n")
            append("  done\n")
            append("  echo '[ZeroTermux Editor] x11vnc failed to listen on ${VNC_PORT}'\n")
            append("  tail -n 8 ${LOG_FILE} 2>/dev/null || true\n")
            append("  return 1\n")
            append("}\n")
            append("start_editor_vnc() {\n")
            append("  ensure_editor_xvfb || return 1\n")
            append("  ensure_editor_x11vnc\n")
            append("}\n")
            append("refresh_editor_gui_display() {\n")
            append("  export DISPLAY=${DISPLAY}\n")
            append("  ensure_editor_x11vnc || return 1\n")
            append("  command -v xrefresh >/dev/null 2>&1 && xrefresh -display \"${DISPLAY}\" || true\n")
            append("}\n")
            append("wait_for_editor_vnc() {\n")
            append("  for _ in \$(seq 1 40); do\n")
            append("    if editor_vnc_port_listening; then\n")
            append("      return 0\n")
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
            append("ensure_editor_vnc() {\n")
            append("  if command -v Xvfb >/dev/null 2>&1 && command -v x11vnc >/dev/null 2>&1; then\n")
            append("    return 0\n")
            append("  fi\n")
            append("  echo ")
            append(shellQuote(installRepoEcho))
            append("\n")
            append("  pkg install -y x11-repo\n")
            append("  echo ")
            append(shellQuote(installPackagesEcho))
            append("\n")
            append("  pkg install -y xorg-server-xvfb x11vnc xorg-fonts-dejavu\n")
            append("  command -v Xvfb >/dev/null 2>&1 && command -v x11vnc >/dev/null 2>&1\n")
            append("}\n")
            append(coreVncShellFunctions())
            append("ensure_editor_vnc || exit 1\n")
            append("start_editor_vnc || exit 1\n")
            append("wait_for_editor_vnc || { echo '[ZeroTermux Editor] VNC server failed to start'; exit 1; }\n")
            append("echo '[ZeroTermux Editor] VNC ready on ${DISPLAY} port ${VNC_PORT}'\n")
            append("export DISPLAY=${DISPLAY}\n")
        }
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
