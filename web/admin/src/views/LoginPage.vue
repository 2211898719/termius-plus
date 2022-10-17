<template>
    <div class="login-page">
        <a-card title="登录" :bordered="false" style="width: 400px">
            <template #extra><span style="color: #999;">{{ config.name }}</span></template>
            <a-form
                :model="formState"
                name="login"
                autocomplete="off"
                @finish="onFinish"
                @finishFailed="onFinishFailed"
            >
                <a-form-item name="username" v-bind="validations.username">
                    <a-input v-model:value="formState.username">
                        <template #prefix>
                            <UserOutlined  />
                        </template>
                    </a-input>
                </a-form-item>
                <a-form-item name="password" v-bind="validations.password">
                    <a-input-password v-model:value="formState.password">
                        <template #prefix>
                            <LockOutlined class="site-form-item-icon" />
                        </template>
                    </a-input-password>
                </a-form-item>

                <a-form-item>
                    <a-button type="primary" block html-type="submit" :disabled="formState.username === '' || formState.password === ''">登录</a-button>
                </a-form-item>
            </a-form>
        </a-card>
    </div>
</template>

<script setup>

import {reactive } from 'vue';
import {Form, message} from 'ant-design-vue';
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';
import {useAuthStore} from "@shared/store/useAuthStore";
import {authApi} from "@/api/auth";
import {useRouter} from 'vue-router';
import config from "@/config";

const useForm = Form.useForm;
const store = useAuthStore();
const router = useRouter();

const formState = reactive({
    username: '',
    password: '',
});

const formRules = reactive({
    username: [
        {
            required: true,
            message: "请输入用户名",
        }
    ],

    password: [
        {
            required: true,
            message: "请输入密码",
        },
    ]
});

const {validateInfos: validations} = useForm(formState, formRules);



const onFinish = async (params) => {
    try {
        const loginedUser = await authApi.login(params);
        store.login(loginedUser);
        await router.push({name: 'welcome'});
    } catch (err) {
        message.error(err.message);
    }
};

const onFinishFailed = (error) => {
    console.log("onFinishFailed", error);
}

</script>

<style lang="less" scoped>
.login-page {
    background: #f3f3f3;
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center
}
</style>
