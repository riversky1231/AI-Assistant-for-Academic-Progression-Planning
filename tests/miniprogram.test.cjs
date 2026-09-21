const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../miniprogram');
let saved, requests, navigation, currentPages;

beforeEach(() => {
  saved = new Map(); requests = []; navigation = [];
  currentPages = [{ route: 'pages/schools/index', options: {} }];
  global.getCurrentPages = () => currentPages;
  global.wx = {
    getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
    getStorageSync: key => saved.get(key), setStorageSync: (key, value) => saved.set(key, value), removeStorageSync: key => saved.delete(key),
    request: options => requests.push(options),
    navigateTo: options => { navigation.push(options.url); if (options.complete) options.complete(); },
    redirectTo: options => navigation.push(options.url), switchTab: options => navigation.push(options.url),
    showToast() {}, stopPullDownRefresh() {}
  };
});
const storage = require('../miniprogram/utils/storage');
const auth = require('../miniprogram/utils/auth');
const api = require('../miniprogram/services/api');
const planning = require('../miniprogram/utils/planning');
const consultation = require('../miniprogram/utils/consultation');
beforeEach(() => consultation.clear());
const account = () => storage.write('session', { tokenName: 'satoken', tokenValue: 'test-token', username: 'student' });

test('chat occupies the middle tab and all consultation entries use tab navigation', () => {
  const config = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
  assert.equal(config.tabBar.list.length, 5);
  assert.equal(config.tabBar.list[2].pagePath, 'pages/chat/index');
  wx.navigateTo = wx.redirectTo = () => assert.fail('Chat must use switchTab');
  for (const name of ['home', 'mine', 'results', 'chat-sources']) {
    page(name).chat();
    assert.equal(navigation.at(-1), '/pages/chat/index');
  }
  auth.finishLogin('/pages/chat/index');
  assert.equal(navigation.at(-1), '/pages/chat/index');
});

test('results tab entry transfers the snapshot and preserves existing or busy drafts', () => {
  account();
  storage.write('result', { profile: validForm() });
  const results = page('results');
  results.chat();
  const chat = page('chat');
  chat.onShow();
  assert.equal(chat.data.draft, consultation.profilePrompt(validForm()));
  const state = consultation.forAccount('test-token');
  state.draft = 'Keep my question';
  results.chat(); chat.onShow();
  assert.equal(chat.data.draft, 'Keep my question');
  state.draft = ''; state.busy = true;
  results.chat(); chat.onShow();
  assert.equal(chat.data.draft, '');
  assert.equal(requests.length, 0);
});

test('chat selects the center tab and restores navigation after keyboard or page dismissal', () => {
  const tab = { data: {}, setData(values) { Object.assign(this.data, values); } };
  const chat = page('chat');
  chat.getTabBar = () => tab;
  chat.onShow();
  assert.equal(tab.data.selected, 2);
  chat.keyboard({ detail: { height: 300 } });
  assert.equal(chat.data.keyboardHeight, 300);
  assert.equal(tab.data.keyboardOpen, true);
  chat.blur();
  assert.equal(tab.data.keyboardOpen, false);
  chat.keyboard({ detail: { height: 300 } });
  chat.onHide();
  assert.equal(chat.data.keyboardHeight, 0);
  assert.equal(tab.data.keyboardOpen, false);
});
const respond = (index, data, statusCode = 200) => requests[index].success({ statusCode, data });
const validForm = () => ({ province: '福建', subject_type: '物理类', score: '580', rank: '15000', major_preference: '计算机', region_preference: '江浙沪' });
function page(name) {
  let definition;
  const file = path.join(root, 'pages', name, 'index.js');
  vm.runInNewContext(fs.readFileSync(file, 'utf8'), { Page: value => { definition = value; }, require: dependency => require(path.resolve(path.dirname(file), dependency)), wx: global.wx, getCurrentPages: global.getCurrentPages, setTimeout: (...args) => global.setTimeout(...args) });
  definition.data = structuredClone(definition.data);
  definition.setData = function (values) {
    for (const [key, value] of Object.entries(values)) {
      const parts = key.split('.'); let target = this.data;
      for (const part of parts.slice(0, -1)) target = target[part];
      target[parts.at(-1)] = value;
    }
  };
  return definition;
}

