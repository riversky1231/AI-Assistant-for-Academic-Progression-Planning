const { test } = require('node:test');
const assert = require('node:assert/strict');
const { blocks } = require('../miniprogram/utils/answer');

test('renders admissions table with headers, alignment and inline emphasis', () => {
  const parsed = blocks('建议如下：\r\n| 学校 | 分数 / 位次 | 位次差 |\r\n| :--- | :---: | ---: |\r\n| **宁波大学** | 587 / 19000 | -4000 |\r\n\r\n请核对招生简章。');
  assert.deepEqual(parsed.map(b => b.kind), ['paragraph', 'table', 'paragraph']);
  const table = parsed[1];
  assert.deepEqual(table.header.map(c => c.align), ['left', 'center', 'right']);
  assert.equal(table.rows[0].cells[0].parts[0].kind, 'bold');
  assert.equal(table.rows[0].cells[1].parts[0].text, '587 / 19000');
  assert.equal(table.rows[0].cells[2].parts[0].text, '-4000');
});

test('supports optional outer pipes, escaped pipes and code pipes', () => {
  const table = blocks('学校 | 备注\n--- | ---\n宁波\\|大学 | `a|b`')[0];
  assert.equal(table.kind, 'table');
  assert.equal(table.rows[0].cells[0].parts[0].text, '宁波|大学');
  assert.equal(table.rows[0].cells[1].parts[0].text, 'a|b');
  assert.equal(table.rows[0].cells[1].parts[0].kind, 'code');
});

test('keeps code fences, malformed tables and HTML inert', () => {
  assert.ok(blocks('```\n| A | B |\n| --- | --- |\n```').every(b => b.kind === 'pre'));
  assert.ok(blocks('| A | B |\n| --- |\n| C | D |').every(b => b.kind === 'paragraph'));
  const table = blocks('| A | B |\n| --- | --- |\n| <script>alert(1)</script> |')[0];
  assert.equal(table.rows[0].cells[0].parts[0].text, '<script>alert(1)</script>');
  assert.deepEqual(table.rows[0].cells[1].parts, []);
  const extra = blocks('| A | B |\n| --- | --- |\n| C | D | E |');
  assert.equal(extra[1].kind, 'paragraph');
});
