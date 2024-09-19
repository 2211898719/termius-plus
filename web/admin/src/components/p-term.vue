<script setup>
import "@xterm/xterm/css/xterm.css";
import Terminal from '../utils/zmodem.js';
// import {Terminal} from "xterm";
import {FitAddon} from "@xterm/addon-fit";
import {computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch} from "vue";
import _ from "lodash";
import {useStorage, useWebSocket} from "@vueuse/core";
import {SearchAddon} from "@xterm/addon-search";
import {TrzszAddon} from 'trzsz';
import {useAuthStore} from "@shared/store/useAuthStore";
import {serverApi} from "@/api/server";
import {Button, message, notification} from "ant-design-vue";
import pako from 'pako';
import {WebglAddon} from '@xterm/addon-webgl';

let authStore = useAuthStore();
// let networkInfo = useNetwork()

let props = defineProps({
  server: {
    type: Object,
    default: () => {
    }
  },
  masterSessionId: {
    type: [String, Number],
    default: "0"
  },
  foreground: {
    type: String,
    default: "white"
  },
  background: {
    type: String,
    default: "#060101"
  },
  loading: {
    type: Boolean,
    default: true
  },
  inputTerminal: {
    type: Boolean,
    default: false
  },
  subSessionUsername: {
    type: Array,
    default: () => {
      return []
    }
  },
  execCommand: {
    type: String,
    default: ""
  }
});

const emit = defineEmits(['update:loading', 'update:subSessionUsername', 'update:inputTerminal', 'hot'])

let frontColor = useStorage('frontColor', "#ffffff")
let backColor = useStorage('backColor', "#000000")
let AutoComplete = useStorage('autoComp', false)
let currentFont = useStorage('currentFont', 'JetBrainsMono-ExtraBold')
let socketSessionId = ref(null)

let fontChannel = new BroadcastChannel("font")
fontChannel.onmessage = (e) => {
  currentFont.value = e.data
  options.fontFamily = currentFont.value + ", sans-serif"
}

let options = {
  rendererType: AutoComplete.value ? "dom" : "canvas", //渲染类型canvas或者dom
  // rows: 123, //行数
  // cols: 321,// 设置之后会输入多行之后覆盖现象
  convertEol: true, //启用时，光标将设置为下一行的开头
  scrollback: AutoComplete.value ? 5000 : 30000, //滚动缓冲区大小
  fontSize: 14, //字体大小
  fontFamily: currentFont.value + ", -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji'",
  height: "100%", //终端高度
  disableStdin: false, //禁用输入
  // cursorStyle: "block", //光标样式
  cursorBlink: true, //光标闪烁
  fastScrollModifier: "alt", //快速滚动时要使用的修饰符
  tabStopWidth: 1,
  screenReaderMode: false,
  drawBoldTextInBrightColors: true,
  theme: {
    foreground: frontColor.value, //前景色
    background: backColor.value, //背景色
    cursor: "white" //设置光标
  }
}

let term = null;
let searchAddon = null;
let socket = null;
let socketSend = null;
let terminal = ref()
let log = ref()
let useSocket = null
let history = ref({
  bash: [],
  mysql: [],
  currentType: "bash",
  mysqlInit: false
})


watch(() => AutoComplete.value, async () => {
  if (AutoComplete.value) {
    await getHistory()
    completeCommand.value = getCompleteCommand();
    writeCompletionToCursorPosition(completeCommand.value)
  } else {
    displayNoneCompletion()
  }
})

onMounted(() => {
  initSocket();
});


watch(() => authStore.session, () => {
  initSocket();
})


let currentServer = computed(() => {
  if (typeof props.server === "string") {
    return JSON.parse(props.server)
  }

  return props.server
})

const initSocket = () => {
  if (!authStore.session || !currentServer.value.id) {
    return
  }

  if (socket) {
    useSocket.close();
  }
  if (term) {
    term.dispose();
  }
  if (terminal.value) {
    terminal.value.innerHTML = "";
  }
  //抛出 loading事件
  emit("update:loading", true)
  let wsProtocol = 'ws';
  if (window.location.protocol === 'https:') {
    wsProtocol = 'wss';
  }

  const host = window.location.host;
  useSocket = useWebSocket(wsProtocol + '://' + host + '/socket/ssh/' + authStore.session + '/' + currentServer.value.id + '/' + props.masterSessionId, {
    onMessage: (w, e) => {
      emit("update:loading", false)
    },
    onError: (e) => {

    },
    onConnected: () => {
      socket = useSocket.ws.value;
      socketSend = socket.send
      initTerm();
    },
  });

}

