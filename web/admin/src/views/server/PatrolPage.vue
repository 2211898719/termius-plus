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
                <div class="example-item" @click="setInputText('检查所有服务器的磁盘使用情况')">检查所有服务器的磁盘使用情况</div>
                <div class="example-item" @click="setInputText('查看 nginx 配置和证书状态')">查看 nginx 配置和证书状态</div>
                <div class="example-item" @click="setInputText('列出所有正在运行的服务')">列出所有正在运行的服务</div>
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
                <div v-if="currentToolCall" class="tool-call-indicator">
                  <div class="tool-call-name">{{ currentToolCall.name }}</div>
                  <div class="tool-call-status">执行中...</div>
                </div>
                <div class="typing-cursor"></div>
              </div>
            </div>
          </div>
          <div class="chat-input-area">
            <div class="ai-input-wrapper">
              <div
                  ref="mentionInputRef"
                  class="mention-input-container"
                  :class="{ disabled: streaming }"
                  contenteditable="true"
                  :data-placeholder="streaming ? '' : '描述你想检查的问题，输入 @ 可选择服务器'"
                  @input="handleInput"
                  @keydown="handleMentionKeydown"
                  @click="handleInputClick"
              ></div>
              <div v-if="showMention && filteredServerTree.length > 0" class="mention-dropdown" ref="mentionDropdownRef">
                <div class="mention-list">
                  <template v-for="group in filteredServerTree" :key="group.id">
                    <div class="mention-group" v-if="group.isGroup">
                      <div class="mention-group-header" @click="toggleGroup(group)">
                        <folder-outlined style="margin-right: 8px; flex-shrink: 0;" />
                        <span class="group-name">{{ group.name }}</span>
                        <span class="group-count">({{ group.children?.length || 0 }})</span>
                        <right-outlined class="group-arrow" :class="{ expanded: group._expanded }" />
                      </div>
                      <div class="mention-group-children" v-if="group._expanded">
                        <div v-for="s in group.children" :key="s.id"
                             :class="['mention-item', { active: s._index === mentionIndex }]"
                             @click="selectMentionServer(s)"
                             @mouseenter="mentionIndex = s._index">
                          <hdd-outlined v-if="s.os === 'LINUX'" style="color: #E45F2B; margin-right: 8px;" />
                          <windows-outlined v-else-if="s.os === 'WINDOWS'" style="color: #E45F2B; margin-right: 8px;" />
                          <cloud-server-outlined v-else style="margin-right: 8px;" />
                          <span class="mention-name">{{ s.name }}</span>
                          <span class="mention-ip">{{ s.ip }}</span>
                        </div>
                      </div>
                    </div>
                    <div v-else
                         :class="['mention-item', { active: group._index === mentionIndex }]"
                         @click="selectMentionServer(group)"
                         @mouseenter="mentionIndex = group._index">
                      <hdd-outlined v-if="group.os === 'LINUX'" style="color: #E45F2B; margin-right: 8px;" />
                      <windows-outlined v-else-if="group.os === 'WINDOWS'" style="color: #E45F2B; margin-right: 8px;" />
                      <cloud-server-outlined v-else style="margin-right: 8px;" />
                      <span class="mention-name">{{ group.name }}</span>
                      <span class="mention-ip">{{ group.ip }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
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
import {ref, onMounted, nextTick, onUnmounted, computed} from 'vue';
import {patrolApi} from '@/api/patrol';
import {serverApi} from '@/api/server';
import {message} from 'ant-design-vue';
import markdownIt from 'markdown-it';
import {CloudServerOutlined, HddOutlined, WindowsOutlined, FolderOutlined, RightOutlined} from '@ant-design/icons-vue';

const activeTab = ref('chat');
const md = markdownIt();

// Chat state
const messages = ref([]);
const messagesRef = ref(null);
const conversationId = ref(null);
const streaming = ref(false);
const streamContent = ref('');
const currentToolCall = ref(null); // 当前正在执行的工具调用
const toolCallResult = ref(''); // 工具执行结果
let eventSource = null;

// @提及服务器功能
let serverTree = ref([]);
let showMention = ref(false);
let mentionFilter = ref('');
let mentionIndex = ref(0);
let mentionJustHandled = ref(false);
const mentionDropdownRef = ref(null);
const mentionInputRef = ref(null);
// 导航索引（用于跟踪键盘导航位置，与Vue响应式分离避免被覆盖）
let navIndex = 0;

// 缓存的扁平服务器列表（避免快速键盘导航时computed重复计算导致索引错乱）
let cachedFlatServers = [];
let lastServerTreeVersion = 0;
let lastFilterValue = '';

// 过滤服务器树
const filterServerTree = (list, filter, parentExpanded = true, indexRef = { current: 0 }) => {
  const result = [];

  list.forEach(item => {
    if (item.isGroup && item.children) {
      const filteredChildren = filterServerTree(item.children, filter, parentExpanded, indexRef);
      if (filteredChildren.length > 0) {
        const group = {
          ...item,
          _expanded: parentExpanded,
          children: filteredChildren
        };
        result.push(group);
      }
    } else if (!item.isGroup) {
      if (!filter ||
          item.name?.toLowerCase().includes(filter) ||
          item.ip?.toLowerCase().includes(filter)) {
        const server = {...item, _index: indexRef.current};
        indexRef.current++;
        result.push(server);
      }
    }
  });

  return result;
};

// 缓存的过滤后服务器树（带持久化的_index）
let cachedFilteredTree = [];
let lastServerTreeHash = '';
let lastMentionFilter = '';

// 计算过滤后服务器树（带索引缓存）
const computeFilteredTree = () => {
  const filter = mentionFilter.value.toLowerCase();
  const serverTreeJson = JSON.stringify(serverTree.value);

  // 只有当服务器列表或过滤器变化时才重新计算
  if (serverTreeJson !== lastServerTreeHash || filter !== lastMentionFilter) {
    lastServerTreeHash = serverTreeJson;
    lastMentionFilter = filter;
    cachedFilteredTree = filterServerTree(serverTree.value, filter, true, { current: 0 });
  }

  return cachedFilteredTree;
};

const filteredServerTree = computed(() => {
  return computeFilteredTree();
});

// 切换分组展开/收起
const toggleGroup = (group) => {
  group._expanded = !group._expanded;
};

// 加载服务器树
const loadAllServers = async () => {
  try {
    let list = await serverApi.list();
    serverTree.value = list;
  } catch (e) {
    console.error('Failed to load servers:', e);
  }
};

// 处理输入事件，检测@符号
const handleInput = () => {
  const inputEl = mentionInputRef.value;
  if (!inputEl) return;

  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return;

  // 检查光标是否在 mention 元素内部，如果在则移到外面
  const range = selection.getRangeAt(0);
  let container = range.endContainer;
  let cursorInMention = false;
  while (container && container !== inputEl) {
    if (container.classList && container.classList.contains('inline-mention')) {
      cursorInMention = true;
      // 光标在 mention 内部，移到 mention 后面
      const newRange = document.createRange();
      newRange.setStartAfter(container);
      newRange.collapse(true);
      selection.removeAllRanges();
      selection.addRange(newRange);
      break;
    }
    container = container.parentNode;
  }

  // 如果刚移动完光标，等待下一个事件处理
  if (cursorInMention) {
    return;
  }

  // 使用 DOM 结构检测光标前的 @ 是否在 mention 外部
  const { hasExternalAt, atNode, atOffset, filterText } = findAtBeforeCursor(inputEl, selection);

  if (hasExternalAt) {
    showMention.value = true;
    mentionFilter.value = filterText;
    mentionIndex.value = 0;
    navIndex = 0;
    return;
  }

  showMention.value = false;
  mentionFilter.value = '';
};

// 找到光标前不在 mention 内的 @ 符号及其后续文本
const findAtBeforeCursor = (el, selection) => {
  const range = selection.getRangeAt(0);
  if (!range) return { hasExternalAt: false };

  // 获取光标位置的节点和偏移
  let node = range.endContainer;
  let offset = range.endOffset;

  // 收集光标前从 @ 后面到光标位置的文本
  let filterText = '';
  let atNode = null;
  let atOffset = 0;

  // 从当前节点、光标偏移位置开始向前遍历
  const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);
  let currentNode = node;
  let currentOffset = offset;

  // 首先收集从光标到往前第一个非 mention 文本节点边界的所有文本
  while (currentNode) {
    if (currentNode === el) break;

    if (currentNode.nodeType !== Node.TEXT_NODE) {
      currentNode = currentNode.previousSibling;
      continue;
    }

    const text = currentNode.textContent;
    const startPos = currentNode === node ? currentOffset : text.length;

    // 遍历当前文本节点
    for (let i = startPos - 1; i >= 0; i--) {
      const char = text[i];

      if (char === '@') {
        // 检查 @ 是否在 mention span 内
        let parent = currentNode.parentNode;
        while (parent && parent !== el) {
          if (parent.classList && parent.classList.contains('inline-mention')) {
            // @ 在 mention 内，跳过这个 mention
            filterText = ''; // 重置 filterText
            break;
          }
          parent = parent.parentNode;
        }
        if (parent === el) {
          // @ 在 mention 外，找到了
          atNode = currentNode;
          atOffset = i;
          return {
            hasExternalAt: true,
            atNode,
            atOffset,
            filterText
          };
        }
      } else if (char === ' ' || char === '\t' || char === '\n') {
        // 遇到空白字符，还没找到 @，继续往前找
        filterText = '';
      } else {
        // 普通字符，收集到 filterText
        filterText = char + filterText;
      }
    }

    // 如果当前文本节点在 mention span 内，跳过该 mention 内的所有内容
    let parent = currentNode.parentNode;
    if (parent && parent !== el && parent.classList && parent.classList.contains('inline-mention')) {
      // 跳过整个 mention span
      let sibling = parent.previousSibling;
      while (sibling && sibling.nodeType !== Node.TEXT_NODE) {
        sibling = sibling.previousSibling;
      }
      currentNode = sibling;
    } else {
      currentNode = currentNode.previousSibling;
    }
  }

  return { hasExternalAt: false };
};

