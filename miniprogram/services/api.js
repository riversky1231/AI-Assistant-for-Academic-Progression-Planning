const { request } = require('../utils/request');
module.exports = {
  health: () => request('/health', 'GET', undefined, { public: true }),
  login: data => request('/auth/login', 'POST', data, { public: true }),
  register: data => request('/auth/register', 'POST', data, { public: true }),
  wechatLogin: data => request('/auth/wechat-login', 'POST', data, { public: true }),
  forgotPassword: data => request('/auth/forgot-password', 'POST', data, { public: true }),
  logout: () => request('/auth/logout', 'POST'),
  me: () => request('/auth/me'),
  profile: () => request('/auth/profile'),
  updateProfile: data => request('/auth/profile', 'PUT', data),
  changePassword: data => request('/auth/change-password', 'POST', data),
  users: () => request('/auth/users'),
  createUser: data => request('/auth/users', 'POST', data),
  updateUser: (id, data) => request('/auth/users/' + encodeURIComponent(id), 'PUT', data),
  resetUserPassword: (id, data) => request('/auth/users/' + encodeURIComponent(id) + '/reset-password', 'POST', data),
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
