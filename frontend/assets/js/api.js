/* ===== PhoneShop API Client ===== */
const API = '';

const api = {
  async request(method, path, body, isForm = false) {
    const opts = { method, credentials: 'include' };
    if (body && isForm) {
      opts.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
      opts.body = new URLSearchParams(body);
    } else if (body) {
      opts.headers = { 'Content-Type': 'application/json' };
      opts.body = JSON.stringify(body);
    }
    const res = await fetch(API + path, opts);
    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('json')) return res.json();
    return { status: res.status, data: null, message: res.statusText };
  },
  get(p) { return this.request('GET', p); },
  post(p, b) { return this.request('POST', p, b); },
  postForm(p, b) { return this.request('POST', p, b, true); },
  put(p, b) { return this.request('PUT', p, b); },
  del(p) { return this.request('DELETE', p); },
};

/* Auth */
const auth = {
  async login(email, password) {
    return api.postForm('/login', { username: email, password });
  },
  async register(email, password, fullName, phone) {
    return api.postForm('/register', { email, password, fullName, phone });
  },
  async logout() { return api.request('POST', '/logout'); },
  isLoggedIn() { return localStorage.getItem('ps_user') !== null; },
  getUser() { try { return JSON.parse(localStorage.getItem('ps_user')); } catch { return null; } },
  setUser(u) { localStorage.setItem('ps_user', JSON.stringify(u)); },
  clear() { localStorage.removeItem('ps_user'); },
  hasRole(r) { const u = this.getUser(); return u && u.roles && u.roles.includes(r); },
};

/* Price formatter */
const fmt = (n) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n || 0);

/* Toast notifications */
function showToast(msg, type = 'info') {
  const old = document.querySelector('.toast');
  if (old) old.remove();
  const t = document.createElement('div');
  t.className = `toast toast-${type}`;
  t.innerHTML = `<i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>${msg}`;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}

/* Navigation */
function navigate(page, params = {}) {
  const query = Object.entries(params).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&');
  window.location.href = page + (query ? '?' + query : '');
}
function getParam(key) { return new URLSearchParams(window.location.search).get(key); }

/* Render helpers */
function renderStars(rating) {
  let s = '';
  for (let i = 1; i <= 5; i++) s += `<i class="fas fa-star${i <= rating ? '' : (i - 0.5 <= rating ? '-half-alt' : '')} " style="color:${i <= rating ? 'var(--warning)' : 'var(--border)'}"></i>`;
  return s;
}

/* Header update */
function updateHeader() {
  const loginBtn = document.getElementById('loginBtn');
  const userArea = document.getElementById('userArea');
  if (!loginBtn) return;
  if (auth.isLoggedIn()) {
    loginBtn.classList.add('hidden');
    if (userArea) userArea.classList.remove('hidden');
  } else {
    loginBtn.classList.remove('hidden');
    if (userArea) userArea.classList.add('hidden');
  }
}

/* UI Interactions */
function toggleMenu() {
  const nav = document.querySelector('.desktop-nav');
  if (nav) {
    if (nav.style.display === 'none' || !nav.style.display) {
      nav.style.display = 'flex';
      nav.style.flexDirection = 'column';
      nav.style.position = 'absolute';
      nav.style.top = 'var(--header-h)';
      nav.style.left = '0';
      nav.style.width = '100%';
      nav.style.background = 'var(--bg-card)';
      nav.style.padding = '16px';
      nav.style.boxShadow = 'var(--shadow-md)';
      nav.style.zIndex = '100';
    } else {
      nav.style.display = 'none';
    }
  }
}

function toggleFilter() {
  const sidebar = document.querySelector('.filter-sidebar');
  if (sidebar) {
    sidebar.classList.toggle('desktop-only');
  }
}
