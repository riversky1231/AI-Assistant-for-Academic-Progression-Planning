const api = require('../../services/api');
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const { schoolView, number } = require('../../utils/planning');
Page({
  data: { loggedIn: false, schools: [], loading: false, error: '', profile: null },
  onShow() {
    auth.selectTab(this, 0);
    const profile = storage.read('profile', null);
    this.setData({ loggedIn: !!auth.session(), profile, rankText: profile ? number(profile.rank) : '' });
    this.load();
  },
  async load() {
    const seq = this._seq = (this._seq || 0) + 1;
    if (!auth.session()) { this.setData({ schools: [], loading: false, error: '' }); return; }
    this.setData({ loading: true, error: '' });
    try { const schools = await api.schools({ limit: 3 }); if (seq === this._seq) this.setData({ schools: schools.map(schoolView) }); }
    catch (error) { if (seq === this._seq) this.setData({ error: error.message, schools: [] }); }
    finally { if (seq === this._seq) this.setData({ loading: false }); }
  },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()); },
  onUnload() { this._seq = (this._seq || 0) + 1; },
  plan() { wx.switchTab({ url: '/pages/recommend/index' }); },
  schools() { wx.switchTab({ url: '/pages/schools/index' }); },
  login() { auth.login('/pages/home/index'); }
});
