<script setup>
import {computed, createVNode, defineExpose, defineProps, nextTick, onMounted, onUnmounted, ref, watch} from "vue";
import {sftpApi} from "@/api/sftp";
import {message, Modal} from "ant-design-vue";
import fileIcon from "@/assets/file-icon/dir.png";
import dirIcon from "@/assets/file-icon/file.png";
import _ from "lodash";
import {uploadFile} from "@/utils/File";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import {computedFileSize} from "@/components/tinymce/File";
import {useAutoAnimate} from "@formkit/auto-animate/vue";
import {useAuthStore} from "@shared/store/useAuthStore";

let authStore = useAuthStore()

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
const [parent, enable] = useAutoAnimate()

let currentPath = ref("/");
let currentPathEdit = ref("/")
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
    sessionId.value = await sftpApi.init({serverId: props.serverId, sessionId:authStore.session})
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

//按名字排序
const normalSort = (files) => {
  return _.sortBy(files, (file) => {
    return file.name
  })
}

const dateSort = (files) => {
  return _.orderBy(files, (file) => {
    return file.attributes.mtime
  }, ['desc'])
}

const typeSort = (files) => {
  return _.sortBy(files, (file) => {
    return file.attributes.type
  })
}

let sortFun = normalSort;

init()

const ls = async () => {
  spinning.value = true
  enable(false)
  searchValue.value = ''
  spinTip.value = "正在获取文件列表"
  page.value = 1
  try {
    currentFiles.value = sortFun(await sftpApi.ls({id: sessionId.value, remotePath: currentPath.value}))
    return true
  } catch (e) {
    message.error(e.message)
    console.error(e)
    return false
  } finally {
    spinning.value = false
    spinTip.value = ""
    nextTick(() => {
      enable(true)
    })
  }
}

