<script setup>
import {defineEmits, defineExpose, reactive, ref, watch} from "vue";
import {useForm} from "ant-design-vue/es/form";
import {commandApi} from "@/api/command";
import {message} from "ant-design-vue";
import PEditor from "@/components/tinymce/p-editor.vue";

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

const emit = defineEmits(['createSuccess', 'commandCreation'])

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

  let res = await commandApi[creationCommandType.value](creationCommandState);
  emit('createSuccess', res)
  await getCommandData();

  commandCreationVisible.value = false;
  message.success("操作成功");
}

const handleEditCommand = (row) => {
  commandCreationVisible.value = true;
  creationCommandType.value = 'update'
  Object.assign(creationCommandState, row)
}

const commandCreation = () => {
  commandCreationVisible.value = true;
  creationCommandType.value = 'create'
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
                <a-list-item>
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
              </template>
            </a-list>
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
            <a-form-item label="备注：" v-bind="commandCreationValidations.remark">
              <p-editor v-model:value="creationCommandState.remark"></p-editor>
            </a-form-item>
          </a-form>
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
