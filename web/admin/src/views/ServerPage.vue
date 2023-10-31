<script setup>
import {computed, createVNode, nextTick, onMounted, reactive, ref, watch} from "vue";
import Sortable from 'sortablejs';
import {walk} from "@/utils/treeUtil";
import {serverApi} from "@/api/server";
import _ from "lodash";
import {Form, Input, message, Modal} from "ant-design-vue";
import PSelect from "@/components/p-select.vue";
import {proxyApi} from "@/api/proxy";
import PEnumSelect from "@/components/p-enum-select.vue";
import ProxyTypeEnum from "@/enums /ProxyTypeEnum";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import PSftp from "@/components/p-sftp.vue";
import {v4} from 'uuid'

const useForm = Form.useForm;

let spinning = ref(false)
const data = ref([{}]);

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

let currentData = ref(data.value)

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
const handleEditProxy = (row) => {
  proxyCreationVisible.value = true;
  creationProxyType.value = 'update'
  Object.assign(creationProxyState, row)
}
const handleAddGroup = () => {
  creationVisible.value = true;
  creationState.isGroup = true
  creationType.value = 'create'
  creationState.parentId = groupBreadcrumb.value[groupBreadcrumb.value.length - 1].id
}

const handleExecCommand = (command) => {
  let el = document.getElementById(tagActiveKey.value);
  if (el) {
    el.contentWindow.postMessage({command: command}, '*')
  }
}

let groupBreadcrumb = ref([{id: 0, isGroup: true, name: 'all'}])

const handleOpenServer = (item) => {
  spinning.value = true;
  let uuid = v4();
  while (serverList.value.findIndex(e => e.operationId === uuid) !== -1) {
    uuid = v4();
  }

  let copyItem = JSON.parse(JSON.stringify(item));
  copyItem.operationId = uuid;
  serverList.value.push(copyItem)
  tagActiveKey.value = copyItem.operationId
}

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

  handleOpenServer(item);
}

//监听iframe加载完成，执行sudo等初始化命令
window.onmessage = (e) => {
  if (e.data.event === 'connected') {
    spinning.value = false
    let server = serverList.value.find(item => item.operationId === tagActiveKey.value)
    if (!server) {
      return;
    }
    if (server.autoSudo && server.username !== 'root') {
      nextTick(() => {
        handleExecCommand("echo \"" + server.password + "\" | sudo -S ls && sudo -i")
      })
    }

    if (server.firstCommand) {
      nextTick(() => {
        handleExecCommand(server.firstCommand)
      })
    }

    nextTick(() => {
      //可以用来实现拖拽分屏
      createSortEl(document.getElementsByClassName('split-box')[0])
    })
  }

  let rule = /^\/[^\0]+$/;
  if (e.data.event === 'command') {
    if (!link.value) {
      return
    }
    //判断e.data.data是不是一个标准的linux路径 比如 /root , /home ，/var/www，如果是就保存下来留着sftp用
    let data = e.data.data.split('\r\n')
    data.forEach(item => {
      if (rule.test(item)) {
        currentDir.value = item
        sftpEl.value.filter(item => item.operationId === tagActiveKey.value).forEach(item => {
          item.changeDir(currentDir.value)
        })
      }
    })
  }
}

let sftpEl = ref([])

const reloadSftp = () => {
  sftpEl.value.filter(item => item.operationId === tagActiveKey.value).forEach(item => {
    item.init()
  })
}

const reloadServer = () => {
  spinning.value = true;
  let server = serverList.value.find(item => item.operationId === tagActiveKey.value)
  let reloadIndex = server.url.indexOf("&reload=1");
  if (reloadIndex !== -1) {
    server.url = server.url.slice(0, reloadIndex)
    return
  }
  server.url = server.url + "&reload=1"
}

let currentDir = ref('/')

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
    });
  }
}

const updateSort = _.debounce(async (sortData) => {
  await serverApi.updateSort(
      sortData.map(item => ({id: item.id, sort: item.sort}))
  )
}, 250, {'maxWait': 1000});


onMounted(() => {
  var elementsByClassNameElement = document.getElementsByClassName('ant-row')[0];

  let able = createSortEl(elementsByClassNameElement)
  console.log(able.toArray())
  console.log(able.sortData)

  let navList = createSortEl(document.getElementsByClassName('ant-tabs-nav-list')[0])
  navList.onEnd = (evt) => {
    //根据evt.oldIndex和evt.newIndex来维护currentData.value
    let oldIndex = evt.oldIndex;
    let newIndex = evt.newIndex;

    let sortData = currentData.value
    let item = sortData[oldIndex];
    sortData.splice(oldIndex, 1);
    sortData.splice(newIndex, 0, item);

    updateSort(sortData)
  }

})

