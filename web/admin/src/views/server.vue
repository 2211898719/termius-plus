<script setup>
import {computed, nextTick, onMounted, reactive, ref, watch} from "vue";
import Sortable from 'sortablejs';
import {walk} from "@/utils/treeUtil";
import {serverApi} from "@/api/server";
import _ from "lodash";
import {Form, message} from "ant-design-vue";
import PSelect from "@/components /p-select.vue";
import {proxyApi} from "@/api/proxy";
import PEnumSelect from "@/components /p-enum-select.vue";
import ProxyTypeEnum from "@/enums /ProxyTypeEnum";

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

  spinning.value = true;
  let uuid = crypto.randomUUID();
  while (serverList.value.findIndex(e => e.operationId === uuid) !== -1) {
    uuid = crypto.randomUUID();
  }

  let copyItem = JSON.parse(JSON.stringify(item));
  copyItem.operationId = uuid;
  serverList.value.push(copyItem)
  tagActiveKey.value = copyItem.operationId
}

window.onmessage = (e) => {
  if (e.data === 'connected') {
    spinning.value = false
    let server = serverList.value.find(item => item.operationId === tagActiveKey.value)
    if (server.autoSudo && server.username !== 'root') {
      nextTick(() => {
        handleExecCommand("echo \"" + server.password + "\" | sudo -S ls && sudo -i")
      })
    }
  }
}

const createSortEl = (el) => {
  if (el) {
    Sortable.create(el, {
      group: {
        name: 'shared',
        pull: 'clone',
        put: false // Do not allow items to be put into this list
      },
      scroll: true,
      dataIdAttr: 'data-id',
      dragClass: "sortable-drag",
      animation: 500,
      onEnd: function (evt) {
        //根据evt.oldIndex和evt.newIndex来维护currentData.value
        let oldIndex = evt.oldIndex;
        let newIndex = evt.newIndex;

        let sortData = currentData.value
        let item = sortData[oldIndex];
        sortData.splice(oldIndex, 1);
        sortData.splice(newIndex, 0, item);

        updateSort(sortData)
      },
    });
  }
}

const updateSort = _.debounce(async (sortData) => {
  await serverApi.updateSort(
      sortData.map(item => ({id: item.id, sort: item.sort}))
  )
}, 250, {'maxWait': 1000});


onMounted(() => {
  createSortEl(document.getElementsByClassName('ant-row')[0])

  createSortEl(document.getElementsByClassName('ant-tabs-nav-list')[0])
})

const onCloseServer = (item) => {
  _.remove(serverList.value, i => i.operationId === item)
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
});

let {
  resetFields: proxyResetCreationFields,
  validate: proxyValidateCreation,
  validateInfos: proxyCreationValidations
} = useForm(creationProxyState, creationProxyRules)

const handleProxyVisibleChange = (visible) => {
  if (!visible) {
    proxyResetCreationFields();
  }
}


watch(creationVisible, (visible) => {
  if (!visible) {
    resetCreationFields();
  }
});

let proxyCreationVisible = ref(false);

