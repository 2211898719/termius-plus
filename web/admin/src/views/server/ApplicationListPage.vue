<script setup>
import {applicationApi} from "@/api/application";
import {message, Modal} from "ant-design-vue";
import {
  computed,
  createVNode,
  defineEmits,
  defineExpose,
  nextTick,
  onMounted,
  reactive,
  ref,
  watch
} from "vue";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import {walk} from "@/utils/treeUtil";
import {useForm} from "ant-design-vue/es/form";
import Sortable from "sortablejs";
import _ from "lodash";
import {copyToClipboard} from "@/utils/copyUtil";
import PEnumSelect from "@/components/p-enum-select.vue";
import ApplicationMonitorTypeEnum from "@/enums/ApplicationMonitorTypeEnum";
import MethodEnum from "@/enums/MethodEnum";
import {formatSeconds} from "@/components/process";
import {serverApi} from "@/api/server";
import PCascader from "@/components/p-cascader.vue";
import GroupCascader from "@/components/group-cascader.vue";
import RelationGraph from 'relation-graph/vue3'
import {proxyApi} from "@/api/proxy";
import PSelect from "@/components/p-select.vue";
import ApplicationMonitorCheckTypeEnum from "@/enums/ApplicationMonitorCheckTypeEnum";
import MonacoEditor from "@/components/MonacoEditor.vue";

let termiusStyleColumn = ref(Math.floor(window.innerWidth / 300));


const resizeObserver = new ResizeObserver(() => {
  termiusStyleColumn.value = Math.floor(window.innerWidth / 300);
});

resizeObserver.observe(window.document.body);


const emit = defineEmits(['openServer', 'update:value', 'change'])

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
  parentId: 0,
  content: "",
  sort: 0,
  latitude: 0,
  longitude: 0,
  identity: "",
  masterMobile: "",
  identityArray: [],
  monitorType: null,
  monitorConfig: {
    url: "",
    checkType: ApplicationMonitorCheckTypeEnum.REGEX.value,
    method: MethodEnum.GET.value,
    headers: [],
    body: "",
    responseRegex: "",
    timeRange: null,
  },
  monitorTestData: {},
  serverList: [],
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
  content: [
    {
      required: !creationState.isGroup,
      message: "请输入链接",
    },
    {
      pattern: /^https?:\/\/[^\s/$.?#].[^\s]*$/,
      message: "链接格式不正确"
    }
  ],
  masterMobile: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "手机号格式不正确"
    }
  ],
  identity: [
    {
      required: !creationState.isGroup,
      message: "请输入身份",
    }
  ],
  parentId: [
    {
      required: true,
      message: "请选择位置",
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

const handleAddApplication = () => {
  creationVisible.value = true;
  creationState.isGroup = false;
  creationType.value = 'create'
  creationState.parentId = groupBreadcrumb.value[groupBreadcrumb.value.length - 1].id
}

const handleEditApplication = (row, flag = true) => {
  creationVisible.value = flag;
  creationType.value = 'update'
  Object.assign(creationState, row)
  if (isJSON(creationState.identity)) {
    try {
      creationState.identityArray = JSON.parse(creationState.identity)
    } catch (e) {
      console.error("处理身份数据出错", e)
    }
  }

  if (isJSON(creationState.monitorConfig)) {
    try {
      creationState.monitorConfig = JSON.parse(creationState.monitorConfig)
      if (!creationState.monitorConfig.checkType) {
        creationState.monitorConfig.checkType = ApplicationMonitorCheckTypeEnum.REGEX.value
      }
      let headers = []
      Object.keys(creationState.monitorConfig.headers).forEach(key => {
            headers.push({
              key: key,
              value: creationState.monitorConfig.headers[key].join(',')
            })

          }
      )

      creationState.monitorConfig.headers = headers

    } catch (e) {
      console.error("处理监控配置数据出错", e)
    }
  }
}

function isJSON(str) {
  return /^\s*(\{.*\}|\[.*\])\s*$/.test(str);
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

const getApplicationList = async () => {
  let list = await applicationApi.list()
  data.value.splice(0)
  data.value.push(...list)

  handleBreadcrumbData(groupBreadcrumb.value[groupBreadcrumb.value.length - 1])
}

getApplicationList()

// let interval = setInterval(() => {
//   getApplicationList()
// }, 1000 * 10)
//
// onBeforeUnmount(() => {
//   clearInterval(interval)
// })

const handleCopyApplication = async (row) => {
  handleEditApplication(row, false);
  creationType.value = 'create'
  creationState.name = `${row.name}-copy`
  await submitCreate()
  await getApplicationList()
}
const handleDelApplication = (item) => {
  let modal = Modal.confirm({
    title: '确定要删除吗?',
    icon: createVNode(ExclamationCircleOutlined),
    content: item.isGroup ? '删除组会丢失组下所有应用信息！！' : '',
    onOk: async () => {
      try {
        await applicationApi.del(item.id)
        await getApplicationList()
        message.success("操作成功");
      } catch (e) {
        modal.destroy()
      }
    },
    onCancel() {
    },
  });
}

const submitCreate = async () => {
  creationState.identity = JSON.stringify(creationState.identityArray)

  await nextTick(async () => {
    try {
      await validateCreation();
    } catch (error) {
      return;
    }
    let submitData = JSON.parse(JSON.stringify(creationState))
    let headers = {}
    submitData.monitorConfig.headers.forEach(header => {
      headers[header.key] = header.value.split(',')
    })
    submitData.monitorConfig.headers = headers
    submitData.monitorConfig = JSON.stringify(submitData.monitorConfig)

    await applicationApi[creationType.value](submitData);
    message.success("操作成功");

    creationVisible.value = false;
    await getApplicationList()
  })

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
  await applicationApi.updateSort(
      sortData.map(item => ({id: item.id, sort: item.sort}))
  )
}, 250, {'maxWait': 1000});


const handleDblclick = (item) => {
  item.onlyUserVisible = false
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
      let elements = document.getElementsByClassName('ant-row');
      for (let i = 0; i < elements.length; i++) {
        createSortEl(elements[i])
      }
    })
    return
  }

  if (props.select) {
    emit('update:value', item)
    emit('change', item)
    return
  }

  openModalData.value = item
  if ((!isJSON(item.identity) || JSON.parse(item.identity).length === 0) && (!item.serverList || item.serverList?.length === 0)) {
    window.open(item.content, '_blank')
    return;
  }

  openModalTitle.value = [...groupBreadcrumb.value.slice(1).map(g => g.name), item.name].join("/")
  openModalVisible.value = true
  if (isJSON(item.identity)) {
    openModalData.value.identityArray = JSON.parse(item.identity)
  }

}

