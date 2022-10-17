<template>
    <page-container title="用户">
        <template #extra>
            <a-button type="primary" @click="creationVisible = true">新增</a-button>
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
                        <a-input v-model:value="searchState.email" placeholder="Email" allow-clear/>
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
                            <a-dropdown>
                                <a class="ant-dropdown-link" @click.prevent>更多 <DownOutlined /></a>
                                <template #overlay>
                                    <a-menu>
                                        <a-menu-item>
                                            <a href="javascript:;">重置密码</a>
                                        </a-menu-item>
                                        <a-menu-item>
                                            <a href="javascript:;">修改邮箱</a>
                                        </a-menu-item>
                                        <a-menu-item>
                                            <a href="javascript:;">禁用用户</a>
                                        </a-menu-item>
                                    </a-menu>
                                </template>
                            </a-dropdown>
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
                    <a-descriptions-item label="Email"  :span="2">{{ detailUser.email }}</a-descriptions-item>
                    <a-descriptions-item label="注册时间">{{ $f.datetime(detailUser.registerAt) }}</a-descriptions-item>
                    <a-descriptions-item label="注册IP">{{ detailUser.registerIp }}</a-descriptions-item>
                    <a-descriptions-item label="登录时间">{{ $f.datetime(detailUser.loginAt) }}</a-descriptions-item>
                    <a-descriptions-item label="登录IP">{{ detailUser.loginIp }}</a-descriptions-item>
                </a-descriptions>
            </a-tab-pane>
            <a-tab-pane key="logs" tab="登录日志">

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

            <a-form-item label="电子邮箱" v-bind="creationValidations.email">
                <a-input v-model:value="creationState.email" />
            </a-form-item>

        </a-form>

    </a-drawer>

</template>

<script setup>

import {reactive, ref, watch} from "vue";
import { DownOutlined } from '@ant-design/icons-vue';
import {useRouter} from "vue-router";
import usePaginationQuery from "@shared/usePaginationQuery";
import {userApi} from "@/api/user";
import { Form, message } from 'ant-design-vue';

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
        title: 'Email',
        dataIndex: 'email',
        key: 'email',
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
   email: "",
   password: "",
});

const creationRules = reactive({
    username: [
        {
            required: true,
            message: "请输入用户名",
        }
    ],
    email: [
        {
            required: true,
            message: "请输入电子邮箱地址",
        },
        {
            type: "email",
            message: "电子邮箱地址格式不正确"
        }
    ],
    password: [
        {
            required: true,
            message: "请输入密码",
        },
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

    await userApi.create(creationState);
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

</script>