test('school queries use the Java envelope, satoken and only nonempty filters', async () => {
  account(); const promise = api.schools({ keyword: ' 福州 ', province: '' });
  assert.equal(requests[0].url, require('../miniprogram/config/api').getBaseUrl() + '/schools');
  assert.equal(requests[0].header.satoken, 'test-token');
  assert.deepEqual(requests[0].data, { limit: 50, keyword: '福州' });
  respond(0, { code: 0, data: [{ id: 2, name: '福州大学' }] });
  assert.equal((await promise)[0].id, 2);
});
test('login uses snake_case response and never attaches an existing token', async () => {
  account(); const promise = api.login({ username: 'student', password: 'password' });
  assert.equal(requests[0].header.satoken, undefined);
  respond(0, { code: 0, data: { token_name: 'satoken', token_value: 'new-token', timeout: 7200 } });
  assert.equal((await promise).token_value, 'new-token');
});
test('401 clears personal data and preserves the requested school route', async () => {
  account(); storage.write('profile', validForm()); storage.write('result', { private: true });
  currentPages = [{ route: 'pages/school-detail/index', options: { id: '2' } }];
  const promise = api.school(2); respond(0, { code: 401, message: '登录已过期' }, 401);
  await assert.rejects(promise, error => error.status === 401);
  assert.equal(saved.size, 0);
  assert.equal(navigation[0], '/pages/login/index?next=' + encodeURIComponent('/pages/school-detail/index?id=2'));
});
test('a late response from a previous account cannot invalidate or populate the new session', async () => {
  account(); const promise = api.schools();
  storage.write('session', { tokenName: 'satoken', tokenValue: 'other-account' });
  respond(0, { code: 401 }, 401);
  await assert.rejects(promise, /登录状态已变更/);
  assert.equal(auth.session().tokenValue, 'other-account'); assert.equal(navigation.length, 0);
});
test('403 keeps the session and reports missing permission without a login loop', async () => {
  account(); const promise = api.schools(); respond(0, { code: 403 }, 403);
  await assert.rejects(promise, /暂无此功能权限/);
  assert.ok(auth.session()); assert.equal(navigation.length, 0);
});
test('invalid credentials and throttling remain on the login page', async () => {
  let promise = api.login({}); respond(0, { code: 401 }, 401); await assert.rejects(promise, /用户名或密码错误/);
  promise = api.login({}); respond(1, { code: 429 }, 429); await assert.rejects(promise, /频繁/);
  assert.equal(navigation.length, 0);
});
test('registration submits the complete registration payload and starts a session', async () => {
  const login = page('login'); login.onLoad({});
  login.setData({ mode: 'register', username: 'new_student', password: 'password88', nickname: '小远', phone: '13800138000', email: 'new@example.com' });
  const submitting = login.submit();
  assert.equal(requests[0].url.endsWith('/auth/register'), true);
  assert.equal(JSON.stringify(requests[0].data), JSON.stringify({ username: 'new_student', password: 'password88', nickname: '小远', phone: '13800138000', email: 'new@example.com' }));
  respond(0, { code: 0, data: { token_name: 'satoken', token_value: 'registered-token', user: { username: 'new_student' } } });
  await submitting;
  assert.equal(auth.session().tokenValue, 'registered-token');
  assert.equal(navigation.at(-1), '/pages/home/index');
});
test('password recovery sends the server request and returns to login on success', async () => {
  const forgot = page('forgot'); forgot.onLoad({ next: encodeURIComponent('/pages/schools/index') });
  forgot.setData({ username: 'student', email: 'student@example.com', newPassword: 'password99', confirmPassword: 'password99' });
  const originalSetTimeout = global.setTimeout; global.setTimeout = callback => callback();
  try {
    const submitting = forgot.submit();
    assert.equal(requests[0].url.endsWith('/auth/forgot-password'), true);
    assert.equal(JSON.stringify(requests[0].data), JSON.stringify({ username: 'student', phone: '', email: 'student@example.com', new_password: 'password99' }));
    respond(0, { code: 0, data: null }); await submitting;
    assert.equal(navigation.at(-1), '/pages/login/index?next=' + encodeURIComponent('/pages/schools/index'));
  } finally { global.setTimeout = originalSetTimeout; }
});
test('network timeout and unexpected success body reject instead of showing false results', async () => {
  let promise = api.health(); requests[0].fail({ errMsg: 'request:fail timeout' }); await assert.rejects(promise, /超时/);
  promise = api.health(); respond(1, '<html>proxy error</html>'); await assert.rejects(promise, /请求失败/);
});
test('device request failures retain native diagnostics without logging credentials or clearing the session', async t => {
  const warning = t.mock.method(console, 'warn', () => {});
  account();
  const nativeError = { errMsg: 'request:fail net::ERR_CONNECTION_REFUSED', errno: 600001, errCode: -102 };
  const promise = api.schools({ keyword: 'private-filter' });
  requests[0].fail(nativeError);
  await assert.rejects(promise, error => error.errMsg === nativeError.errMsg && error.errno === 600001 && error.errCode === -102 && !error.timedOut);
  assert.equal(auth.session().tokenValue, 'test-token');
  assert.equal(navigation.length, 0);
  assert.equal(warning.mock.callCount(), 1);
  const [label, diagnostic] = warning.mock.calls[0].arguments;
  assert.equal(label, '[request:fail]');
  assert.equal(diagnostic.url, requests[0].url);
  assert.equal(diagnostic.errMsg, nativeError.errMsg);
  assert.equal(diagnostic.errno, nativeError.errno);
  assert.doesNotMatch(JSON.stringify(diagnostic), /test-token|satoken|private-filter/);
  const login = api.login({ username: 'private-user', password: 'private-password' });
  requests[1].fail(nativeError);
  await assert.rejects(login);
  assert.doesNotMatch(JSON.stringify(warning.mock.calls[1].arguments), /private-user|private-password/);
});
test('domain rejection is actionable and diagnostic failures cannot leave requests pending', async t => {
  t.mock.method(console, 'warn', () => { throw new Error('debug console unavailable'); });
  const promise = api.health();
  requests[0].fail({ errMsg: 'request:fail url not in domain list' });
  await assert.rejects(promise, error => /域名校验/.test(error.message) && error.errMsg === 'request:fail url not in domain list');
});
test('release builds fail explicitly until the real service URL is configured', async () => {
  wx.getAccountInfoSync = () => ({ miniProgram: { envVersion: 'release' } });
  await assert.rejects(api.health(), /尚未配置/); assert.equal(requests.length, 0);
});
test('recommendation payload matches DTO names and validates score/rank boundaries', () => {
  assert.deepEqual(planning.payload(validForm()), { province: '福建', subject_type: '物理类', score: 580, rank: 15000, major_preference: '计算机', region_preference: '江浙沪' });
  for (const score of ['', '-1', '751', '1.5', '1e2']) assert.throws(() => planning.payload({ ...validForm(), score }), /分数/);
  for (const rank of ['', '0', '-1', '10000001', '1.5']) assert.throws(() => planning.payload({ ...validForm(), rank }), /位次/);
  for (const score of ['0', '750']) assert.equal(planning.payload({ ...validForm(), score }).score, Number(score));
  assert.equal(planning.payload({ ...validForm(), rank: '10000000' }).rank, 10000000);
  assert.throws(() => planning.payload({ ...validForm(), subject_type: '理科' }), /物理类或历史类/);
  assert.throws(() => planning.payload({ ...validForm(), province: '' }), /省份/);
  assert.throws(() => planning.payload({ ...validForm(), major_preference: 'a'.repeat(51) }), /50/);
  assert.equal(planning.payload({ ...validForm(), region_preference: ' ' }).region_preference, null);
});
test('groups retain server classification and order, including empty categories', () => {
  const item = { gap: -2001, school: { id: 1, name: '厦门大学' }, major: { min_rank: 15000 } };
  const groups = planning.resultGroups({ recommendations: { 冲: [item], 稳: [], 保: [] } });
  assert.equal(groups[0].items[0].gapText, '-2,001');
  assert.equal(groups[0].items[0].rankText, '15,000'); assert.equal(groups[1].items.length, 0);
});
test('new school filters cannot be overwritten by a slower previous response', async () => {
  account(); const schools = page('schools');
  schools.setData({ keyword: 'old' }); const oldRequest = schools.load();
  schools.setData({ keyword: 'new' }); const newRequest = schools.load();
  respond(1, { code: 0, data: [{ id: 2, name: '新查询' }] }); await newRequest;
  respond(0, { code: 0, data: [{ id: 1, name: '旧查询' }] }); await oldRequest;
  assert.equal(schools.data.schools[0].name, '新查询'); assert.equal(schools.data.loading, false);
});
test('recommendation submits once, persists the exact submitted profile and opens results', async () => {
  account(); const recommendation = page('recommend'); recommendation.onShow(); recommendation.setData({ form: validForm() });
  const submit = recommendation.submit(); await recommendation.submit(); assert.equal(requests.length, 1);
  assert.equal(requests[0].data.subject_type, '物理类');
  respond(0, { code: 0, data: { recommendations: { 冲: [], 稳: [], 保: [] } } }); await submit;
  assert.equal(storage.read('result').profile.rank, 15000); assert.equal(recommendation.data.busy, false);
  assert.equal(navigation.at(-1), '/pages/results/index');
});
test('guest form survives logging in, but is cleared when logging out', () => {
  const recommendation = page('recommend'); recommendation.onShow(); recommendation.setData({ form: validForm() });
  account(); recommendation.onShow(); assert.equal(recommendation.data.form.score, '580');
  storage.clear(); recommendation.onShow(); assert.equal(recommendation.data.form.score, '');
});
test('school detail filters distinguish candidate province, subject and year', () => {
  const detail = page('school-detail');
  detail.setData({ provinces: ['全部生源地', '福建'], subjects: ['全部科类', '物理类', '历史类'], years: ['全部年份', '2025'], provinceIndex: 1, subjectIndex: 1, yearIndex: 1,
    admissions: [ { province: '福建', subject_type: '物理类', year: 2025 }, { province: '福建', subject_type: '历史类', year: 2025 }, { province: '浙江', subject_type: '物理类', year: 2025 }, { province: '福建', subject_type: '物理类', year: 2024 } ] });
  detail.filter(); assert.equal(detail.data.filtered.length, 1);
});
test('results choose the first nonempty group and retain backend explanations', () => {
  account(); storage.write('result', { profile: validForm(), generatedAt: Date.now(), result: { disclaimer: '演示', recommendations: { 冲: [], 稳: [{ school: { id: 2, name: '福州大学' }, major: { name: '软件工程', min_score: 580, min_rank: 16000 }, reason: '后端原始理由', gap: 1000 }], 保: [] } } });
  const results = page('results'); results.onShow(); assert.equal(results.data.active, 1); assert.equal(results.data.items[0].reason, '后端原始理由');
});
test('login return routes cannot navigate to arbitrary pages', () => {
  auth.finishLogin('https://example.com'); assert.equal(navigation.pop(), '/pages/home/index');
  auth.finishLogin('/pages/schools/index'); assert.equal(navigation.pop(), '/pages/schools/index');
  auth.finishLogin('/pages/school-detail/index?id=2'); assert.equal(navigation.pop(), '/pages/school-detail/index?id=2');
});
test('all declared pages, components and template event handlers exist', () => {
  const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
  const project = JSON.parse(fs.readFileSync(path.join(root, '../project.config.json'), 'utf8'));
  assert.equal(project.miniprogramRoot, 'miniprogram/');
  for (const tab of app.tabBar.list) assert.ok(app.pages.includes(tab.pagePath));
  const entries = [...app.pages, ...Object.values(app.usingComponents).map(value => value.slice(1)), 'custom-tab-bar/index'];
  for (const entry of entries) {
    for (const extension of ['.js', '.json', '.wxml', '.wxss']) assert.ok(fs.existsSync(path.join(root, entry + extension)), entry + extension);
    JSON.parse(fs.readFileSync(path.join(root, entry + '.json'), 'utf8'));
    const script = fs.readFileSync(path.join(root, entry + '.js'), 'utf8');
    new vm.Script(script);
    const template = fs.readFileSync(path.join(root, entry + '.wxml'), 'utf8');
    assert.ok(!/<(?:div|span|br|p|h1)\b/.test(template), entry + ' must use native WXML');
    for (const match of template.matchAll(/(?:bind|catch)(?:tap|input|change|confirm|retry|focus|blur|keyboardheightchange)="(\w+)"/g)) {
      assert.ok(new RegExp('\\b' + match[1] + '\\s*\\(').test(script), entry + ': missing handler ' + match[1]);
    }
  }
});