onMounted(() => {
  let elements = document.querySelector('.server-root .ant-row');
  createSortEl(elements)
})

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


let tagOptions = ref([
  {value: '管理员', text: 'admin'},
  {value: '老师', text: 'teacher'},
  {value: '学生', text: 'student'},
])


let servertagOptions = ref([
  {value: 'web', text: 'web'},
  {value: 'db', text: 'db'},
  {value: '负载', text: '负载'},
  {value: '发布机', text: '发布机'},
  {value: '缓存', text: '缓存'},
  {value: '备份', text: '备份'},
  {value: '转码', text: '转码'},
  {value: 'nfs', text: 'nfs'},
  {value: 'redis', text: 'redis'},
  {value: 'mysql', text: 'mysql'},
  {value: '跳板机', text: '跳板机'},
  {value: '测试', text: '测试'},
])

const addIdentity = () => {
  creationState.identityArray.push({
    username: "",
    password: "",
    tag: "",
  })
}

let openModalVisible = ref(false)
let openModalTitle = ref('')
let openModalData = ref({})

let identityColumns = [

  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username',
  },

  {
    title: '密码',
    dataIndex: 'password',
    key: 'password',
  },
  {
    title: '标签',
    dataIndex: 'tag',
    key: 'tag',
  }
]

let serverColumns = [
  {
    title: '服务器名称',
    dataIndex: 'server',
    key: 'server',
  },
  {
    title: '标签',
    dataIndex: 'tag',
    key: 'tag',
  },
  {
    title: '操作',
    dataIndex: 'action',
    key: 'action',
  },
]

const openApplication = (application) => {
  window.open(application.content, '_blank')
}

const testMonitor = (monitor) => {
  let data = JSON.parse(JSON.stringify(monitor))
  let headers = {}
  data.monitorConfig.headers.forEach(header => {
    headers[header.key] = header.value.split(',')
  })
  data.monitorConfig.headers = headers
  applicationApi.testMonitor({
    type: data.monitorType,
    config: JSON.stringify(data.monitorConfig),
    proxyId: data.proxyId
  }).then(res => {
    if (!res.res) {
      message.error(res.message)
      return
    }
    creationState.monitorTestData = res
  }).catch(err => {
    message.error(err.message)
  })
}


