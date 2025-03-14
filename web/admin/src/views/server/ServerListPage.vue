<script setup>
import {serverApi} from "@/api/server";
import {message, Modal} from "ant-design-vue";
import {
  computed,
  createVNode,
  defineEmits,
  defineExpose,
  nextTick,
  onMounted,
  onUnmounted,
  reactive,
  ref,
  watch
} from "vue";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import {findNodePath, walk} from "@/utils/treeUtil";
import {proxyApi} from "@/api/proxy";
import PSelect from "@/components/p-select.vue";
import PEditor from "@/components/tinymce/p-editor.vue";
import GroupCascader from "@/components/group-cascader.vue";
import {useForm} from "ant-design-vue/es/form";
import Sortable from "sortablejs";
import _, {isNumber} from "lodash";
import PEnumSelect from "@/components/p-enum-select.vue";
import OsEnum from "@/enums/OsEnum";
import {getSurname} from "@/utils/nameUtil";
import autoAnimate from "@formkit/auto-animate";
import {useRoute} from "vue-router";
import PCascader from "@/components/p-cascader.vue";

let termiusStyleColumn = ref(Math.floor(window.innerWidth / 300));


const resizeObserver = new ResizeObserver(() => {
  termiusStyleColumn.value = Math.floor(window.innerWidth / 300);
});

resizeObserver.observe(window.document.body);

let route = useRoute()

const emit = defineEmits(['openServer', 'proxyCreation', 'update:value', 'change'])

const props = defineProps({
  column: {
    type: Number, default: 4,
  },
  select: {
    type: Boolean, default: false,
  },
  value: {
    type: [Object, String, Number], default: null,
  }
})

const creationVisible = ref(false);
const creationType = ref('create');
let groupBreadcrumb = ref([{id: 0, isGroup: true, name: 'all'}])
const data = ref([{}]);
const creationState = reactive({
  name: "",
  ip: "",
  port: "22",
  password: "",
  key: "",
  os: "LINUX",
  proxyId: "",
  parentId: "",
  firstCommand: "",
  isGroup: false,
  username: "",
  remark: "<div></div>",
  autoSudo: true,
  infoTest: true,
  historyGet: true,
  isDb: false,
  keepAlive: true,
  proxyServerId: null,
  dbPort: []
});


const creationRules = computed(() => reactive({
  name: [
    {
      required: true,
      message: "请输入名称",
    },
    {
      min: 1,
      max: 30,
      message: "名称长度在1-30之间",
    }
  ],
  os: [
    {
      required: true,
      message: "请选择操作系统",
    }
  ],
  parentId: [
    {
      required: true,
      message: "请选择位置",
    }
  ],
  ip: [
    {
      required: !creationState.isGroup,
      message: "请输入host",
    },
    {
      pattern: /^(?:(?:[0-9]{1,3}\.){3}[0-9]{1,3}|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,})$/,
      message: "host格式不正确"
    }
  ],
  port: [
    {
      required: !creationState.isGroup,
      message: "请输入端口",
    },
    {
      pattern: /^\d+$/,
      message: "端口格式不正确"
    }
  ],
  username: [
    {
      required: !creationState.isGroup,
      message: "请输入用户名",
    }
  ],
}));

let currentData = ref(data.value)

let isHandleQuery = ref(false)
const handleQuery = () => {
  if (isHandleQuery.value || !route.query.connectId) {
    return
  }

  isHandleQuery.value = true

  findNodePath(data.value, parseInt(route.query.connectId)).forEach((id) => {
    walk(data.value, (node) => {
      if (node.id === id) {
        handleDblclick(node)
      }
    })
  })
}

const findServer = (server) => {
  let clickList = [{
    id: 0,
    isGroup: true,
    name: "all"
  }]
  findNodePath(data.value, server.id).forEach((id) => {
    walk(data.value, (node) => {
      if (node.id === id) {
        clickList.push(node)
      }
    })
  })

  for (let i = 1; i <= clickList.length; i++) {
    setTimeout(() => {
      if (clickList[i - 1].isGroup) {
        handleDblclick(clickList[i - 1])
      } else {
        //把元素变成:active状态
        let el = document.getElementById('serverCardRef' + server.id)
        el.classList.add('ant-card-active')
        setTimeout(() => {
          el.classList.remove('ant-card-active')
        }, 800)
      }
    }, i * 600)
  }
}

