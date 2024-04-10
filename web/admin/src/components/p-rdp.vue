<script setup>

import {onMounted, ref} from "vue";
import {notification} from "ant-design-vue";

let keyboard = {};
let guac = {};
let resultTitle = ref("");
let showResult = ref(false);

let display = ref(null);

let props = defineProps({
  server: {
    type: Object,
    default: () => {
    }
  },
})

const show = () => {
  if (display.value){
    display.value.innerHTML = "";
  }
  let webSocketTunnel = new Guacamole.WebSocketTunnel(
      "/socket/rdp/" + props.server.id
  );
  guac = new Guacamole.Client(
      webSocketTunnel
  );

  let handleResize = () => {
    if (display.value.clientHeight - 20 > 100 && display.value.clientWidth > 100) {
      guac.sendSize(display.value.clientWidth, display.value.clientHeight - 20);
    }
  }

  guac.audioEnabled = true;

  const resizeObserver = new ResizeObserver(_.debounce(handleResize, 1500, {leading: false, trailing: true}));

  resizeObserver.observe(display.value);

  // Add client to display div
  display.value.appendChild(guac.getDisplay().getElement());

  // Error handler message
  guac.onerror = function (error) {
    resultTitle.value = error.message;
    showResult.value = true;
  };


  // Connect
  guac.connect("height=" +
      (display.value.clientHeight - 20) +
      "&width=" +
      display.value.clientWidth);
  // Disconnect on closez
  window.onunload = function () {
    guac.disconnect();
  };

  guac.onstatechange = function (state) {
    if (state === 3) {
      // Mouse
      let mouse = new Guacamole.Mouse(guac.getDisplay().getElement());

      mouse.onmousedown =
          mouse.onmouseup =
              mouse.onmousemove =
                  function (mouseState) {
                    guac.sendMouseState(mouseState);
                  };

      // Keyboard
      keyboard = new Guacamole.Keyboard(document)

      //display.value的鼠标移入事件
      display.value.addEventListener('mouseenter', function () {
        keyboard.onkeydown = function (keySystem) {
          guac.sendKeyEvent(1, keySystem);
        };

        keyboard.onkeyup = function (keySystem) {
          guac.sendKeyEvent(0, keySystem);
        };
      });
      //display.value的鼠标移出事件
      display.value.addEventListener('mouseleave', function () {
        keyboard.onkeydown = null;
        keyboard.onkeyup = null;
      });
    }
  };

}

const close = () => {
  if (guac) {
    guac.disconnect();
  }
  if (keyboard) {
    keyboard.onkeydown = null;
    keyboard.onkeyup = null;
  }
  showResult.value = false;
  resultTitle.value = "";
}

onMounted(() => {
  setTimeout(() => {
    show()
    notification.open({
      message: '提示',
      description:
          '浏览器远程桌面有诸多不便，在服务器上右键下载连接文件，本地连接更方便！'
    });
  }, 0)
})

const reload = () => {
  close()
  show()
}

defineExpose({
  close,
  reload
})

</script>

<template>
  <div
      class="remote-desktop"
  >
    <a-spin :spinning="showResult" :tip="resultTitle.value">
      <div
          ref="display"
      >
      </div>
    </a-spin>
  </div>
</template>

<style lang="less">
.remote-desktop {
  width: 100%;
  height: 100%;

  .reload{
    position: absolute;
    right: 10px;
    top: 10px;
    z-index: 1000;
    cursor: pointer;
    mix-blend-mode: difference;
    width: 30px;
    height: 30px;
    color: #fff;
  }

  div {
    width: 100%;
    height: @height;
  }

  canvas {
    z-index: 100 !important;
  }
}
</style>