let proxyRef = ref()
const handleProxyCreate = async () => {
  try {
    await proxyValidateCreation();
  } catch (error) {
    proxyCreationVisible.value = true;
    return false;
  }

  let res = await proxyApi.create(creationProxyState);
  await proxyRef.value.getData()
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

</script>

<template>
  <page-container title="服务器">
    <a-spin :spinning="spinning" tip="等待中...">
      <a-tabs style="width: 100%;"
              type="editable-card"
              :tabBarGutter="5"
              v-model:activeKey="tagActiveKey"
              :tab-position="'left'">
        <template v-slot:addIcon></template>

        <a-tab-pane tab="服务器" key="server" :closable="false">
          <a-space direction="vertical" size="middle" style="width: 100%;">
            <a-card>
              <div style="display: flex;justify-content: space-between">
                <a-breadcrumb>
                  <a-breadcrumb-item v-for="item in groupBreadcrumb" :key="item.id" @click="handleDblclick(item)">
                    <a>{{ item.name }}</a>
                  </a-breadcrumb-item>
                </a-breadcrumb>
                <div>
                  <a-button @click="handleAddServer" class="my-button">新增服务器</a-button>
                  <a-button class="ml10 my-button" @click="handleAddGroup">新增分组</a-button>
                </div>
              </div>
              <div class="mt30 server">
                <a-list :grid="{ gutter: 16, column: 4 }" :data-source="currentData" row-key="id">
                  <template #renderItem="{ item }">
                    <a-list-item @dblclick="handleDblclick(item)">
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
                              <ungroup-outlined v-if="item.isGroup" class="icon-server"/>
                              <hdd-outlined v-else class="icon-server" style="color: #f25e62;"/>
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
        <a-tab-pane tab="代理" key="proxy" :closable="false">
          <a-space direction="vertical" size="middle" style="width: 100%;">
            <a-card>
              <div style="display: flex;justify-content: space-between">
                <div>

                </div>
                <div>
                  <a-button @click="handleAddServer" class="my-button">新增代理</a-button>
                </div>
              </div>
              <div class="mt30 server">
                <a-list :grid="{ gutter: 16, column: 4 }" :data-source="proxyData" row-key="id">
                  <template #renderItem="{ item }">
                    <a-list-item @dblclick="handleDblclick(item)">
                      <template #actions>
                        <a key="list-loadmore-edit">
                          <edit-outlined @click="handleEditServer(item)"/>
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
        <template v-for="server in serverList" :key="server.operationId">
          <a-tab-pane :tab="server.name">
            <template v-slot:closeIcon>
              <close-outlined @click="onCloseServer(server.operationId)"/>
            </template>
            <iframe class="ssh"
                    title="ssh"
                    :id="server.operationId"
                    :src="server.url">
            </iframe>
          </a-tab-pane>
        </template>
      </a-tabs>
    </a-spin>

  </page-container>
  <a-drawer
      v-model:visible="creationVisible"
      title="新增服务器"
      placement="right"
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

        <a-form-item label="自动sudo" v-bind="creationValidations.aotoSudo">
          <a-switch v-model:checked="creationState.aotoSudo"></a-switch>
        </a-form-item>

        <a-form-item label="私钥" v-bind="creationValidations.key">
          <a-input v-model:value="creationState.key"/>
        </a-form-item>
      </template>
      <a-form-item label="代理" v-bind="creationValidations.proxyId">
        <p-select ref="proxyRef" :api="proxyApi.list" v-model:value="creationState.proxyId"
                  style="width: 90%"></p-select>
        <a-popconfirm
            ok-text="确定"
            cancel-text="取消"
            placement="bottomRight"
            v-model:visible="proxyCreationVisible"
            @visibleChange="handleProxyVisibleChange"
        >
          <template v-slot:okButton>
            <a-button type="primary" size="small" @click="handleProxyCreate">确定</a-button>
          </template>
          <template v-slot:title>
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
          </template>
          <template v-slot:icon>

          </template>
          <a-button style="margin-left: 2%" type="primary" shape="circle">
            <template #icon>
              <plus-outlined/>
            </template>
          </a-button>
        </a-popconfirm>
      </a-form-item>


    </a-form>

  </a-drawer>


</template>

<style scoped lang="less">
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
        line-height: 1;

        span {
          color: #cccdd6;
        }
      }

      .ant-list-item-meta-description {
        line-height: 1;
        color: #cccdd6;
        font-size: 0.8em;
        transform: scale(0.9); /* 用缩放来解决 */
        transform-origin: 0 0; /* 左对齐 */
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

.ssh {
  width: 100%;
  height: 500px;
  border: none;
  resize: both; /* 可以调整宽度和高度 */
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


</style>
