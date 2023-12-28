<script setup>
import "xterm/css/xterm.css";
import Terminal from '../utils/zmodem.js';
// import {Terminal} from "xterm";
import {FitAddon} from "xterm-addon-fit";
import {AttachAddon} from "xterm-addon-attach";
import {nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import _ from "lodash";
import {useStorage, useWebSocket} from "@vueuse/core";
import {Spin,} from 'ant-design-vue';
import {SearchAddon} from "xterm-addon-search";
import { TrzszAddon } from 'trzsz';

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
  // disableStdin: false, //是否应禁用输入。
  // cursorStyle: "block", //光标样式
  cursorBlink: true, //光标闪烁
  // scrollback: 30,
  tabStopWidth: 1,
  screenReaderMode: true,
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
  useSocket = useWebSocket(wsProtocol + '://' + host + '/socket/ssh/' + props.server.id, {
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
    attachAddon.uploadFiles()
    resizeTerminal(term);
  });

  if (props.server.autoSudo && props.server.username !== 'root') {
    nextTick(() => {
      execCommand("echo \"" + props.server.password + "\" | sudo -S ls && sudo -i\n")
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
