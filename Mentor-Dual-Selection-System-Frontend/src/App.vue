<script setup lang="ts">

import NavHeader from "@/components/HeaderFooterMenu/NavHeaderComp.vue";
import UploadCoverComp from "@/components/App/UploadCoverComp.vue";
import {useUploadCoverStore} from "@/stores/UploadCoverStore";
import {ref, watch} from "vue";
import UploadFileComp from "@/components/App/UploadFileComp.vue";
const uploadCoverStore = useUploadCoverStore();
import {useUploadFileStore} from "@/stores/UploadFileStore";
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

<style scoped lang="sass">


</style>