const { getBaseUrl } = require('../config/api');
const storage = require('./storage');
const auth = require('./auth');

function request(path, method = 'GET', data, options = {}) {
  return new Promise((resolve, reject) => {
    let base;
    try { base = getBaseUrl(); } catch (error) { reject(error); return; }
    const saved = auth.session();
    const header = { 'content-type': 'application/json' };
    if (!options.public && saved) header[saved.tokenName] = saved.tokenValue;
    wx.request({
      url: base + path, method, data, header, timeout: options.timeout || 15000,
      success(response) {
        // A response from a previous account must never populate the current account's pages.
        if (!options.public && saved && (!auth.session() || auth.session().tokenValue !== saved.tokenValue)) {
          reject(Object.assign(new Error('登录状态已变更，请重新加载'), { status: 401 })); return;
        }
        const body = response.data;
        if (response.statusCode >= 200 && response.statusCode < 300 && body && body.code === 0) {
          resolve(body.data); return;
        }
        const status = response.statusCode >= 400 ? response.statusCode : (body && body.code) || 500;
        const messages = { 401: '登录已过期，请重新登录', 403: '当前账号暂无此功能权限，请联系管理员', 404: '未找到该院校或服务', 429: '操作过于频繁，请稍后再试', 503: '服务暂不可用，请稍后重试' };
        const error = Object.assign(new Error(messages[status] || (body && body.message) || '请求失败，请稍后重试'), { status, serverMessage: body && typeof body.message === 'string' ? body.message : '' });
        if (status === 401 && !options.public) { storage.clear(); auth.login(); }
        if (options.public && status === 401) error.message = '用户名或密码错误';
        reject(error);
      },
      fail(error = {}) {
        const errMsg = typeof error.errMsg === 'string' ? error.errMsg : '';
        const timedOut = /timeout|timed out/i.test(errMsg);
        let message = timedOut ? '请求超时，请稍后重试' : '暂时连接不上服务，请检查网络后重试';
        if (/url not in domain list/i.test(errMsg)) message = '服务地址未通过微信域名校验，请联系管理员';
        // Keep the native failure for device debugging; never log headers or request data.
        try {
          if (wx.getAccountInfoSync().miniProgram.envVersion === 'develop') {
            console.warn('[request:fail]', { url: base + path, method, errMsg, errno: error.errno, errCode: error.errCode });
          }
        } catch (_) { /* Diagnostics must not prevent the request from rejecting. */ }
        reject(Object.assign(new Error(message), { timedOut, errMsg, errno: error.errno, errCode: error.errCode }));
      }
    });
  });
}
module.exports = { request };
