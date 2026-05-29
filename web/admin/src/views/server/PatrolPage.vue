<template>
  <div class="patrol-page" style="padding: 16px;">
    <a-tabs v-model:activeKey="activeTab">
      <!-- AI 对话 Tab -->
      <a-tab-pane key="chat" tab="AI 对话">
        <div class="chat-container">
          <div class="chat-messages" ref="messagesRef">
            <div v-if="messages.length === 0" class="chat-welcome">
              <div class="welcome-icon">&#129302;</div>
              <div class="welcome-title">AI 巡查助手</div>
              <div class="welcome-desc">我可以帮你检查服务器状态、分析问题、执行运维命令</div>
              <div class="welcome-examples">
                <div class="example-item" @click="inputText='检查所有服务器的磁盘使用情况'">检查所有服务器的磁盘使用情况</div>
                <div class="example-item" @click="inputText='查看 nginx 配置和证书状态'">查看 nginx 配置和证书状态</div>
                <div class="example-item" @click="inputText='列出所有正在运行的服务'">列出所有正在运行的服务</div>
              </div>
            </div>
            <div v-for="(msg, index) in messages" :key="index"
                 :class="['message', msg.role]">
              <div class="message-avatar">
                <span v-if="msg.role === 'user'">&#128100;</span>
                <span v-else>&#129302;</span>
              </div>
              <div class="message-body">
                <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                <div v-if="msg.needsConfirmation" class="message-actions">
                  <a-button type="primary" size="small" @click="confirmCommand(msg, index)">确认执行</a-button>
                  <a-button danger size="small" @click="rejectCommand(msg, index)">拒绝</a-button>
                </div>
              </div>
            </div>
            <div v-if="streaming" class="message assistant">
              <div class="message-avatar"><span>&#129302;</span></div>
              <div class="message-body">
                <div class="message-content" v-html="renderMarkdown(streamContent)"></div>
                <div class="typing-cursor"></div>
              </div>
            </div>
          </div>
          <div class="chat-input-area">
            <a-input
                v-model:value="inputText"
                placeholder="描述你想检查的问题，如：检查 nginx 证书是否过期"
                @pressEnter="sendMessage"
                :disabled="streaming"
                size="large"
            />
            <a-button type="primary" @click="sendMessage" :loading="streaming" size="large">
              {{ streaming ? '思考中...' : '发送' }}
            </a-button>
          </div>
        </div>
      </a-tab-pane>

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
import {ref, onMounted, nextTick, onUnmounted} from 'vue';
import {patrolApi} from '@/api/patrol';
import {message as antMessage} from 'ant-design-vue';
import markdownIt from 'markdown-it';

const activeTab = ref('chat');
const md = markdownIt();

// Chat state
const messages = ref([]);
const inputText = ref('');
const messagesRef = ref(null);
const conversationId = ref(null);
const streaming = ref(false);
const streamContent = ref('');
let eventSource = null;

const renderMarkdown = (content) => md.render(content || '');

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
};

const sendMessage = async () => {
  const text = inputText.value.trim();
  if (!text || streaming.value) return;

  messages.value.push({role: 'user', content: text});
  inputText.value = '';
  scrollToBottom();

  streaming.value = true;
  streamContent.value = '';

  const url = patrolApi.chatStreamUrl(text, conversationId.value);
  eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    const data = event.data;
    if (data === '[DONE]') {
      finishStream();
      return;
    }
    streamContent.value += data;
    scrollToBottom();
  };

  eventSource.onerror = () => {
    finishStream();
  };
};

const finishStream = () => {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  if (streamContent.value) {
    messages.value.push({role: 'assistant', content: streamContent.value});
    streamContent.value = '';
  }
  streaming.value = false;
  scrollToBottom();
};

const confirmCommand = async (msg) => {
  msg.needsConfirmation = false;
  inputText.value = '确认执行: ' + msg.pendingCommand;
  await sendMessage();
};

const rejectCommand = (msg) => {
  msg.needsConfirmation = false;
  messages.value.push({role: 'assistant', content: '已取消执行'});
  scrollToBottom();
};

onUnmounted(() => {
  if (eventSource) {
    eventSource.close();
  }
});
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

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  background: #141414;
  border-radius: 12px;
  overflow: hidden;
}

/* Welcome */
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #888;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: #ccc;
  margin-bottom: 8px;
}

.welcome-desc {
  font-size: 14px;
  margin-bottom: 24px;
}

.welcome-examples {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.example-item {
  padding: 10px 16px;
  background: #1e1e1e;
  border: 1px solid #333;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #aaa;
  transition: all 0.2s;
}

.example-item:hover {
  border-color: #1890ff;
  color: #1890ff;
  background: #1a2a3a;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: #264f78;
}

.message.assistant .message-avatar {
  background: #2d2d2d;
}

.message-body {
  max-width: 80%;
  min-width: 0;
}

.message.user .message-body {
  text-align: right;
}

.message-content {
  padding: 10px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-break: break-word;
}

.message.user .message-content {
  background: #264f78;
  color: #fff;
  border-top-right-radius: 4px;
}

.message.assistant .message-content {
  background: #1e1e1e;
  color: #d4d4d4;
  border-top-left-radius: 4px;
}

.message-content :deep(p) {
  margin: 0 0 10px 0;
}

.message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(code) {
  background: #2d2d2d;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: #e06c75;
}

.message-content :deep(pre) {
  background: #0d0d0d;
  padding: 14px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-content :deep(pre code) {
  background: none;
  color: #abb2bf;
  padding: 0;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.message-content :deep(blockquote) {
  border-left: 3px solid #1890ff;
  padding-left: 12px;
  margin: 8px 0;
  color: #888;
}

.message-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.message-content :deep(th),
.message-content :deep(td) {
  border: 1px solid #333;
  padding: 6px 10px;
  text-align: left;
}

.message-content :deep(th) {
  background: #252525;
}

/* Typing cursor */
.typing-cursor {
  display: inline-block;
  width: 8px;
  height: 16px;
  background: #1890ff;
  margin-left: 2px;
  animation: blink 1s steps(2) infinite;
  vertical-align: text-bottom;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Actions */
.message-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.message.user .message-actions {
  justify-content: flex-end;
}

/* Input */
.chat-input-area {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #222;
  background: #1a1a1a;
}

.chat-input-area :deep(.ant-input) {
  background: #252525;
  border-color: #333;
  color: #fff;
  border-radius: 8px;
}

.chat-input-area :deep(.ant-input:focus) {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.chat-input-area :deep(.ant-btn) {
  border-radius: 8px;
  min-width: 80px;
}

/* Scrollbar */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #333;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #444;
}
</style>