let {
  value: {
    resetFields: resetCreationFields,
    validate: validateCreation,
    validateInfos: creationValidations
  }
} = computed(() => useForm(creationState, creationRules))

watch(creationVisible, (visible) => {
  if (!visible) {
    resetCreationFields();
    netTestResult.value = null
    testNetLoading.value = false
  }
});

const handleAddServer = () => {
  creationVisible.value = true;
  creationState.isGroup = false;
  creationType.value = 'create'
  creationState.parentId = groupBreadcrumb.value[groupBreadcrumb.value.length - 1].id
}

const handleEditServer = (row) => {
  creationVisible.value = true;
  creationType.value = 'update'
  Object.assign(creationState, row)
  creationState.dbPort = row.dbPort.split(',')
}

const handleBreadcrumbData = (item) => {
  if (item.id === 0) {
    currentData.value = data.value
  } else {
    //遍历data找到这个item.id并
    walk(data.value, (node) => {
      if (node.id === item.id) {
        currentData.value = node.children
      }
    });
  }
}

let loading = ref(false)
const getServerList = async () => {
  loading.value = true
  try {
    let list = await serverApi.list()
    data.value.splice(0)
    data.value.push(...list)

    handleBreadcrumbData(groupBreadcrumb.value[groupBreadcrumb.value.length - 1])
    handleQuery()
  } finally {
    loading.value = false
  }

}

getServerList()

// let interval = setInterval(() => {
//   getServerList()
// }, 1000 * 120)
//
// onUnmounted(() => {
//   clearInterval(interval)
// })

const handleCopyCommandServer = (row) => {
  let command = `ssh ${row.username}@${row.ip} -p ${row.port} `
  if (row.proxy) {
    command += ` -o ProxyCommand="nc -X 5 -x ${row.proxy.ip}:${row.proxy.port} %h %p"`
  }

  copyToClipboard(command).then(() => {
    message.success("命令已复制到剪贴板")
    Modal.confirm({
      title: '命令已复制到剪贴板，点击确定复制密码',
      onOk() {
        copyToClipboard(row.password)
        message.success("密码已复制到剪贴板")
      },
    });
  })

}

const handleCopyServer = async (row) => {
  creationType.value = 'create'
  Object.assign(creationState, row)
  creationState.id = null
  creationState.name = creationState.name + '-复制'
  await serverApi[creationType.value](creationState);
  message.success("操作成功");
  await getServerList()
}
const handleDelServer = (item) => {

  let modal = Modal.confirm({
    title: '确定要删除吗?',
    icon: createVNode(ExclamationCircleOutlined),
    content: item.isGroup ? '删除组会丢失组下所有服务器信息！！' : '',
    onOk: async () => {
      try {
        await serverApi.del(item.id)
        await getServerList()
        message.success("操作成功");
      } catch (e) {
        modal.destroy()
      }
    },
    onCancel() {
    },
  });
}

let netTestResult = ref(null)
let testNetLoading = ref(false)

const testNet = async () => {
  testNetLoading.value = true
  let submitData = {...creationState}
  submitData.dbPort = new Set(submitData.dbPort)
  submitData.dbPort = [...submitData.dbPort].join(',')

  netTestResult.value = await serverApi.testServerParams(submitData)
  testNetLoading.value = false
}

const submitCreate = async () => {
  try {
    await validateCreation();
  } catch (error) {
    return;
  }

  let submitData = {...creationState}
  submitData.dbPort = new Set(submitData.dbPort)
  submitData.dbPort = [...submitData.dbPort].join(',')

  await serverApi[creationType.value](submitData);
  message.success("操作成功");

  creationVisible.value = false;
  await getServerList()
}

