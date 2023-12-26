<template>
  <page-container title="系统日志">
    <a-button @click="exportBack">导出备份</a-button>
  </page-container>
</template>
<script setup>

import {fileApi} from "@/api/file";
import {download} from "@/components/tinymce/File";

const exportBack = () => {
  download(fileApi.back())
}
</script>
