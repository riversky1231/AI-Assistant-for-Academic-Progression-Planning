const { getBaseUrl } = require('../config/api');
const storage = require('./storage');
const auth = require('./auth');

/**
 * 统一请求封装：所有后端调用都走这里。
 * 职责：自动带 satoken 头、账户隔离校验（防串号）、统一错误码映射、401 自动登出。
 */
function request(path, method = 'GET', data, options = {}) {
  return new Promise((resolve, reject) => {
    let base;
    try { base = getBaseUrl(); } catch (error) { reject(error); return; }
    // 读取当前登录会话，非公开接口自动附加 token 头
    const saved = auth.session();
    const header = { 'content-type': 'application/json' };
    if (!options.public && saved) header[saved.tokenName] = saved.tokenValue;
    wx.request({
      url: base + path, method, data, header, timeout: options.timeout || 15000,
      success(response) {
        // A response from a previous account must never populate the current account's pages.
        // 账户隔离：响应返回时若 token 已变化（换号/登出），丢弃本次响应，防止串号污染
        if (!options.public && saved && (!auth.session() || auth.session().tokenValue !== saved.tokenValue)) {
          reject(Object.assign(new Error('登录状态已变更，请重新加载'), { status: 401 })); return;
        }
        const body = response.data;
        // 业务成功：HTTP 2xx 且 body.code === 0，直接返回 data
        if (response.statusCode >= 200 && response.statusCode < 300 && body && body.code === 0) {
          resolve(body.data); return;
        }
        // 否则构造带 status 的错误对象，供调用方按错误码给提示
        const status = response.statusCode >= 400 ? response.statusCode : (body && body.code) || 500;
        const messages = { 401: '登录已过期，请重新登录', 403: '当前账号暂无此功能权限，请联系管理员', 404: '未找到该院校或服务', 429: '操作过于频繁，请稍后再试', 503: '服务暂不可用，请稍后重试' };
        const error = Object.assign(new Error(messages[status] || (body && body.message) || '请求失败，请稍后重试'), { status, serverMessage: body && typeof body.message === 'string' ? body.message : '' });
        // 401 且非公开接口：清登录态并跳登录页
        if (status === 401 && !options.public) { storage.clear(); auth.login(); }
        // 公开接口（如登录）的 401 特指「用户名或密码错误」
        if (options.public && status === 401) error.message = '用户名或密码错误';
        reject(error);
      },
      fail(error = {}) {
        // 网络层失败：超时 / 断网 / 域名未校验，给出对应中文提示
        const errMsg = typeof error.errMsg === 'string' ? error.errMsg : '';
        const timedOut = /timeout|timed out/i.test(errMsg);
        let message = timedOut ? '请求超时，请稍后重试' : '暂时连接不上服务，请检查网络后重试';
        if (/url not in domain list/i.test(errMsg)) message = '服务地址未通过微信域名校验，请联系管理员';
        // Keep the native failure for device debugging; never log headers or request data.
        // 仅开发版打印诊断信息，且绝不记录请求头或请求数据
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
