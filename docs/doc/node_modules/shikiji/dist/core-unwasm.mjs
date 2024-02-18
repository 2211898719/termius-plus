import { setDefaultWasmLoader } from 'shikiji-core';
export * from 'shikiji-core';

setDefaultWasmLoader(() => import('shikiji/wasm'));
