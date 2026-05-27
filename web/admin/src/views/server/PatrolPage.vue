<template>
  <div class="patrol-page" style="padding: 16px;">
    <a-tabs v-model:activeKey="activeTab">
      <!-- 脚本管理 Tab -->
      <a-tab-pane key="scripts" tab="巡检脚本">
        <div style="margin-bottom: 16px; display: flex; gap: 8px;">
          <a-button type="primary" @click="showGenerateModal = true">AI 生成脚本</a-button>
          <a-button @click="loadScripts">刷新</a-button>
          <a-button type="primary" ghost @click="executeAllScripts">执行全部</a-button>
        </div>
        <a-table :data-source="scripts" :columns="scriptColumns" row-key="id" :loading="scriptsLoading">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'enabled'">
              <a-switch :checked="record.enabled" @change="toggleScript(record)" />
            </template>
            <template v-if="column.key === 'category'">
              <a-tag :color="categoryColor(record.category)">{{ record.category }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a @click="executeScript(record)">执行</a>
                <a-popconfirm title="确认删除？" @confirm="deleteScript(record.id)">
                  <a style="color: red">删除</a>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 执行记录 Tab -->
      <a-tab-pane key="tasks" tab="执行记录">
        <a-table :data-source="tasks" :columns="taskColumns" row-key="id" :loading="tasksLoading">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
            </template>
            <template v-if="column.key === 'output'">
              <a @click="showOutput(record)">查看</a>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- AI 生成脚本弹窗 -->
    <a-modal v-model:visible="showGenerateModal" title="AI 生成巡检脚本" @ok="generateScript"
             :confirmLoading="generating" width="600px">
      <a-textarea v-model:value="generateDescription" :rows="4"
                  placeholder="描述你想检查什么，如：检查所有磁盘分区使用率，超过 80% 报警" />
      <div v-if="generatedScript" style="margin-top: 16px;">
        <div style="margin-bottom: 8px; font-weight: bold;">生成结果预览：</div>
        <a-textarea v-model:value="generatedScript.scriptContent" :rows="10"
                    style="font-family: monospace;" />
        <div style="margin-top: 8px;">
          <a-input v-model:value="generatedScript.name" placeholder="脚本名称" style="margin-bottom: 8px;" />
          <a-select v-model:value="generatedScript.category" style="width: 200px;">
            <a-select-option value="disk">磁盘</a-select-option>
            <a-select-option value="nginx">Nginx</a-select-option>
            <a-select-option value="security">安全</a-select-option>
            <a-select-option value="service">服务</a-select-option>
            <a-select-option value="custom">自定义</a-select-option>
          </a-select>
        </div>
      </div>
    </a-modal>

    <!-- 输出查看弹窗 -->
    <a-modal v-model:visible="showOutputModal" title="执行输出" :footer="null" width="700px">
      <pre style="max-height: 400px; overflow: auto; background: #1a1a1a; padding: 12px; border-radius: 4px; color: #ccc;">{{ currentOutput }}</pre>
    </a-modal>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import {patrolApi} from '@/api/patrol';
import {message} from 'ant-design-vue';

const activeTab = ref('scripts');
const scripts = ref([]);
const tasks = ref([]);
const scriptsLoading = ref(false);
const tasksLoading = ref(false);
const showGenerateModal = ref(false);
const showOutputModal = ref(false);
const generateDescription = ref('');
const generating = ref(false);
const generatedScript = ref(null);
const currentOutput = ref('');

const scriptColumns = [
  {title: '名称', dataIndex: 'name', key: 'name'},
  {title: '分类', dataIndex: 'category', key: 'category'},
  {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
  {title: '启用', dataIndex: 'enabled', key: 'enabled'},
  {title: '操作', key: 'action'},
];

const taskColumns = [
  {title: '脚本ID', dataIndex: 'scriptId', key: 'scriptId'},
  {title: '服务器ID', dataIndex: 'serverId', key: 'serverId'},
  {title: '状态', dataIndex: 'status', key: 'status'},
  {title: '执行时间', dataIndex: 'executedAt', key: 'executedAt'},
  {title: '输出', key: 'output'},
];

const statusColor = (status) => ({ok: 'green', warning: 'orange', error: 'red'}[status] || 'default');
const categoryColor = (cat) => ({disk: 'blue', nginx: 'purple', security: 'red', service: 'green', custom: 'default'}[cat] || 'default');

const loadScripts = async () => {
  scriptsLoading.value = true;
  try {
    scripts.value = await patrolApi.listScripts();
  } catch (e) {
    message.error('加载脚本失败: ' + e.message);
  }
  scriptsLoading.value = false;
};

const generateScript = async () => {
  if (!generateDescription.value.trim()) return;
  generating.value = true;
  try {
    generatedScript.value = await patrolApi.generateScript(generateDescription.value);
    message.success('脚本已生成，请确认后保存');
  } catch (e) {
    message.error('生成失败: ' + e.message);
  }
  generating.value = false;
};

const toggleScript = async (record) => {
  try {
    await patrolApi.updateScript({id: record.id, enabled: !record.enabled});
    await loadScripts();
  } catch (e) {
    message.error('更新失败: ' + e.message);
  }
};

const executeScript = async (record) => {
  message.info('开始执行...');
  try {
    await patrolApi.executeScriptOnAll(record.id);
    message.success('执行完成');
  } catch (e) {
    message.error('执行失败: ' + e.message);
  }
};

const executeAllScripts = async () => {
  message.info('开始执行全部巡检...');
  try {
    await patrolApi.executeAll();
    message.success('全部执行完成');
  } catch (e) {
    message.error('执行失败: ' + e.message);
  }
};

const deleteScript = async (id) => {
  try {
    await patrolApi.deleteScript(id);
    await loadScripts();
  } catch (e) {
    message.error('删除失败: ' + e.message);
  }
};

const showOutput = (record) => {
  currentOutput.value = record.output || '(无输出)';
  showOutputModal.value = true;
};

onMounted(() => {
  loadScripts();
});
</script>