// 获取光标在纯文本中的位置
const getCursorPosition = (el, selection) => {
  const sel = selection || window.getSelection();
  const range = sel?.getRangeAt(0);
  if (!range) return 0;

  // 如果光标在 mention span 内部（不应该发生，但以防万一），移到 span 后面
  let container = range.endContainer;
  let offset = range.endOffset;
  while (container && container !== el) {
    if (container.classList && container.classList.contains('inline-mention')) {
      // 光标在 mention 内部，移到 mention 后面
      const newRange = document.createRange();
      newRange.setStartAfter(container);
      newRange.collapse(true);
      sel.removeAllRanges();
      sel.addRange(newRange);
      return getCursorPosition(el, sel); // 递归重新计算
    }
    container = container.parentNode;
  }

  // 计算光标前的纯文本长度（排除 mention span 内容）
  let textLen = 0;
  const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);
  let node;
  while (node = walker.nextNode()) {
    if (node === range.endContainer) {
      // 到达光标所在节点，返回长度 + 偏移
      return textLen + range.endOffset;
    }
    // 检查是否在 mention 内
    let current = node.parentNode;
    let isInMention = false;
    while (current && current !== el) {
      if (current.classList && current.classList.contains('inline-mention')) {
        isInMention = true;
        break;
      }
      current = current.parentNode;
    }
    if (!isInMention) {
      textLen += node.textContent.length;
    }
  }
  return textLen;
};

