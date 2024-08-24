<script setup>
import {java} from "@codemirror/lang-java";
import {php, phpLanguage} from "@codemirror/lang-php";
import {sql, MySQL} from "@codemirror/lang-sql";

import {oneDark} from "@codemirror/theme-one-dark";
import PFileTree from "@/components/p-file-tree.vue";
import {computed, nextTick, onMounted, ref} from "vue";
import {sftpApi} from "@/api/sftp";
import CodeMirror from 'vue-codemirror6';
import {useRoute, useRouter} from "vue-router";
import {Split} from "view-ui-plus";
import {message} from "ant-design-vue";
import {serverApi} from "@/api/server";
import PTerm from "@/components/p-term.vue";
import {json} from "@codemirror/lang-json";
import {css} from "@codemirror/lang-css";
import {html} from "@codemirror/lang-html";
import {xml} from "@codemirror/lang-xml";
import {markdown} from "@codemirror/lang-markdown";
import {python} from "@codemirror/lang-python";
import {javascript} from "@codemirror/lang-javascript";
import {SaveOutlined} from "@ant-design/icons-vue";
import {useBase64} from "@vueuse/core";

let route = useRoute()

let sessionId = ref(null)
let rootPath = ref('')
let content = ref('\n\n\n\n\n\n\n\n\n\n\n')
let serverPath = ref('')
let serverName = ref('')
let serverId = ref('')

let CodeMirrorRef = ref(null)

let currentServer = ref(null)
onMounted(async () => {
  sessionId.value = route.query.sessionId
  rootPath.value = route.query.rootPath
  serverPath.value = route.query.serverPath
  serverName.value = route.query.serverName
  serverId.value = route.query.serverId
  currentServer.value = await serverApi.get(serverId.value)
})

let extensions = ref([oneDark])
let currentFile = ref(null)
let loadContentSpinner = ref(false)
let saveContentSpinner = ref(false)

let originContent = ref('\n\n\n\n\n\n\n\n\n\n\n')
const openFileContent = async (file) => {
  loadContentSpinner.value = true
  try {
    currentFile.value = file
    let fileUrl = sftpApi.download({id: sessionId.value, remotePath: file.path})

    let response = await fetch(fileUrl)
    content.value = await response.text()
    originContent.value = content.value

  } catch (e) {

    console.error(e)
    message.error('打开文件失败')
  } finally {
    loadContentSpinner.value = false
    nextTick(() => {
      CodeMirrorRef.value.$el.onkeydown = function (e) {
        e = e || window.event;
        if ((e.which || e.keyCode) === 83 && (e.metaKey || e.ctrlKey)) {
          e.preventDefault ? e.preventDefault() : e.returnValue = false;
          saveFile()
        }
      }
    })
  }
}

const saveFile = async () => {
  if (!currentFile.value) {
    return
  }

  let uploadUrl = sftpApi.uploadFile({id: sessionId.value})
  let formData = new FormData()
  formData.append('file', new Blob([content.value], {type: 'text/plain'}))
  formData.append('remotePath', currentFile.value.path)
  saveContentSpinner.value = true
  try {
    let response = await fetch(uploadUrl, {
      method: 'POST',
      body: formData
    })
    if (response.ok) {
      message.success('保存成功')
    } else {
      message.error('保存失败，检查用户是否有权限')
    }
  } catch (e) {
    console.error(e)
    message.error('保存失败')
  } finally {
    saveContentSpinner.value = false
    // openFileContent(currentFile.value)
  }

}


let splitNum = ref(0.2)
let verticalSplitNum = ref(1)

const changeShowFileTree = () => {
  if (splitNum.value > 0) {
    splitNum.value = 0
  } else {
    splitNum.value = 0.2
  }
}

const changeShowTerminal = () => {
  if (isFirst.value) {
    isFirst.value = false
    verticalSplitNum.value = 0.8
    showTerminal.value = true
  } else {
    if (showTerminal.value) {
      verticalSplitNum.value = 1
      showTerminal.value = false
    } else {
      verticalSplitNum.value = 0.8
      showTerminal.value = true
    }
  }
}

const lang = computed(() => {
  if (!currentFile.value) {
    return java();
  }
  if (currentFile.value.path.endsWith('.java')) {
    return java()
  } else if (currentFile.value.path.endsWith('.php')) {
    return php({baseLanguage: phpLanguage})
  } else if (currentFile.value.path.endsWith('.sql')) {
    return sql({dialect: MySQL})
  } else if (currentFile.value.path.endsWith('.json')) {
    return json()
  } else if (currentFile.value.path.endsWith('.css')) {
    return css()
  } else if (currentFile.value.path.endsWith('.html')) {
    return html()
  } else if (currentFile.value.path.endsWith('.xml')) {
    return xml()
  } else if (currentFile.value.path.endsWith('.md')) {
    return markdown()
  } else if (currentFile.value.path.endsWith('.py')) {
    return python()
  } else if (currentFile.value.path.endsWith('.js')) {
    return javascript()
  }

  return java()
})

