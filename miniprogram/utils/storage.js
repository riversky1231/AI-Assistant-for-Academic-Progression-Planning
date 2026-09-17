const KEYS = { session: 'xiangyuan.session', profile: 'xiangyuan.profile', result: 'xiangyuan.result' };
function read(key, fallback) {
  try { return wx.getStorageSync(KEYS[key]) || fallback; } catch (_) { return fallback; }
}
function write(key, value) {
  wx.setStorageSync(KEYS[key], value);
}
function clear() {
  Object.keys(KEYS).forEach(key => wx.removeStorageSync(KEYS[key]));
}
module.exports = { read, write, clear };
