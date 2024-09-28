<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {httpStudent, httpTeacher} from "@/utils/http";
const userInfoStore = useUserInfoStore();

const userRole = ref(null);
const userDetail = ref({
  uid: -1,
  photoUrl: '',
  teacherPosition: '',
  researchDirection: '',
  professionalDirection: '',
  resume: '',
  netid: '',
  studentClass: null,
  studentGender: null,
});
const userDetailTemp = ref({
  uid: -1,
  photoUrl: '',
  teacherPosition: '',
  researchDirection: '',
  professionalDirection: '',
  resume: '',
  netid: '',
  studentClass: null,
  studentGender: null,
});
const isChangeDetailDisabled = ref(true);

function changeDetailDisabled(){
  isChangeDetailDisabled.value = !isChangeDetailDisabled.value;
}

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
        userDetailTemp.value = userDetail.value
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
        userDetailTemp.value = userDetail.value
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
    <el-form label-width="auto" style="max-width: 600px">
      <el-form-item label="职称">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.teacherPosition"/>
      </el-form-item>
      <el-form-item label="研究方向">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.researchDirection"/>
      </el-form-item>
      <el-form-item label="专业方向">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.professionalDirection"/>
      </el-form-item>
      <el-form-item label="简介">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.resume"/>
      </el-form-item>
    </el-form>
    <button @click="changeDetailDisabled">{{isChangeDetailDisabled? '开启修改': '关闭修改'}}</button>
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