// 获取元素的纯文本内容（不包含HTML标签）
const getTextContent = (el) => {
  // 遍历子节点构建纯文本
  let text = '';
  const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);
  let node;
  while (node = walker.nextNode()) {
    // 检查祖先元素是否是 mention
    let current = node.parentNode;
    let isInMention = false;
    while (current && current !== el) {
      if (current.classList && current.classList.contains('inline-mention')) {
        isInMention = true;
        // 只添加一次 mention 名称（跳过后续的文本节点）
        const name = current.getAttribute('data-name') || '';
        if (name && !current._added) {
          text += '@' + name;
          current._added = true;
        }
        break;
      }
      current = current.parentNode;
    }
    if (!isInMention) {
      text += node.textContent;
    }
  }
  // 清理 _added 标记
  el.querySelectorAll('.inline-mention').forEach(m => m._added = false);
  return text;
};

// 选择服务器
const selectMentionServer = async (server) => {
  insertInlineMention(server);
  showMention.value = false;
  mentionFilter.value = '';
  navIndex = 0;
};

// 获取 OS 图标
const getOsIcon = (os) => {
  if (os === 'LINUX') return '💻';
  if (os === 'WINDOWS') return '🪟';
  return '☁️';
};

// 处理输入框点击
const handleInputClick = () => {
  // 重置下拉菜单状态
  if (showMention.value && mentionFilter.value) {
    // 保持菜单显示
  }
};

