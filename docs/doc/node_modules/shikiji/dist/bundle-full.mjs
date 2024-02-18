import getWasm from 'shikiji/wasm';
export { default as getWasmInlined } from 'shikiji/wasm';
import { bundledLanguages } from './langs.mjs';
export { bundledLanguagesAlias, bundledLanguagesBase, bundledLanguagesInfo } from './langs.mjs';
import { bundledThemes } from './themes.mjs';
export { bundledThemesInfo } from './themes.mjs';
import { createdBundledHighlighter, createSingletonShorthands } from 'shikiji-core';
export * from 'shikiji-core';

const getHighlighter = /* @__PURE__ */ createdBundledHighlighter(
  bundledLanguages,
  bundledThemes,
  getWasm
);
const {
  codeToHtml,
  codeToHast,
  codeToThemedTokens,
  codeToTokensWithThemes,
  getSingletonHighlighter
} = /* @__PURE__ */ createSingletonShorthands(
  getHighlighter
);

export { bundledLanguages, bundledThemes, codeToHast, codeToHtml, codeToThemedTokens, codeToTokensWithThemes, getHighlighter, getSingletonHighlighter };