/**
 * 获取当前终端中最后一行的字符
 */
const getCommand = () => {
  let rows = terminal.value.getElementsByClassName("xterm-rows")[0].childNodes;
  let row = rows[rows.length - 1];
  while (row.childNodes.length === 0) {
    row = row.previousSibling
  }
  let command = ""
  row.childNodes.forEach(item => {
    if (item.className) {
      return
    }
    if (item.innerText) {
      command += item.innerText
    } else {
      command += " "
    }
  })

  return command
}


/**
 * 获取终端中未执行的命令 例如：root@localhost:~# 则去掉
 */
const getUnExecutedCommand = () => {
  let command = getCommand()

  /**
   * 如果进入了mysql模式，则获取mysql的历史命令
   */
  if (command.startsWith("mysql> ")) {
    getMysqlHistory()
    history.value.currentType = "mysql"
    return command.substring("mysql> ".length)
  }

  history.value.currentType = "bash"
  const regex = /^.*?@.*?:.*?[#$]/

  if (regex.test(command)) {
    return command.replace(regex, "")
  }

  return ""
}

const arrayBufferToString = (arrayBuffer) => {
  return new TextDecoder('utf-8').decode(arrayBuffer);
}

function decompressArrayBuffer(arrayBuffer) {
  const compressedData = new Uint8Array(arrayBuffer);
  const decompressedData = pako.ungzip(compressedData);
  return decompressedData.buffer;
}

function compressArrayBuffer(str) {
  return pako.gzip(str, {to: 'array'});
}

const initTerm = () => {
  term = new Terminal({...options, disableStdin: props.masterSessionId != 0});
  socket.send = (data) => {
    if (socket.readyState === 1) {
      sendEvent(JSON.stringify({
        event: "COMMAND",
        data: data
      }));
    }
  }

  const originalAddEventListener = socket.addEventListener;

  function preprocessEvent(event) {

    let data = JSON.parse(arrayBufferToString(decompressArrayBuffer(event.data)));
    switch (data.event) {
      case "COMMAND":
        emit("hot", currentServer.value)
        return {
          type: "COMMAND",
          data: data.data
        }
      case "RESPONSE_AUTH_EDIT_SESSION": {
        let m = JSON.parse(data.data)
        if (m.result) {
          message.success("申请操作" + currentServer.value.name + "成功")
        } else {
          message.error("申请操作" + currentServer.value.name + "拒绝")
        }

        term.setOption("disableStdin", !m.result)
        emit("update:inputTerminal", m.result)

        return {
          type: "RESPONSE_AUTH_EDIT_SESSION",
          data: ""
        }
      }
      case "REQUEST_AUTH_EDIT_SESSION": {
        let message = JSON.parse(data.data)
        let key = `REQUEST_AUTH_EDIT_SESSION-${message.username}-${currentServer.value.name}`;
        notification.open({
          message: '提示',
          duration: 0,
          description:
              `${message.username}申请操作你终端${currentServer.value.name}，是否允许？`,
          btn: () =>
              [h(
                  Button,
                  {
                    type: 'primary',
                    size: 'small',
                    onClick: () => {
                      sendEvent(JSON.stringify({
                        event: "RESPONSE_AUTH_EDIT_SESSION",
                        data: JSON.stringify({
                          sessionId: message.sessionId,
                          username: message.username,
                          result: true
                        })
                      }));
                      notification.close(key);
                    },
                  },
                  {default: () => '同意'},
              ), h(
                  Button,
                  {
                    type: 'primary',
                    size: 'small',
                    style: 'margin-left: 8px',
                    onClick: () => {
                      notification.close(key);
                    },
                  },
                  {default: () => '不同意'},
              )],
          key
        });
        return {
          type: "REQUEST_AUTH_EDIT_SESSION",
          data: ""
        }
      }
      case "JOIN_SESSION":
        message.warning(data.data + "正在观察你操作" + currentServer.value.name)
        emit("update:subSessionUsername", [...props.subSessionUsername, data.data])
        return {
          type: "JOIN_SESSION",
          data: ""
        }
      case "LEAVE_SESSION":
        message.warning(data.data + "已经停止观察你操作" + currentServer.value.name)
        var removeIndex = props.subSessionUsername.indexOf(data.data)
        emit("update:subSessionUsername", [...props.subSessionUsername.slice(0, removeIndex), ...props.subSessionUsername.slice(removeIndex + 1)])
        return {
          type: "LEAVE_SESSION",
          data: ""
        }
      case "MASTER_CLOSE":
        term.write("\r\n主会话已关闭\r\n")
        return {
          type: "MASTER_CLOSE",
          data: ""
        }
      case "SESSION":
        socketSessionId.value = data.data
        return {
          type: "MASTER_CLOSE",
          data: ""
        }
    }
    return event;
  }

// 重写socket对象的addEventListener方法
  socket.addEventListener = function (eventName, listener, options) {
    if (eventName !== 'message') {
      return originalAddEventListener.call(socket, eventName, listener, options);
    }

    // 创建一个新的函数作为代理的listener
    const proxyListener = function (event) {
      // 进行前置处理
      const modifiedEvent = preprocessEvent(event);

      // 调用原始的listener，并传入处理后的事件对象
      return listener(modifiedEvent);
    };

    // 调用原始的addEventListener方法，并传入代理的listener
    return originalAddEventListener.call(socket, eventName, proxyListener, options);
  };


  const attachAddon = new TrzszAddon(socket);

  const fitAddon = new FitAddon();
  term.fitAddon = fitAddon;
  terminal.value.innerHTML = "";
  term.open(terminal.value);
  term.loadAddon(fitAddon);
  term.loadAddon(attachAddon);
  searchAddon = new SearchAddon();
  term.loadAddon(searchAddon);
  term.loadAddon(new WebglAddon());
  term.attachCustomKeyEventHandler((event) => {
    if ((event.type === 'keydown' && ('f' === event.key || 'F' === event.key) && event.ctrlKey && event.shiftKey) || (event.type === 'keydown' && ('f' === event.key || 'F' === event.key) && event.metaKey && event.shiftKey)) {
      searchVisible.value = !searchVisible.value
      return false;
    }
  });

  term.focus();
  // term.onData(() => {
  //   if (!AutoComplete.value) {
  //     return
  //   }
  //
  //   setTimeout(() => {
  //     completeCommand.value = getCompleteCommand();
  //     writeCompletionToCursorPosition(completeCommand.value)
  //   }, 100)
  // })

  // term.onKey(e => {
  //   return;
  //   if (!AutoComplete.value) {
  //     return
  //   }
  //
  //   if (e.domEvent.ctrlKey && (e.domEvent.key === 'w' || e.domEvent.key === 'W')) {
  //     let command = getCompleteCommand()
  //     if (command) {
  //       execCommand(command)
  //       displayNoneCompletion()
  //     }
  //     /**
  //      * 由于xterm.js的实现原理，无法直接阻止事件的默认行为，所以只能抛出一个异常来阻止事件的默认行为
  //      */
  //     throw new Error("stop")
  //   }
  //
  //   if (e.domEvent.ctrlKey && e.domEvent.key === 'q') {
  //     displayNoneCompletion()
  //
  //     throw new Error("stop")
  //   }
  // });

  if (props.server.execCommand && props.masterSessionId == 0) {
    nextTick(() => {
      execCommand(props.server.execCommand + "\n")
    })
  }

  if (props.execCommand) {
    nextTick(() => {
      execCommand(props.execCommand + "\n")
    })
  }

  nextTick(() => {
    resizeTerminal(term);
  });

  if (AutoComplete.value) {
    getHistory();
  }

}

let channel = new BroadcastChannel("theme")
channel.onmessage = (e) => {
  frontColor.value = e.data.frontColor
  backColor.value = e.data.backColor
  term.setOption("theme", {
    foreground: frontColor.value, //前景色
    background: backColor.value, //背景色
    cursor: "white" //设置光标
  });
}

const getHistory = async () => {
  // try {
  //   history.value.bash = _.reverse(await serverApi.getHistory(props.server.id));
  // } catch (e) {
  //   message.error(e.message)
  // }
}

const getMysqlHistory = async () => {
  if (history.value.mysqlInit) {
    return
  }

  history.value.mysqlInit = true
  try {
    history.value.mysql = _.reverse(await serverApi.getMysqlHistory(currentServer.value.id));
  } catch (e) {
    message.error(e.message)
  }
}

const requestAuthEditSession = () => {
  message.info("正在申请操作" + currentServer.value.name)
  sendEvent(JSON.stringify({
    event: "REQUEST_AUTH_EDIT_SESSION"
  }));
}

/**
 * 根据 history 和 getUnExecutedCommand 获取可能的补全命令
 */
const getCompleteCommand = () => {
  let command = getUnExecutedCommand()
  //去掉头部的空格，不能去掉尾部的空格
  command = command.replace(/^\s+/, "")
  if (command) {
    let find = history.value[history.value.currentType].find(item => item.startsWith(command))
    if (find) {
      return find.substring(command.length)
    }

    return ""
  }

  return ""
}

let completeCommand = ref('')

let autoEL = document.createElement("div")

const writeCompletionToCursorPosition = (autoComp) => {
  log.value.getElementsByClassName("xterm-helper-textarea")
  //xterm-helper-textarea
  let xtermTextarea = log.value.getElementsByClassName("xterm-helper-textarea");
  let console = log.value.getElementsByClassName("console")[0];
  if (!xtermTextarea.length) {
    return
  }
  let el = xtermTextarea[0]

  if (!autoComp) {
    autoEL.innerText = ""
    return;
  }

  //计算autoComp前有多少个空格
  let spaceCount = 0;
  for (let i = 0; i < autoComp.length; i++) {
    if (autoComp[i] === " ") {
      spaceCount++
    } else {
      break;
    }
  }

  autoEL.innerText = autoComp
  autoEL.style.left = parseFloat(el.style.left.substring(0, el.style.left.length - 2)) + spaceCount * 8.4 + "px"
  autoEL.style.top = parseFloat(el.style.top.substring(0, el.style.top.length - 2)) + 1 + "px"
  autoEL.style.position = 'fixed'
  autoEL.id = "auto"
  autoEL.style.lineHeight = "1"
  autoEL.className = "auto-complete"
  console.append(autoEL)
}

const displayNoneCompletion = () => {
  autoEL.innerText = ""
}

const resizeTerminal = () => {
  let content = log.value;

  let resizeFun = _.debounce(() => {
    const {width, height} = content.getBoundingClientRect();
    if (width === 0 || height === 0) return
    term.fitAddon.fit();
  }, 100, {leading: false, trailing: true})

  nextTick(() => {
    resizeFun()
    term.focus();
  })

  const resizeObserver = new ResizeObserver(() => {
    resizeFun()
  });

  resizeObserver.observe(content);

  term.onResize((size) => {
    sendEvent(JSON.stringify({
      event: "RESIZE",
      data: {
        cols: size.cols,
        rows: size.rows,
        width: term._core._renderService._renderer._value.dimensions.css.canvas.width,
        height: term._core._renderService._renderer._value.dimensions.css.canvas.height
      }
    }));
  });
}

const execCommand = (command) => {
  sendEvent(JSON.stringify({
    event: "COMMAND",
    data: command
  }))
}

const sendEvent = (event) => {
  socketSend.call(socket, compressArrayBuffer(event));
}

const close = () => {
  if (useSocket) {
    useSocket.close();
  }
  if (term) {
    term.dispose();
  }
  if (terminal.value) {
    terminal.value.innerHTML = "";
  }
}

onBeforeUnmount(() => {
  close()
})

defineExpose({
  reload: () => {
    initSocket();
  },
  focus: () => {
    term.focus();
  },
  close,
  execCommand,
  setDisableStdin: (value) => {
    term.setOption("disableStdin", value)
  },
  requestAuthEditSession,
  setAutoComplete: (value) => {
    AutoComplete.value = value
  },
  getSessionId: () => {
    return socketSessionId.value
  }
})

let searchVisible = ref(false)
let searchText = ref('')
let searchTextInputRef = ref(null)
let regexEnabled = useStorage('search-regex-' + props.server.id, false)
watch(searchVisible, (value) => {
  if (value) {
    searchTextInputRef.value.focus()
    let select = term.getSelection();
    if (select) {
      searchText.value = select
    }
  } else {
    term.focus()
    searchAddon.clearDecorations()
  }
})

const closeSearch = () => {
  searchVisible.value = false
  searchText.value = ''
}

const handleKeyup = (event) => {
  if (event.key === 'Enter' && event.isComposing) {
    return; // 忽略输入法的回车
  }

  searchAddon.findNext(searchText.value, {
    regex: regexEnabled.value
  })
}

const searchKeyListener = (event) => {
  if ((event.type === 'keydown' && ('f' === event.key || 'F' === event.key) && event.ctrlKey && event.shiftKey) || (event.type === 'keydown' && ('f' === event.key || 'F' === event.key) && event.metaKey && event.shiftKey)) {
    searchVisible.value = !searchVisible.value
    return false;
  }

  if (event.type === 'keydown' && event.key === 'Escape'){
    closeSearch()
  }
}

const searchNext = () => {
  searchAddon.findNext(searchText.value, {
    regex: regexEnabled.value
  })
}

const searchPrev = () => {
  searchAddon.findPrevious(searchText.value, {
    regex: regexEnabled.value
  })
}

const changeRegexEnabled = () => {
  regexEnabled.value = !regexEnabled.value
}

</script>


<template>
  <div style="position: relative;">
    <div class="log" ref="log">
      <div class="console" ref="terminal"></div>
    </div>
    <div :class="{'search-drawer':true, 'is-visible':!searchVisible}" @keydown="searchKeyListener">
      <a-input ref="searchTextInputRef" v-model:value="searchText" placeholder="搜索"
               @keydown.enter="handleKeyup"></a-input>
      <a-divider type="vertical" style="height: 24px"/>
      <span :class="{'search-option':true, 'is-active': regexEnabled}" @click="changeRegexEnabled">
        <svg t="1726739810873" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"
             p-id="2957" width="14" height="14"><path
            d="M682.666667 721.92c-14.08 2.133333-28.16 3.413333-42.666667 3.413333-14.506667 0-28.586667-1.28-42.666667-3.413333v-149.76l-106.666666 105.813333c-21.333333-16.64-42.666667-37.973333-59.306667-59.306666l105.813333-106.666667H387.413333c-2.133333-14.08-3.413333-28.16-3.413333-42.666667 0-14.506667 1.28-28.586667 3.413333-42.666666h149.76l-105.813333-106.666667c8.106667-10.666667 16.64-21.333333 27.733333-31.573333 10.24-11.093333 20.906667-19.626667 31.573334-27.733334L597.333333 366.506667V216.746667c14.08-2.133333 28.16-3.413333 42.666667-3.413334 14.506667 0 28.586667 1.28 42.666667 3.413334v149.76l106.666666-105.813334c21.333333 16.64 42.666667 37.973333 59.306667 59.306667L742.826667 426.666667h149.76c2.133333 14.08 3.413333 28.16 3.413333 42.666666 0 14.506667-1.28 28.586667-3.413333 42.666667h-149.76l105.813333 106.666667c-8.106667 10.666667-16.64 21.333333-27.733333 31.573333-10.24 11.093333-20.906667 19.626667-31.573334 27.733333L682.666667 572.16v149.76M213.333333 810.666667a85.333333 85.333333 0 0 1 85.333334-85.333334 85.333333 85.333333 0 0 1 85.333333 85.333334 85.333333 85.333333 0 0 1-85.333333 85.333333 85.333333 85.333333 0 0 1-85.333334-85.333333z"
            p-id="2958"></path></svg>
      </span>
      <a-divider type="vertical" style="height: 24px"/>
      <up-outlined class="search-btn" @click="searchPrev"/>
      <down-outlined class="search-btn" @click="searchNext"/>
      <close-outlined class="search-btn" @click="closeSearch"/>
    </div>
  </div>
</template>

<style lang="less">

.search-drawer {
  width: 350px;
  right: 100px;
  left: unset;
  padding: 6px;
  border-radius: 4px;
  top: 12px;
  //margin-top: 8px;
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all .3s cubic-bezier(.23, 1, .32, 1);
  z-index: 1000;
  background-color: #fff;
  mix-blend-mode: difference;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  .search-option {
    padding: 4px;
    cursor: pointer;
    margin-left: 12px;
    border-radius: 8px;
    width: 28px;
    height: 24px;

    &:hover {
      background-color: #d1d1d1;
    }

    &:first-of-type {
      margin-left: 0;
    }

    &.is-active {
      background-color: #d1d1d1;
    }
  }

  .search-btn {
    padding: 4px;
    border-radius: 50%;
    cursor: pointer;
    margin-left: 6px;

    &:hover {
      background-color: #f5f5f5;
    }

    &:first-of-type {
      margin-left: 0;
    }
  }

}

.is-visible {
  //沿着z轴180旋转
  transform: translateY(calc(-100% - 12px));
  transform-origin: center bottom;
  opacity: 0;
}


.log {
  width: 100%;
  height: 100%;
  background-color: #fff;
}

.console {
  width: 100%;
  height: 100%;
  background-color: #fff;

  .terminal {
    height: 100%;

  }
}

/deep/ .auto-complete {
  position: absolute;
  color: v-bind(frontColor);
  font-family: courier-new, courier, monospace;
  font-size: 14px;
  opacity: 0.6;
}

/deep/ .xterm {
  height: 100%;
}


</style>
