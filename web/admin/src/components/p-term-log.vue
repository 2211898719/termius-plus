<script setup>
import "@xterm/xterm/css/xterm.css";
import Terminal from "@/utils/zmodem";
import {nextTick, onMounted, ref, watch} from "vue";
import {useStorage} from "@vueuse/core";
import {commandLogApi} from "@/api/log";

let frontColor = useStorage('frontColor', "#ffffff")
let backColor = useStorage('backColor', "#000000")

let props = defineProps({
  logId: {
    type: Number,
    default: null
  },
  commandData: {
    type: String,
    default: ""
  },
  fontFamily: {
    type: String,
    default: "JetBrainsMono-ExtraBold"
  }
});


let options = {
  rendererType: "canvas", //渲染类型canvas或者dom
  rows: 35, //行数
  cols: 120,// 设置之后会输入多行之后覆盖现象
  convertEol: true, //启用时，光标将设置为下一行的开头
  scrollback: 100000,//终端中的回滚量
  fontSize: 14, //字体大小
  height: "600px", //终端高度
  fontFamily: props.fontFamily + ", sans-serif",
  disableStdin: true, //是否应禁用输入。
  // cursorStyle: "block", //光标样式
  cursorBlink: false, //光标闪烁
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

let terminal = ref()
const init = async () => {
  let term = new Terminal(options);

  terminal.value.innerHTML = "";
  term.open(terminal.value);
  if (props.logId) {
    let list = await commandLogApi.get(props.logId)
    term.write(list.commandData)
  } else {
    term.write(props.commandData)
  }

}

onMounted(() => {
  if (props.logId) {
    nextTick(() => {
    init()
    })
  } else if (props.commandData) {
    nextTick(() => {
      init()
    })
  }
})


watch(() => props.logId, async (logId) => {
  if (logId) {
    await init()
  }
})

watch(() => props.commandData, async (commandData) => {
  if (commandData) {
    await init()
  }
})

</script>

<template>
  <div>
  <div class="console" ref="terminal"></div>
  </div>
</template>

<style scoped lang="less">
.console {
  width: 80%;
  height: 100%;
  background-color: #fff;
}
</style>
