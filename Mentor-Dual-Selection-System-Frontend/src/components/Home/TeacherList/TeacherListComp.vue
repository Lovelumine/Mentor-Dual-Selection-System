<script setup lang="ts">

import TeacherListSearchComp from "@/components/Home/TeacherList/TeacherListSearchComp.vue";
import TeacherListFuncComp from "@/components/Home/TeacherList/TeacherListFuncComp.vue";
import TeacherListViewComp from "@/components/Home/TeacherList/TeacherListViewComp.vue";
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();

const isFuncShow = ref(false);

function handleIsFuncShow(target: string | null){
  isFuncShow.value = target === 'ADMIN';
}

onMounted(() => {
  if (userInfoStore.userInfo){
    handleIsFuncShow(userInfoStore.userInfo.role);
  }
  else userInfoStore.fetchUserInfo();
})

watch(() => userInfoStore.userInfo, (newValue) => {
  handleIsFuncShow(newValue.role);
})
</script>

<template>
  <div class="title">
    <span>导师列表</span>
    <TeacherListSearchComp/>
  </div>
  <TeacherListFuncComp v-if="isFuncShow"/>
  <TeacherListViewComp/>
</template>

<style scoped lang="sass">
.title
  width: 100%
  min-height: 60px
  background-color: var(--app-surface-muted)
  padding: 10px 20px
  font-size: 20px
  display: flex
  align-items: center
  justify-content: space-between
  gap: 16px
  color: var(--app-text-primary)
  border-bottom: 1px solid var(--app-border)
  box-sizing: border-box

@media (max-width: 1023px)
  .title
    flex-direction: column
    align-items: flex-start

@media (max-width: 767px)
  .title
    min-height: 56px
    padding: 12px 16px
    font-size: 18px
</style>
