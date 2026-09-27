// Render a small Markdown subset as native text, never as executable HTML or links.
// 只渲染 Markdown 子集（粗体/行内代码/标题/列表/引用/代码块）为原生文本节点，
// 不渲染 HTML 和链接——因为模型输出不可信，这样做天然防 XSS。
// 把一行文本按「**粗体** 与 `行内代码`」切分成若干片段
function inline(text) {
  return text.split(/(\*\*[^*\n]+\*\*|`[^`\n]+`)/g).filter(Boolean).map((value, index) => ({
    key: index, kind: value.startsWith('**') ? 'bold' : value.startsWith('`') ? 'code' : 'plain',
    text: value.startsWith('**') ? value.slice(2, -2) : value.startsWith('`') ? value.slice(1, -1) : value
  }));
}
// 把整段回答按行解析成块级结构（代码块/标题/列表/引用/段落）
function blocks(value) {
  const result = [];
  let code = false;
  String(value || '').replace(/\r\n/g, '\n').split('\n').forEach(line => {
    // ``` 标记切换代码块状态
    if (/^\s*```/.test(line)) { code = !code; return; }
    if (!line.trim()) return;
    const heading = !code && /^#{1,6}\s+(.+)$/.exec(line);
    const list = !code && /^\s*(?:[-*+] |\d+[.)] )(.+)$/.exec(line);
    const quote = !code && /^>\s?(.*)$/.exec(line);
    const text = heading ? heading[1] : list ? list[1] : quote ? quote[1] : line;
    result.push({ key: result.length, kind: code ? 'pre' : heading ? 'heading' : list ? 'list' : quote ? 'quote' : 'paragraph', parts: code ? [{ key: 0, kind: 'plain', text }] : inline(text) });
  });
  return result;
}
module.exports = { blocks };