const openServer = (openModalData, record) => {
  emit('openServer', {
    ...record.server,
    path: groupBreadcrumb.value.slice(1).map(g => g.name).join("/"),
    name: openModalData.name + '的' + record.tag + '服务器'
  }, 0)
}

const graphOpenServer = (server) => {
  message.success('后台进入服务器,' + server.name)
  emit('openServer', server, 0)
}

let graphRef$ = ref(null)
let contactVisible = ref(false)
let contactData = ref({})
let showApplicationContent = ref(false)
let relationOptions = ref({
  "defaultNodeShape": 1,
  "downloadImageFileName": "下载",
  "defaultLineShape": 1,
  "layouts": [
    {
      "label": "中心",
      "layoutName": "tree",
      'defaultJunctionPoint': 'border',
      'defaultNodeShape': 0,
      "min_per_width": "150",
      "max_per_width": "301",
      "min_per_height": "150",
      "max_per_height": "501"
    }
  ]
})

const handleOpenApplicationContact = (item) => {
  contactVisible.value = true
  contactData.value = item
  relationOptions.value.downloadImageFileName = '下载' + item.name + '关系图'
  let nodes = []
  let lines = []
  walk([contactData.value], (node) => {
    nodes.push({id: 'app' + node.id, text: node.name, data: {type: 'application', ...node}})
    if (node.serverList) {
      nodes.push(...node.serverList.map(n => ({
        id: 'server' + n.server.id,
        text: n.server.name,
        data: {type: 'server', ...n.server},
        color: '#E45F2B'
      })))
      lines.push(...node.serverList.map(n => ({
        from: 'app' + node.id,
        to: 'server' + n.server.id,
        text: n.tag
      })))
    }
    if (node.children) {
      lines.push(...node.children.map(c => ({from: 'app' + node.id, to: 'app' + c.id})))
    }
  })
  const jsonData = {
    rootId: 'app' + contactData.value.id,
    nodes: nodes,
    lines: lines,
  }

  nextTick(() => {
    graphRef$.value.setJsonData(jsonData)
    graphRef$.value.setOptions(relationOptions.value)
  })
}