const conversationId = '2ea2ab57-8624-4b54-aeb9-64bc37228262';
const reply = (answer = '先了解你的科类和位次。', sources = []) => ({ code: 0, data: { answer, conversation_id: conversationId, sources } });
const inputChat = (chat, value) => chat.input({ detail: { value } });

test('chat uses a separate long timeout, preserves conversation IDs and blocks duplicate sends', async () => {
  account(); const chat = page('chat'); chat.onShow(); inputChat(chat, '  想了解计算机专业  ');
  const first = chat.send(); await chat.send();
  assert.equal(requests.length, 1); assert.equal(requests[0].timeout, 300000);
  assert.equal(requests[0].url.endsWith('/agent/chat'), true);
  assert.equal(requests[0].header.satoken, 'test-token');
  assert.equal(JSON.stringify(requests[0].data), JSON.stringify({ message: '想了解计算机专业' }));
  assert.equal(chat.data.busy, true);
  respond(0, reply()); await first;
  assert.equal(chat.data.messages.length, 2); assert.equal(chat.data.busy, false);
  inputChat(chat, '物理类，15000位'); const second = chat.send();
  assert.equal(requests[1].data.conversation_id, conversationId);
  respond(1, reply('可以进一步比较院校。')); await second;
  assert.equal(chat.data.messages.length, 4);
  const health = api.health(); assert.equal(requests[2].timeout, 15000);
  respond(2, { code: 0, data: { status: 'ok' } }); await health;
});

