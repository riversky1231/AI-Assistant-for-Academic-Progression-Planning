const { schoolView, number, resultGroups } = require('./planning');
function sourcesView(sources) {
  return (Array.isArray(sources) ? sources : []).map((source, index) => {
    const data = source.data;
    const base = { key: index, number: String(index + 1).padStart(2, '0'), open: false };
    if (source.tool === 'search_schools') {
      return Object.assign(base, { kind: 'schools', title: '院校查询', label: '院校数据库', schools: (Array.isArray(data) ? data : []).filter(Boolean).map(schoolView) });
    }
    if (source.tool === 'school_detail' && data) {
      return Object.assign(base, { kind: 'detail', title: '院校与历史录取', label: '院校数据库', school: schoolView(data), admissions: (data.admissions || []).map((item, key) => Object.assign({}, item, { key, rankText: number(item.min_rank), scoreText: number(item.min_score) })) });
    }
    if (source.tool === 'recommend' && data) {
      return Object.assign(base, { kind: 'recommend', title: '冲稳保推荐', label: '历史位次计算', groups: resultGroups(data), message: data.message || '', disclaimer: data.disclaimer || '基于历史数据的规则推荐，不代表录取概率或承诺。' });
    }
    if (source.tool === 'read_skill_resource' && data) {
      return Object.assign(base, { kind: 'reference', title: data.description || '分析背景资料', label: '背景参考', content: typeof data.content === 'string' ? data.content : '' });
    }
    return Object.assign(base, { kind: 'unknown', title: '其他咨询依据', label: '暂不支持展示' });
  });
}
module.exports = { sourcesView };
