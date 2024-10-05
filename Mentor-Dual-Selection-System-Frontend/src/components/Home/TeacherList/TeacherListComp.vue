<script setup lang="ts">

import TeacherListSearchComp from "@/components/Home/TeacherList/TeacherListSearchComp.vue";
import TeacherListFuncComp from "@/components/Home/TeacherList/TeacherListFuncComp.vue";
import TeacherListViewComp from "@/components/Home/TeacherList/TeacherListViewComp.vue";
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();

const isFuncShow = ref(false);

function handleIsFuncShow(target){
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
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
  display: flex
</style>