// 插入内联服务器标签
const insertInlineMention = (server) => {
  const inputEl = mentionInputRef.value;
  if (!inputEl) return;

  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return;

  // 使用 findAtBeforeCursor 找到有效的 @
  const { atNode, atOffset } = findAtBeforeCursor(inputEl, selection);
  if (!atNode) return;

  // 创建范围从 @ 到当前光标
  const range = selection.getRangeAt(0);
  const newRange = document.createRange();
  newRange.setStart(atNode, atOffset);
  newRange.setEnd(range.endContainer, range.endOffset);
  newRange.deleteContents();

  // 创建 inline-mention span
  const span = document.createElement('span');
  span.className = 'inline-mention';
  span.setAttribute('data-id', server.id);
  span.setAttribute('data-name', server.name);
  span.contentEditable = 'false';
  span.innerHTML = `<span class="inline-mention-icon">${getOsIcon(server.os)}</span><span class="inline-mention-name">@${server.name}</span><span class="inline-mention-remove" onclick="this.parentNode.remove(); this.parentNode.dispatchEvent(new Event('input', {bubbles: true}));">×</span>`;

  newRange.insertNode(span);

  // 在 span 后插入零宽字符作为光标位置标记
  const zwc = document.createTextNode('​');
  if (span.nextSibling) {
    span.parentNode.insertBefore(zwc, span.nextSibling);
  } else {
    span.parentNode.appendChild(zwc);
  }

  // 移动光标到零宽字符前
  const finalSel = window.getSelection();
  const finalRange = document.createRange();
  finalRange.setStartBefore(zwc);
  finalRange.collapse(true);
  finalSel.removeAllRanges();
  finalSel.addRange(finalRange);
  inputEl.focus();

  // 触发 input 事件更新状态
  inputEl.dispatchEvent(new Event('input', { bubbles: true }));
};

// 设置输入框文本
const setInputText = (text) => {
  const inputEl = mentionInputRef.value;
  if (!inputEl) return;
  inputEl.innerHTML = text;
  inputEl.dispatchEvent(new Event('input', { bubbles: true }));
  inputEl.focus();
};

// 获取扁平化的服务器列表（带缓存）
const getFlatServers = () => {
  const currentFilter = mentionFilter.value.toLowerCase();
  const currentServerTree = serverTree.value;

  // 只有当服务器列表或过滤器变化时才重新计算
  if (currentServerTree !== lastServerTreeVersion || currentFilter !== lastFilterValue) {
    lastServerTreeVersion = currentServerTree;
    lastFilterValue = currentFilter;
    cachedFlatServers = [];
    const traverse = (items) => {
      items.forEach(item => {
        if (item.isGroup && item.children) {
          traverse(item.children);
        } else if (!item.isGroup) {
          if (!currentFilter ||
              item.name?.toLowerCase().includes(currentFilter) ||
              item.ip?.toLowerCase().includes(currentFilter)) {
            cachedFlatServers.push(item);
          }
        }
      });
    };
    traverse(currentServerTree);
  }

  return cachedFlatServers;
};

