<template>
    <page-container title="用户">
        <template #extra>
            <a-button type="primary" @click="creationVisible = true;tag='create'">新增</a-button>
        </template>

        <a-space direction="vertical" size="middle" style="width: 100%;">
            <a-card>
                <a-form
                    layout="inline"
                    :model="searchState"
                >
                    <a-form-item>
                        <a-input v-model:value="searchState.username" placeholder="用户名" allow-clear/>
                    </a-form-item>

                    <a-form-item>
                        <a-button type="primary" html-type="submit" @click="onSearchSubmit">检索</a-button>
                    </a-form-item>

                </a-form>
            </a-card>

            <a-card>
                <a-table
                    :columns="columns"
                    :data-source="users"
                    :pagination="pagination"
                    rowKey="id"
                    @change="onPaginationChange"
                >
                    <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'action'">
                            <a @click="showDetail(record)">详情</a>
                            <a-divider type="vertical" />
                          <a @click="handleUpdate(record)">修改</a>
                          <a-divider type="vertical" />
                          <a-popconfirm title="确认操作吗？" ok-text="确认" cancel-text="取消" @confirm="handleLock(record)">
                            <a >{{ record.locked? '解锁' : '锁定' }}</a>
                          </a-popconfirm>
                        </template>
                        <template v-else-if="column.key === 'registerAt'">
                            <div>{{ $f.datetime(record.registerAt) }}</div>
                            <div><a-typography-text type="secondary">{{ record.registerIp }}</a-typography-text></div>
                        </template>
                        <template v-else-if="column.key === 'loginAt'">
                            <div>{{ $f.datetime(record.loginAt) }}</div>
                            <div><a-typography-text type="secondary">{{ record.loginIp }}</a-typography-text></div>
                        </template>
                    </template>
                </a-table>
            </a-card>
        </a-space>
    </page-container>

    <a-drawer
        v-model:visible="detailVisible"
        title="用户详情"
        placement="right"
        size="large"
    >
        <a-tabs v-model:activeKey="detailTab">
            <a-tab-pane key="base" tab="基本信息">
                <a-descriptions bordered :column="2">
                    <a-descriptions-item label="ID">{{ detailUser.id }}</a-descriptions-item>
                    <a-descriptions-item label="用户名">{{ detailUser.username }}</a-descriptions-item>
                    <a-descriptions-item label="注册时间">{{ $f.datetime(detailUser.registerAt) }}</a-descriptions-item>
                    <a-descriptions-item label="注册IP">{{ detailUser.registerIp }}</a-descriptions-item>
                    <a-descriptions-item label="登录时间">{{ $f.datetime(detailUser.loginAt) }}</a-descriptions-item>
                    <a-descriptions-item label="登录IP">{{ detailUser.loginIp }}</a-descriptions-item>
                </a-descriptions>
            </a-tab-pane>
        </a-tabs>
    </a-drawer>

    <a-drawer
        v-model:visible="creationVisible"
        title="新增用户"
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

            <a-form-item label="用户名" v-bind="creationValidations.username">
                <a-input v-model:value="creationState.username" />
            </a-form-item>

            <a-form-item label="密码" v-bind="creationValidations.password">
                <a-input-password v-model:value="creationState.password" autocomplete="new-password" />
            </a-form-item>
          <a-form-item label="角色" v-bind="creationValidations.roleIds">
            <p-select :api="roleApi.list" v-model:value="creationState.roleIds" mode="multiple"></p-select>
          </a-form-item>

        </a-form>

    </a-drawer>

</template>

<script setup>

import {reactive, ref, watch} from "vue";
import {useRouter} from "vue-router";
import usePaginationQuery from "@shared/usePaginationQuery";
import {userApi} from "@/api/user";
import {Form, message} from 'ant-design-vue';
import PSelect from "@/components/p-select.vue";
import {roleApi} from "@/api/role";

const useForm = Form.useForm;
const router = useRouter();

const columns = [
    {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
    },
    {
        title: '用户名',
        dataIndex: 'username',
        key: 'username',
    },
    {
        title: '注册时间',
        key: 'registerAt',
        dataIndex: 'registerAt',
        sorter: true,
    },
    {
        title: '最后登录',
        key: 'loginAt',
        dataIndex: 'loginAt',
        sorter: true,
    },
    {
        title: '操作',
        key: 'action',
    },
];

let tag = ref('create')

const searchState = reactive({
    username: '',
    email: '',
});

const {
    rows: users,
    pagination,
    fetchPaginationData,
    onSearchSubmit,
    onPaginationChange,
} = usePaginationQuery(router, searchState, userApi.search);


const creationVisible = ref(false);
const creationState = reactive({
   username: "",
   roleIds: [],
   password: "",
});

const creationRules = reactive({
    username: [
        {
            required: true,
            message: "请输入用户名",
        }
    ],
    password: [
{
            min: 6,
            message: "密码长度不能小于6位",
      }
    ]
});

const {resetFields: resetCreationFields, validate: validateCreation, validateInfos: creationValidations} = useForm(creationState, creationRules);

watch(creationVisible, (visible) => {
    if (!visible) {
        resetCreationFields();
    }
});

const submitCreate = async () => {
    try {
        await validateCreation();
    } catch(error) {
        return;
    }

    await userApi[tag.value](creationState);
    message.success("新增用户成功");

    creationVisible.value = false;
    await fetchPaginationData();
}

const detailVisible = ref(false);
const detailUser = ref(null);
const detailTab = ref('base');
const showDetail = (user) => {
    detailVisible.value = true;
    detailUser.value = user;
}

const handleUpdate = (row) => {
  Object.assign(creationState, row);
  tag.value = 'update';
  creationVisible.value = true;
}

const handleLock = (row) => {
  if (!row.locked){
    userApi.lock(row.id).then(() => {
      message.success("锁定成功");
      fetchPaginationData();
    });
  }else{
    userApi.unlock(row.id).then(() => {
      message.success("解锁成功");
      fetchPaginationData();
    });
  }

}

</script>
