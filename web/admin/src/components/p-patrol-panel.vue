<template>
  <div class="patrol-panel" v-if="visible">
    <div class="patrol-panel-header">
      <span>AI 巡查助手</span>
      <a-button type="text" size="small" @click="close" style="color: #fff">×</a-button>
    </div>
    <div class="patrol-panel-messages" ref="messagesRef">
      <div v-for="(msg, index) in messages" :key="index"
           :class="['message', msg.role]">
        <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
        <div v-if="msg.needsConfirmation" class="message-actions">
          <a-button type="primary" size="small" @click="confirmCommand(msg, index)">确认执行</a-button>
          <a-button size="small" @click="rejectCommand(msg, index)">拒绝</a-button>
        </div>
      </div>
    </div>
    <div class="patrol-panel-input">
      <a-input
          v-model:value="inputText"
          placeholder="描述你想检查的问题，如：检查 nginx 证书是否过期"
          @pressEnter="sendMessage"
          :disabled="loading"
      />
      <a-button type="primary" @click="sendMessage" :loading="loading">发送</a-button>
    </div>
  </div>
</template>

<script setup>
import {ref, nextTick, defineProps, defineEmits} from 'vue';
import {patrolApi} from '@/api/patrol';
import markdownIt from 'markdown-it';

const props = defineProps({
  visible: Boolean,
  serverId: Number
});

const emit = defineEmits(['update:visible']);

const md = markdownIt();
const messages = ref([]);
const inputText = ref('');
const loading = ref(false);
const messagesRef = ref(null);
const conversationId = ref(null);

const renderMarkdown = (content) => md.render(content || '');

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
};

const sendMessage = async () => {
  if (!inputText.value.trim() || loading.value) return;

  const userMessage = inputText.value.trim();
  messages.value.push({role: 'user', content: userMessage});
  inputText.value = '';
  scrollToBottom();

  loading.value = true;
  try {
    const response = await patrolApi.chat(userMessage, props.serverId, conversationId.value);
    conversationId.value = response.conversationId;
    messages.value.push({
      role: 'assistant',
      content: response.reply,
      needsConfirmation: response.needsConfirmation,
      pendingCommand: response.pendingCommand
    });
  } catch (e) {
    messages.value.push({role: 'assistant', content: '请求失败: ' + e.message});
  }
  loading.value = false;
  scrollToBottom();
};

const confirmCommand = async (msg, index) => {
  msg.needsConfirmation = false;
  loading.value = true;
  try {
    const response = await patrolApi.chat('确认执行: ' + msg.pendingCommand, props.serverId, conversationId.value);
    conversationId.value = response.conversationId;
    messages.value.push({role: 'assistant', content: response.reply});
  } catch (e) {
    messages.value.push({role: 'assistant', content: '执行失败: ' + e.message});
  }
  loading.value = false;
  scrollToBottom();
};

const rejectCommand = (msg) => {
  msg.needsConfirmation = false;
  messages.value.push({role: 'assistant', content: '已取消执行'});
  scrollToBottom();
};

const close = () => {
  conversationId.value = null;
  messages.value = [];
  emit('update:visible', false);
};
</script>

<style scoped>
.patrol-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 400px;
  background: #1e1e1e;
  border-left: 1px solid #333;
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.patrol-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #333;
  color: #fff;
  font-weight: bold;
}

.patrol-panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.message {
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 8px;
}

.message.user {
  background: #264f78;
  color: #fff;
  margin-left: 40px;
}

.message.assistant {
  background: #2d2d2d;
  color: #ccc;
  margin-right: 40px;
}

.message-content {
  word-break: break-word;
}

.message-content :deep(p) {
  margin: 0 0 8px 0;
}

.message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(code) {
  background: #444;
  padding: 2px 4px;
  border-radius: 3px;
}

.message-content :deep(pre) {
  background: #1a1a1a;
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
}

.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.patrol-panel-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #333;
}

.patrol-panel-input :deep(.ant-input) {
  flex: 1;
  background: #2d2d2d;
  border-color: #444;
  color: #fff;
}
</style>
