<template>
  <a-modal 
    class="auto-complete-modal" 
    v-model:visible="localVisible" 
    :closable="false" 
    :mask="false"
    :after-close="onClose"
    width="35%"
  >
    <template #header></template>
    <div class="auto-complete-root">
      <a-input 
        ref="inputRef" 
        v-model:value="searchText" 
        placeholder="搜索命令或描述，或输入需求让 AI 生成命令"
        @keydown="handleKeyDown"
        :loading="isGeneratingAi"
      />
      <div class="auto-complete-list" ref="listRef">
        <!-- 历史命令 -->
        <div v-if="filteredHistory.length > 0" class="section-header">
          <span class="section-title">历史命令</span>
        </div>
        <div 
          :ref="el => itemRefs[index] = el"
          :class="{
            'auto-complete-item': true,
            'auto-complete-item-active': selectedCommand === item
          }"
          @click="handleSelect(item)"
          @dblclick="handleDoubleClick(item, $event)"
          v-for="(item, index) in filteredHistory" 
          :key="'history-' + item"
        >
          <div class="command-text">{{ item }}</div>
        </div>

        <!-- AI 命令 -->
        <div v-if="filteredAiCommands.length > 0" class="section-header">
          <span class="section-title">AI 生成命令</span>
        </div>
        <div 
          :ref="el => itemRefs[filteredHistory.length + filteredCommonCommands.length + index] = el"
          :class="{
            'auto-complete-item': true,
            'auto-complete-item-active': selectedCommand === (item.complete || item.remark),
            'ai-command': true,
            'placeholder-command': item.isPlaceholder,
            'no-suggestion-command': item.noSuggestion,
            'generating-command': item.isGenerating
          }"
          @click="handleSelect(item.complete || item.remark)"
          @dblclick="handleDoubleClick(item.complete || item.remark, $event)"
          v-for="(item, index) in filteredAiCommands" 
          :key="'ai-' + (item.complete || item.remark)"
        >
          <div class="command-content">
            <div class="command-text" v-if="item.complete">{{ item.complete }}</div>
            <div class="command-description" v-if="item.remark">
              {{ item.remark }}
            </div>
            <div class="loading-indicator" v-if="item.isGenerating">
              <a-spin size="small" />
              <span class="loading-text">AI 生成中...</span>
            </div>
          </div>
        </div>

        <!-- 常用命令 -->
        <div v-if="filteredCommonCommands.length > 0" class="section-header">
          <span class="section-title">常用命令</span>
        </div>
        <div 
          :ref="el => itemRefs[filteredHistory.length + filteredAiCommands.length + index] = el"
          :class="{
            'auto-complete-item': true,
            'auto-complete-item-active': selectedCommand === item.complete
          }"
          @click="handleSelect(item.complete)"
          @dblclick="handleDoubleClick(item.complete, $event)"
          v-for="(item, index) in filteredCommonCommands" 
          :key="'common-' + item.complete"
        >
          <div class="command-text">{{ item.complete }}</div>
          <div class="command-description" v-if="item.remark">
            {{ item.remark }}
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <div style="text-align: left">{{ selectedCommand }}</div>
    </template>
  </a-modal>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useAutoAnimate } from '@formkit/auto-animate/vue'
import { message } from 'ant-design-vue'
import autoCompleteData from '@/assets/auto-complete.json'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  history: {
    type: Object,
    default: () => ({
      bash: [],
      mysql: [],
      currentType: 'bash'
    })
  },
  commonCommands: {
    type: Array,
    default: () => []
  },
  aiCommands: {
    type: Array,
    default: () => []
  },
  generateAiCommand: {
    type: Function,
    default: () => Promise.resolve(null)
  },
  createPlaceholderAiCommand: {
    type: Function,
    default: () => null
  },
  generateAiCommandForMessage: {
    type: Function,
    default: () => Promise.resolve(null)
  },
  saveAiCommandToHistory: {
    type: Function,
    default: () => {}
  },
  initialText: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:visible', 'confirm', 'close'])

const inputRef = ref(null)
const listRef = ref(null)
const itemRefs = ref([])
const searchText = ref('')
const selectedCommand = ref('')
const isGeneratingAi = ref(false)
const currentAiCommand = ref(null)

// 创建本地的 visible 状态
const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const [autoAnimate] = useAutoAnimate()

// 获取命令描述
const getCommandDescription = (command) => {
  const item = autoCompleteData.find(item => item.complete === command)
  return item ? item.remark : ''
}

// 过滤后的常用命令
const filteredCommonCommands = computed(() => {
  let result = props.commonCommands || []
  if (searchText.value) {
    result = result.filter(item => {
      // 搜索命令本身
      if (item.complete.toLowerCase().includes(searchText.value.toLowerCase())) {
        return true
      }
      // 搜索命令描述
      if (item.remark && item.remark.toLowerCase().includes(searchText.value.toLowerCase())) {
        return true
      }
      return false
    })
  }
  return result
})

