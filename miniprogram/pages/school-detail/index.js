const api = require('../../services/api');
const auth = require('../../utils/auth');
const { schoolView, number } = require('../../utils/planning');
Page({
  data: { loading: true, error: '', school: null, admissions: [], filtered: [], provinces: ['全部生源地'], years: ['全部年份'], subjects: ['全部科类', '物理类', '历史类'], provinceIndex: 0, yearIndex: 0, subjectIndex: 0 },
  onLoad(options) { this.id = options.id; },
  onShow() { if (auth.requireLogin()) this.load(); else this.setData({ loading: false, school: null, error: '请登录后查看院校详情' }); },
  async load() {
    if (!auth.requireLogin()) return;
    if (!/^\d+$/.test(this.id || '') || Number(this.id) < 1) { this.setData({ loading: false, error: '院校编号无效，请从院校库重新选择' }); return; }
    this.setData({ loading: true, error: '' });
    try {
      const school = await api.school(this.id);
      const admissions = (school.admissions || []).map((item, index) => Object.assign({}, item, { key: index, rankText: number(item.min_rank) }));
      this.setData({ school: schoolView(school), admissions,
        provinces: ['全部生源地'].concat([...new Set(admissions.map(item => item.province))]),
        years: ['全部年份'].concat([...new Set(admissions.map(item => item.year))].sort((a, b) => b - a).map(String)),
        provinceIndex: 0, yearIndex: 0, subjectIndex: 0 });
      this.filter();
    } catch (error) { this.setData({ error: error.message, school: null }); }
    finally { this.setData({ loading: false }); }
  },
  change(event) { this.setData({ [event.currentTarget.dataset.field]: Number(event.detail.value) }); this.filter(); },
  filter() {
    const d = this.data;
    this.setData({ filtered: d.admissions.filter(item => (!d.provinceIndex || item.province === d.provinces[d.provinceIndex]) && (!d.yearIndex || String(item.year) === d.years[d.yearIndex]) && (!d.subjectIndex || item.subject_type === d.subjects[d.subjectIndex])) });
  },
  plan() { wx.switchTab({ url: '/pages/recommend/index' }); },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()); }
});
