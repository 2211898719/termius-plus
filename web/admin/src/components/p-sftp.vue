<script setup>
import {computed, createVNode, defineExpose, defineProps, nextTick, onUnmounted, ref} from "vue";
import {sftpApi} from "@/api/sftp";
import {message, Modal} from "ant-design-vue";
import fileIcon from "@/assets/file-icon/dir.png";
import dirIcon from "@/assets/file-icon/file.png";
import _ from "lodash";
import {uploadFile} from "@/utils/File";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";

console.log(fileIcon)

const props = defineProps({
  serverId: {
    type: [Number], default: undefined,
  },
  operationId: {
    type: [String], default: undefined,
  },
});

let spinning = ref(false)
let spinTip = ref('')

let currentPath = ref("/");
let currentPathArray = computed(() => {
  let path = currentPath.value.split("/");
  path = path.filter(item => item !== "")
  let temp = "";

  let res = path.map(item => {
    temp += `/${item}`
    return {
      name: item,
      path: temp
    }
  })

  res.unshift({
    name: "/",
    path: "/"
  })

  return res
})
let currentFiles = ref([]);
let sessionId = ref("")

const init = async () => {
  spinning.value = true
  spinTip.value = "正在连接服务器"
  try {
    if (sessionId.value) {
      await sftpApi.close({id: sessionId.value})
    }
    sessionId.value = await sftpApi.init({id: props.serverId})
    currentPath.value = await sftpApi.pwd({id: sessionId.value})
  } catch (e) {
    console.error(e)
    message.error(e.message)
    return
  } finally {
    spinning.value = false
    spinTip.value = ""
  }

  await ls()
}

init()


const ls = async () => {
  spinning.value = true
  spinTip.value = "正在获取文件列表"
  try {
    currentFiles.value = await sftpApi.ls({id: sessionId.value, remotePath: currentPath.value})
    return true
  } catch (e) {
    message.error(e.message)
    console.error(e)
    return false
  } finally {
    spinning.value = false
    spinTip.value = ""
  }
}

const changeDir = async (path) => {
  let originPath = currentPath.value
  currentPath.value = path
  let res = await ls()
  if (!res) {
    currentPath.value = originPath
    await ls()
  }
}

const handleUpload = () => {
  uploadFile(sftpApi.upload({id: sessionId.value, remotePath: currentPath.value}), async (res, fileName) => {
    console.log(res)
    console.log(fileName)
    await ls()
  }, {remotePath: currentPath.value});
}


defineExpose({
  changeDir,
  init,
  serverId: props.serverId,
  operationId: props.operationId,
})

const handleDownload = (file) => {
  window.open(sftpApi.download({id: sessionId.value,remotePath: currentPath.value + '/' + file.name}))
}


let renameInputs = ref([])
let currentRenameFileIndex = ref(null)
let newFileName = ref('')
let renameFile = ref('')

const handleRename = async (file, index) => {
  renameFile.value = file
  newFileName.value = file.name
  currentRenameFileIndex.value = index
  await nextTick(() => {
    renameInputs.value.forEach(item => {
      item.focus()
    })
  })
}

const confirmRename = _.throttle(async () => {
  if (renameFile.value.name === newFileName.value) {
    currentRenameFileIndex.value = null
    return
  }

  try {
    await sftpApi.rename({
      id: sessionId.value,
      remotePath: currentPath.value + '/' + renameFile.value.name,
      newRemotePath: currentPath.value + '/' + newFileName.value
    })
  } catch (e) {
    console.error(e)
    message.error(e.message)
  }

  await ls()

  currentRenameFileIndex.value = null
}, 1000, {
  leading: true,
  trailing: false,
  maxWait: 1000,
})


const handleDel = (file) => {
  let rmApi = sftpApi.rm
  if (file.directory) {
    rmApi = sftpApi.rmDir
  }

  Modal.confirm({
    title: '确定要删除吗?',
    icon: createVNode(ExclamationCircleOutlined),
    content: file.type === 'DIR' ? '你要删除的是一个文件夹，请小心行事！！!' : '',
    onOk() {
      rmApi({id: sessionId.value, remotePath: currentPath.value + '/' + file.name})
          .then(() => {
            message.success("删除成功")
            ls()
          })
          .catch((e) => {
            console.error(e)
            message.error(e.message)
          })
    },
    onCancel() {
    },
  });
}

