<script setup>
import {defineEmits, defineExpose, reactive, ref, watch} from "vue";
import ProxyTypeEnum from "@/enums/ProxyTypeEnum";
import {useForm} from "ant-design-vue/es/form";
import {proxyApi} from "@/api/proxy";
import {message} from "ant-design-vue";
import PEnumSelect from "@/components/p-enum-select.vue";

let termiusStyleColumn =ref(Math.floor(window.innerWidth / 300));


const resizeObserver = new ResizeObserver(() => {
  termiusStyleColumn.value = Math.floor(window.innerWidth / 300);
});

resizeObserver.observe(window.document.body);


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

const emit = defineEmits(['createSuccess', 'proxyCreation'])

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

const proxyData = ref([])

const getProxyData = async () => {
  proxyData.value = await proxyApi.list()
}

getProxyData()

const handleProxyCreate = async () => {
  try {
    await proxyValidateCreation();
  } catch (error) {
    proxyCreationVisible.value = true;
    return false;
  }

  let res = await proxyApi[creationProxyType.value](creationProxyState);
  emit('createSuccess', res)
  await getProxyData();

  proxyCreationVisible.value = false;
  message.success("操作成功");
}


const handleEditProxy = (row) => {
  proxyCreationVisible.value = true;
  creationProxyType.value = 'update'
  Object.assign(creationProxyState, row)
}

const proxyCreation = () => {
  proxyCreationVisible.value = true;
  creationProxyType.value = 'create'
}

defineExpose({
  proxyCreation
})


</script>

<template>
  <div class="server-root">
    <div class="server-pane">

      <a-space direction="vertical" size="middle" style="width: 100%;">
        <a-card :bodyStyle="{padding:'12px 12px'}">
          <div style="display: flex;justify-content: space-between">
            <div>

            </div>
            <div>
              <a-button @click="proxyCreation" class="my-button">新增代理</a-button>
            </div>
          </div>
          <div class="mt30 server">
            <a-list :grid="{ gutter: 16, column: termiusStyleColumn }" :data-source="proxyData" row-key="id">
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
                          <safety-certificate-outlined class="icon-server" style="color: #E45F2B;"/>
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
            v-model:visible="proxyCreationVisible"
            :title="creationProxyType ==='create'?'新增代理':'修改代理'"
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
      </a-space>
    </div>
  </div>
</template>

<style scoped lang="less">
@import url('../css/termius');

</style>
