(function () {
  const fbState = {
    currentPath: '',
    parentPath: '',
    rootPath: '',
    items: [],
    selected: new Set(),
    lastSelectedPath: null,
    view: localStorage.getItem('zt-fb-view') || 'list',
    sortBy: localStorage.getItem('zt-fb-sort') || 'name',
    sortAsc: localStorage.getItem('zt-fb-sort-asc') !== 'false',
    search: '',
    clipboard: null,
    loading: false
  };

  const TEXT_EXT = new Set([
    'txt', 'log', 'md', 'sh', 'py', 'kt', 'java', 'xml', 'json', 'html', 'htm',
    'css', 'js', 'ts', 'yaml', 'yml', 'conf', 'cfg', 'properties', 'gradle',
    'pro', 'csv', 'ini', 'env', 'sql', 'c', 'cpp', 'h', 'go', 'rs', 'toml'
  ]);
  const IMAGE_EXT = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp']);

  async function fbApi(path, options = {}) {
    const res = await fetch(path, options);
    const ct = res.headers.get('content-type') || '';
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || res.statusText);
    }
    if (ct.includes('application/json')) return res.json();
    return res.text();
  }

  function $(id) {
    return document.getElementById(id);
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function formatSize(bytes) {
    if (!bytes || bytes <= 0) return '—';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let value = bytes;
    let i = 0;
    while (value >= 1024 && i < units.length - 1) {
      value /= 1024;
      i += 1;
    }
    return `${value.toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
  }

  function formatDate(ts) {
    if (!ts) return '—';
    return new Date(ts).toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit'
    });
  }

  function fileExt(name) {
    const idx = name.lastIndexOf('.');
    return idx > 0 ? name.slice(idx + 1).toLowerCase() : '';
  }

  function fileIcon(item) {
    if (item.directory) return '📁';
    const ext = fileExt(item.name);
    if (IMAGE_EXT.has(ext)) return '🖼️';
    if (TEXT_EXT.has(ext)) return '📝';
    if (['zip', 'tar', 'gz', '7z', 'rar'].includes(ext)) return '🗜️';
    if (['mp4', 'mkv', 'avi', 'mov'].includes(ext)) return '🎬';
    if (['mp3', 'wav', 'flac', 'aac'].includes(ext)) return '🎵';
    if (ext === 'pdf') return '📕';
    if (['apk'].includes(ext)) return '📦';
    return '📄';
  }

  function isPreviewable(item) {
    if (item.directory) return false;
    const ext = fileExt(item.name);
    return IMAGE_EXT.has(ext) || TEXT_EXT.has(ext);
  }

  function joinPath(base, name) {
    const root = base.replace(/\/$/, '');
    return `${root}/${name}`;
  }

  function getSelectedItems() {
    return fbState.items.filter(item => fbState.selected.has(item.path));
  }

  function clearSelection() {
    fbState.selected.clear();
    fbState.lastSelectedPath = null;
    updateToolbar();
    renderList();
  }

  function selectOnly(item) {
    fbState.selected.clear();
    fbState.selected.add(item.path);
    fbState.lastSelectedPath = item.path;
    updateToolbar();
    renderList();
  }

  function toggleSelect(item, additive) {
    if (!additive) {
      fbState.selected.clear();
    }
    if (fbState.selected.has(item.path)) {
      if (additive) fbState.selected.delete(item.path);
    } else {
      fbState.selected.add(item.path);
    }
    fbState.lastSelectedPath = item.path;
    updateToolbar();
    renderList();
  }

  function rangeSelect(toItem) {
    const fromPath = fbState.lastSelectedPath;
    if (!fromPath) {
      selectOnly(toItem);
      return;
    }
    const paths = getVisibleItems().map(i => i.path);
    const a = paths.indexOf(fromPath);
    const b = paths.indexOf(toItem.path);
    if (a < 0 || b < 0) {
      selectOnly(toItem);
      return;
    }
    const [start, end] = a < b ? [a, b] : [b, a];
    fbState.selected.clear();
    for (let i = start; i <= end; i += 1) {
      fbState.selected.add(paths[i]);
    }
    fbState.lastSelectedPath = toItem.path;
    updateToolbar();
    renderList();
  }

  function getVisibleItems() {
    let items = [...fbState.items];
    if (fbState.search.trim()) {
      const q = fbState.search.trim().toLowerCase();
      items = items.filter(item => item.name.toLowerCase().includes(q));
    }
    items.sort((a, b) => {
      if (a.directory !== b.directory) return a.directory ? -1 : 1;
      let cmp = 0;
      if (fbState.sortBy === 'size') cmp = (a.size || 0) - (b.size || 0);
      else if (fbState.sortBy === 'modified') cmp = (a.lastModified || 0) - (b.lastModified || 0);
      else cmp = a.name.localeCompare(b.name, 'zh-CN', { sensitivity: 'base' });
      return fbState.sortAsc ? cmp : -cmp;
    });
    return items;
  }

  function updateToolbar() {
    const count = fbState.selected.size;
    const hasSelection = count > 0;
    const single = count === 1;
    $('fb-download').disabled = !hasSelection;
    $('fb-rename').disabled = !single;
    $('fb-copy').disabled = !hasSelection;
    $('fb-cut').disabled = !hasSelection;
    $('fb-delete').disabled = !hasSelection;
    $('fb-paste').disabled = !fbState.clipboard || !fbState.clipboard.paths.length;
    $('fb-select-all').checked = fbState.items.length > 0 &&
      fbState.items.every(item => fbState.selected.has(item.path));

    const totalSize = getSelectedItems().reduce((sum, item) => sum + (item.size || 0), 0);
    let status = `${fbState.items.length} 项`;
    if (fbState.search.trim()) status += ` · 显示 ${getVisibleItems().length} 项`;
    if (hasSelection) status += ` · 已选 ${count} 项 (${formatSize(totalSize)})`;
    if (fbState.clipboard) {
      status += ` · 剪贴板: ${fbState.clipboard.mode === 'cut' ? '剪切' : '复制'} ${fbState.clipboard.paths.length} 项`;
    }
    $('fb-status').textContent = status;
  }

  function renderBreadcrumb() {
    const el = $('fb-breadcrumb');
    el.innerHTML = '';
    const root = fbState.rootPath || fbState.currentPath;
    const current = fbState.currentPath;

    const home = document.createElement('button');
    home.className = 'fb-crumb';
    home.textContent = '🏠 主目录';
    home.addEventListener('click', () => loadFiles(root));
    el.appendChild(home);

    let relative = '';
    let accBase = root.replace(/\/$/, '');
    if (current.startsWith(root) && current.length > root.length) {
      relative = current.slice(root.length).replace(/^\//, '');
    } else if (!current.startsWith(root)) {
      accBase = '';
      relative = current.replace(/^\//, '');
    }

    if (!relative) return;

    let acc = accBase;
    relative.split('/').filter(Boolean).forEach(part => {
      acc = acc ? `${acc}/${part}` : `/${part}`;
      const sep = document.createElement('span');
      sep.className = 'fb-crumb-sep';
      sep.textContent = '/';
      el.appendChild(sep);
      const btn = document.createElement('button');
      btn.className = 'fb-crumb';
      btn.textContent = part;
      const target = acc;
      btn.addEventListener('click', () => loadFiles(target));
      el.appendChild(btn);
    });
  }

  function renderList() {
    const list = $('fb-list');
    const items = getVisibleItems();
    list.className = `fb-list fb-view-${fbState.view}`;
    list.innerHTML = '';

    if (!items.length) {
      list.innerHTML = `<div class="fb-empty">${fbState.search ? '没有匹配的文件' : '此文件夹为空'}</div>`;
      updateToolbar();
      return;
    }

    items.forEach(item => {
      const row = document.createElement('div');
      row.className = `fb-item fb-item-${fbState.view}`;
      if (fbState.selected.has(item.path)) row.classList.add('selected');
      row.dataset.path = item.path;

      const checked = fbState.selected.has(item.path);
      row.innerHTML = fbState.view === 'list'
        ? `
          <label class="fb-check" onclick="event.stopPropagation()">
            <input type="checkbox" ${checked ? 'checked' : ''}>
          </label>
          <span class="fb-col-name"><span class="fb-icon">${fileIcon(item)}</span>${escapeHtml(item.name)}</span>
          <span class="fb-col-size">${item.directory ? '—' : formatSize(item.size)}</span>
          <span class="fb-col-date">${formatDate(item.lastModified)}</span>
        `
        : `
          <label class="fb-check" onclick="event.stopPropagation()">
            <input type="checkbox" ${checked ? 'checked' : ''}>
          </label>
          <div class="fb-grid-icon">${fileIcon(item)}</div>
          <div class="fb-grid-name" title="${escapeHtml(item.name)}">${escapeHtml(item.name)}</div>
          <div class="fb-grid-meta">${item.directory ? '文件夹' : formatSize(item.size)}</div>
        `;

      const checkbox = row.querySelector('input[type=checkbox]');
      checkbox?.addEventListener('change', e => {
        e.stopPropagation();
        if (checkbox.checked) fbState.selected.add(item.path);
        else fbState.selected.delete(item.path);
        fbState.lastSelectedPath = item.path;
        updateToolbar();
        renderList();
      });

      row.addEventListener('click', e => {
        if (e.shiftKey) rangeSelect(item);
        else if (e.ctrlKey || e.metaKey) toggleSelect(item, true);
        else selectOnly(item);
      });

      row.addEventListener('dblclick', e => {
        e.preventDefault();
        if (item.directory) loadFiles(item.path);
        else if (isPreviewable(item)) openPreview(item);
        else downloadItems([item]);
      });

      row.addEventListener('contextmenu', e => {
        e.preventDefault();
        if (!fbState.selected.has(item.path)) selectOnly(item);
        showContextMenu(e, item);
      });

      list.appendChild(row);
    });
    updateToolbar();
  }

  async function loadFiles(path) {
    if (fbState.loading) return;
    fbState.loading = true;
    const list = $('fb-list');
    list.className = `fb-list fb-view-${fbState.view}`;
    list.innerHTML = '<div class="fb-empty">加载中...</div>';
    try {
      const data = await fbApi(`/api/files/list?path=${encodeURIComponent(path || '')}`);
      if (data.ok === false || data.error) throw new Error(data.error || '加载失败');
      fbState.currentPath = data.path;
      fbState.parentPath = data.parent || '';
      fbState.rootPath = data.root || data.path;
      fbState.items = data.items || [];
      fbState.selected.clear();
      fbState.lastSelectedPath = null;
      renderBreadcrumb();
      renderList();
    } catch (err) {
      list.className = 'fb-list';
      list.innerHTML = `<div class="fb-empty fb-error">${escapeHtml(err.message)}</div>`;
      updateToolbar();
    } finally {
      fbState.loading = false;
    }
  }

  function showContextMenu(e, item) {
    const menu = $('fb-context-menu');
    menu.classList.remove('hidden');
    menu.style.left = `${e.clientX}px`;
    menu.style.top = `${e.clientY}px`;
    menu.dataset.itemPath = item.path;
    menu.querySelector('[data-action=preview]').classList.toggle('hidden', !isPreviewable(item));
    menu.querySelector('[data-action=open]').classList.toggle('hidden', !item.directory);
    menu.querySelector('[data-action=download]').classList.toggle('hidden', item.directory);
  }

  async function createFolder() {
    const name = prompt('文件夹名称');
    if (!name || !name.trim()) return;
    const path = joinPath(fbState.currentPath, name.trim());
    await fbApi('/api/files/mkdir', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path })
    });
    await loadFiles(fbState.currentPath);
  }

  async function createFilePrompt() {
    const name = prompt('文件名称');
    if (!name || !name.trim()) return;
    const path = joinPath(fbState.currentPath, name.trim());
    await fbApi('/api/files/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path })
    });
    await loadFiles(fbState.currentPath);
  }

  async function renameSelected() {
    const items = getSelectedItems();
    if (items.length !== 1) return;
    const item = items[0];
    const name = prompt('新名称', item.name);
    if (!name || !name.trim() || name.trim() === item.name) return;
    await fbApi('/api/files/rename', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path: item.path, name: name.trim() })
    });
    await loadFiles(fbState.currentPath);
  }

  function setClipboard(mode) {
    const paths = getSelectedItems().map(i => i.path);
    if (!paths.length) return;
    fbState.clipboard = { mode, paths };
    updateToolbar();
  }

  async function pasteClipboard() {
    if (!fbState.clipboard || !fbState.clipboard.paths.length) return;
    const { mode, paths } = fbState.clipboard;
    for (const from of paths) {
      const name = from.split('/').pop();
      const to = joinPath(fbState.currentPath, name);
      await fbApi(`/api/files/${mode === 'cut' ? 'move' : 'copy'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ from, to })
      });
    }
    if (mode === 'cut') fbState.clipboard = null;
    await loadFiles(fbState.currentPath);
  }

  async function deleteSelected() {
    const items = getSelectedItems();
    if (!items.length) return;
    if (!confirm(`确定删除 ${items.length} 项？此操作不可撤销。`)) return;
    for (const item of items) {
      await fbApi('/api/files/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ path: item.path })
      });
    }
    await loadFiles(fbState.currentPath);
  }

  function downloadItems(items) {
    items.forEach(item => {
      if (!item.directory) {
        window.open(`/api/files/download?path=${encodeURIComponent(item.path)}`, '_blank');
      }
    });
  }

  async function uploadFiles(fileList) {
    const files = Array.from(fileList || []);
    if (!files.length) return;
    $('fb-status').textContent = `正在上传 0/${files.length}...`;
    let done = 0;
    for (const file of files) {
      const fd = new FormData();
      fd.append('path', joinPath(fbState.currentPath, file.name));
      fd.append('file', file);
      await fetch('/api/files/upload', { method: 'POST', body: fd });
      done += 1;
      $('fb-status').textContent = `正在上传 ${done}/${files.length}...`;
    }
    await loadFiles(fbState.currentPath);
  }

  function openPreview(item) {
    const modal = $('fb-preview-modal');
    const title = $('fb-preview-title');
    const body = $('fb-preview-body');
    title.textContent = item.name;
    body.innerHTML = '<div class="fb-empty">加载中...</div>';
    modal.classList.remove('hidden');
    modal.dataset.path = item.path;
    modal.dataset.editable = '0';

    const ext = fileExt(item.name);
    if (IMAGE_EXT.has(ext)) {
      body.innerHTML = `<img class="fb-preview-image" src="/api/files/raw?path=${encodeURIComponent(item.path)}&t=${Date.now()}" alt="">`;
      $('fb-preview-save').classList.add('hidden');
      return;
    }

    if (TEXT_EXT.has(ext)) {
      fbApi(`/api/files/read?path=${encodeURIComponent(item.path)}`).then(data => {
        if (data.ok === false) throw new Error(data.error);
        body.innerHTML = '';
        const editor = document.createElement('textarea');
        editor.id = 'fb-preview-editor';
        editor.spellcheck = false;
        editor.value = data.content;
        body.appendChild(editor);
        modal.dataset.editable = '1';
        $('fb-preview-save').classList.remove('hidden');
      }).catch(err => {
        body.innerHTML = `<div class="fb-error">${escapeHtml(err.message)}</div>`;
        $('fb-preview-save').classList.add('hidden');
      });
    }
  }

  async function savePreview() {
    const modal = $('fb-preview-modal');
    if (modal.dataset.editable !== '1') return;
    const editor = $('fb-preview-editor');
    const path = modal.dataset.path;
    await fbApi('/api/files/write', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path, content: editor.value })
    });
    modal.classList.add('hidden');
    await loadFiles(fbState.currentPath);
  }

  function closePreview() {
    $('fb-preview-modal').classList.add('hidden');
  }

  function setView(view) {
    fbState.view = view;
    localStorage.setItem('zt-fb-view', view);
    document.querySelectorAll('.fb-view-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.view === view);
    });
    $('fb-table-head').classList.toggle('hidden', view !== 'list');
    renderList();
  }

  function bindEvents() {
    $('fb-new-folder').addEventListener('click', createFolder);
    $('fb-new-file').addEventListener('click', createFilePrompt);
    $('fb-refresh').addEventListener('click', () => loadFiles(fbState.currentPath));
    $('fb-up').addEventListener('click', () => {
      if (fbState.parentPath) loadFiles(fbState.parentPath);
    });
    $('fb-download').addEventListener('click', () => downloadItems(getSelectedItems()));
    $('fb-rename').addEventListener('click', renameSelected);
    $('fb-copy').addEventListener('click', () => setClipboard('copy'));
    $('fb-cut').addEventListener('click', () => setClipboard('cut'));
    $('fb-paste').addEventListener('click', pasteClipboard);
    $('fb-delete').addEventListener('click', deleteSelected);

    $('fb-search').addEventListener('input', e => {
      fbState.search = e.target.value;
      renderList();
    });

    $('fb-sort').addEventListener('change', e => {
      fbState.sortBy = e.target.value;
      localStorage.setItem('zt-fb-sort', fbState.sortBy);
      renderList();
    });

    $('fb-sort-dir').addEventListener('click', () => {
      fbState.sortAsc = !fbState.sortAsc;
      localStorage.setItem('zt-fb-sort-asc', String(fbState.sortAsc));
      $('fb-sort-dir').textContent = fbState.sortAsc ? '↑' : '↓';
      renderList();
    });

    document.querySelectorAll('.fb-view-btn').forEach(btn => {
      btn.addEventListener('click', () => setView(btn.dataset.view));
    });

    $('fb-select-all').addEventListener('change', e => {
      if (e.target.checked) {
        fbState.items.forEach(item => fbState.selected.add(item.path));
      } else {
        fbState.selected.clear();
      }
      updateToolbar();
      renderList();
    });

    $('fb-upload').addEventListener('change', e => {
      uploadFiles(e.target.files);
      e.target.value = '';
    });

    const dropzone = $('fb-dropzone');
    dropzone.addEventListener('dragover', e => {
      e.preventDefault();
      dropzone.classList.add('dragover');
    });
    dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
    dropzone.addEventListener('drop', e => {
      e.preventDefault();
      dropzone.classList.remove('dragover');
      uploadFiles(e.dataTransfer.files);
    });

    document.addEventListener('click', e => {
      if (!e.target.closest('#fb-context-menu')) {
        $('fb-context-menu').classList.add('hidden');
      }
    });

    $('fb-context-menu').addEventListener('click', async e => {
      const btn = e.target.closest('button[data-action]');
      if (!btn) return;
      const action = btn.dataset.action;
      const path = $('fb-context-menu').dataset.itemPath;
      const item = fbState.items.find(i => i.path === path);
      if (!item) return;
      $('fb-context-menu').classList.add('hidden');

      if (action === 'open' && item.directory) loadFiles(item.path);
      else if (action === 'preview') openPreview(item);
      else if (action === 'download') downloadItems([item]);
      else if (action === 'rename') { selectOnly(item); await renameSelected(); }
      else if (action === 'copy') { selectOnly(item); setClipboard('copy'); }
      else if (action === 'cut') { selectOnly(item); setClipboard('cut'); }
      else if (action === 'delete') { selectOnly(item); await deleteSelected(); }
      else if (action === 'info') alert(`${item.name}\n路径: ${item.path}\n大小: ${formatSize(item.size)}\n修改: ${formatDate(item.lastModified)}`);
    });

    $('fb-preview-close').addEventListener('click', closePreview);
    $('fb-preview-save').addEventListener('click', savePreview);
    $('fb-preview-modal').addEventListener('click', e => {
      if (e.target === $('fb-preview-modal')) closePreview();
    });

    document.addEventListener('keydown', e => {
      if (!$('panel-files').classList.contains('active')) return;
      if (e.key === 'Delete' && fbState.selected.size) {
        e.preventDefault();
        deleteSelected();
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'a') {
        e.preventDefault();
        fbState.items.forEach(item => fbState.selected.add(item.path));
        updateToolbar();
        renderList();
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'c') {
        if (fbState.selected.size) setClipboard('copy');
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'x') {
        if (fbState.selected.size) setClipboard('cut');
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'v') {
        if (fbState.clipboard) pasteClipboard();
      }
      if (e.key === 'F2' && fbState.selected.size === 1) renameSelected();
      if (e.key === 'F5') { e.preventDefault(); loadFiles(fbState.currentPath); }
      if (e.key === 'Backspace' && fbState.parentPath && !e.target.closest('input, textarea')) {
        loadFiles(fbState.parentPath);
      }
    });
  }

  function init() {
    $('fb-sort').value = fbState.sortBy;
    $('fb-sort-dir').textContent = fbState.sortAsc ? '↑' : '↓';
    setView(fbState.view);
    bindEvents();
  }

  window.ztFiles = {
    init,
    load: loadFiles
  };
})();
