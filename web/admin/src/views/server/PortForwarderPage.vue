<script setup>
import {defineEmits, defineExpose, nextTick, reactive, ref, watch} from "vue";
import {useForm} from "ant-design-vue/es/form";
import {portForwardingApi} from "@/api/port-forwarding";
import {message, Modal} from "ant-design-vue";
import ServerListPage from "@/views/server/ServerListPage.vue";
import {copyToClipboard} from "@/utils/copyUtil";
import PortForWardingStatusEnum from "@/enums/PortForWardingStatusEnum";
import RelationGraph from "relation-graph/vue3";

let termiusStyleColumn = ref(Math.floor(window.innerWidth / 300));


const resizeObserver = new ResizeObserver(() => {
  termiusStyleColumn.value = Math.floor(window.innerWidth / 300);
});

resizeObserver.observe(window.document.body);

const creationPortForwardingType = ref('create');
let defaultState = {
  forwardingName: "",
  localHost: "",
  localPort: "",
  serverId: "",
  serverDto: {},
  remotePort: "",
  remoteHost: ""
}
const creationPortForwardingState = reactive({
  ...defaultState
});

const creationPortForwardingRules = reactive({
  forwardingName: [
    {
      required: true,
      message: "请输入名称",
    }
  ],
  localPort: [
    {
      required: true,
      message: "请输入端口",
    },
    {
      pattern: /^\d+$/,
      message: "端口格式不正确"
    }
  ],
  remotePort: [
    {
      required: true,
      message: "请输入端口",
    },
    {
      pattern: /^\d+$/,
      message: "端口格式不正确"
    }
  ],
  serverId: [
    {
      required: true,
      message: "请选择服务器",
    }
  ],
  remoteHost: [
    {
      required: true,
      message: "请输入host",
    },
    {
      pattern: /^(?:(?:[0-9]{1,3}\.){3}[0-9]{1,3}|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,})$/,
      message: "host格式不正确"
    }
  ],
});

const emit = defineEmits(['createSuccess', 'portForwardingCreation'])

let {
  resetFields: portForwardingResetCreationFields,
  validate: portForwardingValidateCreation,
  validateInfos: portForwardingCreationValidations
} = useForm(creationPortForwardingState, creationPortForwardingRules)

let portForwardingCreationVisible = ref(false);

watch(portForwardingCreationVisible, (visible) => {
  if (!visible) {
    portForwardingResetCreationFields();
  }
});

const portForwardingData = ref([])

const getPortForwardingData = async () => {
  portForwardingData.value = await portForwardingApi.list()
}

[...document.getElementsByClassName('markdown-body')].map(body => {
  return {
    title: body.getElementsByTagName('h1')[0].innerHTML,
    des: body.getElementsByTagName('p')[0].innerHTML,
    body: body.innerHTML
  }
})

getPortForwardingData()

const submitLoading = ref(false)

const handlePortForwardingCreate = async () => {
  submitLoading.value = true
  try {
    await portForwardingValidateCreation();
  } catch (error) {
    portForwardingCreationVisible.value = true;
    submitLoading.value = false
    return false;
  }

  let res;
  try {
    res = await portForwardingApi[creationPortForwardingType.value](creationPortForwardingState);
  } catch (e) {
    message.error(e.message)
    return
  } finally {
    submitLoading.value = false
  }
  emit('createSuccess', res)
  await getPortForwardingData();

  portForwardingCreationVisible.value = false;
  message.success("操作成功");
}


const handleEditPortForwarding = (row) => {
  portForwardingCreationVisible.value = true;
  creationPortForwardingType.value = 'update'
  Object.assign(creationPortForwardingState, row)
}

const portForwardingCreation = () => {
  portForwardingCreationVisible.value = true;
  creationPortForwardingType.value = 'create'
  Object.assign(creationPortForwardingState, defaultState)
  creationPortForwardingState.localHost = currentIp.value
}

let selectServerVisible = ref(false);

