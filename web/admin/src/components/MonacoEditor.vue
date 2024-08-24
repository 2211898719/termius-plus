<script setup>
import {
  ref,
  onMounted,
  onBeforeUnmount,
  watch,
  defineProps,
  defineEmits,
} from "vue";
import loader from "@monaco-editor/loader";
import {registerCopilot} from "monacopilot";

const props = defineProps({
  value: String,
  language: {
    type: String,
    default: "java",
  },
  theme: {
    type: String,
    default: "vs-dark",
  },
});

const emits = defineEmits(["update:value"]);

const editorContainer = ref(null);
let editorInstance = null;

onMounted(() => {
  loader.init().then((monaco) => {
    editorInstance = monaco.editor.create(editorContainer.value, {
      value: props.value || "",
      language: props.language,
      theme: props.theme,
      automaticLayout: true,
      dragAndDrop: false,
      scrollBeyondLastLine: true,
      cursorSmoothCaretAnimation: true,
      autoClosingBrackets: 'always', // 是否自动添加结束括号(包括中括号) "always" | "languageDefined" | "beforeWhitespace" | "never"
      autoClosingDelete: 'always', // 是否自动删除结束括号(包括中括号) "always" | "never" | "auto"
      autoClosingOvertype: 'always', // 是否关闭改写 即使用insert模式时是覆盖后面的文字还是不覆盖后面的文字 "always" | "never" | "auto"
      autoClosingQuotes: 'always', // 是否自动添加结束的单引号 双引号 "always" | "languageDefined" | "beforeWhitespace" | "never"
      // columnSelection: true,
      minimap: {
        enabled: false,
      },

      wordWrap: "on",


    });

    registerCopilot(monaco, editorInstance, {
      endpoint: '/api-admin/ai/complete',
      language: 'javascript',
    });
    registerCopilot(monaco, editorInstance, {
      endpoint: '/api-admin/ai/complete',
      language: 'java',
    });

    editorInstance.onDidChangeModelContent(() => {
      emits("update:value", editorInstance.getValue());
    });
  });
});

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.dispose();
  }
});

watch(
    () => props.language,
    (newLanguage) => {
      if (editorInstance) {
        loader.init().then((monaco) => {
          monaco.editor.setModelLanguage(editorInstance.getModel(), newLanguage);
        });
      }
    }
);

watch(
    () => props.value,
    (newValue) => {
      if (editorInstance && editorInstance.getValue() !== newValue) {
        editorInstance.setValue(newValue);
      }
    }
);
</script>

<template>
  <div ref="editorContainer" class="editor-container"></div>
</template>

<style>
.editor-container {
  width: 100%;
  height: 100%;
}
</style>
