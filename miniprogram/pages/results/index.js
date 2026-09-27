const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const consultation = require('../../utils/consultation');
const { resultGroups, number } = require('../../utils/planning');
Page({
  data: { ready: false, active: 0, groups: [], items: [], profile: {}, total: 0 },
  onShow() {
    if (!auth.requireLogin()) { this.setData({ ready: false }); return; }
    // 从本地读取上次推荐结果，无结果则不渲染
    const saved = storage.read('result', null);
    if (!saved) { this.setData({ ready: false }); return; }
    // 把冲/稳/保三组映射成展示分组，并统计总数、默认选中第一个非空组
    const groups = resultGroups(saved.result);
    const total = groups.reduce((sum, group) => sum + group.items.length, 0);
    const active = Math.max(0, groups.findIndex(group => group.items.length));
    const date = new Date(saved.generatedAt);
    this.setData({ ready: true, groups, active, items: groups[active].items, profile: saved.profile, rankText: number(saved.profile.rank), total, disclaimer: saved.result.disclaimer, message: saved.result.message, generatedText: (date.getMonth() + 1) + '月' + date.getDate() + '日 ' + String(date.getHours()).padStart(2, '0') + ':' + String(date.getMinutes()).padStart(2, '0') });
  },
  select(event) { const active = Number(event.currentTarget.dataset.index); this.setData({ active, items: this.data.groups[active].items }); },
  detail(event) { wx.navigateTo({ url: '/pages/school-detail/index?id=' + event.currentTarget.dataset.id }); },
  edit() { wx.switchTab({ url: '/pages/recommend/index' }); },
  // 带着当前推荐结果去问 AI，自动把档案拼成提问草稿
  chat() {
    const account = auth.session();
    const state = consultation.forAccount(account && account.tokenValue);
    const saved = storage.read('result', null);
    if (state && !state.busy && !state.draft && saved && saved.profile) {
      state.draft = consultation.profilePrompt(saved.profile);
    }
    wx.switchTab({ url: '/pages/chat/index' });
  },
  // 弹窗说明位次差规则，让用户理解分组依据
  explain() { wx.showModal({ title: '推荐依据', content: '位次差 = 历史最低位次 − 你的位次。小于 −2000 为“冲”，−2000 至 2000 为“稳”，大于 2000 为“保”。同类按位次差绝对值排序，最多 5 条。分数用于展示，不参与分类；分组不代表录取概率。', showCancel: false, confirmColor: '#244c3b' }); }
});