const handleMonitorCheckTypeChange = (value) => {
  if (value === ApplicationMonitorCheckTypeEnum.JAVASCRIPT.value) {
    creationState.monitorConfig.responseRegex =
        `function check(body) {
  var json = JSON.parse(body)
  return 'success'
}`
  } else {
    creationState.monitorConfig.responseRegex = ''
  }

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
        <a-card :bodyStyle="{padding:'12px 12px'}" style="border:none">
          <div style="display: flex">
            <div class="body-root" style="width: 100%;">
              <div style="display: flex;justify-content: space-between">
                <a-breadcrumb>
                  <a-breadcrumb-item v-for="item in groupBreadcrumb" :key="item.id" @click="handleDblclick(item)">
                    <a>{{ item.name }}</a>
                  </a-breadcrumb-item>
                </a-breadcrumb>

                <div>
                  <a-button @click="getApplicationList" class="my-button">刷新</a-button>
                  <a-button @click="handleAddApplication" class="ml10 my-button">新增应用</a-button>
                  <a-button class="ml10 my-button" @click="handleAddGroup">新增分组</a-button>
                </div>
              </div>
              <div class="mt30 server">
                <a-list :grid="{ gutter: 16, column: termiusStyleColumn }" :data-source="currentData" row-key="id">
                  <template #renderItem="{ item }">
                    <a-dropdown :trigger="['contextmenu']">

                      <a-list-item class="sortEl" @dblclick="handleDblclick(item)">
                        <template #actions>
                          <a>
                            <edit-outlined @click="handleEditApplication(item)"/>
                          </a>
                        </template>

                        <a-badge-ribbon
                            :text="item.failureCount?(item.isGroup?'异常':('异常'+(formatSeconds(item.failureCount*60)))):'正常'"
                            :color="item.failureCount?'red':'green'"
                            :class="{none:(!item.monitorType&&!item.isGroup)||item.failureCount===null}">
                          <a-card>
                            <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                              <a-list-item-meta
                                  :description="item.isGroup?'group':item.content"
                              >
                                <template #title>
                                  <span>{{ item.name }}</span>
                                </template>
                                <template #avatar>
                                  <appstore-outlined v-if="item.isGroup" class="icon-server"/>
                                  <svg v-else
                                       class="icon-server"
                                       style="width: 1em;color: #E45F2B;height: 1em;vertical-align: middle;fill: currentColor;overflow: hidden;"
                                       viewBox="0 0 1024 1024"
                                       version="1.1"
                                       xmlns="http://www.w3.org/2000/svg"
                                       p-id="4228">
                                    <path
                                        d="M889.75872 276.04992l-0.13824-0.0768-366.1312-209.75616a29.81376 29.81376 0 0 0-15.01696-4.00896 29.952 29.952 0 0 0-15.16032 4.08576L127.37536 276.03456c-9.02144 5.22752-15.0016 14.83264-15.01696 25.83552v419.60448c0 10.69056 5.73952 20.47488 15.01696 25.87648l366.07488 209.81248c4.3264 2.45248 9.50272 3.89632 15.01696 3.89632s10.69056-1.44384 15.17056-3.97824l365.9776-209.73568c9.0368-5.23264 15.01696-14.85824 15.01696-25.87648V301.8496a29.77792 29.77792 0 0 0-14.8736-25.79968zM508.47232 126.464l314.0864 179.98848-314.0864 181.36064-319.10912-178.45248L508.47232 126.464zM172.43136 368.50176l305.664 170.95168 0.3584 340.26496-306.0224-175.39584v-335.8208z m672.13312 335.81568l-306.03264 175.37536-0.3584-339.90656 306.39104-176.99328V704.31744z"
                                        fill=""
                                        p-id="4229"></path>
                                  </svg>
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
                            打开
                          </a-menu-item>
                          <a-menu-item key="4" @click="handleEditApplication(item)">
                            <edit-outlined/>
                            修改
                          </a-menu-item>
                          <a-menu-item key="5" @click="handleOpenApplicationContact(item)">
                            <read-outlined/>
                            关系图
                          </a-menu-item>
                          <a-menu-item key="3" @click="handleCopyApplication(item)">
                            <CopyOutlined/>
                            复制
                          </a-menu-item>
                          <a-menu-item key="1" @click="handleDelApplication(item)">
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

          </div>
        </a-card>

      </a-space>
      <a-drawer
          v-model:visible="contactVisible"
          :title="contactData.name+'的关系图'"
          placement="right"
          width="90%"
          size="large"
      >
        <a-form layout="inline">
          <a-form-item label="显示应用内容">
            <a-switch v-model:checked="showApplicationContent"/>
          </a-form-item>
          <a-form-item label="节点距离" style="width: 200px">
            <a-slider @change="handleOpenApplicationContact(contactData)"
                      v-model:value="relationOptions.layouts[0].min_per_width" :min="10" :max="500" :step="1"/>
          </a-form-item>
        </a-form>
        <relation-graph ref="graphRef$" :options="relationOptions">
          <template #node="{node}">
            <div class="my-node" @dblclick="node.data.type==='server'&&graphOpenServer(node.data)">
              <div class="my-node-text">
                {{ node.text }}
              </div>

              <div v-if="node.data.type==='server'" class="my-node-text">
                {{ node.data.ip }}
                <template v-if="node.data.port!==22">
                  :{{ node.data.port }}
                </template>
              </div>
              <div v-show="showApplicationContent" v-else class="my-node-text">
                {{ node.data.content }}
              </div>
            </div>
          </template>
        </relation-graph>
      </a-drawer>
      <a-drawer
          v-model:visible="creationVisible"
          :title="creationType==='create'?'新增':'修改'"
          placement="right"
          :width="creationState.isGroup?'30%':'95%'"
          size="large"
      >
        <template #extra>
          <a-space>
            <a-button @click="creationVisible = false">取消</a-button>
            <a-button type="primary" @click="submitCreate">提交</a-button>
          </a-space>
        </template>

        <a-form
            :label-col="{ span: creationState.isGroup?4:2 }"
            :wrapper-col="{ span: 22 }"
            autocomplete="off"
        >
          <a-divider v-if="!creationState.isGroup">应用</a-divider>
          <a-form-item label="名称" v-bind="creationValidations.name">
            <a-input v-model:value="creationState.name"/>
          </a-form-item>
          <a-form-item label="位置" v-bind="creationValidations.parentId">
            <GroupCascader v-if="creationVisible"
                           :api="applicationApi.groupList"
                           :disabled="creationState.id"
                           v-model:value="creationState.parentId"></GroupCascader>
          </a-form-item>
          <template v-if="!creationState.isGroup">
            <a-form-item label="链接" v-bind="creationValidations.content">
              <a-input v-model:value="creationState.content"/>
            </a-form-item>
            <a-form-item label="负责人手机号" v-bind="creationValidations.masterMobile">
              <a-input v-model:value="creationState.masterMobile"/>
            </a-form-item>
            <a-form-item label="代理" v-bind="creationValidations.proxyId">
              <p-select ref="proxyRef" :api="proxyApi.list" v-model:value="creationState.proxyId"
                        style="width: 90%"></p-select>
            </a-form-item>
            <a-divider>身份</a-divider>
            <a-form autocomplete="off" layout="inline" v-for="(item, index) in creationState.identityArray"
                    :key="index" style="margin-bottom: 8px;justify-content:center">
              <a-form-item label="用户名">
                <a-input v-model:value="creationState.identityArray[index].username"/>
              </a-form-item>
              <a-form-item label="密码">
                <a-input-password v-model:value="creationState.identityArray[index].password"/>
              </a-form-item>
              <a-form-item label="标签">
                <a-auto-complete
                    v-model:value="creationState.identityArray[index].tag"
                    :options="tagOptions"
                >
                  <a-input></a-input>
                </a-auto-complete>
              </a-form-item>
              <a-form-item>

                <a-button @click="creationState.identityArray.splice(index, 1)">
                  <template #icon>
                    <minus-outlined/>
                  </template>
                </a-button>
              </a-form-item>
            </a-form>
            <div style="text-align: center">
              <a-button @click="addIdentity">
                <template #icon>
                  <plus-outlined/>
                </template>
              </a-button>
            </div>

            <a-divider>监控</a-divider>
            <div style="text-align: center">
              <a-button v-if="!creationState.monitorType"
                        @click="creationState.monitorType=ApplicationMonitorTypeEnum.REQUEST.value">
                <template #icon>
                  <plus-outlined/>
                </template>
              </a-button>
              <a-button v-else @click="creationState.monitorType=null">
                <template #icon>
                  <minus-outlined/>
                </template>
              </a-button>
            </div>
            <div v-if="creationState.monitorType"
                 style="display: flex; justify-content: space-between; margin-bottom: 16px;">
              <a-form autocomplete="off" :label-col="{ span: 3 }"
                      :wrapper-col="{ span: 18 }" style="margin-bottom: 8px; width: 50%;">
                <a-form-item label="监控类型">
                  <p-enum-select v-model:value="creationState.monitorType"
                                 :module="ApplicationMonitorTypeEnum"></p-enum-select>
                </a-form-item>
                <a-form-item label="请求地址">
                  <a-input v-model:value="creationState.monitorConfig.url"/>
                </a-form-item>
                <a-form-item label="请求方法">
                  <p-enum-select v-model:value="creationState.monitorConfig.method"
                                 :module="MethodEnum"></p-enum-select>
                </a-form-item>
                <a-form-item label="请求头">
                  <a-form autocomplete="off" layout="inline"
                          v-for="(item, index) in creationState.monitorConfig.headers"
                          :key="index"
                          style="margin-bottom: 8px;">
                    <a-form-item label="名称">
                      <a-input v-model:value="creationState.monitorConfig.headers[index].key"/>
                    </a-form-item>
                    <a-form-item label="内容">
                      <a-input placeholder="多个用,分隔"
                               v-model:value="creationState.monitorConfig.headers[index].value"/>
                    </a-form-item>
                    <a-form-item style="text-align: center">
                      <a-button @click="creationState.monitorConfig.headers.splice(index, 1)">
                        <template #icon>
                          <minus-outlined/>
                        </template>
                      </a-button>
                    </a-form-item>
                  </a-form>
                  <div style="text-align: center">
                    <a-button
                        @click="creationState.monitorConfig.headers.push({key: '', value: ''})">
                      <template #icon>
                        <plus-outlined/>
                      </template>
                    </a-button>
                  </div>
                </a-form-item>
                <a-form-item label="请求体">
                  <a-textarea v-model:value="creationState.monitorConfig.body"></a-textarea>
                </a-form-item>
                <a-form-item label="校验方法">
                  <p-enum-select :module="ApplicationMonitorCheckTypeEnum"
                                 v-model:value="creationState.monitorConfig.checkType"
                                 @change="handleMonitorCheckTypeChange"></p-enum-select>
                </a-form-item>
                <a-form-item label="校验">
                  <template
                      v-if="creationState.monitorConfig.checkType === ApplicationMonitorCheckTypeEnum.JAVASCRIPT.value">
                    <MonacoEditor style="height: 240px"
                                  v-model:value="creationState.monitorConfig.responseRegex" language="javascript" theme="vs-dark"  />
                    <p>javascript代码，实现校验方法check</p>
                    <p>入参为body，字符串类型</p>
                    <p>返回值类型字符串，若为success则校验成功，否则校验失败。校验失败时返回值为失败原因</p>
                  </template>
                  <template v-else>
                    <a-input v-model:value="creationState.monitorConfig.responseRegex"/>
                    <p>单行包含xxx = .*xxx.*</p>
                    <p>多行包含xxx = (?s).*xxx.*</p>
                    <p>等于xxx = ^xxx$</p>
                  </template>


                </a-form-item>
                <a-form-item label="生效时间段">
                  <a-time-range-picker v-model:value="creationState.monitorConfig.timeRange" format="HH:mm"
                                       value-format="HH:mm"/>
                </a-form-item>
              </a-form>
              <div style="display: flex;align-items:center">
                <a-button type="primary" @click="testMonitor(creationState)">测试</a-button>
              </div>
              <a-form autocomplete="off" :label-col="{ span: 3 }"
                      :wrapper-col="{ span: 18 }" style="margin-bottom: 8px; width: 50%;">
                <a-form-item style="height:40%" label="请求">
                  <a-textarea :auto-size="{ minRows: 3, maxRows: 9 }"
                              :value="creationState.monitorTestData.request"></a-textarea>
                </a-form-item>
                <a-form-item style="height:40%" label="响应">
                  <a-textarea :auto-size="{ minRows: 3, maxRows: 9 }"
                              :value="creationState.monitorTestData.response"></a-textarea>
                </a-form-item>
                <a-form-item label="校验">
                  <a-tag color="#f50" v-if="!creationState.monitorTestData.success">失败</a-tag>
                  <a-tag color="#87d068" v-else>成功</a-tag>
                </a-form-item>
              </a-form>
            </div>
            <a-divider>服务器</a-divider>
            <template v-if="creationState.serverList.length">
              <a-form autocomplete="off" layout="inline" v-for="(item, index) in creationState.serverList"
                      :key="index" style="margin-bottom: 8px;justify-content:center">
                <a-form-item label="服务器">
                  <p-cascader v-model:value="creationState.serverList[index].serverId"
                              :api="serverApi.list"></p-cascader>
                </a-form-item>
                <a-form-item label="标签">
                  <a-auto-complete
                      v-model:value="creationState.serverList[index].tag"
                      :options="servertagOptions"
                  >
                    <a-input></a-input>
                  </a-auto-complete>
                </a-form-item>
                <a-form-item>
                  <a-button @click="creationState.serverList.splice(index, 1)">
                    <template #icon>
                      <minus-outlined/>
                    </template>
                  </a-button>
                </a-form-item>
              </a-form>
            </template>
            <div style="text-align: center">
              <a-button @click="creationState.serverList.push({serverId: '', tag: ''})">
                <template #icon>
                  <plus-outlined/>
                </template>
              </a-button>
            </div>
          </template>
        </a-form>
      </a-drawer>
      <a-modal
          v-model:visible="openModalVisible"
          :title="openModalTitle" okText="打开应用" @ok="openApplication(openModalData)">
        <template v-if="openModalData.identityArray?.length">
          <a-divider>应用身份信息</a-divider>
          <a-table :columns="identityColumns" :data-source="openModalData.identityArray" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'username'">
                {{ record.username }}
                <a @click="copyToClipboard(record.username)">
                  <copy-outlined/>
                </a>
              </template>
              <template v-if="column.dataIndex === 'password'">
                {{ record.password }}
                <a @click="copyToClipboard(record.password)">
                  <copy-outlined/>
                </a>
              </template>
            </template>
          </a-table>
        </template>
        <template v-if="openModalData.serverList?.length">
          <a-divider style="margin-top: 36px;">应用服务器信息</a-divider>
          <a-table :columns="serverColumns" :data-source="openModalData.serverList" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'server'">
                {{ record.server?.name }}
              </template>
              <template v-if="column.dataIndex === 'action'">
                <a-button type="link" @click="openServer(openModalData,record)">
                  <link-outlined/>
                </a-button>
              </template>
            </template>
          </a-table>
        </template>
      </a-modal>
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

.my-node-text {
  white-space: nowrap;
  padding: 4px 8px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
