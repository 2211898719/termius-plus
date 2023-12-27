<template>
  <page-container>

    <a-space direction="vertical" size="middle" style="width: 100%;">
      <!--      <a-card>-->
      <!--        <a-descriptions title="连接信息" :column="4" bordered>-->
      <!--          <a-descriptions-item label="连接名称">{{ dbConnInfo.connName }}</a-descriptions-item>-->
      <!--          <a-descriptions-item label="主机">{{ dbConnInfo.host }}</a-descriptions-item>-->
      <!--          <a-descriptions-item label="端口">{{ dbConnInfo.port }}</a-descriptions-item>-->
      <!--          <a-descriptions-item label="用户">{{ dbConnInfo.username }}</a-descriptions-item>-->
      <!--        </a-descriptions>-->
      <!--      </a-card>-->
      <div style="display: flex">
        <a-menu
            v-model:openKeys="openKeys"
            v-model:selectedKeys="selectedKeys"
            style="width: 240px;height: calc(100vh - 150px);overflow: auto;"
            mode="inline"
            @click="openTables"
        >
          <a-sub-menu :key="item.name" v-for="item in databases" @titleClick="openDatabases(item)">
            <template #icon>
              <DatabaseOutlined/>
            </template>
            <template #title>{{ item.name }}</template>
            <a-menu-item :key="table.table_name" v-for="table in item.tables ">
              <template #icon>
                <TableOutlined/>
              </template>
              {{ table.table_name }}
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
        <div style="width: calc(100vw - 300px);margin-left: 8px">
          <a-table
              :columns="columns"
              :data-source="usePaginationQueryResult.rows"
              :pagination="usePaginationQueryResult.pagination"
              rowKey="id"
              @change="usePaginationQueryResult.onPaginationChange"
              :scroll="{ y: 'calc(100vh - 270px)',x: 700 }"
          >
            <template #bodyCell="{ column, text, record }">
              <a-input v-model:value="record[column.key]" :bordered="false"  />
            </template>

          </a-table>
        </div>
      </div>
    </a-space>
  </page-container>


</template>

<script setup>

import {reactive, ref} from "vue";
import {onBeforeRouteUpdate, useRouter} from "vue-router";
import {dbApi} from "@/api/db";
import usePaginationQuery from "@shared/usePaginationQuery";

const router = useRouter();

const props = defineProps({
  dbId: {
    type: [Number], default: 1,
  }
})

let dbConnInfo = ref({})
const getDbConnInfo = async () => {
  dbConnInfo.value = await dbApi.getDbConnInfo(props.dbId)
}

getDbConnInfo()

let databases = ref([])
const getDatabase = async () => {
  databases.value = await dbApi.showDatabase({dbId: props.dbId})
}

getDatabase()


const selectedKeys = ref(['1']);
const openKeys = ref(['sub1']);

const openTables = async e => {
  let databasesName = e.keyPath[0];
  let tableName = e.keyPath[1];

  let tableColumns = await getTableColumns(props.dbId, databasesName, tableName)
  columns.value = tableColumns.map(item => {
    return {
      title: item,
      dataIndex: item,
      key: item,
      ellipsis: true,
      width: 200,
    }
  })
  columns.value[0].fixed = 'left'
  columns.value[0].width = 100
  let res = usePaginationQuery(router, searchState, async (query) => {
    return await selectTable(props.dbId, databasesName, tableName, query)
  })

  await res.fetchPaginationData()
  usePaginationQueryResult.value= res
};

const openDatabases = async (database) => {
  if (!Array.isArray(database.tables)) {
    database.tables = await getTables(database.name)
  }
};

onBeforeRouteUpdate(async (to, from, next) => {
  next();
  usePaginationQueryResult.value.pullQueryParams(to.query);
  await usePaginationQueryResult.value.fetchPaginationData();
});

const getTables = async (dbName) => {
  return await dbApi.showTables({dbId: props.dbId, schemaName: dbName})
}

const getTableColumns = async (dbId, schemaName, tableName) => {
  return await dbApi.getTableColumns({dbId, schemaName, tableName})
}
const selectTable = async (dbId, schemaName, tableName, params) => {
  return await dbApi.selectTableData({dbId, schemaName, tableName, ...params})
}

const searchState = reactive({});

let usePaginationQueryResult = ref({})


const columns = ref([

]);

const editableData = reactive({});

const edit = (key) => {
  console.log(key)
  editableData[key] = _.cloneDeep(usePaginationQueryResult.value.rows.filter(item => key === item.id)[0]);
};

</script>
