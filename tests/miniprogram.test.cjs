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
const account = () => storage.write('session', { tokenName: 'satoken', tokenValue: 'test-token', username: 'student' });
const respond = (index, data, statusCode = 200) => requests[index].success({ statusCode, data });
const validForm = () => ({ province: '福建', subject_type: '物理类', score: '580', rank: '15000', major_preference: '计算机', region_preference: '江浙沪' });
function page(name) {
  let definition;
  const file = path.join(root, 'pages', name, 'index.js');
  vm.runInNewContext(fs.readFileSync(file, 'utf8'), { Page: value => { definition = value; }, require: dependency => require(path.resolve(path.dirname(file), dependency)), wx: global.wx, getCurrentPages: global.getCurrentPages });
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
test('network timeout and unexpected success body reject instead of showing false results', async () => {
  let promise = api.health(); requests[0].fail({ errMsg: 'request:fail timeout' }); await assert.rejects(promise, /超时/);
  promise = api.health(); respond(1, '<html>proxy error</html>'); await assert.rejects(promise, /请求失败/);
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
    for (const match of template.matchAll(/(?:bind|catch)(?:tap|input|change|confirm|retry)="(\w+)"/g)) {
      assert.ok(new RegExp('\\b' + match[1] + '\\s*\\(').test(script), entry + ': missing handler ' + match[1]);
    }
  }
});
