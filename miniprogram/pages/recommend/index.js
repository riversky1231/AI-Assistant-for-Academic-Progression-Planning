const api = require('../../services/api');
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const { provinces, emptyProfile, payload } = require('../../utils/planning');
Page({
  data: { provinces, provinceIndex: 0, form: emptyProfile(), busy: false, error: '', loggedIn: false, regions: ['不限', '福建', '江浙沪', '北京', '广东'] },
  onShow() {
    auth.selectTab(this, 3);
    const account = auth.session();
    const token = account ? account.tokenValue : '';
    if (this._token !== token) {
      const guestDraft = this._token === '' && token ? this.data.form : {};
      const form = Object.assign(emptyProfile(), guestDraft, storage.read('profile', {}));
      this.setData({ form, provinceIndex: Math.max(0, provinces.indexOf(form.province)), error: '' });
      this._token = token;
    }
    this.setData({ loggedIn: !!account });
  },
  input(event) { this.setData({ ['form.' + event.currentTarget.dataset.field]: event.detail.value, error: '' }); },
  province(event) { const index = Number(event.detail.value); this.setData({ provinceIndex: index, 'form.province': provinces[index], error: '' }); },
  subject(event) { if (!this.data.busy) this.setData({ 'form.subject_type': event.currentTarget.dataset.value }); },
  region(event) { this.setData({ 'form.region_preference': event.currentTarget.dataset.value === '不限' ? '' : event.currentTarget.dataset.value }); },
  save() {
    if (!auth.requireLogin()) return;
    try { const form = payload(this.data.form); storage.write('profile', form); wx.showToast({ title: '考生档案已保存', icon: 'success' }); }
    catch (error) { this.setData({ error: error.message }); }
  },
  async submit() {
    if (this.data.busy || !auth.requireLogin()) return;
    let form;
    try { form = payload(this.data.form); } catch (error) { this.setData({ error: error.message }); return; }
    this.setData({ busy: true, error: '' });
    try {
      const result = await api.recommend(form);
      storage.write('profile', form);
      storage.write('result', { result, profile: form, generatedAt: Date.now() });
      wx.navigateTo({ url: '/pages/results/index' });
    } catch (error) { this.setData({ error: error.message }); }
    finally { this.setData({ busy: false }); }
  }
});
