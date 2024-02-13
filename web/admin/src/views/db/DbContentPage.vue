<template>

  <a-space direction="vertical" size="middle" style="width: 100%;display: flex">
    <a-card>
      <code-mirror
          basic
          :lang="lang"
          :minimal="true"
          :extensions="[oneDark]"

          v-model="sqlData">
      </code-mirror>
      <div style="display: flex;justify-content: end">
        <a-button class="mt5" @click="executeSql" type="primary">执行</a-button>
      </div>
    </a-card>
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
      <div style="width: calc(100vw - 400px);margin-left: 8px">
        <a-table
            :columns="columns"
            :data-source="usePaginationQueryResult.rows"
            :pagination="usePaginationQueryResult.pagination"
            rowKey="id"
            @change="usePaginationQueryResult.onPaginationChange"
            :scroll="{ y: 'calc(100vh - 450px)',x: 700 }"
        >
          <!--            <template #bodyCell="{ column, text, record }">-->
          <!--              <a-input v-model:value="record[column.key]" :bordered="false"  />-->
          <!--            </template>-->

        </a-table>
      </div>
    </div>
  </a-space>


</template>

<script setup>

import {computed, nextTick, reactive, ref} from "vue";
import {onBeforeRouteUpdate, useRouter} from "vue-router";
import {dbApi} from "@/api/db";
import usePaginationQuery from "@shared/usePaginationQuery";
import CodeMirror from 'vue-codemirror6';
import {MySQL, sql} from "@codemirror/lang-sql";
// import {sql} from "@codemirror/lang-sql";
import {oneDark} from '@codemirror/theme-one-dark'

const router = useRouter();

const props = defineProps({
  db: {
    type: [Number, Object], default: () => {
      return {}
    }
  },
  type: {type: Boolean, default: false}
})

// let dbConnInfo = ref({})
// const getDbConnInfo = async () => {
//   dbConnInfo.value = await dbApi.getDbConnInfo(props.db.id)
// }
//
// getDbConnInfo()

let databases = ref([])
const getDatabase = async () => {
  databases.value = await dbApi.showDatabase({dbId: props.db.id, type: props.type})
}

getDatabase()


const selectedKeys = ref(['1']);
const openKeys = ref(['sub1']);

const openTables = async e => {
  let databasesName = e.keyPath[0];
  let tableName = e.keyPath[1];

  let tableColumns = await getTableColumns(props.db.id, databasesName, tableName)
  columns.value = tableColumns.map(item => {
    return {
      title: item.column_comment ? item.column_comment+`(${item.column_name})` : item.column_name,
      dataIndex: item.column_name,
      key: item.column_name,
      ellipsis: true,
      width: 200,
      ordinal_position: item.ordinal_position
    }
  })
  //找到column_key为PRI的字段把它放到第一列,设置为固定列
  let priColumn = tableColumns.filter(item => item.column_key === 'PRI')[0]
  if (priColumn) {
    let priColumnIndex = columns.value.findIndex(item => item.key === priColumn.column_name)
    if (priColumnIndex !== -1) {
      let priColumn = columns.value.splice(priColumnIndex, 1)[0]
      columns.value.unshift(priColumn)
      columns.value[0].fixed = 'left'
    }
  }
  //按ordinal_position排序
  columns.value = _.sortBy(columns.value, ['ordinal_position'], ['desc'])

  let res = usePaginationQuery(router, searchState, async (query) => {
    return await selectTable(props.db.id, databasesName, tableName, query)
  }, false)

  //根据res中的数据自动列宽


  await nextTick(async () => {
    await res.fetchPaginationData()
    usePaginationQueryResult.value = res
  })

};

const openDatabases = async (database) => {
  if (!Array.isArray(database.tables)) {
    let dbInfos = await getTables(database.name)
    database.tables = dbInfos.tables
    database.columns = dbInfos.columns
  }
};

onBeforeRouteUpdate(async (to, from, next) => {
  next();
  usePaginationQueryResult.value.pullQueryParams(to.query);
  await usePaginationQueryResult.value.fetchPaginationData();
});

const getTables = async (dbName) => {
  return await dbApi.showTables({dbId: props.db.id, type: props.type, schemaName: dbName})
}

const getTableColumns = async (dbId, schemaName, tableName) => {
  return await dbApi.getTableColumns({dbId, schemaName, tableName, type: props.type})
}
const selectTable = async (dbId, schemaName, tableName, params) => {
  return await dbApi.selectTableData({dbId, type: props.type, schemaName, tableName, ...params})
}

const searchState = reactive({});

let usePaginationQueryResult = ref({})


const columns = ref([]);

const editableData = reactive({});

const edit = (key) => {
  editableData[key] = _.cloneDeep(usePaginationQueryResult.value.rows.filter(item => key === item.id)[0]);
};

let lang = computed(() => {
  let data = JSON.parse(JSON.stringify(databases.value))
  data.forEach(d => {
    if (!Array.isArray(d.tables)) {
      d.tables = []
    }

    d.tables = d.tables.map(t => {
      return {label: t.table_name, detail: t.table_comment}
    })
  })

  let res = {}
  //库.表的提示
  data.forEach(d => {
    res[d.name] = d.tables
  })

  //库.表.字段的提示
  databases.value.forEach(d => {
    if (!Array.isArray(d.columns)) {
      d.columns = []
    }
    d.columns.forEach(c => {
      let key = c.table_schema + '.' + c.table_name;
      if (!Array.isArray(res[key])) {
        res[key] = []
      }

      res[key].push({
        label: c.column_name,
        detail: c.column_comment
      })
    })
  })

  return sql({
    dialect: MySQL,
    schema: res
  });
})


let sqlData = ref('')


const executeSql = () => {
  dbApi.executeSql({dbId: props.db.id, sql: sqlData.value, type: props.type}).then(res => {
    if (res.length) {
      let tableColumns = Object.keys(res[0]).map(item => {
        return {
          title: item,
          dataIndex: item,
          key: item,
          ellipsis: true,
          width: 200,
        }
      })

      columns.value = tableColumns
      usePaginationQueryResult.value.rows = res
    }
  })
}


</script>

<style scoped>
>>> .cm-content{
  min-height: 100px;
}
</style>
