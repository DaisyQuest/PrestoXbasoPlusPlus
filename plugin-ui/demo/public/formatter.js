const DEFAULT_KEYWORDS = [
  "function",
  "if",
  "else",
  "elseif",
  "endif",
  "for",
  "next",
  "do",
  "while",
  "end",
  "return",
  "class",
  "switch",
  "case",
  "default",
  "enddo",
  "endwhile",
  "endfor",
  "endswitch",
  "endclass"
];

const BLOCK_OPENERS = new Set([
  "function",
  "if",
  "elseif",
  "for",
  "do",
  "while",
  "class",
  "switch",
  "case"
]);

const BLOCK_CLOSERS = new Set([
  "end",
  "endif",
  "endfor",
  "enddo",
  "endwhile",
  "endclass",
  "endswitch",
  "next"
]);

const DIAGNOSTIC_LIMIT = 5;

export function formatText(
  input,
  {
    indentWidth = 2,
    uppercaseKeywords = true,
    trimTrailingWhitespace = true,
    preserveEmptyLines = true,
    maxWidth = 80,
    keywords = DEFAULT_KEYWORDS
  } = {}
) {
  const normalizedInput = input.replace(/\r\n/g, "\n");
  const lines = normalizedInput.split("\n");
  const diagnostics = [];
  const formattedLines = [];
  let indentLevel = 0;

  for (const rawLine of lines) {
    if (!preserveEmptyLines && rawLine.trim() === "") {
      continue;
    }

    const trimmedLine = rawLine.trim();
    if (trimmedLine === "") {
      formattedLines.push("");
      continue;
    }

    const firstToken = trimmedLine.split(/\s+/)[0]?.toLowerCase();

    if (BLOCK_CLOSERS.has(firstToken)) {
      if (indentLevel === 0) {
        diagnostics.push(
          `Unexpected block closer "${firstToken}" on line ${formattedLines.length + 1}.`
        );
      } else {
        indentLevel -= 1;
      }
    }

    let normalizedLine = trimmedLine.replace(/\s+/g, " ");
    if (uppercaseKeywords) {
      const keywordPattern = new RegExp(`\\b(${keywords.join("|")})\\b`, "gi");
      normalizedLine = normalizedLine.replace(keywordPattern, (match) => match.toUpperCase());
    }

    let indentedLine = `${" ".repeat(indentWidth * indentLevel)}${normalizedLine}`;
    if (trimTrailingWhitespace) {
      indentedLine = indentedLine.replace(/[ \t]+$/g, "");
    }

    const wrappedLines = wrapLine(indentedLine, maxWidth);
    formattedLines.push(...wrappedLines);

    if (BLOCK_OPENERS.has(firstToken)) {
      indentLevel += 1;
    }
  }

  if (indentLevel > 0) {
    diagnostics.push(`Unclosed block detected: ${indentLevel} level(s) still open.`);
  }

  return {
    formatted: formattedLines.join("\n"),
    diagnostics: diagnostics.slice(0, DIAGNOSTIC_LIMIT)
  };
}

export function wrapLine(line, maxWidth) {
  if (!maxWidth || maxWidth <= 0 || line.length <= maxWidth) {
    return [line];
  }

  const indentMatch = line.match(/^\s*/);
  const indent = indentMatch ? indentMatch[0] : "";
  const words = line.trim().split(/\s+/);
  const wrappedLines = [];
  let currentLine = indent;

  for (const word of words) {
    const candidate = currentLine.trim().length
      ? `${currentLine.trim()} ${word}`
      : word;
    const candidateWithIndent = `${indent}${candidate}`;

    if (candidateWithIndent.length > maxWidth && currentLine.trim().length) {
      wrappedLines.push(currentLine.trim().length ? `${indent}${currentLine.trim()}` : indent);
      currentLine = word;
    } else {
      currentLine = candidate;
    }
  }

  wrappedLines.push(currentLine.trim().length ? `${indent}${currentLine.trim()}` : indent);
  return wrappedLines;
}
