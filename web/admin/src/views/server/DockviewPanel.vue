<script setup>
import {getCurrentInstance, h, onMounted, ref, render} from "vue";
import ServerContent from "@/views/server/ServerContent.vue";

let props = defineProps({
  params: {
    type: Object
  },
});

const componentIns = getCurrentInstance();

let container = ref(null);

let vNode2Dom = (vNode) => {
  let domEl = document.createElement("div");
  render(vNode, domEl);
  return domEl.firstElementChild;
}

let ptermRef = ref(null);
onMounted(() => {
  if (props.params.params.el){
    container.value.appendChild(props.params.params.el)
    return
  }

  if (props.params.params.server){
    let pTerm = h(ServerContent, {
      ...props.params.params,
      operationId: props.params.params.server.operationId
    })
    ptermRef.value = pTerm
    pTerm.appContext = componentIns.appContext
    let node = vNode2Dom(pTerm);
    node.customClose = () => {
      pTerm.component.exposed.close()
    }
    node.customFocus = () => {
      pTerm.component.exposed.focus()
    }
    container.value.appendChild(node)
  }
})

</script>

<template>
  <div style="height: 100%" ref="container">
  </div>
</template>

<style scoped lang="less">

</style>