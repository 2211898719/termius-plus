<script setup>

import {nextTick, onMounted, ref} from "vue";
import {applicationApi} from "@/api/application";
import {initCesiumMap} from "@/views/dashboard/cesium";
import {serverApi} from "@/api/server";
import {useRoute} from "vue-router";

let route = useRoute()
let GlobeVizRef = ref()
let serverIpData = ref({})
onMounted( async () => {
  serverIpData.value =  await applicationApi.getServerLocation(route.query.id)
  await nextTick(()=>{
    makeMap()
  })
})
const makeMap = () => {

  let add = initCesiumMap(GlobeVizRef.value);

  const eventSource = new EventSource(serverApi.requestMap(route.query.id));
  eventSource.onmessage = (event) => {
    let data = JSON.parse(event.data)
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