let isFirst = ref(true)
let showTerminal = ref(false)

let termRef = ref(null)

const reloadServer = () => {
  termRef.value.reload()
}

let termLoading = ref(false)

let editRootPathVisible = ref(false)

let EditRootPathRef = ref()
let editRootPathModel = ref('')
const handleEditRootPath = () => {
  editRootPathVisible.value = true
  editRootPathModel.value = rootPath.value

  nextTick(() => {
    EditRootPathRef.value.focus()
  })
}

let router = useRouter()
const handleEditRootPathSave = () => {
  editRootPathVisible.value = false
  router.push({name: route.name, query: {...route.query, rootPath: editRootPathModel.value}})
  rootPath.value = editRootPathModel.value
}

const previewFileContent = async () => {
  let url = "http://192.168.101.232:8012/onlinePreview?url="

  let fileUrl = `http://192.168.200.93:8080/api-admin/sftp/preview/${sessionId.value}?remotePath=${currentFile.value.path}&fullfilename=${sessionId.value+":"+currentFile.value.path}`
  console.log(fileUrl)
  let base = useBase64(fileUrl)
  let sign = await base.execute()
  console.log(sign.split(',')[1])

  window.open(`${url}${encodeURIComponent(sign.split(',')[1])}`)
}

const downloadFile = () => {
  let url = sftpApi.download({id: sessionId.value, remotePath: currentFile.value.path})
  window.open(url)
}

</script>

<template>
  <div class="editor-content-page">
    <div class="title">
      <span>
        {{ serverPath + '/' + serverName }}:<span class="root-path" v-if="!editRootPathVisible"
                                                  @click="handleEditRootPath">{{ rootPath }}</span>
        <a-input ref="EditRootPathRef" size="small" style="max-width: 40%;" v-model:value="editRootPathModel" v-else
                 @blur="handleEditRootPathSave" @keydown.enter="handleEditRootPathSave"/>
      </span>
    </div>
    <div class="content">
      <div class="left-bar">
        <svg @click="changeShowFileTree" t="1724057797251" class="icon" :class="{'icon-active':splitNum> 0}"
             viewBox="0 0 1024 1024" version="1.1"
             xmlns="http://www.w3.org/2000/svg" p-id="6059">
          <path
              d="M869.006462 880.667547 165.670715 880.667547c-3.466961-0.642636-6.928806-1.357927-10.416234-1.918699-42.908252-6.864338-79.71146-40.74396-90.024339-82.942037-0.99056-4.059456-1.697665-8.188496-2.533706-12.289907L62.696436 243.391333c0.454348-2.176572 0.899486-4.353145 1.367137-6.522554 9.824763-45.260833 37.551244-74.27361 80.996732-88.787162 6.653537-2.222621 13.728676-3.199878 20.61041-4.751211l242.86433 0c3.989871 2.605338 8.575306 4.606925 11.865235 7.90504 31.887244 31.969109 63.582107 64.123436 95.226828 96.341209 2.934842 2.990101 5.861498 4.300956 10.120498 4.29277 105.524358-0.119727 211.045647-0.11154 316.568982-0.041956 6.932899 0.008186 13.921058 0.318248 20.793582 1.216711 45.231157 5.960759 84.711334 41.614793 95.39772 85.936231 1.052982 4.370541 1.868557 8.801457 2.793626 13.201673l0 433.275008c-0.638543 3.282766-1.238201 6.577812-1.915629 9.846252-8.582469 41.289382-32.04995 69.310575-73.013921 81.557504C880.707968 878.558513 874.795305 879.426277 869.006462 880.667547L869.006462 880.667547zM516.616646 837.125869 853.686545 837.125869c2.587941 0 5.180999-0.005117 7.774057-0.077771 11.299347-0.294712 21.698185-3.586688 30.825053-10.19213 18.267039-13.229303 25.568329-31.782867 25.568329-53.806463-0.021489-136.480394 0.01228-272.951579-0.095167-409.43402-0.004093-5.431709-0.532119-11.04966-1.938142-16.281825-8.157797-30.494525-37.551244-52.244898-70.28374-52.250014-110.416785-0.030699-220.832548-0.063445-331.240123 0.090051-8.711406 0.013303-15.503089-2.715855-21.578458-8.916067-31.621185-32.252565-63.402005-64.345494-95.252411-96.365768-1.602498-1.606591-4.383844-2.888793-6.613628-2.892887-71.39505-0.150426-142.794194 0.197498-214.18822-0.261966-35.032888-0.221034-71.059406 31.486108-70.861908 71.172993 0.839111 170.476673 0.355087 340.953346 0.355087 511.428995 0 1.462305 0.01842 2.914376 0 4.370541-0.110517 14.575973 4.979408 27.259854 14.426571 38.131459 15.152095 17.408485 34.498722 25.341155 57.498552 25.326828C290.923394 837.077773 403.772578 837.125869 516.616646 837.125869L516.616646 837.125869zM516.616646 837.125869"
              fill="#272636" p-id="6060"></path>
        </svg>
        <svg @click="changeShowTerminal" t="1724057670644" class="icon" viewBox="0 0 1024 1024" version="1.1"
             xmlns="http://www.w3.org/2000/svg"
             p-id="4255">
          <path
              d="M89.6 806.4h844.8V217.6H89.6v588.8zM0 128h1024v768H0V128z m242.816 577.536L192 654.72l154.304-154.368L192 346.048l50.816-50.816L448 500.352l-205.184 205.184z m584.32 13.248H512V640h315.072v78.72z"
              fill="#262626" p-id="4256"></path>
        </svg>
      </div>
      <Split class="split-bar" v-model="verticalSplitNum" mode="vertical">
        <template #top>
          <div ref="CodeMirrorRef" class="code-container">
            <Split v-model="splitNum">
              <template #left>
                <div class="code-tree">
                  <a-menu
                      mode="inline"
                  >
                    <p-file-tree :initial-path="rootPath" :sftp-id="sessionId"
                                 @openFileInCodeEditor="openFileContent"></p-file-tree>
                  </a-menu>
                </div>
              </template>
              <template #right>
                <div  class="save-container">
                  <a-button v-if="currentFile" @click="downloadFile">下载</a-button>
                  <a-button type="primary" v-if="content !== originContent" @click="saveFile">保存</a-button>
                </div>
                <div class="code-content">
                  <a-spin :spinning="saveContentSpinner">
                    <a-spin :spinning="loadContentSpinner">
                      <code-mirror
                          basic
                          ref="CodeMirrorRef"
                          :tab="true"
                          v-if="!loadContentSpinner"
                          :lang="lang"
                          :extensions="extensions"
                          v-model:modelValue="content">
                      </code-mirror>
                      <div class="code-loading" v-else>
                      </div>
                    </a-spin>
                  </a-spin>
                </div>
              </template>
            </Split>
          </div>
        </template>
        <template #bottom>
          <div class="bottom-bar-container">
            <a-spin :spinning="termLoading" style="height: 100%;">
              <div class="bottom-bar" v-if="!isFirst" v-show="showTerminal">
                <p-term ref="termRef" v-if="currentServer" master-session-id="0" :server="currentServer"
                        :execCommand="`cd ${rootPath}`" v-model:loading="termLoading"></p-term>
                <reload-outlined @click="reloadServer" class="icon"/>
              </div>
            </a-spin>
          </div>
        </template>
      </Split>
    </div>
  </div>