// 过滤后的 AI 命令
const filteredAiCommands = computed(() => {
  let result = []
  
  // 添加当前自动生成的 AI 命令（如果有的话）
  if (currentAiCommand.value) {
    result.push(currentAiCommand.value)
  }
  
  // 添加历史 AI 命令，但要避免与当前命令重复
  const historyAiCommands = props.aiCommands || []
  const currentCommandText = currentAiCommand.value?.complete
  
  // 过滤掉与当前命令重复的历史命令
  const uniqueHistoryCommands = historyAiCommands.filter(item => 
    item.complete !== currentCommandText
  )
  
  result = [...result, ...uniqueHistoryCommands]
  
  if (searchText.value) {
    result = result.filter(item => {
      // 搜索命令本身
      if (item.complete && item.complete.toLowerCase().includes(searchText.value.toLowerCase())) {
        return true
      }
      // 搜索命令描述
      if (item.remark && item.remark.toLowerCase().includes(searchText.value.toLowerCase())) {
        return true
      }
      return false
    })
  }
  return result
})

// 过滤后的历史记录
const filteredHistory = computed(() => {
  let result = props.history[props.history.currentType] || []
  if (searchText.value) {
    result = result.filter(item => item.startsWith(searchText.value))
  }
  return result
})

// 所有可选项（用于选中逻辑）
const allFilteredItems = computed(() => {
  const historyItems = filteredHistory.value
  const commonItems = filteredCommonCommands.value.map(item => item.complete)
  const aiItems = filteredAiCommands.value.map(item => item.complete || item.remark) // 处理无法提供建议的情况
  return [...historyItems, ...commonItems, ...aiItems]
})

// 监听所有可选项变化，自动选择第一项
watch(allFilteredItems, (newItems) => {
  if (newItems.length && (!selectedCommand.value || !newItems.includes(selectedCommand.value))) {
    selectedCommand.value = newItems[0]
  } else if (!newItems.length) {
    selectedCommand.value = ''
  }
}, { immediate: true })

// 监听搜索文本变化，创建占位 AI 命令
watch(searchText, (newText) => {
  if (newText && newText.trim().length >= 2) {
    const placeholderCommand = props.createPlaceholderAiCommand(newText)
    currentAiCommand.value = placeholderCommand
  } else {
    currentAiCommand.value = null
  }
})

// 处理选择
const handleSelect = (item) => {
  selectedCommand.value = item
  inputRef.value?.focus()
}

// 处理双击
const handleDoubleClick = (item, event) => {
  event.preventDefault()
  event.stopPropagation()
  selectedCommand.value = item
  handleConfirm(event)
}


// 处理键盘事件
const handleKeyDown = (event) => {
  if (event.key === 'ArrowUp') {
    changeSelection('up')
    event.preventDefault()
  } else if (event.key === 'ArrowDown') {
    changeSelection('down')
    event.preventDefault()
  } else if (event.key === 'Enter') {
    // 检查是否是输入法的回车事件
    if (event.isComposing) {
      return // 忽略输入法的回车
    }
    
    // 检查是否正在生成 AI 命令
    if (isGeneratingAi.value) {
      event.preventDefault()
      return // 生成中时不处理回车事件
    }
    
    handleConfirm(event)
  }
}

// 改变选择
const changeSelection = (type) => {
  if (!allFilteredItems.value.length) return
  
  const currentIndex = allFilteredItems.value.indexOf(selectedCommand.value)
  const command = {
    up: 1,
    down: -1,
    center: 0
  }
  
  let newIndex = currentIndex - command[type]
  if (newIndex < 0) {
    newIndex = allFilteredItems.value.length - 1
  } else if (newIndex >= allFilteredItems.value.length) {
    newIndex = 0
  }

  selectedCommand.value = allFilteredItems.value[newIndex]
  
  // 确保 DOM 元素存在再调用 scrollIntoView
  nextTick(() => {
    itemRefs.value[newIndex]?.scrollIntoView({ block: 'center' })
  })
}

