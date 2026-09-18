const api = require('../../services/api');

Page({
  data: { username: '', phone: '', email: '', newPassword: '', confirmPassword: '', busy: false, error: '', visible: false },
  onLoad(options) { this.next = options.next ? decodeURIComponent(options.next) : '/pages/home/index'; },
  input(event) { this.setData({ [event.currentTarget.dataset.field]: event.detail.value, error: '' }); },
  toggle() { this.setData({ visible: !this.data.visible }); },
  backToLogin() { wx.redirectTo({ url: `/pages/login/index?next=${encodeURIComponent(this.next)}` }); },
  async submit() {
    if (this.data.busy) return;
    const username = this.data.username.trim();
    const phone = this.data.phone.trim();
    const email = this.data.email.trim();
    const newPassword = this.data.newPassword;
    if (!username) { this.setData({ error: '请输入需要找回的账号' }); return; }
    if (!phone && !email) { this.setData({ error: '请输入绑定手机号或邮箱' }); return; }
    if (newPassword.length < 8 || newPassword.length > 100) { this.setData({ error: '新密码长度应为 8–100 位' }); return; }
    if (newPassword !== this.data.confirmPassword) { this.setData({ error: '两次输入的新密码不一致' }); return; }
    this.setData({ busy: true, error: '' });
    try {
      await api.forgotPassword({ username, phone, email, new_password: newPassword });
      wx.showToast({ title: '密码已重置', icon: 'success' });
      setTimeout(() => this.backToLogin(), 700);
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ busy: false }); }
  }
});
