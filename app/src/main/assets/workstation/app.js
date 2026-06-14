const CAMERA_TARGET_FPS = 20;
const CAMERA_FRAME_MS = 1000 / CAMERA_TARGET_FPS;

const state = {
  smsAddress: '',
  cameraFacing: 'front',
  cameraRunning: false,
  cameraObjectUrl: null,
  timers: {},
  wsPermissions: {
    master: true,
    terminal: true,
    camera: true,
    files: true,
    phoneSms: true
  }
};

const NAV_LABELS = {
  terminal: '终端',
  files: '文件',
  camera: '摄像头',
  phone: '电话',
  sms: '短信'
};

const PHONE_REQUIRED_SUFFIX = '(需要在手机端打开开关)';
const BLOCKED_MSG = '该功能未在手机端打开开关，请在 ZeroTermux 设置 → ZT电脑工作站 中开启对应权限后再使用。';

async function api(path, options = {}) {
  const res = await fetch(path, options);
  if (!res.ok && res.status !== 204) throw new Error(await res.text());
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text();
}

async function loadDeviceStatus() {
  try {
    const data = await api('/api/device/info');
    const batteryEl = document.getElementById('zt-status-battery');
    const androidEl = document.getElementById('zt-status-android');
    const ztEl = document.getElementById('zt-status-zt');
    const battery = data.battery;
    if (batteryEl) {
      batteryEl.textContent = battery >= 0
        ? `当前电量: ${battery}%`
        : '当前电量: 未知';
    }
    if (androidEl && data.androidVersion) {
      androidEl.textContent = `Android 版本(当前版本: ${data.androidVersion})`;
    }
    if (ztEl && data.ztVersion) {
      ztEl.textContent = `ZT版本(当前版本: ${data.ztVersion})`;
    }
  } catch (_) {}
}

loadDeviceStatus();
setInterval(loadDeviceStatus, 60000);

loadDeviceStatus();
setInterval(loadDeviceStatus, 60000);

async function loadWorkstationPermissions() {
  try {
    const data = await api('/api/workstation/permissions');
    state.wsPermissions = {
      master: !!data.master,
      terminal: !!data.terminal,
      camera: !!data.camera,
      files: !!data.files,
      phoneSms: !!data.phoneSms
    };
    updateNavPermissionLabels();
    refreshActivePanelAccess();
  } catch (_) {}
}

function isPanelAllowed(name) {
  const btn = document.querySelector(`.nav-btn[data-panel="${name}"]`);
  const perm = btn?.dataset?.perm;
  if (!perm) return true;
  return !!state.wsPermissions[perm];
}

function updateNavPermissionLabels() {
  document.querySelectorAll('.nav-btn[data-panel]').forEach(btn => {
    const panel = btn.dataset.panel;
    const perm = btn.dataset.perm;
    const base = NAV_LABELS[panel] || btn.textContent.replace(PHONE_REQUIRED_SUFFIX, '');
    if (perm && !state.wsPermissions[perm]) {
      btn.textContent = base + PHONE_REQUIRED_SUFFIX;
      btn.classList.add('nav-needs-phone');
    } else {
      btn.textContent = base;
      btn.classList.remove('nav-needs-phone');
    }
  });
}

function showBlockedPanel(name) {
  document.querySelectorAll('.panel').forEach(panel => panel.classList.remove('active'));
  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.panel === name);
  });
  const blocked = document.getElementById('panel-blocked');
  const text = document.getElementById('panel-blocked-text');
  if (text) text.textContent = BLOCKED_MSG;
  if (blocked) blocked.classList.add('active');
}

function refreshActivePanelAccess() {
  const activeBtn = document.querySelector('.nav-btn.active[data-perm]');
  if (!activeBtn) return;
  const panel = activeBtn.dataset.panel;
  if (!isPanelAllowed(panel)) showBlockedPanel(panel);
}

