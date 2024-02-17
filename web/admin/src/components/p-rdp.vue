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

  notification.open({
    message: '提示',
    description:
        '浏览器远程桌面有诸多不便，在服务器上右键下载连接文件，本地连接更方便！'
  });

  //var guac = new Guacamole.Client(
  //new Guacamole.HTTPTunnel("/guacamole/tunnel")
  //);

  guac = new Guacamole.Client(
      new Guacamole.WebSocketTunnel(
          "/socket/rdp/" + props.server.id
      )
  );


  let handleResize = () => {
    guac.sendSize(display.value.clientWidth, display.value.clientHeight);
  }

  guac.audioEnabled = true;

  const resizeObserver = new ResizeObserver(_.debounce(handleResize, 1500, {leading: false, trailing: true}));

  resizeObserver.observe(window.document.body);

  // Add client to display div
  display.value.appendChild(guac.getDisplay().getElement());

  // Error handler message
  guac.onerror = function (error) {
    console.log(error);
    resultTitle.value = error.message;
    showResult.value = true;
  };


  // Connect
  guac.connect("height=" +
      display.value.clientHeight +
      "&width=" +
      display.value.clientWidth);
  // Disconnect on closez
  window.onunload = function () {
    guac.disconnect();
  };

  console.log(guac);

  guac.onstatechange = function (state) {
    console.log(state);
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
      keyboard = new Guacamole.Keyboard(document);

      keyboard.onkeydown = function (keySystem) {
        guac.sendKeyEvent(1, keySystem);
      };

      keyboard.onkeyup = function (keySystem) {
        guac.sendKeyEvent(0, keySystem);
      };
    }
  };

  guac.onaudio = function (mimetype, data) {
    console.log(mimetype, data);
  }

  // setTimeout(()=>{
  //   var fileUploadStream = guac.createFileStream("application/octet-stream","C://test.xxx");
  //
  //   // 设置文件上传的事件处理程序
  //   fileUploadStream.onack = function(status) {
  //     if (status.isComplete()) {
  //       console.log("文件上传成功");
  //     } else {
  //       console.log("文件上传失败");
  //     }
  //   };
  //
  //   // 通过文件上传流发送文件数据
  //   var fileData = new Blob([0,1,1,1,1,1,1]);
  //   fileUploadStream.sendBlob(fileData);
  // },5000)
  guac.onconnect = function() {
    console.log(123123123)
    // 文件上传


    // 文件下载
    var fileDownloadStream = guac.createFileStream(Guacamole.StreamMimetypes.APPLICATION_OCTET_STREAM);

    // 设置文件下载的事件处理程序
    fileDownloadStream.onblob = function(blob) {
      var fileURL = URL.createObjectURL(blob);
      console.log("文件下载链接：" + fileURL);

      // 创建一个下载链接
      var link = document.createElement("a");
      link.href = fileURL;
      link.download = "下载的文件名";
      link.click();
    };

    // 请求下载文件
    // fileDownloadStream.sendAck("下载的文件路径");
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
  show()
})

defineExpose({
  close
})


</script>

<template>
  <div
      class="remote-desktop"
  >
    <div
        ref="display"
    >
    </div>
  </div>
</template>

<style lang="less">
.remote-desktop {
  width: 100%;
  height: 100%;

  div {
    width: 100%;
    height: @height;
  }

  canvas {
    z-index: 100 !important;
  }
}
</style>