test('chat validates empty and oversized messages before requesting', async () => {
  account(); const chat = page('chat'); chat.onShow();
  for (const value of ['  ', 'a'.repeat(4001)]) {
    inputChat(chat, value); await chat.send(); assert.ok(chat.data.error);
  }
  assert.equal(requests.length, 0);
  inputChat(chat, 'a'.repeat(4000)); const pending = chat.send();
  assert.equal(requests.length, 1); respond(0, reply()); await pending;
});

test('chat failures restore the question, preserve history and distinguish expired sessions from missing schools', async () => {
  account(); const chat = page('chat'); chat.onShow(); inputChat(chat, '先聊聊');
  const first = chat.send(); respond(0, reply()); await first;
  inputChat(chat, '查学校'); const missing = chat.send();
  respond(1, { code: 404, message: '学校不存在' }, 404); await missing;
  assert.equal(chat.data.expired, false); assert.equal(chat.data.messages.length, 2);
  assert.equal(chat.data.draft, '查学校');
  const expired = chat.send(); respond(2, { code: 404, message: '会话不存在或已过期，请开始新会话' }, 404); await expired;
  assert.equal(chat.data.expired, true); await chat.send(); assert.equal(requests.length, 3);
  inputChat(chat, '修改后的问题'); assert.ok(chat.data.error);
  wx.showModal = options => options.success({ confirm: true }); chat.newChat();
  assert.equal(chat.data.messages.length, 0); assert.equal(chat.data.draft, '修改后的问题');
  const fresh = chat.send(); assert.equal(requests[3].data.conversation_id, undefined);
  respond(3, reply()); await fresh;
});

