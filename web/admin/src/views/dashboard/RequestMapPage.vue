<script setup>

import {nextTick, ref} from "vue";
import {applicationApi} from "@/api/application";
import {initCesiumMap} from "@/views/dashboard/cesium";
import {serverApi} from "@/api/server";

let GlobeVizRef = ref()
let applicationRequestMapData = ref([])
const getRequestMap = async () => {
    applicationRequestMapData.value = await applicationApi.getApplicationRequestMap()

   await nextTick(()=>{
    makeMap()
  })
}
getRequestMap()

const makeMap = () => {

  let add = initCesiumMap(GlobeVizRef.value);

  const eventSource = new EventSource(serverApi.requestMap(288));
  eventSource.onmessage = (event) => {
    let data = JSON.parse(event.data)
    console.log(data)
    add(data.longitude,data.latitude,12.129800,30.259500)
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
