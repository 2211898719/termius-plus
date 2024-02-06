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
  masterSessionId: {
    type: [String,Number],
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
  rendererType: "canvas", //渲染类型canvas或者dom
  // rows: 123, //行数
  // cols: 321,// 设置之后会输入多行之后覆盖现象
  convertEol: true, //启用时，光标将设置为下一行的开头
  scrollback: 50000,//终端中的回滚量
  fontSize: 14, //字体大小
  height: "100%", //终端高度
  disableStdin: !! props.masterSessionId , //禁用输入
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

<style scoped lang="less">
.console {
  width: 100%;
  height: 100%;
  background-color: #fff;
}

</style>
