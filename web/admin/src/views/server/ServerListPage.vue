<script setup>
import {serverApi} from "@/api/server";
import {message, Modal} from "ant-design-vue";
import {computed, createVNode, defineEmits, defineExpose, nextTick, onMounted, reactive, ref, watch} from "vue";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import {walk} from "@/utils/treeUtil";
import {proxyApi} from "@/api/proxy";
import PSelect from "@/components/p-select.vue";
import PEditor from "@/components/tinymce/p-editor.vue";
import GroupCascader from "@/components/group-cascader.vue";
import {useForm} from "ant-design-vue/es/form";
import Sortable from "sortablejs";
import _ from "lodash";
import PEnumSelect from "@/components/p-enum-select.vue";
import OsEnum from "@/enums /OsEnum";

const emit = defineEmits(['openServer', 'proxyCreation', 'update:value', 'change'])

const props = defineProps({
  column: {
    type: Number, default: 4,
  },
  select: {
    type: Boolean, default: false,
  },
  value: {
    type: [Object,String,Number], default: null,
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
  isDb: false,
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

const getServerList = async () => {
  let list = await serverApi.list()
  data.value.splice(0)
  data.value.push(...list)

  handleBreadcrumbData(groupBreadcrumb.value[groupBreadcrumb.value.length - 1])
}

getServerList()

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

  Modal.confirm({
    title: '确定要删除吗?',
    icon: createVNode(ExclamationCircleOutlined),
    content: item.isGroup ? '删除组会丢失组下所有服务器信息！！' : '',
    onOk: async () => {
      await serverApi.del(item.id)
      await getServerList()
      message.success("操作成功");
    },
    onCancel() {
    },
  });
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
  console.log(submitData)

  await serverApi[creationType.value](submitData);
  message.success("操作成功");

  creationVisible.value = false;
  await getServerList()
}

const createSortEl = (el) => {
  if (el) {
    return Sortable.create(el, {
      group: {
        name: 'shared',
        pull: 'clone',
        put: 'true' // Do not allow items to be put into this list
      },
      scroll: true,
      dataIdAttr: 'id',
      sortElement: '.sortEl',
      dragClass: "sortable-drag",
      animation: 500,
      onEnd: handleChangeSort
    });
  }
}

const handleChangeSort = (evt) => {
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


const handleDblclick = (item) => {
  if (item.isGroup) {
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
      createSortEl(document.getElementsByClassName('ant-row')[0])
    })
    return
  }

  if (props.select) {
    emit('update:value', item)
    emit('change', item)
    return
  }

  if (item.os === OsEnum.WINDOWS.value) {
    windowsInfoVisible.value = true
    windowsInfoState.value = item
    return
  }

  emit('openServer', item)
}

const handleOpenNewWindow = (item) => {
  window.open(item.url)
}

onMounted(() => {
  createSortEl(document.getElementsByClassName('ant-row')[0])
})

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

defineExpose({
  getProxyData,
  setProxyId
})

</script>

<template>
  <div class="server-root">
    <div class="server-pane">
      <a-space direction="vertical" size="middle" style="width: 100%;">
        <a-card>
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
            <a-list :grid="{ gutter: 16, column: props.column }" :data-source="currentData" row-key="id">
              <template #renderItem="{ item }">
                <a-dropdown :trigger="['contextmenu']">
                  <a-list-item class="sortEl" @dblclick="handleDblclick(item)">
                    <template #actions>
                      <a key="list-loadmore-edit">
                        <edit-outlined @click="handleEditServer(item)"/>
                      </a>
                    </template>
                    <a-card>
                      <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                        <a-list-item-meta
                            :description="item.isGroup?'group':'server'"
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

                  </a-list-item>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item v-if="!item.isGroup" key="2" @click="handleDblclick(item)">
                        <link-outlined/>
                        连接
                      </a-menu-item>
                      <a-menu-item v-if="!item.isGroup" key="2" @click="handleOpenNewWindow(item)">
                        <link-outlined/>
                        新窗口
                      </a-menu-item>
                      <a-menu-item key="4" @click="handleEditServer(item)">
                        <edit-outlined/>
                        修改
                      </a-menu-item>
                      <a-menu-item key="3" @click="handleCopyServer(item)">
                        <CopyOutlined/>
                        复制
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
              <a-input-password v-model:value="creationState.password"/>
            </a-form-item>

            <a-form-item label="连接时执行命令" v-bind="creationValidations.firstCommand">
              <a-textarea :auto-size="{ minRows: 2, maxRows: 5 }"
                          v-model:value="creationState.firstCommand"></a-textarea>
            </a-form-item>

            <a-form-item label="自动sudo" v-bind="creationValidations.aotoSudo">
              <a-switch v-model:checked="creationState.autoSudo"></a-switch>
            </a-form-item>

            <a-form-item label="私钥" v-bind="creationValidations.key">
              <a-textarea v-model:value="creationState.key"></a-textarea>
            </a-form-item>
            <a-form-item label="是否db服务器" v-bind="creationValidations.isDb">
              <a-switch v-model:checked="creationState.isDb"></a-switch>
            </a-form-item>
            <a-form-item label="db端口" v-bind="creationValidations.dbPort" v-if="creationState.isDb">
              <div :class="{mt5:index!==0}" v-for="(item,index) in creationState.dbPort" :key="index">
                <a-input-number v-model:value="creationState.dbPort[index]"
                                :min="1" :max="65535"/>
                <a-button type="link" @click="creationState.dbPort.splice(index,1)">
                  <delete-outlined/>
                </a-button>
              </div>
              <div class="mt5">
                <a-button @click="creationState.dbPort.push(3306)">+1</a-button>
              </div>
            </a-form-item>
          </template>
          <a-form-item label="代理" v-bind="creationValidations.proxyId">
            <p-select ref="proxyRef" :api="proxyApi.list" v-model:value="creationState.proxyId"
                      style="width: 90%"></p-select>
            <a-button @click="proxyCreation" style="margin-left: 2%" type="primary" shape="circle">
              <template #icon>
                <plus-outlined/>
              </template>
            </a-button>
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

</style>
