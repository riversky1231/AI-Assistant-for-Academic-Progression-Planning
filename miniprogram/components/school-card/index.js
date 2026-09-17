Component({ properties: { school: Object }, methods: { open() { wx.navigateTo({ url: '/pages/school-detail/index?id=' + this.data.school.id }); } } });
