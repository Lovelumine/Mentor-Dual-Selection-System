<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {httpStudent, httpTeacher} from "@/utils/http";
const userInfoStore = useUserInfoStore();

const userRole = ref(null);
const userDetail = ref(null);

onMounted(() => {
  if (userInfoStore.userInfo) getUserDetail(userInfoStore.userInfo.role);

})

function getUserDetail(target: string) {
  if (target === 'TEACHER') {
    httpTeacher({
      url: '/my-detail',
      method: 'GET',
      headers: {
        Accept: '*/*',
        Authorization: 'Bearer ' + localStorage.getItem('token')
      }
    }).then(res => {
      console.log(res);
      if (res.data.code === 200) {
        userDetail.value = res.data.data;
      } else {
        alert(res.data.data.error);
      }
    }).catch(err => {
      alert(JSON.parse(err.requests.responseText).data.error);
      console.error(err);
    })
  } else if (target === 'STUDENT') {
    httpStudent({
      url: '/my-detail',
      method: 'GET',
      headers: {
        Accept: '*/*',
        Authorization: 'Bearer ' + localStorage.getItem('token')
      }
    }).then(res => {
      if (res.data.code === 200) {
        userDetail.value = res.data.data;
      } else {
        alert(res.data.data.error);
      }
    }).catch(err => {
      alert(JSON.parse(err.requests.responseText).data.error);
    })
  }
}

watch(() => userInfoStore.userInfo, (newValue) => {
  userRole.value = newValue.role;
  getUserDetail(newValue.role);
})
</script>

<template>
  <div class="my_detail_box">
    <h3>账号详细信息</h3>
    <div>
      {{userDetail}}
    </div>
  </div>
</template>

<style scoped lang="sass">
.my_detail_box
  box-shadow: #005826 0 0 5px
  border-radius: 10px
  width: 100%
  margin: 10px auto
  text-align: center
  h3
    color: #005826
    font-weight: bold
</style>