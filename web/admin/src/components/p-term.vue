<script setup>
import "xterm/css/xterm.css";
import Terminal from '../utils/zmodem.js';
// import {Terminal} from "xterm";
import {FitAddon} from "xterm-addon-fit";
import {nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import _ from "lodash";
import {useStorage, useWebSocket} from "@vueuse/core";
import {Spin,} from 'ant-design-vue';
import {SearchAddon} from "xterm-addon-search";
import {TrzszAddon} from 'trzsz';
import {useAuthStore} from "@shared/store/useAuthStore";

let authStore = useAuthStore();
// let networkInfo = useNetwork()

let props = defineProps({
  server: {
    type: Object,
    default: () => {
    }
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
  rendererType: "canvas", //渲染类型canvas或者dom
  // rows: 123, //行数
  // cols: 321,// 设置之后会输入多行之后覆盖现象
  convertEol: true, //启用时，光标将设置为下一行的开头
  scrollback: 50000,//终端中的回滚量
  fontSize: 14, //字体大小
  height: "100%", //终端高度
  disableStdin: false, //是否应禁用输入。
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
  useSocket = useWebSocket(wsProtocol + '://' + host + '/socket/ssh/' + authStore.session + '/' + props.server.id, {
    autoReconnect: {
      retries: 3,
      delay: 3000,
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


let currentCommand = ref("")

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
  term.onData((e) => {
    if (e === '\x08' || e === '\u007f') {
      currentCommand.value = currentCommand.value.slice(0, -1);
    } else {
      currentCommand.value += e;
    }

    currentCommand.value = currentCommand.value.split("\n")[0]
    console.log(currentCommand.value)
    getCursorPosition()
  })

  term.onKey(e => {
    if (e.domEvent.key === 'c' && e.domEvent.ctrlKey) {
      // 处理 Ctrl+C 的逻辑
      currentCommand.value = ""
    }
    if (e.domEvent.ctrlKey && e.domEvent.key === 'w') {
      term.write(autoComp)
      autoEL.innerText = ''
      currentCommand.value = ""
    }
  });

  nextTick(() => {

    resizeTerminal(term);
  });

  if (props.server.autoSudo && props.server.username !== 'root') {
    nextTick(() => {
      execCommand(`echo '${props.server.password}' | sudo -S ls && sudo -i && ls\n`)
    })
  }

  if (props.server.firstCommand) {
    nextTick(() => {
      execCommand(props.server.firstCommand + "\n")
    })
  }

  setInterval(() => {
    execCommand("")
  }, 1000 * 60 * 5);
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

let autoCompData = ["ps -ef | grep java", "ps -aux | grep php", "lsof -i:3306"]

let autoEL = document.getElementById("auto") || document.createElement("div")
let autoComp = "";

const getCursorPosition = () => {
  //xterm-helper-textarea
  let xtermTextarea = document.getElementsByClassName("xterm-helper-textarea");
  let console = document.getElementsByClassName("console")[0];
  if (!xtermTextarea.length) {
    return
  }
  let el = xtermTextarea[0]


  let find = autoCompData.find(t => t.startsWith(currentCommand.value))
  if (!find) {
    autoEL.innerText = ""
    return;
  }

  autoComp = find.substring(find.indexOf(currentCommand.value) + currentCommand.value.length, find.length)

  autoEL.innerText = autoComp
  autoEL.style.left = el.style.left;
  autoEL.style.top = el.style.top;
  autoEL.style.position = 'fixed'
  autoEL.style.fontSize = '14.7px'
  autoEL.style.paddingLeft = '8px'
  autoEL.id = "auto"
  autoEL.style.lineHeight = "1"
  autoEL.style.color = "#bbbaba"
  console.append(autoEL)
}

setTimeout(() => {
  getCursorPosition()
}, 3000)

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
  execCommand
})


</script>


<template>
  <div ref="log">
    <spin class="console" :spinning="loading">
      <div class="console" ref="terminal"></div>
    </spin>
  </div>
</template>

<style scoped lang="less">
.console {
  width: 100%;
  height: 100%;
  background-color: #fff;
}

</style>
