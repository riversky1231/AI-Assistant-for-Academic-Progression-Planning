const api = require('../../services/api');
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
Page({
  data: { mode: 'login', username: '', password: '', nickname: '', phone: '', email: '', busy: false, error: '', visible: false },
  onLoad(options) { this.next = options.next ? decodeURIComponent(options.next) : '/pages/home/index'; },
  input(event) { this.setData({ [event.currentTarget.dataset.field]: event.detail.value, error: '' }); },
  mode(event) {
    const mode = event.currentTarget.dataset.mode;
    if (mode !== 'login' && mode !== 'register') return;
    this.setData({ mode, error: '', visible: false });
  },
  toggle() { this.setData({ visible: !this.data.visible }); },
  goForgot() { wx.navigateTo({ url: `/pages/forgot/index?next=${encodeURIComponent(this.next)}` }); },
  saveSession(result, fallbackUsername) {
    if (!result.token_name || !result.token_value) throw new Error('登录响应不完整，请联系管理员');
    const user = result.user || {};
    storage.clear();
    storage.write('session', {
      tokenName: result.token_name,
      tokenValue: result.token_value,
      username: user.username || fallbackUsername,
      nickname: user.nickname || '',
      permissions: result.permissions || []
    });
  },
  async submit() {
    if (this.data.busy) return;
    const username = this.data.username.trim();
    const password = this.data.password;
    if (!username || username.length > 50) { this.setData({ error: '请输入有效的账号（最多 50 字）' }); return; }
    if (password.length < 8 || password.length > 100) { this.setData({ error: '密码长度应为 8–100 位' }); return; }
    this.setData({ busy: true, error: '' });
    try {
      const payload = { username, password };
      if (this.data.mode === 'register') {
        Object.assign(payload, { nickname: this.data.nickname.trim(), phone: this.data.phone.trim(), email: this.data.email.trim() });
      }
      const result = this.data.mode === 'register' ? await api.register(payload) : await api.login(payload);
      this.saveSession(result, username);
      this.setData({ password: '' });
      auth.finishLogin(this.next);
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ busy: false }); }
  },
  async wechat() {
    if (this.data.busy) return;
    this.setData({ busy: true, error: '' });
    wx.login({
      success: async result => {
        try {
          if (!result.code) throw new Error('微信登录失败，请重试');
          const login = await api.wechatLogin({ code: result.code, nickname: this.data.nickname.trim() });
          this.saveSession(login, '微信用户');
          auth.finishLogin(this.next);
        } catch (error) { this.setData({ error: error.message }); }
        finally { this.setData({ busy: false }); }
      },
      fail: () => this.setData({ busy: false, error: '微信登录失败，请稍后重试' })
    });
  }
});
