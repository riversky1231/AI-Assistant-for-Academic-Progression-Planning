const { blocks } = require('../../utils/answer');
Component({
  properties: { content: { type: String, value: '', observer(value) { this.setData({ blocks: blocks(value) }); } } },
  data: { blocks: [] }
});
