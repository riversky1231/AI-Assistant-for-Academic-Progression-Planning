const { request } = require('../utils/request');
module.exports = {
  health: () => request('/health', 'GET', undefined, { public: true }),
  login: data => request('/auth/login', 'POST', data, { public: true }),
  logout: () => request('/auth/logout', 'POST'),
  me: () => request('/auth/me'),
  schools: (filters = {}) => {
    const data = { limit: 50 };
    if (filters.keyword && filters.keyword.trim()) data.keyword = filters.keyword.trim();
    if (filters.province) data.province = filters.province;
    if (filters.limit) data.limit = filters.limit;
    return request('/schools', 'GET', data);
  },
  school: id => request('/schools/' + encodeURIComponent(id)),
  recommend: data => request('/recommend', 'POST', data)
};