test('chat handles busy, throttled, unavailable and timeout responses without automatic retries', async () => {
  account(); const chat = page('chat'); chat.onShow();
  for (const status of [403, 409, 429, 502, 503, 504]) {
    inputChat(chat, '我的问题'); const index = requests.length; const pending = chat.send();
    respond(index, { code: status, message: 'server error' }, status); await pending;
    assert.equal(requests.length, index + 1); assert.equal(chat.data.busy, false);
    assert.equal(chat.data.draft, '我的问题'); assert.ok(chat.data.error);
    assert.equal(chat.data.messages.length, 0); assert.equal(chat.data.expired, false);
  }
  const pending = chat.send(); requests.at(-1).fail({ errMsg: 'request:fail timeout' }); await pending;
  assert.match(chat.data.error, /避免连续发送/); assert.equal(chat.data.draft, '我的问题');
});

test('invalid chat responses never add a fabricated answer or lose the question', async () => {
  account(); const chat = page('chat'); chat.onShow(); inputChat(chat, '帮我看看');
  const pending = chat.send(); respond(0, { code: 0, data: { answer: '', conversation_id: conversationId } }); await pending;
  assert.equal(chat.data.messages.length, 0); assert.equal(chat.data.draft, '帮我看看');
  assert.match(chat.data.error, /不完整/);
});

