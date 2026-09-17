const provinces = ['北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江', '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南', '湖北', '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州', '云南', '西藏', '陕西', '甘肃', '青海', '宁夏', '新疆'];
const emptyProfile = () => ({ province: '', subject_type: '物理类', score: '', rank: '', major_preference: '', region_preference: '' });
function payload(form) {
  const province = String(form.province || '').trim();
  if (province.length < 2 || province.length > 20) throw new Error('请选择生源省份');
  if (!['物理类', '历史类'].includes(form.subject_type)) throw new Error('请选择物理类或历史类');
  const score = String(form.score).trim();
  const rank = String(form.rank).trim();
  if (!/^\d+$/.test(score) || Number(score) > 750) throw new Error('请输入 0–750 之间的整数分数');
  if (!/^\d+$/.test(rank) || Number(rank) < 1 || Number(rank) > 10000000) throw new Error('请输入 1–10000000 之间的整数位次');
  const major = String(form.major_preference || '').trim();
  const region = String(form.region_preference || '').trim();
  if (major.length > 50 || region.length > 50) throw new Error('专业或地区偏好不能超过 50 字');
  return { province, subject_type: form.subject_type, score: Number(score), rank: Number(rank), major_preference: major || null, region_preference: region || null };
}
function number(value) { return value === null || value === undefined ? '—' : String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ','); }
function schoolView(school) { return Object.assign({}, school, { initial: (school.name || '校').slice(0, 1) }); }
function resultGroups(result) {
  return ['冲', '稳', '保'].map((name, index) => ({
    name, tone: ['reach', 'steady', 'safe'][index], title: ['冲一冲', '稳一稳', '保一保'][index],
    items: ((result.recommendations || {})[name] || []).map((item, i) => Object.assign({}, item, {
      key: name + '-' + i, school: schoolView(item.school), rankText: number(item.major.min_rank),
      gapText: (item.gap > 0 ? '+' : '') + number(item.gap)
    }))
  }));
}
module.exports = { provinces, emptyProfile, payload, number, schoolView, resultGroups };
