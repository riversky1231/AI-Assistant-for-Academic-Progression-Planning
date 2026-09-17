Component({
  data: { selected: 0, tabs: [
    { path: '/pages/home/index', text: '首页', icon: 'home' },
    { path: '/pages/schools/index', text: '院校库', icon: 'school' },
    { path: '/pages/recommend/index', text: '志愿推荐', icon: 'plan' },
    { path: '/pages/mine/index', text: '我的', icon: 'user' }
  ] },
  methods: { switchTab(event) { wx.switchTab({ url: this.data.tabs[event.currentTarget.dataset.index].path }); } }
});
