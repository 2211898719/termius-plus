<script setup>
import {computed, createVNode, nextTick, onMounted, reactive, ref, watch} from "vue";
import Sortable from 'sortablejs';
import {walk} from "@/utils/treeUtil";
import {serverApi} from "@/api/server";
import _ from "lodash";
import {Form, message, Modal} from "ant-design-vue";
import PSelect from "@/components/p-select.vue";
import {proxyApi} from "@/api/proxy";
import PEnumSelect from "@/components/p-enum-select.vue";
import ProxyTypeEnum from "@/enums /ProxyTypeEnum";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import PSftp from "@/components/p-sftp.vue";


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

//监听iframe加载完成，执行sudo等初始化命令
window.onmessage = (e) => {
  if (e.data.event === 'connected') {
    spinning.value = false
    let server = serverList.value.find(item => item.operationId === tagActiveKey.value)
    console.log(server.autoSudo)
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
      createSortEl(document.getElementsByClassName('split-box')[0])
    })
  }

  let rule = /^\/[^\0]+$/;
  if (e.data.event === 'command') {
    if (!link.value){
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
  var elementsByClassNameElement = document.getElementsByClassName('ant-row')[0];

  let able = createSortEl(elementsByClassNameElement)
  console.log(able.toArray())
  console.log(able.sortData)

  createSortEl(document.getElementsByClassName('ant-tabs-nav-list')[0])
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

</script>

<template>
  <div class="server-root">


    <a-spin :spinning="spinning" tip="等待中...">
      <a-tabs style="width: 100%;"
              type="editable-card"
              :tabBarGutter="5"
              :hideAdd="true"
              v-model:activeKey="tagActiveKey"
              :tab-position="'left'">

        <a-tab-pane class="server-pane" tab="服务器" key="server" :closable="false">
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
        <template v-for="server in serverList" :key="server.operationId">
          <a-tab-pane :tab="server.name">
            <template v-slot:closeIcon>
              <close-outlined @click="onCloseServer(server.operationId)"/>
            </template>
            <div class="split-box">
              <div class="ssh-content">
                <a-card title="终端">
                  <!--                  <template #extra><a href="#">more</a></template>-->
                  <iframe class="ssh"
                          title="ssh"
                          :id="server.operationId"
                          :src="server.url">
                  </iframe>
                </a-card>
              </div>
              <div class="link-root">
                <a-popover title="sftp关联终端">
                  <template #content>
                    <p>在终端使用部分命令时自动切换路径如（pwd...）</p>
                  </template>
                <link-outlined :class="{'link-icon':true,'link-icon-activation':link,}" @click="link=!link"/>
                </a-popover>
              </div>
              <div class="sftp-content">
                <a-card title="sftp">
                  <p-sftp class="sftp" ref="sftpEl" :operation-id="server.operationId" :server-id="server.id"></p-sftp>
                </a-card>
              </div>
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
        width="50%"
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
            <a-input v-model:value="creationState.key"/>
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

  .split-box{
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

  .link-root{
    display: flex;
    justify-content: center;
    margin-top: 12px;
    align-items: center;
    .link-icon{
      font-size: 2em;
    }

    .link-icon-activation{
      color: #1daa6c;
    }
  }


}

</style>
