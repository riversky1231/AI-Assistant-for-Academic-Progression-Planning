const api = require('../../services/api');
const auth = require('../../utils/auth');
const { provinces, schoolView } = require('../../utils/planning');
Page({
  data: { provinces: ['全部地区'].concat(provinces), provinceIndex: 0, keyword: '', schools: [], loading: false, error: '', loggedIn: false },
  onShow() { auth.selectTab(this, 1); this.setData({ loggedIn: !!auth.session() }); this.load(); },
  input(event) { this.setData({ keyword: event.detail.value }); },
  province(event) { this.setData({ provinceIndex: Number(event.detail.value) }); this.load(); },
  clear() { this.setData({ keyword: '', provinceIndex: 0 }); this.load(); },
  async load() {
    const seq = this._seq = (this._seq || 0) + 1;
    if (!auth.session()) { this.setData({ schools: [], loading: false, error: '' }); return; }
    this.setData({ loading: true, error: '', schools: [] });
    try {
      const schools = await api.schools({ keyword: this.data.keyword, province: this.data.provinceIndex ? this.data.provinces[this.data.provinceIndex] : '' });
      if (seq === this._seq) this.setData({ schools: schools.map(schoolView) });
    } catch (error) { if (seq === this._seq) this.setData({ error: error.message }); }
    finally { if (seq === this._seq) this.setData({ loading: false }); }
  },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()); },
  onUnload() { this._seq = (this._seq || 0) + 1; },
  login() { auth.login('/pages/schools/index'); }
});
