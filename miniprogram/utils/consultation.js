// Kept in memory only. Token ownership also protects against late replies after logout.
let current = null;
let sequence = 0;
let listener = null;
function clear() { current = null; if (listener) listener(); }
function subscribe(callback) {
  listener = callback;
  return () => { if (listener === callback) listener = null; };
}
function notify(state) { if (isCurrent(state) && listener) listener(); }
function forAccount(owner) {
  if (!owner) { clear(); return null; }
  if (!current || current.owner !== owner) {
    current = { owner, conversationId: '', messages: [], draft: '', busy: false, error: '', expired: false };
  }
  return current;
}
function isCurrent(state) { return !!state && current === state; }
function nextId() { sequence += 1; return 'message-' + sequence; }
function profilePrompt(profile) {
  if (!profile) return '';
  return `我是${profile.province}${profile.subject_type}考生，${profile.score}分，全省位次${profile.rank}。` +
    (profile.major_preference ? `意向专业：${profile.major_preference}。` : '') +
    (profile.region_preference ? `意向地区：${profile.region_preference}。` : '') +
    '请结合历史录取数据，帮我分析院校、专业和冲稳保的选择。';
}
function sourceSummary(sources) {
  const names = { search_schools: '院校查询', school_detail: '录取详情', recommend: '冲稳保推荐', read_skill_resource: '背景资料' };
  return (Array.isArray(sources) ? sources : []).map((source, index) => ({
    index, title: names[source.tool] || '其他依据', background: source.tool === 'read_skill_resource'
  }));
}
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
