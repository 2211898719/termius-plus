<template>
  <page-container :title="route.meta.title">
    <template #extra>
      <a-button type="primary" @click="handleCreate">新增</a-button>
    </template>

    <a-space direction="vertical" size="middle" style="width: 100%;">
      <a-card>
        <a-form layout="inline" :model="searchState">
          <a-form-item>
            <a-input v-model:value="searchState.name" placeholder="角色名" allow-clear/>
          </a-form-item>
          <a-form-item>
            <a-input v-model:value="searchState.serverPermission" placeholder="服务器权限" allow-clear/>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" @click="onSearchSubmit">检索</a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card>
        <a-table :columns="columns" :data-source="dataSource" :pagination="pagination" rowKey="id"
                 @change="onPaginationChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'&&record.name!=='ROLE_SUPER_ADMIN'">
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical"/>
              <a-popconfirm title="确定要删除吗？" @confirm="handleDelete(record)">
                <a>删除</a>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-space>
  </page-container>


  <a-drawer v-model:visible="creationVisible" :title="creationType==='create'?'新增':'编辑'" placement="right"
            size="large" @afterVisibleChange="closed">
    <template #extra>
      <a-space>
        <a-button @click="creationVisible = false">取消</a-button>
        <a-button type="primary" @click="submit">提交</a-button>
      </a-space>
    </template>

    <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }" autocomplete="off">
      <a-form-item label="角色名" v-bind="creationValidations.name">
        <a-input v-model:value="creationState.name"/>
      </a-form-item>
      <a-form-item label="服务器权限" v-bind="creationValidations.serverPermission">
        <p-cascader :multiple="true" v-model:value="creationState.serverPermission" :api="serverApi.list"></p-cascader>
      </a-form-item>
    </a-form>

  </a-drawer>

</template>

<script setup>

import {reactive, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import usePaginationQuery from "@shared/usePaginationQuery";
import {roleApi} from "@/api/role";
import {Form, message} from 'ant-design-vue';
import PCascader from "@/components/p-cascader.vue";
import {serverApi} from "@/api/server";

const useForm = Form.useForm;
const router = useRouter();
const route = useRoute();

const columns = [
  {
    title: '角色名',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '操作',
    key: 'action',
  },
];

const searchState = reactive({
  name: "",
  serverPermission: "",
});

const {
  rows: dataSource,
  pagination,
  fetchPaginationData,
  onSearchSubmit,
  onPaginationChange,
} = usePaginationQuery(router, searchState, roleApi.search);

const creationType = ref("create")
const creationVisible = ref(false);
let creationState = reactive({
  name: "",
  serverPermission: [],
});

const creationRules = reactive({
  name: [
    {
      required: true,
      message: "请输入角色名",
    },
  ],
});

const {
  resetFields: resetCreationFields,
  validate: validateCreation,
  validateInfos: creationValidations,
  clearValidate: clearValidate
} = useForm(creationState, creationRules);

const handleCreate = () => {
  creationVisible.value = true
  creationType.value = "create"
}

const handleEdit = (row) => {
  creationVisible.value = true
  creationType.value = "update"
  Object.assign(creationState, row)
  try {
    creationState.serverPermission = JSON.parse(creationState.serverPermission)
  } catch (e) {
    creationState.serverPermission = []
  }

}

const handleDelete = async (row) => {
  let res = await roleApi.delete({id: row.id});
  if (res) {
    await fetchPaginationData();
    message.success("删除成功")
  }
}

const closed = (visible) => {
  if (visible) {
    return
  }

  resetCreationFields();
  clearValidate();
}

const submit = async () => {
  try {
    await validateCreation();
  } catch (error) {
    return;
  }
  let subData = JSON.parse(JSON.stringify(creationState))
  subData.serverPermission = JSON.stringify(subData.serverPermission)
  try {
    await roleApi[creationType.value](subData);
  } catch (error) {
    message.error(error.message);
    return;
  }

  message.success(creationType.value === "create" ? "新增成功" : "修改成功");

  creationVisible.value = false;
  await fetchPaginationData();
}


</script>

