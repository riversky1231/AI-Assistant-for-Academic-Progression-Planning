const api = require('../../services/api');
const auth = require('../../utils/auth');
const { schoolView, number } = require('../../utils/planning');
Page({
  data: { loading: true, error: '', school: null, filtered: [], totalAdmissions: 0, hasMore: false, provinces: ['全部生源地'], years: ['全部年份'], subjects: ['全部科类', '物理类', '历史类'], provinceIndex: 0, yearIndex: 0, subjectIndex: 0 },
  pageSize: 30,
  onLoad(options) { this.id = options.id; },
  onShow() { if (auth.requireLogin()) this.load(); else this.setData({ loading: false, school: null, error: '请登录后查看院校详情' }); },
  async load() {
    if (!auth.requireLogin()) return;
    if (!/^\d+$/.test(this.id || '') || Number(this.id) < 1) { this.setData({ loading: false, error: '院校编号无效，请从院校库重新选择' }); return; }
    this.setData({ loading: true, error: '' });
    try {
      const school = await api.school(this.id);
      // Keep the full history on the logic layer. Passing the response to setData
      // copied it into school, admissions and filtered, tripling the payload.
      const { admissions: rawAdmissions = [], ...schoolSummary } = school;
      this._admissions = (Array.isArray(rawAdmissions) ? rawAdmissions : []).map((item, index) => ({
        key: index,
        province: item.province,
        subject_type: item.subject_type,
        year: item.year,
        min_score: item.min_score,
        rankText: number(item.min_rank),
        major: item.major ? {
          name: item.major.name,
          category: item.major.category,
          description: String(item.major.description || '').slice(0, 1200)
        } : { name: '', category: '', description: '' }
      }));
      this.setData({ school: schoolView(schoolSummary),
        provinces: ['全部生源地'].concat([...new Set(this._admissions.map(item => item.province))]),
        years: ['全部年份'].concat([...new Set(this._admissions.map(item => item.year))].sort((a, b) => b - a).map(String)),
        provinceIndex: 0, yearIndex: 0, subjectIndex: 0 }, () => this.filter());
    } catch (error) { this.setData({ error: error.message, school: null }); }
    finally { this.setData({ loading: false }); }
  },
  change(event) { this.setData({ [event.currentTarget.dataset.field]: Number(event.detail.value) }, () => this.filter()); },
  filter() {
    const d = this.data;
    this._matchingAdmissions = (this._admissions || []).filter(item => (!d.provinceIndex || item.province === d.provinces[d.provinceIndex]) && (!d.yearIndex || String(item.year) === d.years[d.yearIndex]) && (!d.subjectIndex || item.subject_type === d.subjects[d.subjectIndex]));
    const filtered = this._matchingAdmissions.slice(0, this.pageSize);
    this.setData({ filtered, totalAdmissions: this._matchingAdmissions.length, hasMore: filtered.length < this._matchingAdmissions.length });
  },
  loadMore() {
    const current = this.data.filtered.length;
    const filtered = this._matchingAdmissions.slice(0, current + this.pageSize);
    this.setData({ filtered, hasMore: filtered.length < this._matchingAdmissions.length });
  },
  plan() { wx.switchTab({ url: '/pages/recommend/index' }); },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()); }
});
