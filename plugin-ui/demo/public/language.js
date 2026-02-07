export const DEFAULT_KEYWORDS = [
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

export const BLOCK_OPENERS = new Set([
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

export const BLOCK_CLOSERS = new Set([
  "end",
  "endif",
  "endfor",
  "enddo",
  "endwhile",
  "endclass",
  "endswitch",
  "next"
]);