// 确认选择
const handleConfirm = async (event) => {
  if (selectedCommand.value) {
    // 检查是否是占位 AI 命令
    const currentCommand = currentAiCommand.value
    if (currentCommand && currentCommand.isPlaceholder && selectedCommand.value === currentCommand.complete) {
      // 这是占位 AI 命令，需要真正生成
      isGeneratingAi.value = true
      
      // 为当前 AI 命令添加 loading 状态
      currentAiCommand.value = {
        ...currentCommand,
        isGenerating: true
      }
      
      try {
        const realCommand = await props.generateAiCommandForMessage(currentCommand.originalMessage)
        if (realCommand) {
          if (realCommand.noSuggestion) {
            // 无法提供建议的情况
            currentAiCommand.value = realCommand
            selectedCommand.value = realCommand.complete || ''
            message.warning(realCommand.remark)
          } else if (realCommand.complete) {
            // 生成成功后，更新当前 AI 命令为真正的命令
            currentAiCommand.value = realCommand
            // 选中新生成的命令
            selectedCommand.value = realCommand.complete
            // 保存到历史记录
            props.saveAiCommandToHistory(realCommand)
            message.success('AI 命令生成成功')
          } else {
            message.error('AI 命令生成失败')
          }
        } else {
          message.error('AI 命令生成失败')
        }
      } catch (error) {
        console.error('AI 命令生成失败:', error)
        message.error('AI 命令生成失败')
      } finally {
        isGeneratingAi.value = false
      }
      } else {
        // 检查是否是无法提供建议的 AI 命令
        if (currentCommand && currentCommand.noSuggestion) {
        // 无法提供建议的命令，不执行任何操作
        message.warning('无法提供建议，请尝试其他需求')
        return
      }
      
      // 检查选中的是否是有效的命令（不是空字符串）
      if (!selectedCommand.value || selectedCommand.value.trim() === '') {
        // 选中的是空命令，不执行
        return
      }
      
      // 普通命令，直接执行
      emit('confirm', selectedCommand.value)
      emit('update:visible', false)
    }
  }
  event?.preventDefault()
}

// 关闭弹窗
const onClose = () => {
  emit('close')
}

// 监听弹窗显示状态
watch(() => props.visible, (value) => {
  if (value) {
    searchText.value = props.initialText
    selectedCommand.value = '' // 重置选中状态
    nextTick(() => {
      inputRef.value?.focus()
      if (filteredHistory.value.length) {
        selectedCommand.value = filteredHistory.value[0]
        // 延迟执行 changeSelection，确保 DOM 已经渲染
        setTimeout(() => {
          changeSelection('center')
        }, 100)
      }
    })
  } else {
    // 弹窗关闭时清理状态
    selectedCommand.value = ''
    searchText.value = ''
  }
})

// 监听初始文本变化
watch(() => props.initialText, (newText) => {
  if (props.visible) {
    searchText.value = newText
  }
})
</script>

<style lang="less" scoped>
.auto-complete-modal {
  :deep(.ant-modal-body) {
    padding: 12px;
  }

  .auto-complete-root {
    .auto-complete-list {
      margin-top: 12px;
      height: 50vh;
      overflow-y: auto;

      .section-header {
        padding: 8px 12px;
        background-color: #fafafa;
        border-bottom: 1px solid #e8e8e8;
        margin-top: 8px;

        &:first-child {
          margin-top: 0;
        }

        .section-title {
          font-size: 12px;
          color: #666;
          font-weight: 500;
        }
      }

      .auto-complete-item {
        padding: 8px 12px;
        border-radius: 4px;
        width: 100%;
        cursor: pointer;
        border-bottom: 1px solid #f0f0f0;
        transition: all 0.2s ease;

        &:hover {
          background-color: #f5f5f5;
        }

        &-active {
          background-color: #e6f7ff;
          border-left: 3px solid #1890ff;
          border-bottom: 1px solid #e6f7ff; // 统一边框颜色
        }

        &.ai-command {
          border-left: 2px solid #52c41a;
          
          &-active {
            background-color: #f6ffed;
            border-left: 3px solid #52c41a;
            border-bottom: 1px solid #f6ffed;
          }
        }

        &.placeholder-command {
          border-left: 2px solid #faad14;
          
          .command-text {
            color: #faad14;
            font-style: italic;
          }
          
          .command-description {
            color: #faad14;
          }
          
          &-active {
            background-color: #fffbe6;
            border-left: 3px solid #faad14;
            border-bottom: 1px solid #fffbe6;
          }
        }

        &.no-suggestion-command {
          border-left: 2px solid #ff4d4f;
          
          .command-text {
            color: #ff4d4f;
            font-style: italic;
          }
          
          .command-description {
            color: #ff4d4f;
          }
          
          &-active {
            background-color: #fff2f0;
            border-left: 3px solid #ff4d4f;
            border-bottom: 1px solid #fff2f0;
          }
        }

        &.generating-command {
          border-left: 2px solid #1890ff;
          
          .command-text {
            color: #1890ff;
            font-style: italic;
          }
          
          .command-description {
            color: #1890ff;
          }
          
          &-active {
            background-color: #e6f7ff;
            border-left: 3px solid #1890ff;
            border-bottom: 1px solid #e6f7ff;
          }
        }

        .command-text {
          font-family: 'Courier New', monospace;
          font-size: 14px;
          color: #262626;
          margin-bottom: 4px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .command-description {
          font-size: 12px;
          color: #8c8c8c;
          line-height: 1.4;
        }

        .command-content {
          width: 100%;
        }

        .loading-indicator {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-top: 4px;
          
          .loading-text {
            font-size: 12px;
            color: #1890ff;
            font-style: italic;
          }
        }
      }
    }
  }
}
</style>
            