</template>

<style scoped lang="less">

.editor-content-page {
  height: 100vh;
  width: 100%;
  overflow: hidden;

  .content {
    display: flex;
    height: calc(100vh - 44px);

    .left-bar {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      align-items: center;
      border-right: 1px solid #ccc;
      padding: 6px;

      .icon {
        color: #ccc;
        width: 32px;
        padding: 4px;
        border-radius: 4px;
      }

      .icon:hover {
        background-color: #ccc;
      }

      .icon-active {
        background-color: #ccc;
      }
    }

    .split-bar {
      height: 100%;
    }
  }

  .title {
    border-bottom: 1px solid #ccc;
    padding: 8px;
    font-size: 16px;
    font-weight: bold;
    height: 40px;

    .root-path {
      cursor: pointer;
    }
  }

  .code-container {
    display: flex;
    flex-direction: row;
    height: 100%;
    width: 100%;


    .code-tree {
      width: 100%;
      height: 100%;
      overflow: scroll;
    }

    .save-container{
      margin: 8px;
      display: flex;
      justify-content: space-between;
    }

    .code-content {
      width: 100%;
      height: calc(100% - 48px);
      overflow: scroll;
      position: revert;

      .code-loading {
        height: 50vh;
        width: 100%;
        background-color: #282c34;
      }

      .save-icon {
        position: absolute;
        z-index: 9999;
        top: 24px;
        right: 24px;
        color: #ff0000;
      }
    }
  }

  .bottom-bar-container {
    height: 100%;

    .ant-spin-nested-loading {
      height: 100%;

      :deep(.ant-spin-container) {
        height: 100%;

        .bottom-bar {
          height: 100%;
          border-top: 1px solid #ccc;
          padding: 8px;
          position: relative;

          .icon {
            position: absolute;
            top: 24px;
            right: 24px;
            mix-blend-mode: difference;
            color: #ccc;
            cursor: pointer;
            z-index: 1000;
          }
        }
      }
    }


  }

}

</style>
