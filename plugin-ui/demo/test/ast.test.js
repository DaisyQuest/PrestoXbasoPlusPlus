import assert from "node:assert/strict";
import test from "node:test";
import { buildAst, countAstNodes, describeAstNode, maxAstDepth } from "../public/ast.js";

test("buildAst returns an empty root for blank input", () => {
  const result = buildAst("");
  assert.equal(result.diagnostics.length, 0);
  assert.equal(result.root.children.length, 0);
  assert.equal(countAstNodes(result.root), 1);
  assert.equal(maxAstDepth(result.root), 1);
  assert.equal(describeAstNode(result.root), "L1");
});

test("buildAst captures blocks, statements, and closers", () => {
  const input = "function demo\nreturn x\nend";
  const result = buildAst(input, { includeClosers: true });
  const block = result.root.children[0];

  assert.equal(result.diagnostics.length, 0);
  assert.equal(block.type, "Block");
  assert.equal(block.label, "FUNCTION demo");
  assert.equal(block.children.length, 2);
  assert.equal(block.children[0].type, "Statement");
  assert.equal(block.children[1].type, "Closer");
  assert.equal(block.endLine, 3);
  assert.equal(describeAstNode(block), "L1-L3");
});

test("buildAst records unexpected closers", () => {
  const input = "endif\nreturn x";
  const result = buildAst(input);

  assert.equal(result.diagnostics.length, 1);
  assert.match(result.diagnostics[0], /Unexpected closer/);
  assert.equal(result.root.children[0].type, "UnexpectedCloser");
  assert.equal(result.root.children[1].type, "Statement");
});

test("buildAst records unclosed blocks", () => {
  const input = "function demo\nif x\nreturn x";
  const result = buildAst(input);
  const functionNode = result.root.children[0];
  const ifNode = functionNode.children[0];

  assert.equal(result.diagnostics.length, 1);
  assert.match(result.diagnostics[0], /Unclosed block detected/);
  assert.ok(functionNode.unclosed);
  assert.ok(ifNode.unclosed);
});

test("countAstNodes and maxAstDepth handle null nodes", () => {
  assert.equal(countAstNodes(null), 0);
  assert.equal(maxAstDepth(null), 0);
  assert.equal(describeAstNode(null), "L?");
});
