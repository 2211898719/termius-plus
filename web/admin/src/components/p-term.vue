<script setup>
import "xterm/css/xterm.css";
import {Terminal} from "xterm";
import {FitAddon} from "xterm-addon-fit";
import {AttachAddon} from "xterm-addon-attach";
import {nextTick, onMounted, ref, watch} from "vue";
import _ from "lodash";
import {useWebSocket} from "@vueuse/core";
import {useStorage} from "@vueuse/core/index";

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

let options = {
  rendererType: "canvas", //渲染类型canvas或者dom
  // rows: this.rows, //行数
  // cols: this.cols,// 设置之后会输入多行之后覆盖现象
  // convertEol: true, //启用时，光标将设置为下一行的开头
  // scrollback: 10,//终端中的回滚量
  fontSize: 14, //字体大小
  // disableStdin: false, //是否应禁用输入。
  // cursorStyle: "block", //光标样式
  cursorBlink: true, //光标闪烁
  // scrollback: 30,
  // tabStopWidth: 4,
  theme: {
    foreground: props.foreground, //前景色
    background: props.background, //背景色
    cursor: "white" //设置光标
  }
}

let term = null;
let socket = null;
let terminal = ref()
let log = ref()
let loading = ref(false)

onMounted(() => {
  initSocket();
});

const initSocket = () => {
  if (socket) {
    socket.close();
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
  const useSocket = useWebSocket(wsProtocol + '://' + host + '/socket/ssh/' + props.server.id, {
    autoReconnect: {
      onFailed: (e) => {
        console.log(e)
      }
    },
    onConnected: () => {
      socket = useSocket.ws.value;
      initTerm();
      loading.value = false
    },
  });
}

const initTerm = () => {
  term = new Terminal(options);
  const attachAddon = new AttachAddon(socket);

  const fitAddon = new FitAddon();
  term.fitAddon = fitAddon;
  term.loadAddon(attachAddon);
  term.loadAddon(fitAddon);

  terminal.value.innerHTML = "";
  term.open(terminal.value);

  term.focus();

  nextTick(() => {
    resizeTerminal(term);
  });
}

let frontColor = useStorage('frontColor', "#ffffff")
let backColor = useStorage('backColor', "#000000")

watch(() => frontColor, () => {
  console.log(123)
  if (term) {
    term.setOption("theme", {
      foreground: frontColor.value, //前景色
      background: backColor.value, //背景色
      cursor: "white" //设置光标
    });
  }
})

watch(() => backColor, () => {
  console.log(123)
  if (term) {
    term.setOption("theme", {
      foreground: frontColor.value, //前景色
      background: backColor.value, //背景色
      cursor: "white" //设置光标
    });
  }
})

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

defineExpose({
  reload: () => {
    initSocket();
  }
})

</script>


<template>

  <div ref="log">
    <a-spin :spinning="loading">
      <div class="console" ref="terminal"></div>
    </a-spin>
  </div>

</template>

<style scoped lang="less">
.console {
  width: 100%;
  height: 100%;
  background-color: #060101;
}
</style>
