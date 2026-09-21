const auth = require('../../utils/auth');
const consultation = require('../../utils/consultation');
const { sourcesView } = require('../../utils/consultation-sources');
Page({
  data: { available: false, sources: [], count: 0, referenceCount: 0 },
  onLoad(options) { this.messageId = options.message; },
  onShow() {
    const account = auth.session();
    const state = consultation.forAccount(account && account.tokenValue);
    const message = state && state.messages.find(item => item.id === this.messageId && item.role === 'assistant');
    const sources = message ? sourcesView(message.sources) : [];
    this.setData({ available: !!message, sources, count: sources.length, referenceCount: sources.filter(item => item.kind === 'reference').length });
  },
  toggle(event) {
    const index = Number(event.currentTarget.dataset.index);
    const sources = this.data.sources.map((source, i) => i === index ? Object.assign({}, source, { open: !source.open }) : source);
    this.setData({ sources });
  },
  detail(event) {
    const id = String(event.currentTarget.dataset.id || '');
    if (/^\d+$/.test(id) && Number(id) > 0) wx.navigateTo({ url: '/pages/school-detail/index?id=' + id });
  },
  chat() {
    const pages = getCurrentPages();
    if (pages.length > 1 && pages[pages.length - 2].route === 'pages/chat/index') wx.navigateBack();
    else wx.switchTab({ url: '/pages/chat/index' });
  }
});
