<script setup lang="ts">

import NavHeader from "@/components/HeaderFooterMenu/NavHeaderComp.vue";
import UploadCoverComp from "@/components/App/UploadCoverComp.vue";
import {useUploadCoverStore} from "@/stores/UploadCoverStore";
import {ref, watch} from "vue";
import UploadFileComp from "@/components/App/UploadFileComp.vue";
const uploadCoverStore = useUploadCoverStore();
import {useUploadFileStore} from "@/stores/UploadFileStore";
import FooterComp from "@/components/HeaderFooterMenu/FooterComp.vue";
const uploadFileStore = useUploadFileStore();

const isCoverShowComp = ref(false);
const isFileCoverShow = ref(false);

watch([
  () => uploadCoverStore.isCoverShow,
  () => uploadFileStore.isLoading,
], ([newIsCoverShow, newIsFileCoverShow]) => {
  isCoverShowComp.value = newIsCoverShow;
  isFileCoverShow.value = newIsFileCoverShow;
})

</script>

<template>
  <UploadCoverComp v-if="isCoverShowComp" />
  <UploadFileComp v-if="isFileCoverShow"/>
  <NavHeader/>
  <router-view/>
</template>

<style  lang="sass">
html, body
  overflow: hidden  // 禁止页面出现滚动条
  height: 100%      // 设置页面高度为 100%
// 其他样式
</style>
