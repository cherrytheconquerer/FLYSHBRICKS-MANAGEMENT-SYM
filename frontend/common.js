/* common.js - shared across all pages */
const APP = '/bricks';

(function setDate() {
  const el = document.getElementById('topbar-date');
  if (!el) return;
  el.textContent = new Date().toLocaleDateString('en-IN', {
    weekday:'short', year:'numeric', month:'short', day:'numeric'
  });
})();

(function markActive() {
  const page = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('nav a').forEach(a => {
    if (a.getAttribute('href') === page) a.classList.add('active');
  });
})();

function toast(msg, type) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.style.background = type === 'error' ? 'var(--danger)' : 'var(--success)';
  el.style.color      = type === 'error' ? '#fff' : '#000';
  el.querySelector('span').textContent = msg;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 3200);
}
