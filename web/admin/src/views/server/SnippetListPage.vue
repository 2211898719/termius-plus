<script setup>
import {createVNode, defineEmits, defineExpose, reactive, ref, watch} from "vue";
import {useForm} from "ant-design-vue/es/form";
import {commandApi} from "@/api/command";
import {message, Modal} from "ant-design-vue";
import PEditor from "@/components/tinymce/p-editor.vue";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import ServerListPage from "@/views/server/ServerListPage.vue";


let termiusStyleColumn =ref(Math.floor(window.innerWidth / 300));


const resizeObserver = new ResizeObserver(() => {
  termiusStyleColumn.value = Math.floor(window.innerWidth / 300);
});

resizeObserver.observe(window.document.body);


const creationCommandType = ref('create');
const creationCommandState = reactive({
  name: "",
  command: "",
  remark: "",
  serverIds: [],
  serverDtos: []
});

const creationCommandRules = reactive({
  name: [
    {
      required: true,
      message: "请输入名称",
    }
  ],
  command: [
    {
      required: true,
      message: "请输入命令",
    }
  ]
});

const emit = defineEmits(['createSuccess', 'commandCreation', 'openServer'])

let {
  resetFields: commandResetCreationFields,
  validate: commandValidateCreation,
  validateInfos: commandCreationValidations
} = useForm(creationCommandState, creationCommandRules)

let commandCreationVisible = ref(false);

watch(commandCreationVisible, (visible) => {
  if (!visible) {
    commandResetCreationFields();
  }
});

const commandData = ref([])

const getCommandData = async () => {
  commandData.value = await commandApi.list()
}

getCommandData()

const handleCommandCreate = async () => {
  try {
    await commandValidateCreation();
  } catch (error) {
    commandCreationVisible.value = true;
    return false;
  }

  let submitData = {
    ...creationCommandState
  }

  submitData.serverIds = submitData.serverIds.filter(s=>!!s).join(',')

  let res = await commandApi[creationCommandType.value](submitData);
  emit('createSuccess', res)
  await getCommandData();

  commandCreationVisible.value = false;
  message.success("操作成功");
}

const handleEditCommand = (row) => {
  commandCreationVisible.value = true;
  creationCommandType.value = 'update'
  Object.assign(creationCommandState, row)

  creationCommandState.serverIds = row.serverIds.split(',').map(item => parseInt(item))
}

const commandCreation = () => {
  commandCreationVisible.value = true;
  creationCommandType.value = 'create'
}

const handleDel = (item) => {
  let modal = Modal.confirm({
    title: '确定要删除吗?',
    icon: createVNode(ExclamationCircleOutlined),
    content: item.isGroup ? '删除组会丢失组下所有服务器信息！！' : '',
    onOk: async () => {
      try {
        await commandApi.delete({id: item.id})
        await getCommandData()
        message.success("操作成功");
      } catch (e) {
        modal.destroy()
      }
    },
    onCancel() {
    },
  });
}

let selectServerVisible = ref(false)

const removeServer = (id) => {
  creationCommandState.serverIds = creationCommandState.serverIds.filter(item => item !== id);
  creationCommandState.serverDtos = creationCommandState.serverDtos.filter(item => item.id !== id);
}

const handleSelectServer = (server) => {
  if (!Array.isArray(creationCommandState.serverIds)) {
    creationCommandState.serverIds = []
  }
  if (!Array.isArray(creationCommandState.serverDtos)) {
    creationCommandState.serverDtos = []
  }
  if (creationCommandState.serverIds.includes(server.id)) {
    message.error("已经选择了该服务器");
    return
  }

  creationCommandState.serverIds.push(server.id);
  creationCommandState.serverDtos.push(server);
  selectServerVisible.value = false;
}

let argsVisible = ref(false)
let serverArgsData = ref({})
let currentSnippet = ref({})
let args = ref([])

