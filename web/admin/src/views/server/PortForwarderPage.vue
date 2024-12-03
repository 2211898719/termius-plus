<script setup>
import {defineEmits, defineExpose, reactive, ref, watch} from "vue";
import {useForm} from "ant-design-vue/es/form";
import {portForwardingApi} from "@/api/port-forwarding";
import {message, Modal} from "ant-design-vue";
import ServerListPage from "@/views/server/ServerListPage.vue";
import {copyToClipboard} from "@/utils/copyUtil";

let termiusStyleColumn =ref(Math.floor(window.innerWidth / 300));


const resizeObserver = new ResizeObserver(() => {
  termiusStyleColumn.value = Math.floor(window.innerWidth / 300);
});

resizeObserver.observe(window.document.body);

const creationPortForwardingType = ref('create');
const creationPortForwardingState = reactive({
  forwardingName: "",
  localPort: "",
  serverId: "",
  serverDto: {},
  remotePort: "",
  remoteHost: ""
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

const handlePortForwardingCreate = async () => {
  try {
    await portForwardingValidateCreation();
  } catch (error) {
    portForwardingCreationVisible.value = true;
    return false;
  }

  let res;
  try {
    res = await portForwardingApi[creationPortForwardingType.value](creationPortForwardingState);
  } catch (e) {
    message.error(e.message)
    return
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
}

let selectServerVisible = ref(false);

const handleSelectServer = (server) => {
  creationPortForwardingState.serverId = server.id;
  creationPortForwardingState.serverDto = server;
  creationPortForwardingState.remoteHost = server.ip;
  selectServerVisible.value = false;
}


const handleCloseServer = (item) => {
  portForwardingApi.stop({localPort: item.localPort}).then(() => {
    message.success("操作成功");
    getPortForwardingData()
  }).catch(e => {
    message.error(e.message)
  })
}

const handleCopy = (row) => {
  console.log(row)
  //ssh -L localhost:10086:192.168.201.90:8888 -o ProxyCommand="nc -X 5 -x 192.168.101.189:1080 %h %p"  root@192.168.201.90 -NT
  let command = `ssh -L 127.0.0.1:${row.localPort}:${row.remoteHost}:${row.remotePort} ${row.serverDto.username}@${row.serverDto.ip} -p ${row.serverDto.port} -NT`
  if (row.serverDto.proxy) {
    command += ` -o ProxyCommand="nc -X 5 -x ${row.serverDto.proxy.ip}:${row.serverDto.proxy.port} %h %p"`
  }

  copyToClipboard(command).then(() => {
    Modal.confirm({
      title: '命令已复制到剪贴板，点击确定复制密码',
      onOk() {
        copyToClipboard(row.serverDto.password)
      },
    });
  })

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
              <a-button @click="portForwardingCreation" class="my-button">新增端口转发</a-button>
            </div>
          </div>
          <div class="mt30 server">
            <a-list :grid="{ gutter: 16, column: termiusStyleColumn }" :data-source="portForwardingData" row-key="id">
              <template #renderItem="{ item }">
                <a-dropdown :trigger="['contextmenu']">
                <a-list-item>
                  <template #actions>
                    <a key="list-loadmore-edit">
                      <edit-outlined @click="handleEditPortForwarding(item)"/>
                    </a>
                  </template>
                  <a-card>
                    <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                      <a-list-item-meta
                          :description="item.localHost+':'+item.localPort+'<-'+item.serverDto.name+':'+item.remotePort"
                      >
                        <template #title>
                          <span>{{ item.forwardingName }}</span>
                        </template>
                        <template #avatar>
                          <expand-alt-outlined class="icon-server" style="color: #E45F2B;"/>

                        </template>
                      </a-list-item-meta>
                    </a-skeleton>
                  </a-card>

                </a-list-item>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item key="close" @click="handleCloseServer(item)">
                        <DeleteOutlined/>
                        关闭
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
              <a-button type="primary" @click="handlePortForwardingCreate">提交</a-button>
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
              <a-input-number v-model:value="creationPortForwardingState.localPort" :min="8200" :max="8500"></a-input-number>
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
              <a-input-number v-model:value="creationPortForwardingState.remotePort" :min="1" :max="65535"></a-input-number>
            </a-form-item>
          </a-form>
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

</style>