let mkdirVisible = ref(false)
let mkdirName = ref('')
const handleMkdir = () => {
  mkdirVisible.value = true
  mkdirName.value = ''
}
const mkdir = () => {
  if (!mkdirName.value) {
    message.error("请输入目录名")
    return
  }
  sftpApi.mkdir({id: sessionId.value, remotePath: currentPath.value + '/' + mkdirName.value})
      .then(() => {
        message.success("创建成功")
        ls()
      })
      .catch((e) => {
        console.error(e)
        message.error(e.message)
      })
  mkdirVisible.value = false
}


//组件关闭时，关闭sftp连接
onUnmounted(async () => {
  if (sessionId.value) {
    await sftpApi.close({id: sessionId.value})
  }
})

</script>

<template>
  <div class="sftp-root">
    <a-spin v-model:spinning="spinning" :tip="spinTip">
      <a-card :bordered="false" :hoverable="false">
        <template v-slot:title>
          <div class="head">
            <a-breadcrumb>
              <a-breadcrumb-item v-for="path in currentPathArray" :key="path.path" @click="changeDir(path.path)">
                <component :is="path.path===currentPath?'span':'a'" v-if="path.name!=='/'">{{ path.name }}</component>
                <component :is="path.path===currentPath?'span':'a'" v-else>
                  <home-outlined/>
                </component>
              </a-breadcrumb-item>
            </a-breadcrumb>
            <div>
              <a-button class="ml10" @click="ls">刷新</a-button>
              <a-button class="ml10" @click="handleMkdir">新建目录</a-button>
              <a-button class="ml10" @click="handleUpload">上传</a-button>
            </div>

          </div>
        </template>
        <a-card-grid :bordered="false" :hoverable="false" v-for="(file,index) in currentFiles" :key="file.name"
                     @dblclick="file.attributes.type==='REGULAR'?'':changeDir(currentPath+'/'+file.name)" :title="file.name">
          <a-dropdown :trigger="['contextmenu']">
            <div>
              <div>
                <a-image class="icon" :preview="false" :src="fileIcon" v-if="file.attributes.type==='DIRECTORY'"></a-image>
                <svg v-else-if="file.attributes.type==='SYMLINK'" class="icon"  style="vertical-align: middle;fill: currentColor;overflow: hidden;" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="24103"><path d="M625.792 302.912V64L1024 482.112l-398.208 418.176V655.36C341.312 655.36 142.208 750.912 0 960c56.896-298.688 227.584-597.312 625.792-657.088z" fill="#262626" p-id="24104"></path></svg>
                <a-image class="icon" :preview="false" :src="dirIcon" v-else></a-image>
              </div>
              <div>
                <a-input ref="renameInputs" @blur="confirmRename" @pressEnter="confirmRename"
                         v-if="currentRenameFileIndex===index" v-model:value="newFileName"
                         style="text-align: center"/>
                <p v-else class="fileName">{{ file.name }}</p>
              </div>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item v-if="!file.directory" key="4" @click="handleDownload(file)">
                  <cloud-download-outlined/>
                  下载
                </a-menu-item>
                <a-menu-item key="3" @click="handleRename(file,index)">
                  <edit-outlined/>
                  重命名
                </a-menu-item>

                <a-menu-item key="1" @click="handleDel(file)">
                  <DeleteOutlined/>
                  删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-card-grid>
      </a-card>
    </a-spin>
    <a-modal v-model:visible="mkdirVisible" title="新建目录" @ok="mkdir">
      <a-input v-model:value="mkdirName" placeholder="请输入目录名"/>
    </a-modal>
  </div>
</template>

<style scoped lang="less">
.sftp-root {
  .head {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center
  }

  min-height: 500px;

  :deep(.ant-card-body){
    margin-top: 24px;
    height: 400px;
    overflow: scroll;
  }
  :deep(.ant-card-grid) {
    box-shadow: none;
    width: 16.6%;
    height: 130px;
    text-align: center;
    padding: 8px;
    font-size: 16px;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .icon {
      width: 50%;
      height: 50%;
    }

    &:hover {
      transform: scale(1.1);
    }

  }

  .fileName {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.ml10 {
  margin-left: 10px;
}

.ml5 {
  margin-left: 5px;
}

</style>
