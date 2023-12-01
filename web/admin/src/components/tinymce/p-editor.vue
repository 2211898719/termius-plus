<template>
  <div style="border: 1px solid #ccc">
    <Toolbar
        style="border-bottom: 1px solid #ccc"
        :editor="editorRef"
        :defaultConfig="toolbarConfig"
        :mode="mode"
    />
    <Editor
        style="height: auto;min-height: 200px; overflow-y: hidden;"
        :defaultConfig="editorConfig"
        :mode="mode"
        v-model="currentValue"
        @onCreated="handleCreated"
        @onChange="handleChange"
    />
  </div>
</template>

<script setup>

import {defineEmits, defineProps, onBeforeUnmount, onUpdated, ref, shallowRef} from 'vue'
import {Editor, Toolbar} from '@wangeditor/editor-for-vue'
import {upload} from "@/utils/File";
import {fileApi} from "@/api/file";

const props = defineProps({
  value: {
    type: String, default: ""
  }
})

let currentValue = ref('')

onUpdated(() => {
  if (props.value) {
    currentValue.value = props.value
    // editorRef.value.setHtml(props.value)
  }
})

//
// watch(() => props.value, (val) => {
//   nextTick(() => {
//     currentValue.value = val
//   })
//
// })


// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef()

const mode = ref('default') // 编辑器模式，可选值：default、simple、classic

// 内容 HTML

const toolbarConfig = {
  excludeKeys: [
    'group-video'
  ]
}
const editorConfig = {
  placeholder: '请输入内容...', MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        let data = await upload(fileApi.upload(), file, {})
        let url = fileApi.getFile(data.uuid)
        let alt = file.name
        insertFn(url, alt, url)
      },
    }
  }
}


// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const handleCreated = (editor) => {
  editorRef.value = editor // 记录 editor 实例，重要！
}

const emit = defineEmits(['update:value', 'change'])


const handleChange = () => {
  if (currentValue.value) {
    emit("update:value", currentValue.value);
  }
}


</script>