let sorting = ref(false)

const createSortEl = (el) => {
  if (el) {
    return Sortable.create(el, {
      group: {
        name: 'shared',
        pull: 'clone',
        put: 'true' // Do not allow items to be put into this list
      },
      sort: !searchText.value,
      scroll: true,
      dataIdAttr: 'id',
      sortElement: '.sortEl',
      dragClass: "sortable-drag",
      animation: 500,
      onStart: () => {
        sorting.value = true
        resetAutoAnimateAndSortable()
      },
      onEnd: handleChangeSort,
    });
  } else {
    console.log(el)
  }
}

const handleChangeSort = (evt) => {
  sorting.value = false
  resetAutoAnimateAndSortable()
  //根据evt.oldIndex和evt.newIndex来维护currentData.value
  let oldIndex = evt.oldIndex;
  let newIndex = evt.newIndex;

  let sortData = currentData.value
  let item = sortData[oldIndex];
  sortData.splice(oldIndex, 1);
  sortData.splice(newIndex, 0, item);

  updateSort(sortData)
}

const updateSort = _.debounce(async (sortData) => {
  await serverApi.updateSort(
      sortData.map(item => ({id: item.id, sort: item.sort}))
  )
}, 250, {'maxWait': 1000});

let multipleSelection = ref(new Set())


const handleSelect = (item) => {
  if (multipleSelection.value.has(item.id)) {
    multipleSelection.value.delete(item.id)
  } else {
    multipleSelection.value.add(item.id)
  }
}

const handleDblclick = (item, masterSessionId = 0) => {
  item.onlyUserVisible = false
  if (item.isGroup) {
    sorting.value = true
    resetAutoAnimateAndSortable()
    //维护面包屑
    let index = groupBreadcrumb.value.findIndex(i => i.id === item.id);
    if (index === -1) {
      groupBreadcrumb.value.push(item);
    } else {
      groupBreadcrumb.value = groupBreadcrumb.value.splice(0, index + 1)
    }

    //维护列表
    handleBreadcrumbData(item)
    nextTick(() => {
      sorting.value = false
      resetAutoAnimateAndSortable()
      if (!searchData.value.length && currentData.value.length) {
        searchText.value = ''
      }
    })
    return
  }

  if (props.select) {
    emit('update:value', item)
    emit('change', item)
    return
  }

  emit('openServer', {...item, path: groupBreadcrumb.value.slice(1).map(g => g.name).join("/")}, masterSessionId)
}

const handleDownloadWindowsRdp = (item) => {
  windowsInfoState.value = item
  windowsInfoVisible.value = true
}

const proxyCreation = () => {
  emit('proxyCreation')
}

const handleAddGroup = () => {
  creationVisible.value = true;
  creationState.isGroup = true
  creationType.value = 'create'
  creationState.parentId = groupBreadcrumb.value[groupBreadcrumb.value.length - 1].id
}


let proxyRef = ref()

const getProxyData = async () => {
  if (proxyRef.value) {
    await proxyRef.value.getData()
  }
}

const setProxyId = (id) => {
  creationState.proxyId = id
}

let windowsInfoVisible = ref(false)
let windowsInfoState = ref({})

function copyToClipboard(textToCopy) {
  // navigator clipboard 需要https等安全上下文
  if (navigator.clipboard && window.isSecureContext) {
    // navigator clipboard 向剪贴板写文本
    return navigator.clipboard.writeText(textToCopy);
  } else {
    // 创建text area
    let textArea = document.createElement("textarea");
    textArea.value = textToCopy;
    // 使text area不在viewport，同时设置不可见
    textArea.style.position = "absolute";
    textArea.style.opacity = 0;
    textArea.style.left = "-999999px";
    textArea.style.top = "-999999px";
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    return new Promise((res, rej) => {
      // 执行复制命令并移除文本框
      document.execCommand('copy') ? res() : rej();
      textArea.remove();
    });
  }
}

