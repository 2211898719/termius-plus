<script setup>
import "xterm/css/xterm.css";
import Terminal from '../utils/zmodem.js';
// import {Terminal} from "xterm";
import {FitAddon} from "xterm-addon-fit";
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from "vue";
import _ from "lodash";
import {useStorage, useWebSocket} from "@vueuse/core";
import {Spin,} from 'ant-design-vue';
import {SearchAddon} from "xterm-addon-search";
import {TrzszAddon} from 'trzsz';
import {useAuthStore} from "@shared/store/useAuthStore";
import {serverApi} from "@/api/server";

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
});

let frontColor = useStorage('frontColor', "#ffffff")
let backColor = useStorage('backColor', "#000000")

let options = {
  rendererType: "dom", //渲染类型canvas或者dom
  // rows: 123, //行数
  // cols: 321,// 设置之后会输入多行之后覆盖现象
  convertEol: true, //启用时，光标将设置为下一行的开头
  scrollback: 50000,//终端中的回滚量
  fontSize: 14, //字体大小
  height: "100%", //终端高度
  disableStdin: !!props.masterSessionId, //禁用输入
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
let socket = null;
let terminal = ref()
let log = ref()
let loading = ref(false)
let useSocket = null
let history = ref([])
let AutoComplete = useStorage('autoComp', false)
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

const initSocket = () => {
  if (socket) {
    useSocket.close();
  }
  if (term) {
    term.dispose();
  }
  if (terminal.value) {
    terminal.value.innerHTML = "";
  }
  loading.value = true
  let wsProtocol = 'ws';
  if (window.location.protocol === 'https:') {
    wsProtocol = 'wss';
  }

  const host = window.location.host;
  useSocket = useWebSocket(wsProtocol + '://' + host + '/socket/ssh/' + authStore.session + '/' + props.server.id + '/' + props.masterSessionId, {
    autoReconnect: {
      /**
       在代码的世界里追寻，重连九十九次，耐心勤。每次间隔五秒钟的等待，程序员的心绪，静静躁动。
       连接断开，网络崩溃，错误信息满屏幕跳跃。但我不放弃，不言退缩，调试代码，寻找修复。
       每次重连，希望重生，服务器响应，灵魂燃。逐渐恢复，网络连通，程序员的坚持，不曾停。
       无声的代码，编织着梦，每次重连，是一次命运。虽然苦涩，却充满希望，程序员的诗，用心深。
       99次的重连纪念，程序员的辛劳不言言。温柔的重连，坚韧中，代码世界，永不停。
       */
      retries: 99,
      delay: 5000,
      onFailed: (e) => {
        loading.value = false
      }
    },
    onMessage: (w, e) => {
      if (loading.value) {
        loading.value = false
      }

    },
    onError: (e) => {

    },
    onConnected: () => {
      socket = useSocket.ws.value;
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
  const regex = /^.*?@.*?:.*?#/

  if (regex.test(command)) {
    return command.replace(regex, "")
  }

  return ""
}


const initTerm = () => {
  term = new Terminal(options);
  const attachAddon = new TrzszAddon(socket);

  const fitAddon = new FitAddon();
  term.fitAddon = fitAddon;
  term.loadAddon(fitAddon);
  term.loadAddon(attachAddon);
  const searchAddon = new SearchAddon();
  term.loadAddon(searchAddon);

  terminal.value.innerHTML = "";
  term.open(terminal.value);

  term.focus();
  term.onData(() => {
    if (!AutoComplete.value) {
      return
    }

    setTimeout(() => {
      completeCommand.value = getCompleteCommand();
      writeCompletionToCursorPosition(completeCommand.value)
    }, 100)
  })

  term.onKey(e => {
    if (!AutoComplete.value) {
      return
    }

    if (e.domEvent.ctrlKey && e.domEvent.key === 'w') {
      let command = getCompleteCommand()
      if (command) {
        execCommand(command)
        displayNoneCompletion()
      }
      /**
       * 由于xterm.js的实现原理，无法直接阻止事件的默认行为，所以只能抛出一个异常来阻止事件的默认行为
       */
      throw new Error("stop")
    }

    if (e.domEvent.ctrlKey && e.domEvent.key === 'q') {
      displayNoneCompletion()

      throw new Error("stop")
    }
  });

  nextTick(() => {
    resizeTerminal(term);
  });

  getHistory();

  setInterval(() => {
    execCommand("")
  }, 1000 * 60 * 5);
}

setInterval(() => {
  if (term) {
    getHistory()
  }
}, 1000 * 60 * 1)

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
  history.value = _.reverse(await serverApi.getHistory(props.server.id));
}

/**
 * 根据 history 和 getUnExecutedCommand 获取可能的补全命令
 */
const getCompleteCommand = () => {
  let command = getUnExecutedCommand()
  //去掉头部的空格，不能去掉尾部的空格
  command = command.replace(/^\s+/, "")
  console.log("当前命令" + command)
  if (command) {
    let find =history.value.find(item => item.startsWith(command))
    if (find){
      return find.substring(command.length)
    }

    return ""
  }

  return ""
}

let completeCommand = ref('')

let autoEL =  document.createElement("div")

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
    const size = {
      cols: parseInt(width / term._core._renderService._renderer.dimensions.actualCellWidth, 10) - 1,
      rows: parseInt(height / term._core._renderService._renderer.dimensions.actualCellHeight, 10)
    };

    term.resize(size.cols, size.rows)
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
    socket.send(JSON.stringify({
      event: "resize",
      data: {
        cols: size.cols,
        rows: size.rows,
        width: term._core._renderService._renderer.dimensions.actualCellWidth,
        height: term._core._renderService._renderer.dimensions.actualCellHeight
      }
    }));
  });
}

const execCommand = (command) => {
  socket.send(command);
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
  setAutoComplete: (value) => {
    AutoComplete.value = value
  }
})


</script>


<template>
  <div ref="log">
    <spin class="console" :spinning="loading">
      <div class="console" ref="terminal"></div>
    </spin>
  </div>
</template>

<style lang="less" scoped>
.console {
  width: 100%;
  height: 100%;
  background-color: #fff;
}

/deep/ .auto-complete {
  position: absolute;
  color: #dadada;
  font-family: courier-new, courier, monospace;
  font-size: 14px;
}

</style>
