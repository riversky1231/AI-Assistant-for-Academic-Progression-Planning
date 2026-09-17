// 开发者工具使用本机地址；体验版、正式版需填写自己的 HTTPS 服务域名。
const BASE_URLS = {
  develop: 'http://192.168.43.80:8080',
  trial: '',
  release: ''
};

function getBaseUrl() {
  const version = wx.getAccountInfoSync().miniProgram.envVersion || 'develop';
  const url = BASE_URLS[version];
  if (!url) throw new Error('尚未配置当前版本的服务地址，请联系管理员');
  return url.replace(/\/+$/, '');
}

module.exports = { getBaseUrl };
