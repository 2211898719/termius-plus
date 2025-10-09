import { ref, computed, watch } from 'vue'
import { useStorage } from '@vueuse/core'
import { serverApi } from '@/api/server'
import { chat2Command } from '@/api/ai'
import { message } from 'ant-design-vue'
import _ from 'lodash'
import autoCompleteData from '@/assets/auto-complete.json'

export function useAutoComplete(server, term, logRef) {
  // 存储相关
  const AutoComplete = useStorage('autoComp-' + server.value.id, true)
  const history = ref({
    bash: [],
    mysql: [],
    currentType: "bash",
    mysqlInit: false
  })

  // 常用命令补全数据
  const commonCommands = ref(autoCompleteData)

  // AI 生成的命令（全局共享，不区分服务器）
  const aiCommands = useStorage('aiCommands-global', [])

  // 补全相关状态
  const completeCommand = ref('')
  const autoEL = document.createElement("div")

  // 获取历史记录
  const getHistory = async () => {
    try {
      history.value.bash = uniqueArray(await serverApi.getHistory(server.value.id))
    } catch (e) {
      message.error(e.message)
    }
  }

  // 获取 MySQL 历史记录
  const getMysqlHistory = async () => {
    if (history.value.mysqlInit) {
      return
    }

    history.value.mysqlInit = true
    try {
      history.value.mysql = uniqueArray(await serverApi.getMysqlHistory(server.value.id))
    } catch (e) {
      message.error(e.message)
    }
  }

  // 数组去重并保持顺序
  function uniqueArray(arr) {
    const uniqueSet = new Set()
    const uniqueArray = []

    for (const item of arr) {
      if (!uniqueSet.has(item)) {
        uniqueSet.add(item)
        uniqueArray.push(item)
      } else {
        uniqueArray.splice(uniqueArray.indexOf(item), 1)
        uniqueArray.push(item)
      }
    }

    return uniqueArray
  }

  // 获取终端中未执行的命令
  const getUnExecutedCommand = (command, trim = true) => {
    if (!command) {
      return ""
    }

    // 如果进入了mysql模式，则获取mysql的历史命令
    if (command.startsWith("mysql> ")) {
      getMysqlHistory()
      history.value.currentType = "mysql"
      return trim ? command.substring("mysql> ".length).trim() : command.substring("mysql> ".length)
    }

    history.value.currentType = "bash"
    const regex = /^.*?@.*?:.*?[#$]/

    if (regex.test(command)) {
      return trim ? command.replace(regex, "").trim() : command.replace(regex, "")
    }

    return ""
  }

  // 获取终端最后一行非空命令
  const getTerminalLastNotBlackCommand = () => {
    const termInstance = term()
    if (!termInstance || !termInstance.buffer) return ""
    
    for (let i = termInstance.buffer.active.length - 1; i >= 0; i--) {
      let line = termInstance.buffer.active.getLine(i).translateToString();
      if (line.trim()) {
        return line
      }
    }
    return ""
  }

  // 查找第一个匹配的补全命令
  const findFirstCompleteCommand = () => {
    let last = getTerminalLastNotBlackCommand()
    let command = getUnExecutedCommand(last)
    // 去掉头部的空格，不能去掉尾部的空格
    command = command.replace(/^\s+/, "")
    if (command) {
      let find = history.value[history.value.currentType].findLast(item => item.startsWith(command))
      if (find) {
        return find
      }
      return ""
    }
    return ""
  }

  // 根据 history 和 getUnExecutedCommand 获取可能的补全命令
  const getCompleteCommand = () => {
    let find = findFirstCompleteCommand()
    if (find) {
      let last = getTerminalLastNotBlackCommand()
      let command = getUnExecutedCommand(last)
      return find.substring(command.length)
    }
    return ""
  }

  // 在光标位置写入补全内容
  const writeCompletionToCursorPosition = (autoComp) => {
    const logElement = logRef?.value
    if (!logElement) return

    let xtermTextarea = logElement.getElementsByClassName("xterm-helper-textarea")
    let console = logElement.getElementsByClassName("console")[0]
    if (!xtermTextarea.length || !console) {
      return
    }
    let el = xtermTextarea[0]

    if (!autoComp) {
      autoEL.innerText = ""
      return
    }

    // 计算autoComp前有多少个空格
    let spaceCount = 0
    for (let i = 0; i < autoComp.length; i++) {
      if (autoComp[i] === " ") {
        spaceCount++
      } else {
        break
      }
    }

    autoEL.innerText = autoComp
    autoEL.style.left = parseFloat(el.style.left.substring(0, el.style.left.length - 2)) + spaceCount * 8.5 + 8.5 + "px"
    autoEL.style.top = parseFloat(el.style.top.substring(0, el.style.top.length - 2)) + 10.5 + "px"
    autoEL.style.position = 'fixed'
    autoEL.id = "auto"
    autoEL.style.lineHeight = "1"
    autoEL.className = "auto-complete"
    console.append(autoEL)
  }

  // 隐藏补全显示
  const displayNoneCompletion = () => {
    autoEL.innerText = ""
  }

  // 处理补全逻辑
  const handleComplete = _.debounce(() => {
    completeCommand.value = getCompleteCommand()
    writeCompletionToCursorPosition(completeCommand.value)
  }, 100, { leading: false, trailing: true })

  // AI 生成命令
  const generateAiCommand = async (message) => {
    try {
      const response = await fetch(`/api-admin/ai/chat2Command?message=${encodeURIComponent(message)}`)
      if (response.ok) {
        const data = await response.json()
        console.log('AI API 响应:', data) // 添加调试日志
        if (data.command) {
          // 将 AI 生成的命令添加到 aiCommands 中
          const newCommand = {
            complete: data.command,
            remark: `AI 生成: ${message}`,
            isAi: true
          }
          aiCommands.value = [newCommand, ...aiCommands.value.slice(0, 4)] // 保留最新的 5 个 AI 命令
          return newCommand
        } else {
          console.warn('API 返回格式异常:', data)
          message.error('AI 返回格式异常')
        }
      } else {
        console.error('API 请求失败:', response.status, response.statusText)
        message.error(`AI 请求失败: ${response.status}`)
      }
    } catch (error) {
      console.error('AI 命令生成失败:', error)
      message.error('AI 命令生成失败')
    }
    return null
  }

  // 创建占位 AI 命令（不请求接口）
  const createPlaceholderAiCommand = (message) => {
    if (!message || message.trim().length < 2) {
      return null
    }
    
    return {
      complete: `AI 生成: ${message}`,
      remark: `点击回车生成命令`,
      isAi: true,
      isPlaceholder: true,
      originalMessage: message
    }
  }

  // 真正生成 AI 命令（用户选择后调用）
  const generateAiCommandForMessage = async (message) => {
    try {
      const data = await chat2Command(message)
      console.log('AI API 响应:', data) // 添加调试日志
      
      // 检查是否有 command 字段且不为空
      if (data.command && data.command.trim()) {
        // 返回生成的命令，不自动添加到 aiCommands 中
        const newCommand = {
          complete: data.command,
          remark: `AI 生成: ${message}`,
          isAi: true
        }
        return newCommand
      } else {
        // 无法提供建议的情况
        console.warn('AI 无法提供建议:', data)
        return {
          complete: null,
          remark: `AI 无法为 "${message}" 提供建议`,
          isAi: true,
          noSuggestion: true
        }
      }
    } catch (error) {
      console.error('AI 命令生成失败:', error)
      message.error('AI 命令生成失败')
    }
    return null
  }

  // 保存 AI 命令到历史记录
  const saveAiCommandToHistory = (command) => {
    if (command && command.complete) {
      // 检查是否已存在相同的命令，避免重复
      const existingIndex = aiCommands.value.findIndex(item => item.complete === command.complete)
      if (existingIndex !== -1) {
        // 如果已存在，移除旧记录
        aiCommands.value.splice(existingIndex, 1)
      }
      // 添加到最前面，保留最新的 10 个 AI 命令
      aiCommands.value = [command, ...aiCommands.value.slice(0, 9)]
    }
  }

  // 清理 AI 命令缓存
  const clearAiCommandsCache = () => {
    aiCommands.value = []
  }

  // 获取 AI 命令缓存统计
  const getAiCommandsStats = () => {
    return {
      count: aiCommands.value.length,
      commands: aiCommands.value.map(item => ({
        command: item.complete,
        remark: item.remark
      }))
    }
  }

  // 监听自动补全开关
  watch(() => AutoComplete.value, async () => {
    if (AutoComplete.value) {
      await getHistory()
      completeCommand.value = getCompleteCommand()
      writeCompletionToCursorPosition(completeCommand.value)
    } else {
      displayNoneCompletion()
    }
  })

  return {
    AutoComplete,
    history,
    commonCommands,
    aiCommands,
    completeCommand,
    getHistory,
    getMysqlHistory,
    getCompleteCommand,
    writeCompletionToCursorPosition,
    displayNoneCompletion,
    handleComplete,
    findFirstCompleteCommand,
    getUnExecutedCommand,
    getTerminalLastNotBlackCommand,
    generateAiCommand,
    createPlaceholderAiCommand,
    generateAiCommandForMessage,
    saveAiCommandToHistory,
    clearAiCommandsCache,
    getAiCommandsStats
  }
}
