const api = require('../../services/api');
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const { number } = require('../../utils/planning');
Page({
  data: { loggedIn: false, username: '', profile: null, hasResult: false, loading: false, error: '', permissions: [], healthText: '', checking: false, loggingOut: false },
  onShow() { auth.selectTab(this, 3); this.refresh(); },
  async refresh() {
    const account = auth.session();
    const profile = storage.read('profile', null);
    this.setData({ loggedIn: !!account, username: account ? account.username : '', profile, rankText: profile ? number(profile.rank) : '', hasResult: !!storage.read('result', null), permissions: [], error: '' });
    if (!account) return;
    this.setData({ loading: true });
    try {
      const user = await api.me();
      const permissions = user.permissions || [];
      storage.write('session', Object.assign({}, account, { permissions }));
      this.setData({ permissions: [ { label: '院校查询', enabled: permissions.includes('school:read') }, { label: '志愿推荐', enabled: permissions.includes('recommend:use') } ] });
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
