import { formatText } from "./formatter.js";

const sourceInput = document.querySelector("#source");
const formattedOutput = document.querySelector("#formatted");
const diagnosticsList = document.querySelector("#diagnostics");
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
