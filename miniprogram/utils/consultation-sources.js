// 把后端返回的 sources（本轮实际执行的工具及结果）按工具类型转换成可展示视图。
const { schoolView, number, resultGroups } = require('./planning');
function sourcesView(sources) {
  return (Array.isArray(sources) ? sources : []).map((source, index) => {
    const data = source.data;
    const base = { key: index, number: String(index + 1).padStart(2, '0'), open: false };
    // 院校查询：渲染院校列表
    if (source.tool === 'search_schools') {
      return Object.assign(base, { kind: 'schools', title: '院校查询', label: '院校数据库', schools: (Array.isArray(data) ? data : []).filter(Boolean).map(schoolView) });
    }
    // 院校详情：渲染院校 + 各专业历史录取
    if (source.tool === 'school_detail' && data) {
      return Object.assign(base, { kind: 'detail', title: '院校与历史录取', label: '院校数据库', school: schoolView(data), admissions: (data.admissions || []).map((item, key) => Object.assign({}, item, { key, rankText: number(item.min_rank), scoreText: number(item.min_score) })) });
    }
    // 冲稳保推荐：复用结果分组视图
    if (source.tool === 'recommend' && data) {
      return Object.assign(base, { kind: 'recommend', title: '冲稳保推荐', label: '历史位次计算', groups: resultGroups(data), message: data.message || '', disclaimer: data.disclaimer || '基于历史数据的规则推荐，不代表录取概率或承诺。' });
    }
    // 背景资料：仅作参考，明确标注不当作实时数据
    if (source.tool === 'read_skill_resource' && data) {
      return Object.assign(base, { kind: 'reference', title: data.description || '分析背景资料', label: '背景参考', content: typeof data.content === 'string' ? data.content : '' });
    }
    return Object.assign(base, { kind: 'unknown', title: '其他咨询依据', label: '暂不支持展示' });
  });
}
module.exports = { sourcesView };
