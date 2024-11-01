<template>
  <page-container :title="route.meta.title">
    <template #extra>
      <a-button @click="exportBack">导出sql备份</a-button>
      <a-button @click="clearAllConnections">清理所有服务器连接</a-button>
    </template>

    <a-space direction="vertical" size="middle" style="width: 100%;">
      <a-card>
        <a-form layout="inline" :model="searchState">
          <a-form-item>
            <p-select label="username" placeholder="用户" v-model:value="searchState.userId"
                      :api="userApi.list"></p-select>
          </a-form-item>
          <a-form-item>
            <p-cascader placeholder="服务器" v-model:value="searchState.serverId" :api="serverApi.list"></p-cascader>
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
            <template v-if="column.key === 'action'">
              <a @click="showDetail(record)">查看详情</a>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-space>
  </page-container>


  <a-drawer v-model:visible="creationVisible" title="日志详情" placement="right" width="90%"
            size="large" >
    <p-term-log :log-id="creationState.id"></p-term-log>
  </a-drawer>

</template>

<script setup>

import {reactive, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import usePaginationQuery from "@shared/usePaginationQuery";
import {commandLogApi} from "@/api/log";
import {download} from "@/components/tinymce/File";
import {fileApi} from "@/api/file";
import PTermLog from "@/components/p-term-log.vue";
import {serverApi} from "@/api/server";
import PCascader from "@/components/p-cascader.vue";
import PSelect from "@/components/p-select.vue";
import {userApi} from "@/api/user";
import {message} from "ant-design-vue";

const router = useRouter();
const route = useRoute();

const columns = [
  {
    title: '操作用户',
    dataIndex: 'userName',
    key: 'userName',
  },
  {
    title: '服务器',
    dataIndex: 'serverName',
    key: 'serverName',
  },
  {
    title: '操作',
    key: 'action',
  },
];

const searchState = reactive({
  sessionId: "",
  userId: "",
  serverId: "",
  commandData: "",
});

const {
  rows: dataSource,
  pagination,
  fetchPaginationData,
  onSearchSubmit,
  onPaginationChange,
} = usePaginationQuery(router, searchState, commandLogApi.search, false);

const creationVisible = ref(false);
let creationState = reactive({
  sessionId: "",
  userId: "",
  serverId: "",
  commandData: "",
});


const showDetail = (record) => {
  creationState = record;
  creationVisible.value = true;
}

const exportBack = () => {
  download(fileApi.back())
}

const clearAllConnections = async () => {
  await serverApi.clearAllConnections()
  message.success("清理成功")
}
</script>

