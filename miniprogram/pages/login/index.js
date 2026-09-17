const api = require('../../services/api');
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
Page({
  data: { username: '', password: '', busy: false, error: '', visible: false },
  onLoad(options) { this.next = options.next ? decodeURIComponent(options.next) : '/pages/home/index'; },
  input(event) { this.setData({ [event.currentTarget.dataset.field]: event.detail.value, error: '' }); },
  toggle() { this.setData({ visible: !this.data.visible }); },
  async submit() {
    if (this.data.busy) return;
    const username = this.data.username.trim();
    const password = this.data.password;
    if (!username || username.length > 50) { this.setData({ error: '请输入有效的账号（最多 50 字）' }); return; }
    if (password.length < 8 || password.length > 100) { this.setData({ error: '密码长度应为 8–100 位' }); return; }
    this.setData({ busy: true, error: '' });
    try {
      const result = await api.login({ username, password });
      if (!result.token_name || !result.token_value) throw new Error('登录响应不完整，请联系管理员');
      storage.clear();
      storage.write('session', { tokenName: result.token_name, tokenValue: result.token_value, username, permissions: result.permissions || [] });
      this.setData({ password: '' });
      auth.finishLogin(this.next);
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ busy: false }); }
  }
});