test('a chat response finishing after leaving the page updates a reopened consultation', async () => {
  account(); const original = page('chat'); original.onShow(); inputChat(original, '专业怎么选');
  const pending = original.send(); original.onHide(); original.onUnload();
  const reopened = page('chat'); reopened.onShow(); assert.equal(reopened.data.busy, true);
  await reopened.send(); assert.equal(requests.length, 1);
  respond(0, reply('一起梳理兴趣与目标。')); await pending;
  assert.equal(reopened.data.busy, false); assert.equal(reopened.data.messages[1].text, '一起梳理兴趣与目标。');
});

test('logout and account switches discard pending consultation replies and private drafts', async () => {
  account(); const original = page('chat'); original.onShow(); inputChat(original, '私人问题');
  const pending = original.send(); storage.clear();
  assert.equal(original.data.messages.length, 0); assert.equal(original.data.loggedIn, false);
  storage.write('session', { tokenName: 'satoken', tokenValue: 'other-account' });
  const other = page('chat'); other.onShow(); inputChat(other, '另一个账号的问题');
  respond(0, reply('旧账号回复')); await pending;
  assert.equal(other.data.messages.length, 0); assert.equal(other.data.draft, '另一个账号的问题');
  assert.equal(auth.session().tokenValue, 'other-account');
});

test('401 clears the consultation and login returns to the chat entry', async () => {
  account(); currentPages = [{ route: 'pages/chat/index' }];
  const chat = page('chat'); chat.onShow(); inputChat(chat, '提问');
  const pending = chat.send(); respond(0, { code: 401 }, 401); await pending;
  assert.equal(chat.data.messages.length, 0); assert.equal(chat.data.loggedIn, false);
  assert.equal(navigation.at(-1), '/pages/login/index?next=' + encodeURIComponent('/pages/chat/index'));
  auth.finishLogin('/pages/chat/index'); assert.equal(navigation.at(-1), '/pages/chat/index');
  auth.finishLogin('/pages/chat/index?arbitrary=true'); assert.equal(navigation.at(-1), '/pages/home/index');
});

test('profile and recommendation entries prepare editable drafts without sending personal data automatically', () => {
  account(); storage.write('profile', { ...validForm(), score: 0 });
  const chat = page('chat'); chat.onShow(); chat.useProfile();
  assert.match(chat.data.draft, /0分/); assert.match(chat.data.draft, /全省位次15000/);
  storage.write('result', { profile: { ...validForm(), rank: 23000 } });
  inputChat(chat, '');
  chat.onLoad({ from: 'results' }); chat.onShow();
  assert.match(chat.data.draft, /23000/); assert.equal(requests.length, 0);
  assert.equal(saved.has('xiangyuan.chat'), false);
});

test('consultation retains only the latest eight complete rounds and clears them for a new topic', async () => {
  account(); const chat = page('chat'); chat.onShow();
  for (let i = 0; i < 9; i++) {
    inputChat(chat, '问题' + i); const pending = chat.send(); respond(i, reply('回答' + i)); await pending;
  }
  assert.equal(chat.data.messages.length, 16); assert.equal(chat.data.messages[0].text, '问题1');
  wx.showModal = options => options.success({ confirm: false }); chat.newChat();
  assert.equal(chat.data.messages.length, 16);
  wx.showModal = options => options.success({ confirm: true }); chat.newChat();
  assert.equal(chat.data.messages.length, 0); assert.equal(chat.data.draft, '');
  inputChat(chat, '新话题'); const pending = chat.send(); assert.equal(requests[9].data.conversation_id, undefined);
  respond(9, reply()); await pending;
});

