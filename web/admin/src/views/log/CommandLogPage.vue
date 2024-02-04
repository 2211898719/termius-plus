<template>
  <page-container :title="route.meta.title">
    <template #extra>
      <a-button @click="exportBack">导出sql备份</a-button>
    </template>

    <a-space direction="vertical" size="middle" style="width: 100%;">
      <a-card>
        <a-form layout="inline" :model="searchState">
          <a-form-item>
            <a-input v-model:value="searchState.sessionId" placeholder="会话id" allow-clear/>
          </a-form-item>
          <a-form-item>
            <a-input v-model:value="searchState.userId" placeholder="用户ID" allow-clear/>
          </a-form-item>
          <a-form-item>
            <a-input v-model:value="searchState.serverId" placeholder="服务器id" allow-clear/>
          </a-form-item>
          <a-form-item>
            <a-input v-model:value="searchState.commandData" placeholder="命令数据" allow-clear/>
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


  <a-drawer v-model:visible="creationVisible" title="日志详情" placement="right" width="60%"
            size="large" >
    <div>


    <p-term-log :log-id="creationState.id"></p-term-log>
    </div>
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
} = usePaginationQuery(router, searchState, commandLogApi.search);

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
</script>

