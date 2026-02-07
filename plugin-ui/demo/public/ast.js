import { BLOCK_CLOSERS, BLOCK_OPENERS } from "./language.js";

const DEFAULT_ROOT_LABEL = "Program";

const createNode = ({ type, label, line, token = null, content = null }) => ({
  type,
  label,
  line,
  token,
  content,
  endLine: null,
  unclosed: false,
  children: []
});

const formatNodeLabel = (token, trimmedLine) => {
  const remainder = trimmedLine.slice(token.length).trim();
  return remainder ? `${token.toUpperCase()} ${remainder}` : token.toUpperCase();
};

export const buildAst = (input, { includeClosers = false } = {}) => {
  const normalizedInput = input.replace(/\r\n/g, "\n");
  const lines = normalizedInput.split("\n");
  const root = createNode({
    type: "Program",
    label: DEFAULT_ROOT_LABEL,
    line: lines.length ? 1 : 0
  });
  const diagnostics = [];
  const stack = [root];

  lines.forEach((rawLine, index) => {
    const trimmedLine = rawLine.trim();
    if (!trimmedLine) {
      return;
    }

    const lineNumber = index + 1;
    const firstToken = trimmedLine.split(/\s+/)[0]?.toLowerCase();

    if (!firstToken) {
      return;
    }

    if (BLOCK_CLOSERS.has(firstToken)) {
      if (stack.length === 1) {
        diagnostics.push(`Unexpected closer "${firstToken}" on line ${lineNumber}.`);
        stack[0].children.push(
          createNode({
            type: "UnexpectedCloser",
            label: `${firstToken.toUpperCase()} (unexpected)`,
            line: lineNumber,
            token: firstToken,
            content: trimmedLine
          })
        );
        return;
      }

      const closingNode = stack.pop();
      closingNode.endLine = lineNumber;
      closingNode.closer = firstToken;

      if (includeClosers) {
        closingNode.children.push(
          createNode({
            type: "Closer",
            label: firstToken.toUpperCase(),
            line: lineNumber,
            token: firstToken,
            content: trimmedLine
          })
        );
      }
      return;
    }

    if (BLOCK_OPENERS.has(firstToken)) {
      const node = createNode({
        type: "Block",
        label: formatNodeLabel(firstToken, trimmedLine),
        line: lineNumber,
        token: firstToken,
        content: trimmedLine
      });
      stack[stack.length - 1].children.push(node);
      stack.push(node);
      return;
    }

    stack[stack.length - 1].children.push(
      createNode({
        type: "Statement",
        label: trimmedLine,
        line: lineNumber,
        content: trimmedLine
      })
    );
  });

  if (stack.length > 1) {
    diagnostics.push(
      `Unclosed block detected: ${stack.length - 1} open block(s) remaining.`
    );
    while (stack.length > 1) {
      const openNode = stack.pop();
      openNode.unclosed = true;
    }
  }

  return { root, diagnostics };
};

export const countAstNodes = (node) => {
  if (!node) {
    return 0;
  }

  return node.children.reduce((total, child) => total + countAstNodes(child), 1);
};

export const maxAstDepth = (node) => {
  if (!node) {
    return 0;
  }

  if (node.children.length === 0) {
    return 1;
  }

  const childDepths = node.children.map((child) => maxAstDepth(child));
  return 1 + Math.max(...childDepths);
};

export const describeAstNode = (node) => {
  if (!node) {
    return "L?";
  }

  if (node.endLine && node.endLine !== node.line) {
    return `L${node.line}-L${node.endLine}`;
  }

  if (node.line) {
    return `L${node.line}`;
  }

  return "L?";
};
