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
  const lines = String(value || '').replace(/\r\n?/g, '\n').split('\n');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (/^\s*```/.test(line)) { code = !code; continue; }
    if (!line.trim()) continue;
    const header = !code && tableCells(line);
    const separator = !code && tableCells(lines[i + 1] || '');
    if (header && separator && header.length === separator.length && separator.every(cell => /^:?-{3,}:?$/.test(cell))) {
      const alignments = separator.map(cell => cell.startsWith(':') && cell.endsWith(':') ? 'center' : cell.endsWith(':') ? 'right' : 'left');
      const row = (cells, key) => ({ key, cells: header.map((_, column) => ({
        key: column, align: alignments[column], parts: inline(cells[column] || '')
      })) });
      const table = { key: result.length, kind: 'table', header: row(header, 0).cells, rows: [], width: header.length * 220 };
      i++;
      while (i + 1 < lines.length) {
        const cells = tableCells(lines[i + 1]);
        // Keep malformed rows visible as text instead of silently dropping extra cells.
        if (!cells || cells.length > header.length) break;
        table.rows.push(row(cells, table.rows.length));
        i++;
      }
      result.push(table);
      continue;
    }
    const heading = !code && /^#{1,6}\s+(.+)$/.exec(line);
    const list = !code && /^\s*(?:[-*+] |\d+[.)] )(.+)$/.exec(line);
    const quote = !code && /^>\s?(.*)$/.exec(line);
    const text = heading ? heading[1] : list ? list[1] : quote ? quote[1] : line;
    result.push({ key: result.length, kind: code ? 'pre' : heading ? 'heading' : list ? 'list' : quote ? 'quote' : 'paragraph', parts: code ? [{ key: 0, kind: 'plain', text }] : inline(text) });
  }
  return result;
}

function tableCells(line) {
  const text = line.trim();
  if (!text || /^```/.test(text)) return null;
  const cells = [];
  let cell = '', codeDelimiter = '', pipes = 0;
  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    if (char === '\\' && (text[i + 1] === '|' || text[i + 1] === '\\')) {
      cell += text[++i];
    } else if (char === '`') {
      let run = '`';
      while (text[i + 1] === '`') { run += '`'; i++; }
      if (!codeDelimiter) codeDelimiter = run;
      else if (codeDelimiter === run) codeDelimiter = '';
      cell += run;
    } else if (char === '|' && !codeDelimiter) {
      cells.push(cell.trim()); cell = ''; pipes++;
    } else cell += char;
  }
  if (!pipes) return null;
  cells.push(cell.trim());
  if (text.startsWith('|')) cells.shift();
  if (cells[cells.length - 1] === '' && text.endsWith('|')) cells.pop();
  return cells.length ? cells : null;
}
module.exports = { blocks };
