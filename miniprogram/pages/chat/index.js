const api = require('../../services/api');
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const consultation = require('../../utils/consultation');
Page({
  data: {
    loggedIn: false, messages: [], draft: '', busy: false, error: '', expired: false,
    profile: null, scrollTarget: '', keyboardHeight: 0,
    suggestions: [
      { title: '学校和专业，怎么选？', detail: '从兴趣、城市和就业方向一起看', prompt: '选大学时，学校、专业和城市应该怎样权衡？请先了解我的情况再给建议。', icon: '◎' },
      { title: '帮我理清冲稳保', detail: '了解位次差，安排志愿梯度', prompt: '我想了解冲稳保的划分依据，以及怎样合理安排志愿梯度。', icon: '↗' },
      { title: '聊聊专业与未来', detail: '结合家庭预算，考虑长期发展', prompt: '我还没有确定专业，想结合兴趣、家庭预算和未来发展梳理方向。请引导我补充信息。', icon: '✦' }
    ]
  },
  onLoad(options) { this.fromResults = options && options.from === 'results'; },
  onShow() {
    this._unloaded = false;
    const account = auth.session();
    this._state = consultation.forAccount(account && account.tokenValue);
    if (this._unsubscribe) this._unsubscribe();
    this._unsubscribe = consultation.subscribe(() => this.render());
    this.setData({ loggedIn: !!account, profile: account ? storage.read('profile', null) : null });
    if (this._state && this.fromResults) {
      if (!this._state.busy && !this._state.draft) {
        const saved = storage.read('result', null);
        if (saved && saved.profile) this._state.draft = consultation.profilePrompt(saved.profile);
      }
      this.fromResults = false;
    }
    this.render();
  },
  onUnload() { this._unloaded = true; if (this._unsubscribe) this._unsubscribe(); },
  onHide() { if (this._unsubscribe) this._unsubscribe(); this.setData({ keyboardHeight: 0 }); },
  render() {
    if (this._unloaded) return;
    const state = this._state;
    const account = auth.session();
    if (!consultation.isCurrent(state) || !account || account.tokenValue !== state.owner) {
      this.setData({ loggedIn: !!account, messages: [], draft: '', busy: false, error: '', expired: false }); return;
    }
    const messages = state.messages.map(item => ({ id: item.id, role: item.role, text: item.text, sources: consultation.sourceSummary(item.sources) }));
    this.setData({ messages, draft: state.draft, busy: state.busy, error: state.error, expired: state.expired,
      scrollTarget: state.busy ? 'thinking' : messages.length ? messages[messages.length - 1].id : '' });
  },
  input(event) {
    if (!this._state || this._state.busy) return;
    this._state.draft = event.detail.value;
    if (!this._state.expired) this._state.error = '';
    this.setData({ draft: event.detail.value, error: this._state.error });
  },
  suggest(event) {
    if (!auth.session()) { this.login(); return; }
    if (!this._state || this._state.busy) return;
    const suggestion = this.data.suggestions[Number(event.currentTarget.dataset.index)];
    if (!suggestion) return;
    this._state.draft = suggestion.prompt;
    if (!this._state.expired) this._state.error = '';
    this.render();
  },
  useProfile() {
    if (!auth.session()) { this.login(); return; }
    if (!this._state || this._state.busy) return;
    if (!this.data.profile) { this.plan(); return; }
    const profile = consultation.profilePrompt(this.data.profile);
    const draft = this._state.draft.trim();
    const combined = draft && !draft.includes(profile) ? profile + '\n\n' + draft : profile;
    if (combined.length > 4000) { this._state.error = '加入档案后超过 4000 字，请先精简问题。'; this.render(); return; }
    this._state.draft = draft.includes(profile) ? draft : combined;
    if (!this._state.expired) this._state.error = '';
    this.render();
  },
  async send() {
    if (!auth.requireLogin()) return;
    const account = auth.session();
    const state = this._state;
    if (!consultation.isCurrent(state) || state.owner !== account.tokenValue || state.busy || state.expired) return;
    const message = state.draft.trim();
    if (!message || message.length > 4000) {
      state.error = !message ? '先写下你想了解的问题吧。' : '问题最多 4000 字，请精简后再发送。'; this.render(); return;
    }
    const payload = { message };
    if (state.conversationId) payload.conversation_id = state.conversationId;
    const pendingId = consultation.nextId();
    state.messages.push({ id: pendingId, role: 'user', text: message });
    state.busy = true; state.error = ''; state.draft = '';
    this.render();
    try {
      const result = await api.chat(payload);
      if (!this.owns(state)) return;
      if (!result || typeof result.answer !== 'string' || !result.answer.trim() ||
          !/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(result.conversation_id || '')) {
        throw new Error('咨询返回内容不完整，请稍后重试。');
      }
      state.conversationId = result.conversation_id;
      state.messages.push({ id: consultation.nextId(), role: 'assistant', text: result.answer, sources: Array.isArray(result.sources) ? result.sources : [] });
      state.messages = state.messages.slice(-16);
    } catch (error) {
      if (!this.owns(state)) return;
      state.messages = state.messages.filter(item => item.id !== pendingId);
      state.draft = message;
      const failure = consultation.chatError(error);
      state.error = failure.message; state.expired = failure.expired;
    } finally {
      state.busy = false;
      consultation.notify(state);
      this.render();
    }
  },
  owns(state) {
    const account = auth.session();
    return consultation.isCurrent(state) && account && account.tokenValue === state.owner;
  },
  newChat() {
    const state = this._state;
    if (!this.owns(state) || state.busy) return;
    const reset = () => {
      if (!this.owns(state) || state.busy) return;
      const draft = state.expired ? state.draft : '';
      consultation.clear(); this._state = consultation.forAccount(state.owner);
      this._state.draft = draft; this.render();
    };
    if (!state.messages.length && !state.draft) { reset(); return; }
    wx.showModal({ title: state.expired ? '开启新咨询？' : '开始一个新话题？', content: state.expired ? '将清空旧对话，保留未发送成功的问题。请重新补充考生背景。' : '当前对话与草稿将清空。新咨询需要重新提供考生背景。', confirmText: '开启新咨询', confirmColor: '#244c3b', success: result => { if (result.confirm) reset(); } });
  },
  sources(event) {
    wx.navigateTo({ url: '/pages/chat-sources/index?message=' + encodeURIComponent(event.currentTarget.dataset.id) });
  },
  copy(event) {
    if (!this.owns(this._state)) return;
    const message = this._state.messages.find(item => item.id === event.currentTarget.dataset.id);
    if (message) wx.setClipboardData({ data: message.text });
  },
  keyboard(event) { this.setData({ keyboardHeight: Math.max(0, Number(event.detail.height) || 0) }); },
  blur() { this.setData({ keyboardHeight: 0 }); },
  login() { auth.login('/pages/chat/index'); },
  plan() { wx.switchTab({ url: '/pages/recommend/index' }); }
});
