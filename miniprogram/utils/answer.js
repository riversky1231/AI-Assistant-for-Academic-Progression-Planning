// Render a small Markdown subset as native text, never as executable HTML or links.
function inline(text) {
  return text.split(/(\*\*[^*\n]+\*\*|`[^`\n]+`)/g).filter(Boolean).map((value, index) => ({
    key: index, kind: value.startsWith('**') ? 'bold' : value.startsWith('`') ? 'code' : 'plain',
    text: value.startsWith('**') ? value.slice(2, -2) : value.startsWith('`') ? value.slice(1, -1) : value
  }));
}
function blocks(value) {
  const result = [];
  let code = false;
  String(value || '').replace(/\r\n/g, '\n').split('\n').forEach(line => {
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
