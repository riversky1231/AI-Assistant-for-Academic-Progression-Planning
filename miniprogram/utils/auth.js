const storage = require('./storage');
const TABS = ['/pages/home/index', '/pages/schools/index', '/pages/recommend/index', '/pages/mine/index'];
let redirecting = false;

function session() { return storage.read('session', null); }
function currentUrl() {
  const pages = getCurrentPages();
  const page = pages[pages.length - 1];
  if (!page) return TABS[0];
  const path = '/' + page.route;
  return path === '/pages/school-detail/index' && page.options && page.options.id
    ? path + '?id=' + encodeURIComponent(page.options.id) : path;
}
function login(next) {
  if (redirecting || currentUrl() === '/pages/login/index') return;
  redirecting = true;
  wx.navigateTo({
    url: '/pages/login/index?next=' + encodeURIComponent(next || currentUrl()),
    complete() { redirecting = false; }
  });
}
function requireLogin() {
  if (session()) return true;
  login();
  return false;
}
function finishLogin(next) {
  const path = (next || '').split('?')[0];
  if (TABS.includes(path)) { wx.switchTab({ url: path }); return; }
  if (next === '/pages/chat/index') { wx.redirectTo({ url: next }); return; }
  if (path === '/pages/school-detail/index' && /^\/pages\/school-detail\/index\?id=\d+$/.test(next)) {
    wx.redirectTo({ url: next }); return;
  }
  wx.switchTab({ url: TABS[0] });
}
function selectTab(page, selected) {
  if (typeof page.getTabBar === 'function' && page.getTabBar()) page.getTabBar().setData({ selected });
}
module.exports = { session, login, requireLogin, finishLogin, selectTab };