test('adding a profile or opening results preserves an existing draft and does not duplicate profile context', () => {
  account(); storage.write('profile', validForm()); storage.write('result', { profile: { ...validForm(), rank: 23000 } });
  const chat = page('chat'); chat.onShow(); inputChat(chat, '我希望学费少一些');
  chat.useProfile(); const draft = chat.data.draft;
  assert.match(draft, /学费少一些/); assert.match(draft, /全省位次15000/);
  chat.useProfile(); assert.equal(chat.data.draft, draft);
  chat.onLoad({ from: 'results' }); chat.onShow(); assert.equal(chat.data.draft, draft);
  inputChat(chat, '字'.repeat(3999)); chat.useProfile();
  assert.equal(chat.data.draft.length, 3999); assert.match(chat.data.error, /超过 4000/);
  assert.equal(requests.length, 0);
});

test('source details show all four tool shapes, original grouping, background content and empty records', async () => {
  account(); const chat = page('chat'); chat.onShow(); inputChat(chat, '请查数据');
  const recommendation = { category: '保', gap: -9999, school: { id: 2, name: '福州大学' }, major: { name: '软件工程', min_score: 580, min_rank: 16000 }, reason: '保持后端分组和理由' };
  const sources = [
    { tool: 'search_schools', data: [] },
    { tool: 'school_detail', data: { id: 2, name: '福州大学', admissions: [{ province: '福建', subject_type: '物理类', year: 2025, min_score: null, min_rank: 16000, major: { name: '软件工程' } }] } },
    { tool: 'recommend', data: { recommendations: { 冲: [], 稳: [], 保: [recommendation] }, disclaimer: '后端免责声明' } },
    { tool: 'read_skill_resource', data: { description: '研究背景', kind: 'background_reference', content: '# 背景\n不是实时政策。' } }
  ];
  const pending = chat.send(); respond(0, reply('回答', sources)); await pending;
  const messageId = chat.data.messages[1].id;
  assert.equal(chat.data.messages[1].sources.length, 4);
  assert.equal(chat.data.messages[1].sources[3].content, undefined);
  const details = page('chat-sources'); details.onLoad({ message: messageId }); details.onShow();
  assert.equal(details.data.available, true); assert.equal(details.data.referenceCount, 1);
  assert.equal(details.data.sources[0].schools.length, 0);
  assert.equal(details.data.sources[1].admissions[0].scoreText, '—');
  assert.equal(details.data.sources[1].admissions[0].rankText, '16,000');
  assert.equal(details.data.sources[2].groups[2].items[0].reason, recommendation.reason);
  assert.equal(details.data.sources[2].disclaimer, '后端免责声明');
  details.toggle({ currentTarget: { dataset: { index: 3 } } }); assert.equal(details.data.sources[3].open, true);
  details.detail({ currentTarget: { dataset: { id: 2 } } }); assert.equal(navigation.at(-1), '/pages/school-detail/index?id=2');
  storage.clear(); details.onShow(); assert.equal(details.data.available, false); assert.equal(details.data.sources.length, 0);
});

test('source pages handle missing snapshots without making network requests', () => {
  account(); const details = page('chat-sources'); details.onLoad({ message: 'missing' }); details.onShow();
  assert.equal(details.data.available, false); assert.equal(requests.length, 0);
  details.chat(); assert.equal(navigation.at(-1), '/pages/chat/index');
});

test('answer rendering treats HTML and links as inert native text while preserving headings and emphasis', () => {
  const { blocks } = require('../miniprogram/utils/answer');
  const result = blocks('# 标题\n- **重点** 与 `位次`\n<script>alert(1)</script>\n[链接](javascript:alert(1))');
  assert.equal(result[0].kind, 'heading'); assert.equal(result[1].kind, 'list');
  assert.equal(result[1].parts[0].kind, 'bold'); assert.equal(result[1].parts[2].kind, 'code');
  assert.equal(result[2].parts[0].text, '<script>alert(1)</script>');
  assert.equal(result[3].parts[0].text, '[链接](javascript:alert(1))');
  const template = fs.readFileSync(path.join(root, 'components/answer-text/index.wxml'), 'utf8');
  assert.equal(template.includes('rich-text'), false); assert.equal(template.includes('web-view'), false);
});
