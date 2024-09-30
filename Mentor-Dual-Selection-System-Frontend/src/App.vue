<script setup lang="ts">

import NavHeader from "@/components/HeaderFooterMenu/NavHeaderComp.vue";
import UploadCoverComp from "@/components/App/UploadCoverComp.vue";
import {useUploadCoverStore} from "@/stores/UploadCoverStore";
import {ref, watch} from "vue";
const uploadCoverStore = useUploadCoverStore();

const isCoverShowComp = ref(false);

watch(() => uploadCoverStore.isCoverShow, (newVal) => {
  isCoverShowComp.value = newVal;
})

</script>

<template>
  <UploadCoverComp v-if="isCoverShowComp" />
  <NavHeader/>
  <router-view/>
</template>

<style scoped lang="sass">


</style>