// 滚动到指定索引的元素
const scrollToMentionIndex = (index) => {
  nextTick(() => {
    const dropdown = mentionDropdownRef.value;
    if (!dropdown) return;

    const items = dropdown.querySelectorAll('.mention-item');
    const activeItem = dropdown.querySelector('.mention-item.active');

    if (activeItem && items.length > 0) {
      const dropdownRect = dropdown.getBoundingClientRect();
      const itemRect = activeItem.getBoundingClientRect();

      // 如果元素在可见区域之外，进行滚动（使用instant避免键盘快速导航时动画滞后）
      if (itemRect.top < dropdownRect.top) {
        activeItem.scrollIntoView({block: 'start', behavior: 'instant'});
      } else if (itemRect.bottom > dropdownRect.bottom) {
        activeItem.scrollIntoView({block: 'end', behavior: 'instant'});
      }
    }
  });
};

// 键盘导航
const handleMentionKeydown = (e) => {
  if (e.key === 'Enter') {
    if (showMention.value) {
      // 下拉菜单打开时，Enter选择服务器
      e.preventDefault();
      e.stopPropagation();
      mentionJustHandled.value = true;
      const dropdown = mentionDropdownRef.value;
      const activeItem = dropdown?.querySelector('.mention-item.active');
      if (activeItem) {
        const serverName = activeItem.querySelector('.mention-name')?.textContent;
        if (serverName) {
          const allServers = getFlatServers();
          const server = allServers.find(s => s.name === serverName);
          if (server) {
            // 插入 inline mention
            insertInlineMention(server);
          }
        }
      }
      nextTick(() => {
        showMention.value = false;
        navIndex = 0;
        setTimeout(() => { mentionJustHandled.value = false; }, 0);
      });
    } else {
      // 下拉菜单关闭时，Enter发送消息
      e.preventDefault();
      sendMessage();
    }
    return;
  }

  if (!showMention.value) return;

  const dropdown = mentionDropdownRef.value;
  if (!dropdown) return;

  const items = dropdown.querySelectorAll('.mention-item');
  if (!items.length) return;

  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault();
    const dropdown = mentionDropdownRef.value;
    if (!dropdown) return;
    const items = dropdown.querySelectorAll('.mention-item');
    if (!items.length) return;

    // 计算下一个导航索引（使用navIndex而不是mentionIndex避免Vue响应式覆盖）
    if (e.key === 'ArrowDown') {
      navIndex = (navIndex + 1) % items.length;
    } else {
      navIndex = (navIndex - 1 + items.length) % items.length;
    }

    // 直接操作DOM - 移除当前active，添加新的active
    const currentActive = dropdown.querySelector('.mention-item.active');
    if (currentActive) {
      currentActive.classList.remove('active');
    }
    const targetItem = items[navIndex];
    targetItem.classList.add('active');

    // 直接设置scrollTop到目标位置（使目标项在容器中央居中）
    dropdown.scrollTop = targetItem.offsetTop - (dropdown.offsetHeight / 2) + (targetItem.offsetHeight / 2);
  } else if (e.key === 'Escape') {
    showMention.value = false;
    navIndex = 0;
  }
};

const renderMarkdown = (content) => md.render(content || '');

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
};

const sendMessage = async () => {
  if (mentionJustHandled.value) return;

  const inputEl = mentionInputRef.value;
  if (!inputEl) return;

  const text = getTextContent(inputEl).trim();
  if (!text || streaming.value) return;

  // 提取所有 inline-mention 中的服务器 ID
  const mentions = inputEl.querySelectorAll('.inline-mention');
  const serverIds = [...mentions].map(m => m.getAttribute('data-id')).filter(Boolean);

  messages.value.push({role: 'user', content: text});
  inputEl.innerHTML = '';
  scrollToBottom();

  streaming.value = true;
  streamContent.value = '';
  currentToolCall.value = null;
  toolCallResult.value = '';

  let url = patrolApi.chatStreamUrl(text, conversationId.value);
  if (serverIds.length > 0) {
    url += '&serverIds=' + serverIds.join(',');
  }
  eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    const data = event.data;
    if (data === '[DONE]') {
      finishStream();
      return;
    }

    // 解析事件格式
    if (data.startsWith('text:')) {
      streamContent.value += data.substring(5);
    } else if (data.startsWith('tool_call:')) {
      try {
        const toolCall = JSON.parse(data.substring(10));
        currentToolCall.value = {
          name: toolCall.name,
          arguments: toolCall.arguments
        };
      } catch (e) {
        console.error('解析工具调用失败:', e);
      }
    }
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
  currentToolCall.value = null;
  toolCallResult.value = '';
  scrollToBottom();
};

