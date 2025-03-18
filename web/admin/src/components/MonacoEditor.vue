<script setup>
import {defineEmits, defineProps, onBeforeUnmount, onMounted, ref, watch,} from "vue";
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
  fileNames: {
    type: String,
    default: "base",
  }
});

const emits = defineEmits(["update:value"]);

const editorContainer = ref(null);
let editorInstance = null;

let registerCopilots = [];

loader.config({ 'vs/nls': { availableLanguages: { '*': 'zh-cn' } } });
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

    let lanTypeMap = {
      'java': 'java',
      'php': 'php',
      'sql': 'mysql',
      'json': 'json',
      'css': 'css',
      'less': 'less',
      'scss': 'scss',
      'html': 'html',
      'xml': 'xml',
      'md': 'markdown',
      'py': 'python',
      'js': 'javascript',
      'twig': 'twig',
      'sh': 'shell',
      'ts': 'typescript',
      'yaml': 'yaml',
      'yml': 'yaml',
      'ini': 'ini',
    }
    Object.keys(lanTypeMap).map((key) => lanTypeMap[key]).forEach(e => {
      registerCopilots.push(
          registerCopilot(monaco, editorInstance, {
            endpoint: '/api-admin/ai/complete',
            language: e,
            filename: props.fileNames,
          })
      );
    })

    registerCopilots.push(
        registerCopilot(monaco, editorInstance, {
          endpoint: '/api-admin/ai/complete',
          language: 'plaintext',
          filename: props.fileNames,
        })
    );

    editorInstance.onDidChangeModelContent(() => {
      emits("update:value", editorInstance.getValue());
    });

    if (props.value){
      initValue(props.value);
    }

    if (props.language){
      initLanguage(props.language);
    }
  });
});

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.dispose();
  }
  if (registerCopilots){
    registerCopilots.forEach(e => e.deregister());
  }
});

function initLanguage(newLanguage) {
  if (editorInstance) {
    loader.init().then((monaco) => {
      monaco.editor.setModelLanguage(editorInstance.getModel(), newLanguage);
    });
  }
}

watch(
    () => props.language,
    (newLanguage) => {
      initLanguage(newLanguage);
    }
,{immediate: true});

function initValue(newValue) {
  if (editorInstance && editorInstance.getValue() !== newValue) {
    editorInstance.setValue(newValue);
  }
}

watch(
    () => props.value,
    (newValue) => {
      initValue(newValue);
    }
,{immediate: true});


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