function switchPanel(name) {
  if (name !== 'settings' && !isPanelAllowed(name)) {
    showBlockedPanel(name);
    return;
  }
  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.panel === name);
  });
  document.querySelectorAll('.panel').forEach(panel => {
    panel.classList.toggle('active', panel.id === `panel-${name}`);
  });
  document.getElementById('panel-blocked')?.classList.remove('active');
  if (name === 'terminal') {
    window.ztStartTerminal?.();
  }
  if (name === 'files') window.ztFiles?.load('');
  if (name !== 'camera') {
    stopCameraPoll(false);
  }
  if (name === 'phone') loadContacts();
  if (name === 'sms') loadSmsThreads();
  if (name === 'settings') loadSettings();
}

document.querySelectorAll('.nav-btn').forEach(btn => {
  btn.addEventListener('click', () => switchPanel(btn.dataset.panel));
});

loadWorkstationPermissions();
setInterval(loadWorkstationPermissions, 15000);

window.ztFiles?.init?.();

function setCameraStatus(text) {
  const el = document.getElementById('camera-status');
  if (el) el.textContent = text;
}

function updateCameraButtons() {
  const openBtn = document.getElementById('camera-open');
  const closeBtn = document.getElementById('camera-close');
  const switchBtn = document.getElementById('camera-switch');
  if (openBtn) openBtn.disabled = state.cameraRunning;
  if (closeBtn) closeBtn.disabled = !state.cameraRunning;
  if (switchBtn) switchBtn.disabled = !state.cameraRunning;
}

function updateCameraFacingLabel() {
  const label = document.getElementById('camera-facing-label');
  if (label) {
    label.textContent = state.cameraFacing === 'front' ? '前置摄像头' : '后置摄像头';
  }
}

async function openCameraPreview() {
  if (state.cameraRunning) return;
  state.cameraFacing = 'front';
  updateCameraFacingLabel();
  setCameraStatus('正在启动前置摄像头...');
  try {
    await api('/api/camera/start', { method: 'POST' });
    state.cameraRunning = true;
    updateCameraButtons();
    setCameraStatus('摄像头运行中');
    startCameraPoll();
  } catch (e) {
    state.cameraRunning = false;
    updateCameraButtons();
    setCameraStatus('摄像头启动失败，请检查权限');
  }
}

function startCameraPoll() {
  if (!state.cameraRunning) return;
  clearTimeout(state.timers.camera);
  const loop = async () => {
    if (!state.cameraRunning) return;
    const started = performance.now();
    await loadCameraFrame(state.cameraFacing);
    const elapsed = performance.now() - started;
    const delay = Math.max(0, CAMERA_FRAME_MS - elapsed);
    state.timers.camera = setTimeout(loop, delay);
  };
  loop();
}

async function loadCameraFrame(facing) {
  const img = document.getElementById('camera-preview');
  if (!img) return;
  try {
    const res = await fetch(`/api/camera/frame?facing=${facing}&t=${Date.now()}`);
    if (!res.ok || res.status === 204) return;
    const blob = await res.blob();
    if (!blob.size) return;
    const url = URL.createObjectURL(blob);
    if (state.cameraObjectUrl) URL.revokeObjectURL(state.cameraObjectUrl);
    state.cameraObjectUrl = url;
    img.src = url;
  } catch (_) {}
}

function clearCameraPreviewImages() {
  if (state.cameraObjectUrl) {
    URL.revokeObjectURL(state.cameraObjectUrl);
    state.cameraObjectUrl = null;
  }
  const img = document.getElementById('camera-preview');
  if (img) img.removeAttribute('src');
}

function switchCameraFacing() {
  if (!state.cameraRunning) return;
  state.cameraFacing = state.cameraFacing === 'front' ? 'back' : 'front';
  updateCameraFacingLabel();
  setCameraStatus(`正在切换到${state.cameraFacing === 'front' ? '前置' : '后置'}摄像头...`);
  loadCameraFrame(state.cameraFacing).then(() => {
    if (state.cameraRunning) setCameraStatus('摄像头运行中');
  });
}