const openServer = (snippet) => {
  if (!snippet.serverDtos || !snippet.serverDtos.length) {
    return
  }

  currentSnippet.value = snippet

  //提取命令模版中的占位符例如{{}}
  const regex = /{{(.*?)}}/g;
  const matches = [];
  let match;

  while ((match = regex.exec(snippet.command)) !== null) {
    matches.push(match[1].trim());
  }

  if (matches.length !== 0) {
    args.value = matches
    argsVisible.value = true
    snippet.serverDtos.forEach(s => {
      if (!serverArgsData.value[s.id]) {
        serverArgsData.value[s.id] = {}
      }
      args.value.forEach(arg => {
        let cache = localStorage.getItem(`command_${snippet.id}_${s.id}_${arg}`);
        serverArgsData.value[s.id][arg] = cache ? cache : ''
      })
    })

    return
  }

  let serverDtos = JSON.parse(JSON.stringify(snippet.serverDtos))
  serverDtos.forEach(s => {
    s.execCommand = snippet.command
  })

  for (let i = 0; i < serverDtos.length; i++) {
    setTimeout(() => {
      emit('openServer', serverDtos[i])
    }, i * 100)
  }

}
const handleExec = () => {
  argsVisible.value = false
  currentSnippet.value.serverDtos.forEach(s => {
    let execCommand = currentSnippet.value.command
    args.value.forEach(arg => {
      execCommand = execCommand.replace(`{{${arg}}}`, serverArgsData.value[s.id][arg])
      //使用命令id，服务器id，参数值做一个本地缓存
      localStorage.setItem(`command_${currentSnippet.value.id}_${s.id}_${arg}`, serverArgsData.value[s.id][arg])
    })
    s.execCommand = execCommand

    setTimeout(() => {
      emit('openServer', s)
    }, 100)
  })
}


defineExpose({
  commandCreation
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
              <a-button @click="commandCreation" class="my-button">新增命令</a-button>
            </div>
          </div>
          <div class="mt30 server">
            <a-list :grid="{ gutter: 16, column: termiusStyleColumn }" :data-source="commandData" row-key="id">
              <template #renderItem="{ item }">
                <a-dropdown :trigger="['contextmenu']">
                  <a-list-item @dblclick="openServer(item)">
                  <template #actions>
                    <a key="list-loadmore-edit">
                      <edit-outlined @click="handleEditCommand(item)"/>
                    </a>
                  </template>
                  <a-card>
                    <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                      <a-list-item-meta
                          :description="item.command"
                      >
                        <template #title>
                          <span>{{ item.name }}</span>
                        </template>
                        <template #avatar>
                          <mac-command-outlined class="icon-server" style="color: #F6C445;" />
                        </template>
                      </a-list-item-meta>
                    </a-skeleton>
                  </a-card>

                </a-list-item>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item key="1" @click="handleDel(item)">
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
        <a-drawer
            v-model:visible="commandCreationVisible"
            :title="creationCommandType ==='create'?'新增命令片段':'修改命令片段'"
            placement="right"
            width="70%"
            size="large"
        >
          <template #extra>
            <a-space>
              <a-button @click="commandCreationVisible = false">取消</a-button>
              <a-button type="primary" @click="handleCommandCreate">提交</a-button>
            </a-space>
          </template>

          <a-form

              :label-col="{ span: 3 }"
              :wrapper-col="{ span: 18 }"
              autocomplete="off"
          >
            <a-form-item label="名称：" v-bind="commandCreationValidations.name">
              <a-input v-model:value="creationCommandState.name"/>
            </a-form-item>
            <a-form-item label="命令：" v-bind="commandCreationValidations.command">
              <a-textarea :auto-size="{ minRows: 3, maxRows: 10 }" v-model:value="creationCommandState.command"></a-textarea>
            </a-form-item>
            <a-form-item label="服务器：" v-bind="commandCreationValidations.serverId">
              <div>
              <span v-if="creationCommandState.serverIds?.length">
                <a-tag v-for="item in creationCommandState.serverDtos" :key="item.id" closable
                       @close="removeServer(item.id)">{{ item.name }}</a-tag>
              </span>
                <a-button type="link" @click="selectServerVisible = true">添加服务器</a-button>
              </div>
            </a-form-item>
            <a-form-item label="备注：" v-bind="commandCreationValidations.remark">
              <p-editor v-model:value="creationCommandState.remark"></p-editor>
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
        <a-drawer
            v-model:visible="argsVisible"
            title="命令参数"
            placement="right"
            width="70%"
            size="large"
        >
          <template #extra>
            <a-space>
              <a-button @click="argsVisible = false">取消</a-button>
              <a-button type="primary" @click="handleExec">提交</a-button>
            </a-space>
          </template>
          <a-card style="margin-top: 8px" :key="item.id"
                  v-for="item in currentSnippet.serverDtos" :title="item.name">
            <a-form
                :label-col="{ span: 6 }"
                :wrapper-col="{ span: 15 }"
                autocomplete="off"

            >
              <a-form-item v-for="arg in args" :key="arg" :label="arg+':'">
                <a-input v-model:value="serverArgsData[item.id][arg]"/>
              </a-form-item>
            </a-form>
          </a-card>

        </a-drawer>
      </a-space>
    </div>
  </div>
</template>

<style scoped lang="less">
@import url('../css/termius');
//#9AC1F0
//#72FA93
//#A0E548

</style>
