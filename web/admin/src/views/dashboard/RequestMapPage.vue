<script setup>

import {nextTick, onMounted, ref} from "vue";
import {applicationApi} from "@/api/application";
import {initCesiumMap} from "@/views/dashboard/cesium";
import {serverApi} from "@/api/server";
import {useRoute} from "vue-router";
import {message} from "ant-design-vue";

let route = useRoute()
let GlobeVizRef = ref()
let serverIpData = ref({})
onMounted( async () => {
  serverIpData.value =  await applicationApi.getServerLocation(route.query.id)
  if (serverIpData.value.latitude==='未知' ||serverIpData.value.longitude==='未知'){
    message.error('服务器位置未知，无法显示地图')
    return
  }
  await nextTick(()=>{
    makeMap()
  })
})
const makeMap = () => {

  let add = initCesiumMap(GlobeVizRef.value);

  const eventSource = new EventSource(serverApi.requestMap(route.query.id));
  eventSource.onmessage = (event) => {
    let data = JSON.parse(event.data)
    if (!data.latitude ||!data.longitude){
      return
    }
    console.log(data)
    add(data.longitude,data.latitude,serverIpData.value.longitude,serverIpData.value.latitude)
  }
}
</script>

<template>
  <div>
    <div ref="GlobeVizRef"></div>
  </div>
</template>

<style scoped lang="less">

</style>
