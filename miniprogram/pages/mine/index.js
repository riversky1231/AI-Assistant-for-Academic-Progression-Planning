const api = require('../../services/api');
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const { number } = require('../../utils/planning');
Page({
  data: { loggedIn: false, username: '', account: null, profile: null, hasResult: false, loading: false, savingAccount: false, changingPassword: false, users: [], usersOpen: false, managing: false, error: '', permissions: [], roles: [], healthText: '', checking: false, loggingOut: false, accountForm: { nickname: '', phone: '', email: '' }, passwordForm: { oldPassword: '', newPassword: '' } },
  onShow() { auth.selectTab(this, 3); this.refresh(); },
  input(event) {
    const group = event.currentTarget.dataset.group;
    const field = event.currentTarget.dataset.field;
    this.setData({ [`${group}.${field}`]: event.detail.value, error: '' });
  },
  async refresh() {
    const account = auth.session();
    const profile = storage.read('profile', null);
    this.setData({ loggedIn: !!account, username: account ? (account.nickname || account.username) : '', profile, rankText: profile ? number(profile.rank) : '', hasResult: !!storage.read('result', null), permissions: [], roles: [], error: '' });
    if (!account) return;
    this.setData({ loading: true });
    try {
      const [me, user] = await Promise.all([api.me(), api.profile()]);
      const permissions = me.permissions || [];
      const roles = me.roles || [];
      storage.write('session', Object.assign({}, account, { username: user.username, nickname: user.nickname || '', permissions }));
      this.setData({
        username: user.nickname || user.username,
        account: user,
        accountForm: { nickname: user.nickname || '', phone: user.phone || '', email: user.email || '' },
        permissions: [
          { label: '院校查询', enabled: permissions.includes('school:read') },
          { label: '志愿推荐', enabled: permissions.includes('recommend:use') },
          { label: '账号管理', enabled: permissions.includes('account:manage') }
        ],
        roles
      });
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ loading: false }); }
  },
  login() { auth.login('/pages/mine/index'); },
  plan() { wx.switchTab({ url: '/pages/recommend/index' }); },
  results() { wx.navigateTo({ url: '/pages/results/index' }); },
  async health() {
    if (this.data.checking) return;
    this.setData({ checking: true, healthText: '' });
    try { const result = await api.health(); this.setData({ healthText: result.status === 'ok' ? '服务连接正常' : '服务状态异常' }); }
    catch (error) { this.setData({ healthText: error.message }); }
    finally { this.setData({ checking: false }); }
  },
  async saveAccount() {
    if (this.data.savingAccount) return;
    this.setData({ savingAccount: true, error: '' });
    try {
      const user = await api.updateProfile(this.data.accountForm);
      const account = auth.session();
      storage.write('session', Object.assign({}, account, { username: user.username, nickname: user.nickname || '' }));
      wx.showToast({ title: '资料已更新', icon: 'success' });
      this.setData({ account: user, username: user.nickname || user.username });
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ savingAccount: false }); }
  },
  async changePassword() {
    if (this.data.changingPassword) return;
    const form = this.data.passwordForm;
    if (form.oldPassword.length < 8 || form.newPassword.length < 8) { this.setData({ error: '密码长度至少 8 位' }); return; }
    this.setData({ changingPassword: true, error: '' });
    try {
      await api.changePassword({ old_password: form.oldPassword, new_password: form.newPassword });
      wx.showToast({ title: '密码已修改', icon: 'success' });
      this.setData({ passwordForm: { oldPassword: '', newPassword: '' } });
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ changingPassword: false }); }
  },
  async loadUsers() {
    if (this.data.managing) return;
    this.setData({ managing: true, error: '', usersOpen: true });
    try { this.setData({ users: await api.users() }); }
    catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ managing: false }); }
  },
  async toggleUser(event) {
    const user = this.data.users[event.currentTarget.dataset.index];
    if (!user || this.data.managing) return;
    this.setData({ managing: true, error: '' });
    try {
      await api.updateUser(user.id, { nickname: user.nickname || '', phone: user.phone || '', email: user.email || '', enabled: !user.enabled });
      this.setData({ users: await api.users() });
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ managing: false }); }
  },
  resetUser(event) {
    const user = this.data.users[event.currentTarget.dataset.index];
    const id = user && user.id;
    if (!id) return;
    wx.showModal({ title: '重置密码', editable: true, placeholderText: '输入新密码，至少 8 位', confirmText: '重置', confirmColor: '#244c3b', success: async result => {
      if (!result.confirm) return;
      const password = (result.content || '').trim();
      if (password.length < 8) { this.setData({ error: '新密码至少 8 位' }); return; }
      this.setData({ managing: true, error: '' });
      try { await api.resetUserPassword(id, { new_password: password }); wx.showToast({ title: '已重置', icon: 'success' }); }
      catch (error) { this.setData({ error: error.message }); }
      finally { this.setData({ managing: false }); }
    } });
  },
  about() { wx.showModal({ title: '关于向远', content: '向远是一款升学规划助手，帮助你了解院校、专业和历史录取信息。冲稳保按同省同科类历史位次差分类，使用本地演示数据，不承诺录取结果。考生档案与最近推荐仅保存在本机，退出账号后清除。', showCancel: false, confirmColor: '#244c3b' }); },
  logout() {
    if (this.data.loggingOut) return;
    wx.showModal({ title: '退出当前账号？', content: '将清除本机的考生档案和最近推荐，重新登录后需要再次填写。', confirmText: '退出登录', confirmColor: '#244c3b', success: async result => {
      if (!result.confirm || this.data.loggingOut) return;
      this.setData({ loggingOut: true, error: '' });
      try { await api.logout(); storage.clear(); this.refresh(); }
      catch (error) { if (error.status === 401) this.refresh(); else this.setData({ error: error.message + '，尚未退出，请重试' }); }
      finally { this.setData({ loggingOut: false }); }
    } });
  },
  onPullDownRefresh() { this.refresh().finally(() => wx.stopPullDownRefresh()); }
});