const handleSelectServer = (server) => {
  creationPortForwardingState.serverId = server.id;
  creationPortForwardingState.serverDto = server;
  creationPortForwardingState.remoteHost = server.ip;
  selectServerVisible.value = false;
}

let currentIp = ref('')

const getLocalIp = () => {
  portForwardingApi.getLocalIp().then(res => {
    currentIp.value = res
  })
}

getLocalIp()

const handleCloseServer = (item) => {
  portForwardingApi.stop({id: item.id}).then(() => {
    message.success("操作成功");
    getPortForwardingData()
  }).catch(e => {
    message.error(e.message)
  })
}


const handleDelServer = (item) => {
  portForwardingApi.del({id: item.id}).then(() => {
    message.success("操作成功");
    getPortForwardingData()
  }).catch(e => {
    message.error(e.message)
  })
}


const handleStartServer = (item) => {
  item.startLoading = true
  item.startPercent = 0
  let interval = setInterval(() => {
    if (item.startPercent >= 99) {
      item.startPercent = 99
      return
    }
    item.startPercent += 3
  }, 1000)
  portForwardingApi.start({id: item.id}).then(() => {
    interval && clearInterval(interval)
    item.startPercent = 100
    setTimeout(() => {
      message.success("操作成功");
      getPortForwardingData()
      item.startPercent = 0
      item.startLoading = false
    }, 500)
  }).catch(e => {
    message.error(e.message)
    item.startPercent = 0
    item.startLoading = false
  })
}

const handleCopy = (row) => {
  //ssh -L localhost:10086:192.168.201.90:8888 -o ProxyCommand="nc -X 5 -x 192.168.101.189:1080 %h %p"  root@192.168.201.90 -NT
  let command = buildSshCommand(row)

  copyToClipboard(command).then(() => {
    Modal.confirm({
      title: '命令已复制到剪贴板，点击确定复制密码',
      onOk() {
        copyToClipboard(row.serverDto.password)
      },
    });
  })
}

const copyPassword = (row) => {
  copyToClipboard(row.password).then(() => {

  })
}

const copySshCommand = (row) => {
  let command = buildSshCommand(row)
  copyToClipboard(command).then(() => {
  })
}

const buildSshCommand = (row) => {
  let command = `ssh -L 127.0.0.1:${row.localPort}:${row.remoteHost}:${row.remotePort} ${row.serverDto.username}@${row.serverDto.ip} -p ${row.serverDto.port} -NT`
  if (row.serverDto.proxy) {
    command += ` -o ProxyCommand="nc -X 5 -x ${row.serverDto.proxy.ip}:${row.serverDto.proxy.port} %h %p"`
  }

  return command
}

watch(() => selectServerVisible, (val) => {
  graphRef$.value.setOptions(relationOptions.value)
})

watch(() => creationPortForwardingState, (val) => {
  if (!val) {
    return
  }

  if (!portForwardingCreationVisible.value) {
    return;
  }

  if (!creationPortForwardingState.localHost || !creationPortForwardingState.localPort || !creationPortForwardingState.remoteHost || !creationPortForwardingState.remotePort) {
    return;
  }

  let jsonData = {
    rootId: 'app',
    nodes: [
      {
        id: 'user',
        text: "",
        data: {type: 'user'},
        color: '#E45F2B'
      },
      {
        id: 'me',
        text: creationPortForwardingState.localHost + ":" + creationPortForwardingState.localPort,
        data: {type: 'server', ...creationPortForwardingState},
        color: '#E45F2B'
      },
      {
        id: 'to',
        text: creationPortForwardingState.remoteHost + ":" + creationPortForwardingState.remotePort,
        data: {type: 'server', ...creationPortForwardingState},
        color: '#E45F2B'
      }
    ],
    lines: [{
      from: 'user',
      to: 'me',
      text: "请求",
      dashType: 1
    }, {
      from: 'me',
      to: 'to',
      text: "请求",
      dashType: 1
    }],
  }

  if (creationPortForwardingState.serverDto.ip !== creationPortForwardingState.remoteHost) {
    jsonData = {
      rootId: 'app',
      nodes: [
        {
          id: 'user',
          text: "",
          data: {type: 'user'},
          color: '#E45F2B'
        },
        {
          id: 'me',
          text: creationPortForwardingState.localHost + ":" + creationPortForwardingState.localPort,
          data: {type: 'server', ...creationPortForwardingState},
          color: '#E45F2B'
        },
        {
          id: 'proxy',
          text: creationPortForwardingState.serverDto.ip + "【代理】",
          data: {type: 'server', ...creationPortForwardingState.serverDto},
          color: '#9600fa'
        },
        {
          id: 'to',
          text: creationPortForwardingState.remoteHost + ":" + creationPortForwardingState.remotePort,
          data: {type: 'server', ...creationPortForwardingState},
          color: '#E45F2B'
        }
      ],
      lines: [{
        from: 'user',
        to: 'me',
        text: "请求",
        dashType: 1
      }, {
        from: 'me',
        to: 'proxy',
        text: "请求",
        dashType: 1
      }, {
        from: 'proxy',
        to: 'to',
        text: "请求",
        dashType: 1
      }],
    }
  }

  nextTick(() => {
    graphRef$.value.setJsonData(jsonData)
  })

}, {deep: true})

