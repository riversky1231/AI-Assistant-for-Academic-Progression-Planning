// Kept in memory only. Token ownership also protects against late replies after logout.
// 咨询会话只存内存（不落本地缓存），按账户 token 隔离；token 归属校验也能挡住登出后的迟到回复。
let current = null;
let sequence = 0;
let listener = null;
// 清空当前会话并通知订阅方重绘
function clear() { current = null; if (listener) listener(); }
// 订阅会话变化（chat 页用），返回取消订阅函数
function subscribe(callback) {
  listener = callback;
  return () => { if (listener === callback) listener = null; };
}
// 仅当 state 仍是当前会话时才通知，避免旧会话触发新页面
function notify(state) { if (isCurrent(state) && listener) listener(); }
// 按账户取会话：换账户则新建；同一账户复用内存态
function forAccount(owner) {
  if (!owner) { clear(); return null; }
  if (!current || current.owner !== owner) {
    current = { owner, conversationId: '', messages: [], draft: '', busy: false, error: '', expired: false };
  }
  return current;
}
// 判断 state 是否为当前激活会话
function isCurrent(state) { return !!state && current === state; }
// 生成递增消息 id
function nextId() { sequence += 1; return 'message-' + sequence; }
// 把考生档案拼成自然语言提问，一键带入聊天
function profilePrompt(profile) {
  if (!profile) return '';
  return `我是${profile.province}${profile.subject_type}考生，${profile.score}分，全省位次${profile.rank}。` +
    (profile.major_preference ? `意向专业：${profile.major_preference}。` : '') +
    (profile.region_preference ? `意向地区：${profile.region_preference}。` : '') +
    '请结合历史录取数据，帮我分析院校、专业和冲稳保的选择。';
}
// 把后端 sources 映射成前端可展示的标题；read_skill_resource 标记为背景资料
function sourceSummary(sources) {
  const names = { search_schools: '院校查询', school_detail: '录取详情', recommend: '冲稳保推荐', read_skill_resource: '背景资料' };
  return (Array.isArray(sources) ? sources : []).map((source, index) => ({
    index, title: names[source.tool] || '其他依据', background: source.tool === 'read_skill_resource'
  }));
}
// 错误码 → 友好提示；404 且提示「会话」时标记会话过期
function chatError(error) {
  if (error.status === 404 && /会话/.test(error.serverMessage || '')) {
    return { expired: true, message: '这段咨询已过期。你的问题已保留，请开启新咨询，并重新补充考生背景。' };
  }
  const messages = {
    404: '暂未找到相关院校或服务，可以调整问题后再试。',
    409: '上一条咨询仍在处理中，请稍等片刻再发送。',
    429: '咨询次数较多，请一分钟后再试。你的问题已保留。',
    502: '暂时未能完成分析，请稍后重试。你的问题已保留。',
    503: '智能咨询暂不可用，你仍可使用院校库和志愿推荐。',
    504: '分析超时，服务可能仍在处理，请稍后再试，避免连续发送。'
  };
  return { expired: false, message: error.timedOut ? messages[504] : messages[error.status] || error.message };
}
module.exports = { clear, forAccount, isCurrent, nextId, profilePrompt, sourceSummary, chatError, subscribe, notify };
