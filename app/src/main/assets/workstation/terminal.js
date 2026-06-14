(function () {
  let term = null;
  let fitAddon = null;
  let ws = null;
  let reconnectTimer = null;
  let lastSentCols = 0;
  let lastSentRows = 0;
  let resizeTimer = null;
  let wsConnected = false;

  function wsUrl() {
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${proto}//${location.host}/ws/terminal`;
  }

  function sendResizeNow(cols, rows) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    cols = cols || term.cols;
    rows = rows || term.rows;
    if (cols === lastSentCols && rows === lastSentRows) return;
    lastSentCols = cols;
    lastSentRows = rows;
    const msg = new Uint8Array(5);
    msg[0] = 1;
    msg[1] = (cols >> 8) & 0xff;
    msg[2] = cols & 0xff;
    msg[3] = (rows >> 8) & 0xff;
    msg[4] = rows & 0xff;
    ws.send(msg);
  }

  function scheduleResize() {
    if (!term || !fitAddon) return;
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      fitAddon.fit();
      sendResizeNow();
    }, 200);
  }

  function sendInput(data) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    const bytes = new TextEncoder().encode(data);
    const msg = new Uint8Array(bytes.length + 1);
    msg[0] = 0;
    msg.set(bytes, 1);
    ws.send(msg);
  }

  function connectWebSocket() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return;
    }
    ws = new WebSocket(wsUrl());
    ws.binaryType = 'arraybuffer';
    ws.onopen = () => {
      wsConnected = true;
      scheduleResize();
      term?.focus();
    };
    ws.onmessage = (ev) => {
      if (typeof ev.data === 'string') {
        term.write(ev.data);
      } else {
        term.write(new Uint8Array(ev.data));
      }
    };
    ws.onclose = () => {
      ws = null;
      wsConnected = false;
      if (window.ztTerminalActive) {
        reconnectTimer = setTimeout(connectWebSocket, 3000);
      }
    };
    ws.onerror = () => {
      try { ws.close(); } catch (e) {}
    };
  }

  function disconnectWebSocket() {
    window.ztTerminalActive = false;
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (ws) {
      ws.close();
      ws = null;
    }
    wsConnected = false;
  }

  function initTerminal() {
    const container = document.getElementById('terminal-container');
    if (!container || term) return;

    term = new Terminal({
      cursorBlink: true,
      fontSize: 14,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      theme: {
        background: '#1a1a1a',
        foreground: '#f0f0f0',
        cursor: '#f0f0f0'
      },
      allowProposedApi: true,
      scrollback: 5000
    });
    const FitAddonClass = (window.FitAddon && window.FitAddon.FitAddon) || window.FitAddon;
    fitAddon = new FitAddonClass();
    term.loadAddon(fitAddon);
    term.open(container);
    fitAddon.fit();

    term.onData(sendInput);
    term.onResize(({ cols, rows }) => sendResizeNow(cols, rows));

    window.addEventListener('resize', () => {
      if (window.ztTerminalActive) {
        scheduleResize();
      }
    });
  }

  window.ztStartTerminal = function () {
    window.ztTerminalActive = true;
    initTerminal();
    if (!wsConnected) {
      connectWebSocket();
    } else if (fitAddon) {
      scheduleResize();
    }
    term?.focus();
  };

  window.ztStopTerminal = function () {
    disconnectWebSocket();
  };
})();
