<script setup>

import {nextTick, onMounted, ref} from "vue";
import Globe from "globe.gl";
import {applicationApi} from "@/api/application";

let GlobeVizRef = ref()
let applicationRequestMapData = ref([])
const getRequestMap = async () => {
    applicationRequestMapData.value = await applicationApi.getApplicationRequestMap()

  nextTick(()=>{
    makeMap()
  })
}
getRequestMap()

const makeMap = () => {

  const arcsData = [];

  var globeInstanceGlobeGenericInstance = Globe()
      .globeImageUrl('//unpkg.com/three-globe/example/img/earth-night.jpg')
      .arcsData(arcsData)
      .arcColor('color')
      .arcDashLength(() => Math.random())
      .arcDashGap(() => Math.random())
      .arcDashAnimateTime(() => Math.random() * 4000 + 500)
      (GlobeVizRef.value);

  let index = 0;
  setInterval(()=>{
    arcsData.push({
      startLat: applicationRequestMapData.value[index].latitude,
      startLng: applicationRequestMapData.value[index].longitude,
      endLat: 30.259500,
      endLng: 120.129800,
      color: [['red', 'white', 'blue', 'green'][Math.round(Math.random() * 3)], ['red', 'white', 'blue', 'green'][Math.round(Math.random() * 3)]]
    })

    index++;
    requestAnimationFrame(()=>{
      globeInstanceGlobeGenericInstance.arcsData(arcsData)
    })

  },2000)

}
</script>

<template>
  <div>
    <div ref="GlobeVizRef"></div>
  </div>
</template>

<style scoped lang="less">

</style>
