// 登录态管理：会话读取、登录跳转、登录后回跳、Tab 选中态。
const storage = require('./storage');
const TABS = ['/pages/home/index', '/pages/schools/index', '/pages/chat/index', '/pages/recommend/index', '/pages/mine/index'];
// 防止重复触发跳转登录
let redirecting = false;

// 读取当前会话（含 token）
function session() { return storage.read('session', null); }
// 当前页面路径（用于登录后回跳）
function currentUrl() {
  const pages = getCurrentPages();
  const page = pages[pages.length - 1];
  if (!page) return TABS[0];
  const path = '/' + page.route;
  return path === '/pages/school-detail/index' && page.options && page.options.id
    ? path + '?id=' + encodeURIComponent(page.options.id) : path;
}
// 跳转登录页，携带登录后要回跳的地址
function login(next) {
  if (redirecting || currentUrl() === '/pages/login/index') return;
  redirecting = true;
  wx.navigateTo({
    url: '/pages/login/index?next=' + encodeURIComponent(next || currentUrl()),
    complete() { redirecting = false; }
  });
}
// 需要登录才能继续；未登录则跳登录页并返回 false
function requireLogin() {
  if (session()) return true;
  login();
  return false;
}
// 登录成功后按来源回跳：Tab 页用 switchTab，详情页用 redirectTo，其余回首页
function finishLogin(next) {
  const path = (next || '').split('?')[0];
  if (TABS.includes(next)) { wx.switchTab({ url: path }); return; }

  if (path === '/pages/school-detail/index' && /^\/pages\/school-detail\/index\?id=\d+$/.test(next)) {
    wx.redirectTo({ url: next }); return;
  }
  wx.switchTab({ url: TABS[0] });
}
// 设置自定义 TabBar 的选中态
function selectTab(page, selected) {
  if (typeof page.getTabBar === 'function' && page.getTabBar()) page.getTabBar().setData({ selected, keyboardOpen: false });
}
module.exports = { session, login, requireLogin, finishLogin, selectTab };