let graphRef$ = ref(null)

let relationOptions = ref({
  "defaultNodeShape": 1,
  "downloadImageFileName": "下载",
  "defaultLineShape": 1,
  "allowShowMiniToolBar": false,
  "layouts": [
    {
      from: 'left',
      "label": "中心",
      "layoutName": "tree",
      'defaultJunctionPoint': 'border',
      'defaultNodeShape': 0,
      "min_per_width": "180",
      "max_per_width": "301",
      "min_per_height": "150",
      "max_per_height": "501"
    }
  ]
})

const handleDblclick = (item) => {
  window.open(`http://${item.localHost}:${item.localPort}`)
}

defineExpose({
  portForwardingCreation
})


</script>

<template>
  <div class="server-root">
    <div class="server-pane">

      <a-space direction="vertical" size="middle" style="width: 100%;">
        <a-card :bodyStyle="{padding:'12px 12px'}" style="border:none">
          <div class="body-root">
            <div style="display: flex;justify-content: space-between">
              <div>

              </div>
              <div>
                <a-button @click="getPortForwardingData" class="my-button">刷新</a-button>
                <a-button @click="portForwardingCreation" class="my-button" style="margin-left: 8px;">新增端口转发
                </a-button>
              </div>
            </div>
            <div class="mt30 server">
              <a-list :grid="{ gutter: 16, column: termiusStyleColumn }" :data-source="portForwardingData" row-key="id">
                <template #renderItem="{ item }">
                  <a-dropdown :trigger="['contextmenu']">
                    <a-list-item @dblclick="handleDblclick(item)">
                      <template #actions>
                        <a key="list-loadmore-edit">
                          <edit-outlined @click="handleEditPortForwarding(item)"/>
                        </a>
                      </template>
                      <a-badge :dot="true" :color="item.status===PortForWardingStatusEnum.STOP.value?'red':'green'"
                               :offset="[-2,3]">
                        <a-card>
                          <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                            <a-list-item-meta
                            >
                              <template #title>
                                <span>{{ item.forwardingName }}</span>
                              </template>
                              <template #description>
                                <span>
                                  <template v-if="!item.startLoading">{{
                                      item.localHost + ':' + item.localPort + '<-' + item.serverDto.name + ':' + item.remotePort
                                    }}</template>
                                  <template v-else> <a-progress :percent="item.startPercent"/></template>
                                </span>
                              </template>
                              <template #avatar>
                                <expand-alt-outlined class="icon-server" style="color: #E45F2B;"/>

                              </template>
                            </a-list-item-meta>
                          </a-skeleton>
                        </a-card>
                      </a-badge>
                    </a-list-item>

                    <template #overlay>
                      <a-menu>
                        <a-menu-item key="close" @click="handleStartServer(item)">
                          <DeleteOutlined/>
                          启动
                        </a-menu-item>
                        <a-menu-item key="close" @click="handleCloseServer(item)">
                          <DeleteOutlined/>
                          关闭
                        </a-menu-item>
                        <a-menu-item key="close" @click="handleDelServer(item)">
                          <DeleteOutlined/>
                          删除
                        </a-menu-item>
                        <a-menu-item key="copy" @click="handleCopy(item)">
                          <code-outlined/>
                          复制为ssh命令
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </template>
              </a-list>
            </div>
          </div>
        </a-card>
        <a-drawer
            v-model:visible="portForwardingCreationVisible"
            :title="creationPortForwardingType ==='create'?'新增端口转发':'修改端口转发'"
            placement="right"
            width="40%"
            size="large"
        >
          <template #extra>
            <a-space>
              <a-button @click="portForwardingCreationVisible = false">取消</a-button>
              <a-button type="primary" @click="handlePortForwardingCreate" :loading="submitLoading">提交</a-button>
            </a-space>
          </template>

          <a-form
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 15 }"
              autocomplete="off"
          >
            <a-form-item label="名称：" v-bind="portForwardingCreationValidations.forwardingName">
              <a-input v-model:value="creationPortForwardingState.forwardingName"/>
            </a-form-item>
            <a-form-item label="本地端口：" v-bind="portForwardingCreationValidations.localPort">
              <a-input-number v-model:value="creationPortForwardingState.localPort" :min="8200"
                              :max="8500"></a-input-number>
            </a-form-item>
            <a-form-item label="服务器：" v-bind="portForwardingCreationValidations.serverId">
              <div @click="selectServerVisible = true">
              <span v-if="creationPortForwardingState.serverId">

                <a-tag>{{ creationPortForwardingState.serverDto.name }}</a-tag>
              </span>
                <a-button v-else>选择服务器</a-button>
              </div>
            </a-form-item>
            <a-form-item label="远端ip：" v-bind="portForwardingCreationValidations.remoteHost">
              <a-input v-model:value="creationPortForwardingState.remoteHost"></a-input>
            </a-form-item>
            <a-form-item label="服务器端口：" v-bind="portForwardingCreationValidations.remotePort">
              <a-input-number v-model:value="creationPortForwardingState.remotePort" :min="1"
                              :max="65535"></a-input-number>
            </a-form-item>

            <a-form-item label="本地命令："
                         v-if="creationPortForwardingState.serverId && creationPortForwardingState.localPort && creationPortForwardingState.remoteHost && creationPortForwardingState.remotePort">
              <a-alert :message="buildSshCommand(creationPortForwardingState)" type="success"/>
              <div style="margin-top: 8px;">
                <a-button type="primary" @click="copySshCommand(creationPortForwardingState)">复制命令</a-button>
                <a-button type="primary" @click="copyPassword(creationPortForwardingState.serverDto)"
                          style="margin-left: 16px;">复制密码
                </a-button>
              </div>
            </a-form-item>
          </a-form>
          <div>
            <a-divider>示意图</a-divider>
            <relation-graph ref="graphRef$" :options="relationOptions" style="height: 200px">
              <template #node="{node}">
                <div class="my-node">
                  <div class="my-node-text">

                    <template v-if="node.data.type === 'user'">
                      <user-outlined/>
                      用户
                    </template>
                    <template v-else>
                      {{ node.text }}
                    </template>
                  </div>
                </div>
              </template>
            </relation-graph>
          </div>
          <a-drawer
              v-model:visible="selectServerVisible"
              :title="'选择服务器'"
              placement="right"
              width="80%"
              size="large"
          >
            <server-list-page :column="4" @change="handleSelectServer" :select="true"></server-list-page>
          </a-drawer>
        </a-drawer>
      </a-space>
    </div>
  </div>
</template>

<style scoped lang="less">
@import url('../css/termius');

:deep(.ant-badge) {
  width: 100%;
}

:deep(.ant-progress) {
  line-height: 1;
}

:deep(.ant-progress-text) {
  color: #fff;
}

.my-node {
  width: 100%;
}

</style>
