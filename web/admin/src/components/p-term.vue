<script>
import "xterm/css/xterm.css";
import {Terminal} from "xterm";
import {FitAddon} from "xterm-addon-fit";
import {AttachAddon} from "xterm-addon-attach";
import {nextTick} from "vue";

export default {
  props: {
    server: {
      type: Object,
      default: () => {
      }
    }
  },
  data() {
    return {
      term: null,
      socket: null,
      rows: 32,
      cols: 200,
      SetOut: false,
      isKey: false,
      style: {}
    };
  },
  mounted() {
    this.initSocket();

  },
  beforeUnmount() {
    this.socket.close();
    // this.term.dispose();
  },
  methods: {
    //Xterm主题
    initTerm() {
      const term = new Terminal({
        rendererType: "canvas", //渲染类型
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
          foreground: "white", //字体
          background: "#060101", //背景色
          cursor: "white" //设置光标
        }
      });
      const attachAddon = new AttachAddon(this.socket);

      const fitAddon = new FitAddon();
      term.fitAddon = fitAddon;
      term.loadAddon(attachAddon);
      term.loadAddon(fitAddon);

      term.open(document.getElementById("terminal"));

      term.focus();
      // let _this = this;
      //限制和后端交互，只有输入回车键才显示结果
      // term.prompt = () => {
      //   term.write("\r\n$ ");
      // };
      // term.prompt();

      nextTick(() => {
        this.resize_terminal(term);
      });
    },
    //webShell主题
    initSocket() {
      // const WebSocketUrl = "ws://localhost:8080/ws/ssh";
      const WebSocketUrl = "ws://localhost:8080/ws/ssh/" + this.server.id;
      this.socket = new WebSocket(
          WebSocketUrl
      );
      this.socketOnClose(); //关闭
      this.socketOnOpen(); //
      this.socketOnError();
    },
    //webshell链接成功之后操作
    socketOnOpen() {
      this.socket.onopen = () => {
        // 链接成功后
        this.initTerm();
      };
    },
    //webshell关闭之后操作
    socketOnClose() {
      this.socket.onclose = () => {
        console.log("close socket");
      };
    },
    //webshell错误信息
    socketOnError() {
      this.socket.onerror = () => {
        console.log("socket 链接失败");
      };
    },
    //特殊处理
    onSend(data) {
      data = this.base.isObject(data) ? JSON.stringify(data) : data;
      data = this.base.isArray(data) ? data.toString() : data;
      data = data.replace(/\\\\/, "\\");
      this.shellWs.onSend(data);
    },
    //删除左右两端的空格
    trim(str) {
      return str.replace(/(^\s*)|(\s*$)/g, "");
    },
    resize_terminal(term) {
      var content = document.getElementById('log');
      setTimeout(() => {
        // document.getElementById('terminal').getElementsByClassName('terminal')[0].classList.add('fullscreen');
        term.fitAddon.fit();
        // this.handleResize(term)
      }, 200)

      const resizeObserver = new ResizeObserver((entries) => {
        entries.forEach((entry) => {
          const {width, height} = entry.contentRect;
          const size = {
            cols: parseInt(width / term._core._renderService._renderer.dimensions.actualCellWidth, 10) - 1,
            rows: parseInt(height / term._core._renderService._renderer.dimensions.actualCellHeight, 10)
          };

          term.resize(size.cols, size.rows)
        });
      });

      term.onResize((size) => {
        this.socket.send(JSON.stringify({
          event: "resize",
          data: {
            cols: size.cols,
            rows: size.rows,
            width: term._core._renderService._renderer.dimensions.actualCellWidth,
            height: term._core._renderService._renderer.dimensions.actualCellHeight
          }
        }));
      });

      resizeObserver.observe(content);
    },
  }
};
</script>


<template>
  <div id="log" style="margin:10px auto;">
    <div class="console" id="terminal"></div>
  </div>
</template>

<style scoped lang="less">
.console {
  width: 100%;
  height: 100%;
}
</style>