const downloadWindowsRdpConfig = (item) => {
  let ipAddress = item.ip;
  let port = item.port;
  let username = item.username;
  let password = item.password;

  let rdpConfig = "full address:s:" + ipAddress + ":" + port + "\nusername:s:" + username + "\npassword 51:b:" + btoa(password);

  // 创建Blob对象
  let blob = new Blob([rdpConfig], {type: "application/octet-stream"});

  // 创建下载链接
  let downloadLink = document.createElement("a");
  downloadLink.href = URL.createObjectURL(blob);
  downloadLink.download = item.name + ".rdp";

  // 添加到DOM并模拟点击下载
  document.body.appendChild(downloadLink);
  downloadLink.click();

  // 清理
  document.body.removeChild(downloadLink);
  URL.revokeObjectURL(downloadLink.href);

  copyToClipboard(item.password)
  message.success("密码已复制到剪贴板");
}

let searchText = ref('')

const list = ref()

const searchData = computed(() => {
  nextTick(() => {
    resetAutoAnimateAndSortable()
  })

  if (searchText.value) {
    changeSortEnable(false)
  } else {
    changeSortEnable(true)
  }

  if (!currentData.value?.length) {
    return []
  }

  return currentData.value.filter(item => searchLike(item, searchText.value))
})

const searchLike = (item, text) => {
  if (item.isGroup) {
    return item.name?.includes(text) || (item.children?.length && item.children.some(child => searchLike(child, text)))
  } else {
    return item.name?.includes(text) || item.remark?.includes(text) || item.ip?.includes(text)
  }
}

let sortContent = ref()

const resetAutoAnimateAndSortable = () => {
  let el = list?.value?.$el.getElementsByClassName("ant-row")[0];
  if (!el) {
    return
  }

  autoAnimate(el, {duration: sorting?.value ? 0 : 300})
  //使用 parentNode 避免重复创建，判断是否为游离节点
  if (!sortContent.value?.el?.parentNode) {
    sortContent.value = createSortEl(el)
  }
}

const changeSortEnable = (enable) => {
  if (sortContent.value) {
    sortContent.value.option("disabled", !enable)
  }
}

defineExpose({
  getProxyData,
  setProxyId,
  findServer
})

</script>