const confirmCommand = async (msg) => {
  msg.needsConfirmation = false;
  const inputEl = mentionInputRef.value;
  if (inputEl) {
    inputEl.innerHTML = '确认执行: ' + msg.pendingCommand;
    inputEl.dispatchEvent(new Event('input', { bubbles: true }));
  }
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
  loadAllServers();
});
</script>

<style>
/* Inline mention styles - not scoped because elements are created dynamically via innerHTML */
.inline-mention {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 7px;
  height: 24px;
  background: #f4f4f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 13px;
  vertical-align: middle;
  margin: 2px 4px 2px 0;
  cursor: default;
  color: #333;
}

.inline-mention:hover {
  background: #e6e6e6;
  border-color: #bfbfbf;
}

.inline-mention-icon {
  font-size: 12px;
  opacity: 0.75;
}

.inline-mention-name {
  font-weight: 400;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-mention-remove {
  margin-left: 2px;
  cursor: pointer;
  opacity: 0.5;
  font-size: 10px;
  line-height: 1;
  color: #999;
}

.inline-mention-remove:hover {
  opacity: 1;
  color: #333;
}
</style>

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

/* Tool call indicator */
.tool-call-indicator {
  margin-top: 8px;
  padding: 8px 12px;
  background: #252525;
  border: 1px solid #1890ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.tool-call-name {
  color: #1890ff;
  font-family: monospace;
  font-weight: 500;
}

.tool-call-status {
  color: #888;
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
  align-items: flex-end;
}

.chat-input-area :deep(.ant-btn) {
  border-radius: 8px;
  min-width: 80px;
}

/* Contenteditable input */
.mention-input-container {
  flex: 1;
  min-height: 40px;
  max-height: 120px;
  padding: 8px 12px;
  background: #252525;
  border: 1px solid #333;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  line-height: 1.5;
  overflow-y: auto;
  outline: none;
  cursor: text;
}

.mention-input-container:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.mention-input-container:empty::before {
  content: attr(data-placeholder);
  color: #666;
  pointer-events: none;
}

.mention-input-container.disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

/* @mention styles */
.ai-input-wrapper {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.mention-dropdown {
  position: absolute;
  top: 0;
  left: 0;
  width: 400px;
  z-index: 1000;
  background: #252525;
  border: 1px solid #333;
  border-radius: 8px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.3);
  max-height: 240px;
  overflow-y: auto;
  margin-bottom: 8px;
  transform: translateY(-100%);
}

.mention-list {
  padding: 4px 0;
}

.mention-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #d4d4d4;
  overflow: hidden;
}

.mention-item :deep(.anticon) {
  flex-shrink: 0;
}

.mention-item:hover, .mention-item.active {
  background: #1890ff;
  color: #fff;
}

.mention-group {
  margin: 4px 0;
}

.mention-group-header {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #888;
  font-size: 13px;
  background: #1e1e1e;
  overflow: hidden;
}

.group-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.mention-group-header:hover {
  color: #d4d4d4;
  background: #2a2a2a;
}

.group-count {
  margin-left: 4px;
  color: #666;
  font-size: 12px;
}

.group-arrow {
  margin-left: auto;
  transition: transform 0.2s;
  font-size: 10px;
}

.group-arrow.expanded {
  transform: rotate(90deg);
}

.mention-group-children {
  margin-left: 16px;
}

.mention-name {
  flex: 1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.mention-ip {
  color: #888;
  font-size: 12px;
  margin-left: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
  flex-shrink: 0;
}

.mention-item:hover .mention-ip,
.mention-item.active .mention-ip {
  color: #e0e0e0;
}
</style>
