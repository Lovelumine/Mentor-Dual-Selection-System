<script setup lang="ts">
import NavHeader from "@/components/HeaderFooterMenu/NavHeaderComp.vue";
import UploadCoverComp from "@/components/App/UploadCoverComp.vue";
import { useUploadCoverStore } from "@/stores/UploadCoverStore";
import { ref, watch } from "vue";
import UploadFileComp from "@/components/App/UploadFileComp.vue";
const uploadCoverStore = useUploadCoverStore();
import { useUploadFileStore } from "@/stores/UploadFileStore";
const uploadFileStore = useUploadFileStore();

const isCoverShowComp = ref(false);
const isFileCoverShow = ref(false);

watch(
  [
    () => uploadCoverStore.isCoverShow,
    () => uploadFileStore.isLoading,
  ],
  ([newIsCoverShow, newIsFileCoverShow]) => {
    isCoverShowComp.value = newIsCoverShow;
    isFileCoverShow.value = newIsFileCoverShow;
  },
);
</script>

<template>
  <UploadCoverComp v-if="isCoverShowComp" />
  <UploadFileComp v-if="isFileCoverShow" />
  <div class="app_shell">
    <NavHeader />
    <router-view />
  </div>
</template>

<style lang="sass">
html, body
  min-height: 100%

#app
  min-height: 100vh
  background: var(--app-page-bg)

.app_shell
  min-height: 100vh
</style>