<template>
  <div class="server-root">
    <div class="server-pane">
      <a-space direction="vertical" size="middle" style="width: 100%;">
        <a-card :bodyStyle="{padding:'12px 12px'}" style="border:none">
          <div class="search">
            <a-input v-model:value="searchText" class="search-input" placeholder="搜索服务器"/>
          </div>
          <div class="body-root">
            <div style="display: flex;justify-content: space-between">
              <a-breadcrumb>
                <a-breadcrumb-item v-for="item in groupBreadcrumb" :key="item.id" @click="handleDblclick(item)">
                  <a>{{ item.name }}</a>
                </a-breadcrumb-item>
              </a-breadcrumb>

              <div>
                <a-button @click="getServerList" class="my-button">刷新</a-button>
                <a-button @click="handleAddServer" class="ml10 my-button">新增服务器</a-button>
                <a-button class="ml10 my-button" @click="handleAddGroup">新增分组</a-button>
              </div>
            </div>
            <div class="mt30 server">
              <a-list ref="list" :grid="{ gutter: 16, column: termiusStyleColumn }" :data-source="searchData"

                      row-key="id">
                <template #renderItem="{ item }">
                  <a-dropdown :trigger="['contextmenu']">
                    <a-list-item class="sortEl selected-active" @dblclick="handleDblclick(item)"
                                 @click="handleSelect(item)">
                      <template #actions>
                        <a-popover :visible="item.onlyUserVisible" v-if="item.onlyConnect?.length" @dblclick.stop
                                   @click.stop title="在线用户"
                                   placement="bottom"
                                   trigger="click">
                          <template #content>
                            <div class="onlyUser" v-for="only in item.onlyConnect" :key="only.user.username"
                                 @click.stop="handleDblclick(item,only.masterSessionId)">
                              <span>{{ only.user.username }}</span>
                              <double-right-outlined/>
                            </div>
                          </template>
                          <a-avatar class="avatar" :title="item.onlyConnect[0].user.username">
                            {{ getSurname(item.onlyConnect[0].user.username) }}
                          </a-avatar>
                        </a-popover>
                        <a>
                          <edit-outlined @click="handleEditServer(item)"/>
                        </a>
                      </template>

                      <a-badge-ribbon :text="item.onlyTag" :class="{none:!item.onlyTag}">
                        <a-card :id="'serverCardRef'+item.id">
                          <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                            <a-list-item-meta
                                :description="item.isGroup?('group '+(item.proxy?item.proxy.name:'')):('server '+item.username+'@'+item.ip+':'+item.port)"
                            >
                              <template #title>
                                <span>{{ item.name }}</span>
                              </template>
                              <template #avatar>
                                <ApartmentOutlined v-if="item.isGroup" class="icon-server"/>
                                <hdd-outlined v-else-if="item.os===OsEnum.LINUX.value" class="icon-server"
                                              style="color: #E45F2B;"/>
                                <windows-outlined v-else-if="item.os===OsEnum.WINDOWS.value" class="icon-server"
                                                  style="color: #E45F2B;"/>
                              </template>
                            </a-list-item-meta>
                          </a-skeleton>
                        </a-card>
                      </a-badge-ribbon>
                    </a-list-item>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item v-if="!item.isGroup" key="2" @click="handleDblclick(item)">
                          <link-outlined/>
                          连接
                        </a-menu-item>
                        <a-menu-item v-if="!item.isGroup&&item.os===OsEnum.WINDOWS.value" key="5"
                                     @click="handleDownloadWindowsRdp(item)">
                          <cloud-download-outlined/>
                          下载本地连接文件
                        </a-menu-item>
                        <a-menu-item key="4" @click="handleEditServer(item)">
                          <edit-outlined/>
                          修改
                        </a-menu-item>
                        <a-menu-item key="3" @click="handleCopyServer(item)">
                          <CopyOutlined/>
                          复制
                        </a-menu-item>
                        <a-menu-item key="5" v-if="!item.isGroup" @click="handleCopyCommandServer(item)">
                          <code-outlined/>
                          复制为ssh命令
                        </a-menu-item>

                        <a-menu-item key="1" @click="handleDelServer(item)">
                          <DeleteOutlined/>
                          删除
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </template>
              </a-list>
            </div>
          </div>
        </a-card>
      </a-space>
      <a-drawer
          v-model:visible="creationVisible"
          :title="creationType==='create'?'新增':'修改'"
          placement="right"
          width="80%"
          size="large"
      >
        <template #extra>
          <a-space>
            <span v-if="isNumber(netTestResult) ">
              <span v-if="netTestResult>0">
                延迟{{netTestResult}}ms
              </span>
              <span v-else>
                连接服务器失败
              </span>
            </span>

            <a-button type="primary" @click="testNet" :loading="testNetLoading">测试网络连接</a-button>
            <a-button @click="creationVisible = false">取消</a-button>
            <a-button type="primary" @click="submitCreate">提交</a-button>
          </a-space>
        </template>

        <a-form
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 18 }"
            autocomplete="off"
        >

          <a-form-item label="名称" v-bind="creationValidations.name">
            <a-input v-model:value="creationState.name"/>
          </a-form-item>
          <a-form-item label="位置" v-bind="creationValidations.parentId">
            <GroupCascader v-if="creationVisible"
                           :disabled="creationState.id"
                           v-model:value="creationState.parentId"></GroupCascader>
          </a-form-item>
          <template v-if="!creationState.isGroup">
            <a-form-item label="操作系统" v-bind="creationValidations.os">
              <p-enum-select :module="OsEnum" v-model:value="creationState.os"></p-enum-select>
            </a-form-item>
            <a-form-item label="host" v-bind="creationValidations.ip">
              <a-input v-model:value="creationState.ip"/>
            </a-form-item>

            <a-form-item label="端口" v-bind="creationValidations.port">
              <a-input v-model:value="creationState.port"/>
            </a-form-item>
            <a-form-item label="用户名" v-bind="creationValidations.username">
              <a-input v-model:value="creationState.username"/>
            </a-form-item>

            <a-form-item label="密码" v-bind="creationValidations.password">
              <a-input-password autocomplete="new-password" v-model:value="creationState.password"/>
            </a-form-item>

            <a-form-item label="连接时执行命令" v-bind="creationValidations.firstCommand">
              <a-textarea :auto-size="{ minRows: 2, maxRows: 5 }"
                          v-model:value="creationState.firstCommand"></a-textarea>
            </a-form-item>
            <a-form-item label="自动保活" v-bind="creationValidations.keepAlive">
              <a-switch v-model:checked="creationState.keepAlive"></a-switch>
              <p>某些服务器ssl版本过新可能会出现连接失败</p>
              <pre>strict KEX violation: unexpected packet type 2 (seqnr 1)</pre>
              <p>可以通过关闭该设置解决</p>
            </a-form-item>
            <a-form-item label="自动sudo" v-bind="creationValidations.aotoSudo">
              <a-switch v-model:checked="creationState.autoSudo"></a-switch>
            </a-form-item>
            <a-form-item label="加入监控看板" v-bind="creationValidations.infoTest">
              <a-switch v-model:checked="creationState.infoTest"></a-switch>
            </a-form-item>
            <a-form-item label="获取history" v-bind="creationValidations.historyGet">
              <a-switch v-model:checked="creationState.historyGet"></a-switch>
              <p>获取服务器的历史命令，以提供命令补全功能，需要服务器支持</p>
            </a-form-item>
            <a-form-item label="私钥" v-bind="creationValidations.key">
              <a-textarea v-model:value="creationState.key"></a-textarea>
            </a-form-item>
          </template>
          <a-form-item label="代理" v-bind="creationValidations.proxyId" v-if="!creationState.proxyServerId">
            <p-select ref="proxyRef" :api="proxyApi.list" v-model:value="creationState.proxyId"
                      :placeholder="creationState?.proxy?creationState.proxy.name:''"
                      style="width: 90%"></p-select>
            <a-button @click="proxyCreation" style="margin-left: 2%" type="primary" shape="circle">
              <template #icon>
                <plus-outlined/>
              </template>
            </a-button>
          </a-form-item>
          <a-form-item label="中转服务器" v-bind="creationValidations.proxyServerId">
            <p-cascader v-model:value="creationState.proxyServerId" :api="serverApi.list"></p-cascader>
            <p>使用某个服务器作为代理，访问目标服务器（优先级高于<a>代理</a>）</p>
          </a-form-item>

          <a-form-item label="备注" v-bind="creationValidations.remark">
            <p-editor v-model:value="creationState.remark"></p-editor>
          </a-form-item>
        </a-form>

      </a-drawer>

      <a-drawer
          v-model:visible="windowsInfoVisible"
          class="custom-class"
          style="color: red"
          title="windows服务器信息"
          placement="right"
          width="40%"
      >
        <a-form
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 18 }"
            autocomplete="off"
        >
          <a-form-item label="连接文件：">
            <a-button class="copy-btn" @click="downloadWindowsRdpConfig(windowsInfoState)">下载</a-button>
          </a-form-item>
          <a-form-item label="密码：">
            <a-input-password v-model:value="windowsInfoState.password"></a-input-password>
          </a-form-item>
        </a-form>
      </a-drawer>
    </div>
  </div>
</template>

<style scoped lang="less">
@import url('../css/termius');

.avatar {
  cursor: pointer;
  color: #f56a00;
  background-color: #fde3cf;
}

.ant-popover-inner-content {
  background-color: #f5f5f5;

  .onlyUser {
    cursor: pointer;
    padding: 4px 8px;
    display: flex;
    justify-content: space-between;
    align-items: center;


    &:hover {
      background-color: #e6f7ff;
    }
  }
}

/deep/ .none {
  display: none;
}
</style>
