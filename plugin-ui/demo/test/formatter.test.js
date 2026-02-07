import assert from "node:assert/strict";
import test from "node:test";
import { formatText, wrapLine } from "../public/formatter.js";

test("formatText returns empty output for empty input", () => {
  const result = formatText("");
  assert.equal(result.formatted, "");
  assert.deepEqual(result.diagnostics, []);
});

test("formatText normalizes indentation and uppercase keywords", () => {
  const input = "function demo\nif x\nreturn x\nendif\nend";
  const result = formatText(input, { indentWidth: 2, maxWidth: 120 });
  const expected = [
    "FUNCTION demo",
    "  IF x",
    "    RETURN x",
    "  ENDIF",
    "END"
  ].join("\n");
  assert.equal(result.formatted, expected);
  assert.deepEqual(result.diagnostics, []);
});

test("formatText can preserve or drop empty lines", () => {
  const input = "function demo\n\nreturn x\nend";
  const preserved = formatText(input, { preserveEmptyLines: true, maxWidth: 120 });
  assert.equal(preserved.formatted.split("\n").length, 4);

  const compact = formatText(input, { preserveEmptyLines: false, maxWidth: 120 });
  assert.equal(compact.formatted.split("\n").length, 3);
});

test("formatText reports unexpected closers and unclosed blocks", () => {
  const input = "endif\nfunction demo\nreturn 1";
  const result = formatText(input, { maxWidth: 120, uppercaseKeywords: false });
  assert.equal(result.diagnostics.length, 2);
  assert.match(result.diagnostics[0], /Unexpected block closer/);
  assert.match(result.diagnostics[1], /Unclosed block detected/);
});

test("formatText wraps long lines", () => {
  const input = "function demo\nreturn this is a long line for wrapping\nend";
  const result = formatText(input, { maxWidth: 30, indentWidth: 2 });
  const lines = result.formatted.split("\n");
  assert.ok(lines.some((line) => line.length <= 30));
});

test("wrapLine returns original line when width allows", () => {
  const line = "  short line";
  assert.deepEqual(wrapLine(line, 40), [line]);
});

test("wrapLine splits long lines with indentation", () => {
  const line = "  this line is too long to stay on one row";
  const wrapped = wrapLine(line, 20);
  assert.ok(wrapped.length > 1);
  wrapped.forEach((wrappedLine) => {
    assert.ok(wrappedLine.startsWith("  "));
  });
});