function stopCameraPoll(releaseCamera = true) {
  clearTimeout(state.timers.camera);
  delete state.timers.camera;
  if (!state.cameraRunning) return;
  state.cameraRunning = false;
  updateCameraButtons();
  if (releaseCamera) {
    fetch('/api/camera/release', { method: 'POST' }).catch(() => {});
    setCameraStatus('请点击「打开摄像头」开始预览');
    clearCameraPreviewImages();
  }
}

document.getElementById('camera-open').addEventListener('click', openCameraPreview);
document.getElementById('camera-close').addEventListener('click', () => stopCameraPoll(true));
document.getElementById('camera-switch').addEventListener('click', switchCameraFacing);

async function loadContacts() {
  const data = await api('/api/contacts');
  const list = document.getElementById('contacts-list');
  list.innerHTML = '';
  (data.contacts || []).forEach(c => {
    const el = document.createElement('div');
    el.className = 'list-item card';
    el.innerHTML = `<strong>${escapeHtml(c.name || '未知')}</strong><br>${escapeHtml(c.phone)}`;
    list.appendChild(el);
  });
}

async function loadSmsThreads() {
  const data = await api('/api/sms/threads');
  const list = document.getElementById('sms-threads');
  list.innerHTML = '';
  (data.threads || []).forEach(t => {
    const el = document.createElement('div');
    el.className = 'list-item card';
    el.innerHTML = `<strong>${escapeHtml(t.address)}</strong><br>${escapeHtml(t.body)}`;
    el.addEventListener('click', () => openSmsDialog(t.address));
    list.appendChild(el);
  });
}

async function openSmsDialog(address) {
  state.smsAddress = address;
  document.getElementById('sms-dialog-title').textContent = address;
  document.getElementById('sms-dialog').classList.remove('hidden');
  await refreshSmsMessages();
}

async function refreshSmsMessages() {
  const data = await api(`/api/sms/messages?address=${encodeURIComponent(state.smsAddress)}`);
  const box = document.getElementById('sms-messages');
  box.innerHTML = '';
  (data.messages || []).forEach(m => {
    const el = document.createElement('div');
    const isOut = m.type === 2;
    el.className = `sms-msg ${isOut ? 'out' : 'in'}`;
    el.textContent = m.body;
    box.appendChild(el);
  });
  box.scrollTop = box.scrollHeight;
}

document.getElementById('sms-dialog-close').addEventListener('click', () => {
  document.getElementById('sms-dialog').classList.add('hidden');
});
document.getElementById('sms-send-btn').addEventListener('click', async () => {
  const body = document.getElementById('sms-compose-input').value.trim();
  if (!body) return;
  await api('/api/sms/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ address: state.smsAddress, body })
  });
  document.getElementById('sms-compose-input').value = '';
  await refreshSmsMessages();
});

async function loadSettings() {
  const pages = await api('/api/settings/pages');
  const settings = await api('/api/settings/zt');
  const pagesEl = document.getElementById('settings-pages');
  pagesEl.innerHTML = '';
  (pages.pages || []).forEach(p => {
    const el = document.createElement('div');
    el.className = 'list-item card';
    el.innerHTML = `<strong>${escapeHtml(p.title)}</strong><br><small>${escapeHtml(p.description)}</small>`;
    pagesEl.appendChild(el);
  });
  const listEl = document.getElementById('settings-list');
  listEl.innerHTML = '';
  (settings.settings || []).forEach(s => {
    if (s.type !== 'boolean') return;
    const row = document.createElement('div');
    row.className = 'setting-row card';
    const label = document.createElement('span');
    label.textContent = `${s.group} · ${s.title}`;
    const sw = document.createElement('input');
    sw.type = 'checkbox';
    sw.checked = !!s.value;
    sw.addEventListener('change', async () => {
      await api('/api/settings/zt', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key: s.key, value: String(sw.checked) })
      });
    });
    row.appendChild(label);
    row.appendChild(sw);
    listEl.appendChild(row);
  });
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

loadWorkstationPermissions().then(() => switchPanel('terminal'));