const changeDir = async (path) => {
  if (path === currentPath.value) {
    // if (!editPathVisible.value) {
    //   startEditPath()
    // }
    return
  }

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
  const ele = document.createElement('a'); //新建一个a标签
  ele.setAttribute('href', sftpApi.download({id: sessionId.value, remotePath: currentPath.value + '/' + file.name}));
  ele.click();
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

 let modal =  Modal.confirm({
    title: '确定要删除吗?',
    icon: createVNode(ExclamationCircleOutlined),
    content: file.type === 'DIR' ? '你要删除的是一个文件夹，请小心行事！！!' : '',
    onOk() {
      return  rmApi({id: sessionId.value, remotePath: currentPath.value + '/' + file.name})
          .then(() => {
            message.success("删除成功")
            ls()
          })
          .catch((e) => {
            console.error(e)
            message.error(e.message)
          }).finally(() => {
            modal.destroy()
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

const handleClickFile = (file) => {
  if (file.attributes.type === 'REGULAR') {
    fileViewVisible.value = true
    fileView.value = file
    return
  }

  changeDir(currentPath.value + '/' + file.name)
}

watch(currentFiles, (files) => {
  searchFiles.value = files
})

let searchFiles = ref([])

let searchValue = ref('')

const search = _.debounce(async (value) => {
  if (!value.target.value) {
    searchFiles.value = currentFiles.value
    return
  }
  //过滤currentFiles
  searchFiles.value = currentFiles.value.filter(item => item.name.includes(value.target.value))
})

let fileViewVisible = ref(false)
let fileView = ref({})

//permissions转换为rwx-rwx-rwx格式
//permissions : ["USR_W", "USR_R", "OTH_R", "GRP_R"]
const permissionsToRwx = (permissions) => {
  let res = ''
  if (permissions.includes('USR_R')) {
    res += 'r'
  } else {
    res += '-'
  }
  if (permissions.includes('USR_W')) {
    res += 'w'
  } else {
    res += '-'
  }
  if (permissions.includes('USR_X')) {
    res += 'x'
  } else {
    res += '-'
  }
  if (permissions.includes('GRP_R')) {
    res += 'r'
  } else {
    res += '-'
  }
  if (permissions.includes('GRP_W')) {
    res += 'w'
  } else {
    res += '-'
  }
  if (permissions.includes('GRP_X')) {
    res += 'x'
  } else {
    res += '-'
  }
  if (permissions.includes('OTH_R')) {
    res += 'r'
  } else {
    res += '-'
  }
  if (permissions.includes('OTH_W')) {
    res += 'w'
  } else {
    res += '-'
  }
  if (permissions.includes('OTH_X')) {
    res += 'x'
  } else {
    res += '-'
  }

  //每三个字符加一个,，最后一个不加
  let temp = ''
  for (let i = 0; i < res.length; i++) {
    temp += res[i]
    if ((i + 1) % 3 === 0 && i !== res.length - 1) {
      temp += ','
    }
  }

  return temp
}

let sortTypeValue = ref('normal')

const handleChangeSort = (e) => {
  sortFun = normalSort
  if (e === 'date') {
    sortFun = dateSort
  } else if (e === 'type') {
    sortFun = typeSort
  }else if (e === 'size') {
    sortFun = (files) => {
      return _.orderBy(files, (file) => {
        return file.attributes.size
      }, ['desc'])
    }
  }

  currentFiles.value = sortFun(currentFiles.value)
}

//组件关闭时，关闭sftp连接
onUnmounted(async () => {
  if (sessionId.value) {
    await sftpApi.close({id: sessionId.value})
  }
})

let currentPageData = computed(() => {
  return searchFiles.value.slice((page.value - 1) * size.value, page.value * size.value)
})

let page = ref(1)
let size = ref(100)

let editPathVisible = ref(false)

const handleChangePath = () => {
  editPathVisible.value = false
  changeDir(currentPathEdit.value)
}

let editInput = ref(null)
const startEditPath = () => {
  editPathVisible.value = true
  currentPathEdit.value = currentPath.value
  nextTick(() => {
    editInput.value.focus()
  })
}

let width = ref("16.6%")

let sftpRootEl = ref(null)

onMounted(() => {
  //监测sftpRootEl的大小变化
  const observer = new ResizeObserver(() => {
    width.value = `${100 / Math.floor(sftpRootEl.value.clientWidth / 100)}%`
  })

  observer.observe(sftpRootEl.value)
})


</script>

<template>
  <div ref="sftpRootEl" class="sftp-root">
    <a-spin v-model:spinning="spinning" :tip="spinTip">
      <a-card :bordered="false" :hoverable="false">
        <template v-slot:title>
          <div class="head">
            <a-input ref="editInput" v-if="editPathVisible" v-model:value="currentPathEdit" @blur="handleChangePath"
                     @keyup.enter="handleChangePath"></a-input>
            <div v-else style="display: flex;align-items: center;padding-right: 48px" @click="startEditPath">
              <a-breadcrumb>
                <a-breadcrumb-item v-for="path in currentPathArray" :key="path.path" @click.stop="changeDir(path.path)">
                  <component :is="path.path===currentPath?'span':'a'" v-if="path.name!=='/'">{{ path.name }}</component>
                  <component :is="path.path===currentPath?'span':'a'" v-else>
                    <home-outlined/>
                  </component>
                </a-breadcrumb-item>
              </a-breadcrumb>
            </div>


            <div>
              <a-input-search class="ml10" style="width: 200px" @input="search"
                              v-model:value="searchValue"></a-input-search>
              <a-button class="ml10" @click="ls">刷新</a-button>
              <a-button class="ml10" @click="handleMkdir">新建目录</a-button>
              <a-button class="ml10" @click="handleUpload">上传</a-button>
              <a-select class="ml10" v-model:value="sortTypeValue" @change="handleChangeSort">
                <a-select-option value="normal">默认排序</a-select-option>
                <a-select-option value="date">按时间排序</a-select-option>
                <a-select-option value="type">按类型排序</a-select-option>
                <a-select-option value="size">按大小排序</a-select-option>
              </a-select>
            </div>

          </div>
        </template>
        <div ref="parent" style="overflow: hidden">
          <a-card-grid :style="{width: width}" :bordered="false" :hoverable="false" v-for="(file,index) in currentPageData" :key="file.name"
                       @click="handleClickFile(file)" :title="file.name">
            <a-dropdown :trigger="['contextmenu']">
              <div>
                <div>
                  <a-image class="icon" :preview="false" :src="fileIcon"
                           v-if="file.attributes.type==='DIRECTORY'"></a-image>
                  <svg v-else-if="file.attributes.type==='SYMLINK'" class="icon"
                       style="width:40%;height:40%;vertical-align: middle;fill: currentColor;overflow: hidden;"
                       viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="24103">
                    <path
                        d="M625.792 302.912V64L1024 482.112l-398.208 418.176V655.36C341.312 655.36 142.208 750.912 0 960c56.896-298.688 227.584-597.312 625.792-657.088z"
                        fill="#262626" p-id="24104"></path>
                  </svg>
                  <a-image class="icon" :preview="false" :src="dirIcon" v-else></a-image>
                </div>
                <div>
                  <a-input ref="renameInputs" @blur="confirmRename" @pressEnter="confirmRename"
                           v-if="currentRenameFileIndex===index" v-model:value="newFileName"
                           style="text-align: center"/>
                  <p v-else class="fileName">
                    {{ file.name }}
                  </p>
                </div>

                  <div class="size">
                    {{ computedFileSize(file.attributes.size) }}
                  </div>
                  <div class="time">
                    {{ $f.datetime(file.attributes.mtime * 1000) }}
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
        </div>
        <div v-if="searchFiles.length>50" style="margin-top: 16px;display: flex;flex-direction: row;justify-content: center">
          <a-pagination
              v-model:current="page"
              v-model:page-size="size"
              :total="searchFiles.length"
          />
        </div>
      </a-card>
    </a-spin>
    <a-modal v-model:visible="fileViewVisible" :title="fileView.name" ok-text="下载" @ok="handleDownload(fileView)"
             width="60%">
      <a-form>
        <a-form-item title="详细信息">
          <a-descriptions bordered>
            <a-descriptions-item label="名称" :span="2">{{ fileView.name }}</a-descriptions-item>
            <a-descriptions-item label="大小">{{ computedFileSize(fileView.attributes.size) }}</a-descriptions-item>
            <a-descriptions-item label="权限">{{
                permissionsToRwx(fileView.attributes.permissions)
              }}
            </a-descriptions-item>
            <a-descriptions-item label="用户">{{ fileView.attributes.uid }}</a-descriptions-item>
            <a-descriptions-item label="组">{{ fileView.attributes.gid }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{
                $f.datetime(fileView.attributes.atime * 1000)
              }}
            </a-descriptions-item>
            <a-descriptions-item label="修改时间">{{
                $f.datetime(fileView.attributes.mtime * 1000)
              }}
            </a-descriptions-item>
          </a-descriptions>
        </a-form-item>
      </a-form>
    </a-modal>
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

  //min-height: @height;

  :deep(.ant-card-body) {
    margin-top: 24px;
    //height: calc(@height - 180px);
    overflow: scroll;
  }


  :deep(.ant-card-grid) {
    box-shadow: none;

    width: calc(100% / 6);
    height: 150px;
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
    width: 100%;
    text-align: center;
    font-size: 14px;
    line-height: 1.5;
    color: var(--context_primary);
    max-width: 100%;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    overflow-wrap: break-word;
    margin-bottom: 2px;
    transition: color .3s ease;
    white-space: normal;
  }


    .time {
      color: rgba(0, 0, 0, .45);
      font-size: 12px;
      text-align: center;
    }

    .size {
      color: rgba(0, 0, 0, .45);
      font-size: 12px;
      text-align: center;
    }


}

.ml10 {
  margin-left: 10px;
}

.ml5 {
  margin-left: 5px;
}


</style>