const onCloseServer = (item) => {
  _.remove(serverList.value, i => i.operationId === item)
  if (item === tagActiveKey.value) {
    tagActiveKey.value = 'server'
  }
}

let tagActiveKey = ref()

let serverList = ref([])


const creationVisible = ref(false);
const creationType = ref('create');
const creationState = reactive({
  name: "",
  ip: "",
  port: "22",
  password: "",
  key: "",
  proxyId: "",
  parentId: "",
  firstCommand: "",
  isGroup: false,
  username: "",
  autoSudo: true,
});

const creationRules = computed(() => reactive({
  name: [
    {
      required: true,
      message: "请输入名称",
    },
    {
      min: 1,
      max: 10,
      message: "名称长度在1-10之间",
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

let {
  value: {
    resetFields: resetCreationFields,
    validate: validateCreation,
    validateInfos: creationValidations
  }
} = computed(() => useForm(creationState, creationRules))

const creationProxyType = ref('create');
const creationProxyState = reactive({
  name: "",
  ip: "",
  type: ProxyTypeEnum.HTTP.value,
  port: "22",
  password: "",
  username: "",
});

const creationProxyRules = reactive({
  name: [
    {
      required: true,
      message: "请输入名称",
    }
  ],
  ip: [
    {
      required: true,
      message: "请输入host",
    },
    {
      pattern: /^(?:(?:[0-9]{1,3}\.){3}[0-9]{1,3}|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,})$/,
      message: "host格式不正确"
    }
  ],
  port: [
    {
      required: true,
      message: "请输入端口",
    },
    {
      pattern: /^\d+$/,
      message: "端口格式不正确"
    }
  ],
  type: [
    {
      required: true,
      message: "请选择类型",
    }
  ],
});

let {
  resetFields: proxyResetCreationFields,
  validate: proxyValidateCreation,
  validateInfos: proxyCreationValidations
} = useForm(creationProxyState, creationProxyRules)

let proxyCreationVisible = ref(false);

watch(proxyCreationVisible, (visible) => {
  if (!visible) {
    proxyResetCreationFields();
  }
});


watch(creationVisible, (visible) => {
  if (!visible) {
    resetCreationFields();
  }
});


let proxyRef = ref()

const proxyCreation = () => {
  proxyCreationVisible.value = true;
  creationProxyType.value = 'create'
}
const handleProxyCreate = async () => {
  try {
    await proxyValidateCreation();
  } catch (error) {
    proxyCreationVisible.value = true;
    return false;
  }

  let res = await proxyApi[creationProxyType.value](creationProxyState);
  if (proxyRef.value) {
    await proxyRef.value.getData()
  }
  await getProxyData();
  await getServerList()
  creationState.proxyId = res.id
  proxyCreationVisible.value = false;
  message.success("操作成功");
}

const submitCreate = async () => {
  try {
    await validateCreation();
  } catch (error) {
    return;
  }

  await serverApi[creationType.value](creationState);
  message.success("操作成功");

  creationVisible.value = false;
  await getServerList()
}

const proxyData = ref([])

const getProxyData = async () => {
  proxyData.value = await proxyApi.list()
}

getProxyData()

let link = ref(true)

const handleCopy = (server) => {
  handleOpenServer(server)
}

const handleRename = (server) => {
  server.rename = server.name
  Modal.confirm({
    title: '重命名',
    icon: createVNode(ExclamationCircleOutlined),
    content: createVNode(Input, {
      onChange: (e) => {
        server.rename = e.target.value
      }
    }),
    onOk: async () => {
      server.name = server.rename
    },
    onCancel() {
      server.rename = ''
    },
  });
}

const handleAddTags = (server, tag) => {
  if (server.tag === tag) {
    server.tag = ''
  } else {
    server.tag = tag
  }
}


let tags = ref([
  `                         <svg t="1696430949034" class="tags" viewBox="0 0 1024 1024" version="1.1"
                               xmlns="http://www.w3.org/2000/svg" p-id="8012" width="200" height="200">
                            <path
                                d="M301.3 496.7c-23.8 0-40.2-10.5-41.6-26.9H205c0.9 43.4 36.9 70.3 93.9 70.3 59.1 0 95-28.4 95-75.5 0-35.8-20-55.9-64.5-64.5l-29.1-5.6c-23.8-4.7-33.8-11.9-33.8-24.2 0-15 13.3-24.5 33.4-24.5 20.1 0 35.3 11.1 36.6 27h53c-0.9-41.7-37.5-70.3-90.3-70.3-54.4 0-89.7 28.9-89.7 73 0 35.5 21.2 58 62.5 65.8l29.7 5.9c25.8 5.2 35.6 11.9 35.6 24.4 0.1 14.7-14.5 25.1-36 25.1z"
                                p-id="8013"></path>
                            <path
                                d="M928 140H96c-17.7 0-32 14.3-32 32v496c0 17.7 14.3 32 32 32h380v112H304c-8.8 0-16 7.2-16 16v48c0 4.4 3.6 8 8 8h432c4.4 0 8-3.6 8-8v-48c0-8.8-7.2-16-16-16H548V700h380c17.7 0 32-14.3 32-32V172c0-17.7-14.3-32-32-32z m-40 488H136V212h752v416z"
                                p-id="8014"></path>
                            <path
                                d="M828.5 486.7h-95.8V308.5h-57.4V534h153.2zM529.9 540.1c14.1 0 27.2-2 39.1-5.8l13.3 20.3h53.3L607.9 511c21.1-20 33-51.1 33-89.8 0-73.3-43.3-118.8-110.9-118.8s-111.2 45.3-111.2 118.8c-0.1 73.7 43 118.9 111.1 118.9z m0-190c31.6 0 52.7 27.7 52.7 71.1 0 16.7-3.6 30.6-10 40.5l-5.2-6.9h-48.8L542 491c-3.9 0.9-8 1.4-12.2 1.4-31.7 0-52.8-27.5-52.8-71.2 0.1-43.6 21.2-71.1 52.9-71.1z"
                                p-id="8015"></path>
                          </svg>`
  ,
  `<svg t="1696431138103" class="tags" viewBox="0 0 1024 1024" version="1.1"
        xmlns="http://www.w3.org/2000/svg" p-id="16226" width="200" height="200">
  <path
      d="M752 321.84l132.7-132.69c63.79 89.55 55.75 214.52-24.57 294.85-60 60-144.9 79.57-221.37 59.2L292.59 889.38a114.66 114.66 0 0 1-162.16 0 114.65 114.65 0 0 1 0-162.15l346.18-346.18c-20.37-76.47-0.78-161.38 59.21-221.36 80.32-80.33 205.3-88.37 294.85-24.6L698 267.79z m98.58 9.53l-45.2 45.2a75.48 75.48 0 0 1-106.75 0l-55.41-55.4a75.48 75.48 0 0 1 0-106.75l45.2-45.2a152.56 152.56 0 0 0-109.79 57c-31.67 39.2-39.46 92.71-26.48 141.41l9.67 36.29-377.34 377.36a38.23 38.23 0 0 0 0 54.05 38.21 38.21 0 0 0 54.05 0L615.89 458l36.29 9.67c48.7 13 102.22 5.2 141.42-26.48a152.61 152.61 0 0 0 57-109.82z"
      fill="#949DA6" p-id="16227"></path>
  <path
      d="M656.23 487.31L893 724.12a116.5 116.5 0 0 1 0 164.73 116.48 116.48 0 0 1-164.73 0L491.5 652z m-37.74 72.08l-54.91 54.91 219.64 219.64A38.83 38.83 0 0 0 838.13 779zM261.578 312.297l54.907-54.907L501.71 442.617l-54.906 54.907z"
      fill="#949DA6" p-id="16228"></path>
  <path
      d="M316.49 312.3l-77.22 15.44a75.48 75.48 0 0 1-84.18-44.28L96.85 147.57l54.91-54.91 135.89 58.24a75.48 75.48 0 0 1 44.28 84.18z m-61.05-90.7l-51.94-22.28 22.28 51.93 24.72-4.93z"
      fill="#949DA6" p-id="16229"></path>
</svg>`
  ,
  `  <svg t="1696430996543" class="tags" viewBox="0 0 1024 1024" version="1.1"
          xmlns="http://www.w3.org/2000/svg" p-id="9844" width="200" height="200">
  <path
      d="M707.2 79.9H238.6c-11.6 0-21 9.4-21 21V923c0 11.6 9.4 21 21 21h704.9V316.3L707.2 79.9"
      fill="#FFFFFF" p-id="9845"></path>
  <path
      d="M943.6 960h-705c-20.4 0-37-16.6-37-37V100.9c0-20.4 16.6-37 37-37h468.6c4.2 0 8.3 1.7 11.3 4.7L954.9 305c3 3 4.7 7.1 4.7 11.3V944c0 8.9-7.2 16-16 16z m-705-864.1c-2.7 0-5 2.2-5 5V923c0 2.7 2.2 5 5 5h689V323l-227-227h-462z"
      fill="#00365B" p-id="9846"></path>
  <path d="M943.6 316.3H707.2V79.9z" fill="#FFFFFF" p-id="9847"></path>
  <path
      d="M943.6 332.3H707.2c-8.8 0-16-7.2-16-16V79.9c0-6.5 3.9-12.3 9.9-14.8s12.9-1.1 17.4 3.5L954.9 305c4.6 4.6 5.9 11.5 3.5 17.4-2.5 6-8.3 9.9-14.8 9.9z m-220.4-32H905L723.2 118.6v181.7z"
      fill="#00365B" p-id="9848"></path>
  <path
      d="M104.7 414.2h698.8c22.4 0 40.6 19.8 40.6 44.3v301.4c0 24.5-18.2 44.3-40.6 44.3H104.7c-22.4 0-40.6-19.8-40.6-44.3V458.5c-0.1-24.5 18.1-44.3 40.6-44.3z"
      fill="#B56FE1" p-id="9849"></path>
  <path
      d="M241.2 736c4.5 0 8.9-1.6 13.2-4.9 4.3-3.3 6.4-8.9 6.4-17 0-7.8-2-13.4-6-16.8s-8.6-5.1-13.6-5.1h-74V488.3c0-6-2.3-10.8-6.8-14.2-4.5-3.4-10.4-5.1-17.7-5.1-6.8 0-12.6 1.7-17.4 5.1-4.8 3.4-7.2 8.1-7.2 14.2v220.5c0 7.6 2.4 14 7.2 19.3 4.8 5.3 11.3 7.9 19.6 7.9h96.3z m162.8 7.5c20.4 0 38.1-3.6 53-10.8 15-7.2 27.4-17 37.2-29.5 9.8-12.5 17.1-27.2 21.9-44.2 4.8-17 7.2-35.3 7.2-54.9s-2.4-37.9-7.2-54.7c-4.8-16.9-12.1-31.5-21.9-44C484.4 493 472 483.2 457 476c-15-7.2-32.7-10.8-53-10.8-21.1 0-39.2 3.6-54.2 10.8-15 7.2-27.3 17-37 29.5s-16.8 27.2-21.3 44.2c-4.5 17-6.8 35.2-6.8 54.6 0 20.1 2.5 38.8 7.6 55.9 5 17.1 12.6 31.8 22.7 44.2 10.1 12.3 22.5 22 37.4 28.9s31.9 10.2 51.6 10.2z m0-43c-13.3 0-24.5-2.8-33.4-8.3-8.9-5.5-16.2-12.8-21.7-21.9-5.5-9.1-9.4-19.4-11.7-31-2.3-11.6-3.4-23.3-3.4-35.1 0-13.1 1.3-25.4 3.8-37 2.5-11.6 6.5-21.7 12.1-30.4 5.5-8.7 12.8-15.6 21.7-20.8 8.9-5.2 19.8-7.7 32.7-7.7s23.7 2.5 32.7 7.6c8.9 5 16.2 12 21.7 20.8s9.6 19.1 12.1 31c2.5 11.8 3.8 24.4 3.8 37.8 0 13.3-1.1 25.8-3.4 37.4-2.3 11.6-6.2 21.6-11.7 30.2-5.5 8.6-12.8 15.3-21.7 20.2-9.2 4.7-20.3 7.2-33.6 7.2z m278.2 43c20.4 0 37.6-3.2 51.7-9.6 14.1-6.4 25.6-15.1 34.5-26.1 8.9-10.9 15.4-23.6 19.3-37.9 3.9-14.3 5.9-29.5 5.9-45.3 0-13.1-1.8-22.3-5.3-27.6s-10.4-7.9-20.8-7.9h-63.1c-8.8 0-15.4 1.8-19.6 5.3-4.3 3.5-6.4 9.3-6.4 17.4 0 8.1 2 13.7 6 16.8 4 3.1 10.6 4.7 19.6 4.7h40.4c0 9.8-1.6 18.8-4.9 27-3.3 8.2-7.7 15.2-13.4 21.1-5.7 5.9-12.2 10.6-19.6 14s-15.4 5.1-24 5.1c-11.8 0-22.7-2.3-32.5-6.8s-18.2-10.9-25.3-19.3c-7-8.3-12.5-18.4-16.4-30.2-3.9-11.8-5.9-25.2-5.9-40 0-14.6 2-27.8 6-39.6s9.4-21.9 16.2-30.2c6.8-8.3 14.9-14.7 24.2-19.3 9.3-4.5 19.3-6.8 29.8-6.8 12.1 0 22.6 2 31.5 5.9 8.9 3.9 18.4 11.3 28.5 22.1 3 3 6.3 5 9.8 6 3.5 1 7 1.4 10.6 1.1 3.5-0.3 6.9-1.1 10-2.6 3.1-1.5 5.9-3.3 8.1-5.3 4.5-3.3 6.6-7.2 6.2-11.9-0.4-4.7-2.3-9.4-5.9-14.3-3.5-4.9-8.2-9.8-14-14.7-5.8-4.9-12.1-9.5-18.9-13.8-11.3-6.5-22-10.8-32.1-12.6-10.1-1.9-20.9-2.8-32.5-2.8-20.1 0-38.1 3.6-53.8 10.9-15.7 7.3-29 17.2-39.8 29.8-10.8 12.6-19.1 27.4-24.7 44.6-5.7 17.1-8.5 35.6-8.5 55.5 0 20.9 3 39.8 8.9 56.6 5.9 16.9 14.5 31.3 25.7 43.2 11.2 12 24.7 21.2 40.6 27.8 16.2 6.5 34 9.7 53.9 9.7z"
      fill="#FFFFFF" p-id="9850"></path>
</svg>`,
  `  <svg t="1696431206829" class="tags" viewBox="0 0 1024 1024" version="1.1"
       xmlns="http://www.w3.org/2000/svg" p-id="17235" width="200" height="200">
    <path
        d="M855.04 385.024q19.456 2.048 38.912 10.24t33.792 23.04 21.504 37.376 2.048 54.272q-2.048 8.192-8.192 40.448t-14.336 74.24-18.432 86.528-19.456 76.288q-5.12 18.432-14.848 37.888t-25.088 35.328-36.864 26.112-51.2 10.24l-567.296 0q-21.504 0-44.544-9.216t-42.496-26.112-31.744-40.96-12.288-53.76l0-439.296q0-62.464 33.792-97.792t95.232-35.328l503.808 0q22.528 0 46.592 8.704t43.52 24.064 31.744 35.84 12.288 44.032l0 11.264-53.248 0q-40.96 0-95.744-0.512t-116.736-0.512-115.712-0.512-92.672-0.512l-47.104 0q-26.624 0-41.472 16.896t-23.04 44.544q-8.192 29.696-18.432 62.976t-18.432 61.952q-10.24 33.792-20.48 65.536-2.048 8.192-2.048 13.312 0 17.408 11.776 29.184t29.184 11.776q31.744 0 43.008-39.936l54.272-198.656q133.12 1.024 243.712 1.024l286.72 0z"
        p-id="17236"></path>
  </svg>`
])

let frontColor = ref(localStorage.getItem("frontColor") ?? "#ffffff")
let backColor = ref(localStorage.getItem("backColor") ?? "#000000")

//把#ffffff颜色转换成rgb(255,255,255)格式
const hexToRgb = (hex) => {
  let rgb = [];
  hex = hex.substr(1);//去除前缀 # 号

  if (hex.length === 3) { // 处理 "#abc" 成 "#aabbcc"
    hex = hex.replace(/(.)/g, '$1$1');
  }

  hex.replace(/../g, function (color) {
    rgb.push(parseInt(color, 0x10));//按16进制将字符串转换为数字
  });

  return `rgb(${rgb.join(", ")})`;
}

//把样式保存到本地
const saveStyle = () => {
  localStorage.setItem('frontColor', frontColor.value)
  localStorage.setItem('backColor', backColor.value)
  message.success("保存成功");
}

let themes = ref([
  {name: '默认', frontColor: '#ffffff', backColor: '#000000'},
  {
    name: 'Solarized Dark',
    frontColor: '#93a1a1',
    backColor: '#002b36'
  },
  {
    name: 'Monokai',
    frontColor: '#f8f8f2',
    backColor: '#272822'
  },
  {
    name: 'Dracula',
    frontColor: '#f8f8f2',
    backColor: '#282a36'
  }, {
    name: 'Nord',
    frontColor: '#d8dee9',
    backColor: '#2e3440'
  },
  {
    name: 'One Dark',
    frontColor: '#abb2bf',
    backColor: '#282c34'
  }, {
    name: 'Material ',
    frontColor: '#c5c8c6',
    backColor: '#1d1f21'
  }, {
    name: 'Tomorrow ',
    frontColor: '#c5c8c6',
    backColor: '#1d1f21'
  }, {
    name: 'Zenburn',
    frontColor: '#dcdccc',
    backColor: '#3f3f3f'
  },
  {
    name: 'Termius',
    frontColor: '#00CC74',
    backColor: '#141729'
  }

])

const selectThemes = (theme) => {
  frontColor.value = theme.frontColor
  backColor.value = theme.backColor
  saveStyle()
}

let sftpEnable = ref(false)

const changeSftpEnable = () => {
  sftpEnable.value = !sftpEnable.value
  if (sftpEnable.value) {
    nextTick(() => {
      sftpEl.value.filter(item => item.operationId === tagActiveKey.value).forEach(item => {
        item.$el.scrollIntoView({behavior: 'smooth'})
      })
    })

    message.success("开启sftp成功");
  } else {
    message.success("关闭sftp成功");
  }
}

</script>

<template>
  <div class="server-root">
    <a-spin :spinning="spinning" tip="等待中...">
      <a-tabs style="width: 100%;"
              type="editable-card"
              :tabBarGutter="8"
              :hideAdd="true"
              v-model:activeKey="tagActiveKey"
              :tab-position="'left'">

        <a-tab-pane class="server-pane" key="server" :closable="false">
          <template v-slot:tab>
            服务器
          </template>
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
                <a-list :grid="{ gutter: 16, column: 4 }" :data-source="currentData" row-key="id">
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
                                <hdd-outlined v-else class="icon-server" style="color: #f25e62;"/>
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
        </a-tab-pane>
        <a-tab-pane class="proxy-pane" tab="代理" key="proxy" :closable="false">
          <a-space direction="vertical" size="middle" style="width: 100%;">
            <a-card>
              <div style="display: flex;justify-content: space-between">
                <div>

                </div>
                <div>
                  <a-button @click="proxyCreation" class="my-button">新增代理</a-button>
                </div>
              </div>
              <div class="mt30 server">
                <a-list :grid="{ gutter: 16, column: 4 }" :data-source="proxyData" row-key="id">
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <template #actions>
                        <a key="list-loadmore-edit">
                          <edit-outlined @click="handleEditProxy(item)"/>
                        </a>
                      </template>
                      <a-card>
                        <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                          <a-list-item-meta
                              :description="item.type+','+item.ip+':'+item.port"
                          >
                            <template #title>
                              <span>{{ item.name }}</span>
                            </template>
                            <template #avatar>
                              <safety-certificate-outlined class="icon-server" style="color: #f25e62;"/>
                            </template>
                          </a-list-item-meta>
                        </a-skeleton>
                      </a-card>

                    </a-list-item>
                  </template>
                </a-list>
              </div>
            </a-card>
          </a-space>
        </a-tab-pane>
        <a-tab-pane class="setting-pane" tab="设置" key="setting" :closable="false">
          <a-space direction="vertical" size="middle" style="width: 100%;">
            <a-card>
              <a-card title="主题色">
                <a-card-grid @click="selectThemes(item)"
                             :style="`width: 15%; text-align: center; background-color: ${item.backColor}; color: ${item.frontColor}`"
                             v-for="item in themes" :key="item.backColor" :title="item.name">
                  {{ item.name }}
                </a-card-grid>

              </a-card>
              <a-form :label-col="{ span: 4 }"
                      :wrapper-col="{ span: 20 }"
                      style="margin-top: 30px"
                      autocomplete="off">
                <a-form-item label="字体颜色">
                  <input type="color" v-model="frontColor"/>
                </a-form-item>
                <a-form-item label="背景颜色">
                  <input type="color" v-model="backColor"/>
                </a-form-item>
                <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
                  <a-button type="primary" @click.prevent="saveStyle">保存</a-button>
                </a-form-item>
              </a-form>
            </a-card>
          </a-space>
        </a-tab-pane>
        <template v-for="server in serverList" :key="server.operationId">
          <a-tab-pane>
            <template v-slot:tab>
              <a-dropdown :trigger="['contextmenu']" class="dropdown">
                <div style="display: flex;justify-content: center;align-items: center;">
                  <div style="line-height: 1px;" v-html="server.tag"></div>
                  <div class="ml5">{{ server.name }}</div>
                </div>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="4" @click="handleCopy(server)">
                      <template #icon>
                        <CopyOutlined/>
                      </template>
                      复制
                    </a-menu-item>
                    <a-menu-item key="5" @click="handleRename(server)">
                      <a-sub-menu key="7">
                        <template #icon>
                          <tag-outlined/>
                        </template>
                        <template #title>标签</template>
                        <a-menu-item @click="handleAddTags(server,item)" v-for="item in tags" :key="item.length">
                          <template #icon>
                            <div v-html="item"></div>
                          </template>
                        </a-menu-item>

                      </a-sub-menu>
                    </a-menu-item>
                    <a-menu-item key="3" @click="handleRename(server)">
                      <template #icon>
                        <edit-outlined/>
                      </template>
                      重命名
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
            <template v-slot:closeIcon>
              <close-outlined @click="onCloseServer(server.operationId)"/>
            </template>
            <div class="split-box">
              <div class="ssh-content">
                <a-card title="终端">
                  <template #extra>
                    <a-button type="link" style="display: flex;justify-content: center;align-items: center"
                              :class="{green:sftpEnable}" @click="changeSftpEnable">
                      <template v-slot:icon>
                        <svg t="1696435355552" class="tags" viewBox="0 0 1024 1024" version="1.1"
                             xmlns="http://www.w3.org/2000/svg" p-id="19507" width="200" height="200">
                          <path
                              d="M972.8 249.856h-14.336l-0.512-108.032c0-25.6-20.992-45.568-46.08-45.568l-413.184 2.56h-3.584l-23.552-25.088c-9.728-10.24-23.04-16.384-37.376-16.384l-381.952-0.512C24.064 56.832 1.536 79.36 1.024 107.52L0 914.432c0 13.824 5.12 26.624 14.848 36.352 9.728 9.728 22.528 14.848 36.352 15.36l921.088 1.024c28.16 0 51.2-22.528 51.2-51.2l0.512-614.912c0-28.16-23.04-50.688-51.2-51.2z m-105.984-61.44l0.512 61.44-232.96-0.512-55.296-59.392 287.744-1.536zM921.088 865.28L102.4 864.256 108.032 158.72l303.616 0.512 162.816 176.128c9.728 10.24 23.04 16.384 37.376 16.384l310.272 0.512-1.024 513.024z"
                              p-id="19508"></path>
                          <path
                              d="M531.968 441.344c-9.216 1.536-17.408 7.168-22.528 15.36-9.728 15.872-6.144 36.864 8.192 48.128l28.16 22.016-183.808 1.024c-18.432 0-33.28 16.384-33.28 35.84s15.36 35.328 33.792 35.328l284.16-1.536c18.432 0 33.28-16.384 33.28-35.84 0-9.728-4.096-18.944-10.752-25.6-1.536-2.048-3.584-4.096-5.632-5.632l-106.496-82.944c-7.168-5.632-15.872-7.68-25.088-6.144zM647.168 639.488l-283.648 2.048c-18.432 0-33.28 16.384-33.28 35.84 0 9.728 4.096 18.944 10.752 25.6 1.536 2.048 3.584 4.096 5.632 5.632l106.496 82.944c5.632 4.608 12.8 6.656 19.968 6.656 11.264 0 21.504-6.144 27.648-15.872 4.608-7.68 6.656-16.896 5.12-25.6-1.536-9.216-6.144-17.408-13.312-22.528l-28.16-22.016 183.808-1.024c18.432 0 33.28-16.384 33.28-35.84-1.024-19.968-15.872-35.84-34.304-35.84z"
                              p-id="19509"></path>
                        </svg>
                      </template>
                    </a-button>

                    <a-button type="link" @click="reloadServer">
                      <template v-slot:icon>
                        <reload-outlined/>
                      </template>
                    </a-button>
                  </template>
                  <iframe class="ssh"
                          title="ssh"
                          :id="server.operationId"
                          :src="server.url+'&bgcolor='+hexToRgb(backColor)+'&fontcolor='+hexToRgb(frontColor)">
                  </iframe>
                </a-card>
              </div>
              <template v-if="sftpEnable">
                <div class="link-root">
                  <a-popover title="sftp关联终端">
                    <template #content>
                      <p>在终端使用部分命令时自动切换路径如（pwd...）</p>
                    </template>
                    <link-outlined :class="{'link-icon':true,'link-icon-activation':link}" @click="link=!link"/>
                  </a-popover>
                </div>
                <div class="sftp-content">
                  <a-card title="sftp">
                    <template #extra>

                      <a-button type="link" @click="reloadSftp">
                        <template v-slot:icon>
                          <reload-outlined/>
                        </template>
                      </a-button>
                    </template>
                    <p-sftp class="sftp" ref="sftpEl" :operation-id="server.operationId"
                            :server-id="server.id"></p-sftp>
                  </a-card>
                </div>
              </template>

            </div>
          </a-tab-pane>
        </template>
      </a-tabs>
    </a-spin>

    <a-drawer
        v-model:visible="proxyCreationVisible"
        title="新增代理"
        placement="right"
        width="40%"
        size="large"
    >
      <template #extra>
        <a-space>
          <a-button @click="proxyCreationVisible = false">取消</a-button>
          <a-button type="primary" @click="handleProxyCreate">提交</a-button>
        </a-space>
      </template>

      <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 15 }"
          autocomplete="off"
      >
        <a-form-item label="名称：" v-bind="proxyCreationValidations.name">
          <a-input v-model:value="creationProxyState.name"/>
        </a-form-item>
        <a-form-item label="host：" v-bind="proxyCreationValidations.ip">
          <a-input v-model:value="creationProxyState.ip"/>
        </a-form-item>
        <a-form-item label="端口：" v-bind="proxyCreationValidations.port">
          <a-input v-model:value="creationProxyState.port"/>
        </a-form-item>
        <a-form-item label="类型：" v-bind="proxyCreationValidations.type">
          <p-enum-select style="max-width: 100%" v-model:value="creationProxyState.type"
                         :module="ProxyTypeEnum"></p-enum-select>
        </a-form-item>
        <a-form-item label="用户名：" v-bind="proxyCreationValidations.username">
          <a-input v-model:value="creationProxyState.username"/>
        </a-form-item>
        <a-form-item label="密码：" v-bind="proxyCreationValidations.password">
          <a-input v-model:value="creationProxyState.password"/>
        </a-form-item>
      </a-form>
    </a-drawer>
    <a-drawer
        v-model:visible="creationVisible"
        title="新增服务器"
        placement="right"
        width="60%"
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
        <template v-if="!creationState.isGroup">
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
            <a-textarea :auto-size="{ minRows: 2, maxRows: 5 }" v-model:value="creationState.firstCommand"></a-textarea>
          </a-form-item>

          <a-form-item label="自动sudo" v-bind="creationValidations.aotoSudo">
            <a-switch v-model:checked="creationState.autoSudo"></a-switch>
          </a-form-item>

          <a-form-item label="私钥" v-bind="creationValidations.key">
            <a-textarea v-model:value="creationState.key"></a-textarea>
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


      </a-form>

    </a-drawer>
  </div>
</template>

<style scoped lang="less">
.server-root {

  .mt10 {
    margin-top: 10px;
  }

  .mt30 {
    margin-top: 30px;
  }

  .ml10 {
    margin-left: 10px;
  }

  .icon-server {
    font-size: 30px;
    color: #1890ff;
  }

  .ant-list-item-meta {
    display: flex;
    flex-direction: row;
    align-items: center;
  }


  .server-pane, .proxy-pane {
    :deep(.ant-card-body) {
      padding: 12px;
      background-color: #1e1f32;
    }

    .server {
      :deep(.ant-list) {
        .ant-list-item {
          &:hover {
            .ant-list-item-action {
              opacity: 1 !important;
            }
          }
        }

        .ant-empty-description {
          color: #ffffff;
        }
      }


      :deep(.ant-list-item-action) {
        position: absolute;
        right: 16px;
        opacity: 0;
        top: 50%;
        transform: translateY(-50%);
      }

      .ant-card {
        border-radius: 16px;
        overflow: hidden;
        //鼠标变成手指
        cursor: pointer;
        //鼠标点击显示绿色边框

        &:active {
          border: 2px solid #1daa6c;
        }

        //添加透明边框，解决hover时挤大容器
        border: 2px solid #292a3d;


        :deep(.ant-card-body) {
          background-color: #292a3d;
          color: #cccdd6;
          //禁用文字选中
          -webkit-user-select: none;
          -moz-user-select: none;
          -ms-user-select: none;
          user-select: none;


          &:hover {
            background-color: #32364a;
          }

          .ant-list-item-meta-title {
            color: #cccdd6;
            line-height: 1;
            //单行文字溢出显示省略号
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;


            span {
              color: #cccdd6;
            }
          }

          .ant-list-item-meta-description {
            line-height: 1;
            color: #9a9daa;
            font-size: 0.8em;
            transform: scale(0.9); /* 用缩放来解决 */
            transform-origin: 0 0; /* 左对齐 */
          }
        }
      }
    }
  }


  :deep(.ant-breadcrumb) {
    color: #cccdd6;

    .ant-breadcrumb-separator {
      color: #cccdd6;
    }

    span {
      a {
        color: #1daa6c;
      }
    }

    span:last-child {
      a {
        color: #cccdd6;
      }
    }

    .ant-breadcrumb-link {
      color: #cccdd6;
    }
  }

  .split-box {
    min-height: 1000px;
    margin-bottom: 100px;

    .ssh-content {
      .ssh {
        width: 100%;
        height: 500px;
        border: none;
        resize: both; /* 可以调整宽度和高度 */
      }
    }

    .sftp-content {
      margin-top: 12px;

      .sftp {
        width: 100%;
        height: 500px;
        border: none;
        resize: both; /* 可以调整宽度和高度 */
      }
    }
  }


  .my-button {
    background-color: #292a3d;
    color: #fff;
    border: none;
    border-radius: 10px;

    &:hover {
      background-color: #32364a;
    }
  }

  .ant-dropdown-menu {
    color: #fff;
    background-color: #292a3d;
    border: 1px solid #000;
    box-shadow: 0 2px 8px rgba(0, 0, 0, .15);

    :deep(.ant-dropdown-menu-item, .ant-dropdown-menu-submenu-title) {
      color: #fff;
    }
  }

  .link-root {
    display: flex;
    justify-content: center;
    margin-top: 12px;
    align-items: center;

    .link-icon {
      font-size: 2em;
    }

    .link-icon-activation {
      color: #1daa6c;
    }
  }

}

:deep(.ant-card-extra) {
  display: flex;
}


:deep(.tags) {
  width: 20px;
  height: 20px;
  font-size: 20px;
}


:deep(.ant-dropdown-menu-submenu-title) {
  padding-left: 0px;
}

.ml5 {
  margin-left: 5px;
}

:deep(.green) {
  color: #1daa6c;
  //设置svg颜色
  svg {
    fill: #1daa6c;
  }
}

</style>
