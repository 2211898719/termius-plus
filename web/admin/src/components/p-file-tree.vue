<script setup>
import {defineProps, ref, watch} from "vue";
import {sftpApi} from "@/api/sftp";
import {message} from "ant-design-vue";
import _ from "lodash";

const props = defineProps({
  sftpId: {
    type: String, default: ""
  },
  initialPath: {
    type: String, default: ""
  },
  treeData: {
    type: Array, default: null
  }
});

const emit = defineEmits(['openFileInCodeEditor'])

let treeData = ref([])

const sortFileByName = (files) => {
  //文件夹在前，文件在后，相同类型按字母顺序排序
  return _.sortBy(files, (file) => {
    if (file.attributes.type === 'DIRECTORY') {
      return '0' + file.name
    } else if (file.attributes.type === 'REGULAR') {
      return '1' + file.name
    } else {
      return '2' + file.name
    }
  })
}

const openCode = async () => {
  let files;
  try {
    files = await sftpApi.ls({id: props.sftpId, remotePath: props.initialPath})
  } catch (e) {
    console.error(e)
    message.error(e.message)
    return
  }
  treeData.value = sortFileByName(files)
  treeData.value.forEach(item => {
    if (item.attributes.type === 'DIRECTORY') {
      item.children = []
    } else {
      item.children = false
    }
  })
}

watch(() => props.initialPath, async (e) => {
  if (e && !props.treeData) {
    await openCode()
  } else {
    treeData.value = props.treeData
  }
}, {deep: true})

watch(() => props.treeData, (e) => {
  treeData.value = e
})

if (props.initialPath && !props.treeData) {
  openCode()
} else {
  treeData.value = props.treeData
}

const openSubMenu = async (file) => {
  if (file.attributes.type === 'REGULAR' || file.attributes.type === 'SYMLINK') {
    emit('openFileInCodeEditor', file)
    return
  } else {
    if (file.opened) {
      file.opened = !file.opened;
      return
    }
    file.spinning = true
    try {
      file.children = sortFileByName(await sftpApi.ls({id: props.sftpId, remotePath: file.path}))
    } catch (e) {
      console.error(e)
      message.error(e.message)
    } finally {
      file.spinning = false
      file.opened = !file.opened;
    }
  }
}


const currentOpenFileInCodeEditor = (file) => {
  emit('openFileInCodeEditor', file)
}

const requireApi = require.context('@/assets/file-type-icon', true, /\.svg$/);

let icons = ref({})
requireApi.keys().forEach(e => {
  let name = e.substring(2, e.length - 4);
  icons.value[name.toUpperCase()] = requireApi(e)
  icons.value[name.toLocaleLowerCase()] = requireApi(e)
})
</script>

<template>

  <template :key="item.path" v-for="item in treeData">
    <a-spin :spinning="!!item.spinning" v-if="item.attributes.type==='DIRECTORY' ">
      <a-sub-menu @titleClick="openSubMenu(item)" :key="item.path+'----subMenu'">
        <template #icon>
          <folder-outlined v-if="item.attributes.type==='DIRECTORY'"/>
          <link-outlined v-else-if="item.attributes.type==='SYMLINK'"/>
        </template>
        <template #title>{{ item.name }}</template>
        <p-file-tree v-if="item.children?.length" :treeData="item.children" :sftpId="sftpId"
                     @openFileInCodeEditor="currentOpenFileInCodeEditor"/>
        <div v-if="!item.children?.length && !item.spinning">
          <a-empty/>
        </div>
      </a-sub-menu>
    </a-spin>
    <a-menu-item @click="openSubMenu(item)"
                 v-else-if="item.attributes.type==='REGULAR' || item.attributes.type==='SYMLINK'"
                 :title="item.attributes.size > 1024*1024*50? '文件过大，不便在线预览' : item.name"
                 :key="item.path+'----menuItem'" :disabled="item.attributes.size > 1024*1024*50">
      <template #icon>
        <img :src="icons[item.name.split('.').pop()]??icons['1']">
      </template>
      {{ item.name }}
    </a-menu-item>
  </template>

</template>

<style scoped lang="less">

</style>
