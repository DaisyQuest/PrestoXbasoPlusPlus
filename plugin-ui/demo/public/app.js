import { buildAst, countAstNodes, describeAstNode, maxAstDepth } from "./ast.js";
import { formatText } from "./formatter.js";

const sourceInput = document.querySelector("#source");
const formattedOutput = document.querySelector("#formatted");
const diagnosticsList = document.querySelector("#diagnostics");
const astTree = document.querySelector("#ast-tree");
const astDiagnosticsList = document.querySelector("#ast-diagnostics");
const astCount = document.querySelector("#ast-count");
const astDepth = document.querySelector("#ast-depth");
const applyButton = document.querySelector("#apply");
const resetButton = document.querySelector("#reset");

const indentControl = document.querySelector("#indent");
const wrapControl = document.querySelector("#wrap");
const uppercaseControl = document.querySelector("#uppercase");
const preserveControl = document.querySelector("#preserve");

const TEMPLATE_SNIPPET = `function parse_input( cSource )
   local cResult := cSource
   if Empty( cResult )
      return ""
   endif
   return cResult
end`;

const readOptions = () => ({
  indentWidth: Number.parseInt(indentControl.value, 10),
  maxWidth: Number.parseInt(wrapControl.value, 10),
  uppercaseKeywords: uppercaseControl.checked,
  preserveEmptyLines: preserveControl.checked
});

const renderAstNode = (node) => {
  const item = document.createElement("li");
  item.className = `ast-node ast-${node.type.toLowerCase()}`;

  const label = document.createElement("div");
  label.className = "ast-label";

  const title = document.createElement("span");
  title.className = "ast-title";
  title.textContent = node.label;

  const meta = document.createElement("span");
  meta.className = "ast-label-meta";
  meta.textContent = describeAstNode(node);

  label.append(title, meta);

  if (node.children.length > 0) {
    const details = document.createElement("details");
    details.open = node.type === "Program" || node.children.length < 3;

    const summary = document.createElement("summary");
    summary.append(label);
    details.append(summary);

    const childrenList = document.createElement("ul");
    childrenList.className = "ast-children";
    node.children.forEach((child) => {
      childrenList.append(renderAstNode(child));
    });
    details.append(childrenList);

    if (node.unclosed) {
      const warning = document.createElement("div");
      warning.className = "ast-warning";
      warning.textContent = "Unclosed block";
      details.append(warning);
    }

    item.append(details);
  } else {
    item.append(label);
  }

  return item;
};

const updateOutput = () => {
  const result = formatText(sourceInput.value, readOptions());
  formattedOutput.textContent = result.formatted || "(formatter produced an empty result)";

  diagnosticsList.innerHTML = "";
  if (result.diagnostics.length === 0) {
    const item = document.createElement("li");
    item.textContent = "No diagnostics. Blocks look balanced.";
    diagnosticsList.append(item);
  } else {
    result.diagnostics.forEach((diagnostic) => {
      const item = document.createElement("li");
      item.textContent = diagnostic;
      diagnosticsList.append(item);
    });
  }

  const astResult = buildAst(sourceInput.value, { includeClosers: true });
  const nodeCount = Math.max(0, countAstNodes(astResult.root) - 1);
  const depthCount = Math.max(0, maxAstDepth(astResult.root) - 1);
  astCount.textContent = `${nodeCount}`;
  astDepth.textContent = `${depthCount}`;

  astTree.innerHTML = "";
  const rootList = document.createElement("ul");
  rootList.className = "ast-list";
  rootList.append(renderAstNode(astResult.root));
  astTree.append(rootList);

  astDiagnosticsList.innerHTML = "";
  if (astResult.diagnostics.length === 0) {
    const item = document.createElement("li");
    item.textContent = "AST is balanced. All blocks closed.";
    astDiagnosticsList.append(item);
  } else {
    astResult.diagnostics.forEach((diagnostic) => {
      const item = document.createElement("li");
      item.textContent = diagnostic;
      astDiagnosticsList.append(item);
    });
  }
};

applyButton.addEventListener("click", updateOutput);
sourceInput.addEventListener("input", updateOutput);

resetButton.addEventListener("click", () => {
  sourceInput.value = TEMPLATE_SNIPPET;
  indentControl.value = "2";
  wrapControl.value = "80";
  uppercaseControl.checked = true;
  preserveControl.checked = true;
  updateOutput();
});

sourceInput.value = TEMPLATE_SNIPPET;
updateOutput();
