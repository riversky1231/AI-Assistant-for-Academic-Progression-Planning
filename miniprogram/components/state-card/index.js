Component({ properties: { loading: Boolean, title: String, description: String, action: String }, methods: { retry() { this.triggerEvent('retry'); } } });
