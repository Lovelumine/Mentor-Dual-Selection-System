<script setup lang="ts">
import {onMounted, ref} from "vue";
import {http, httpStudent} from "@/utils/http";
import {Message} from "@element-plus/icons-vue";

const allTeacherDetailsList = ref([]);

onMounted(() => {
  http({
    url: '/search/teachers',
    method: 'GET',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    }
  }).then(res => {
    if (res.data.code === 200){
      allTeacherDetailsList.value = res.data.data;
      for (let i = 0; i < allTeacherDetailsList.value.length; i++){
        httpStudent({
          url: '/teachers',
          method: 'GET',
          headers: {
            Accept: '*/*',
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          }
        }).then(res => {
          if (res.data.code === 200){
            const tdl = res.data.data;
            for (let j = 0; j < tdl.length; j++){
              if (allTeacherDetailsList.value[i].uid === tdl[j].uid){
                allTeacherDetailsList.value[i].professionalDirection = tdl[j].professionalDirection;
              }
            }
          }
        }).catch(err => {
          console.error(err);
        })
      }
    } else alert('导师信息获取失败！');
  }).catch(err => {
    alert('导师信息获取失败！');
    console.error(err);
  })
})
</script>

<template>
  <div class="title">
    <span>本院导师简历</span>
  </div>
  <div class="container">
    <div class="teacher_resume_item" v-for="(item, index) in allTeacherDetailsList" :key="index">
      <div class="photo">
        <img :src="item.photourl" alt="photo"/>
      </div>
      <div class="item_box">
        <span class="fullname">{{`${item.teacherposition}：${item.fullName}`}}</span>
        <span class="email">电子邮箱：{{item.email}}</span>
        <span class="professional">专业方向：{{item.professionalDirection}}</span>
        <hr class="hea_con_hr"/>
        <div class="detail_box">
          <div class="research_direction">研究方向：{{item.research_direction}}</div>
          <div class="resume">{{item.resume}}</div>
        </div>
        <div class="avatar">
          <img :src="item.avatarUrl" alt="avatar"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
.container
  max-height: calc(100vh - 120px)
  overflow: auto
  padding: 20px
  display: flex
  flex-wrap: wrap
  gap: 100px
  row-gap: 50px
  .teacher_resume_item
    width: 500px
    height: 300px
    position: relative
    .item_box
      width: 100%
      height: 250px
      bottom: 0
      position: absolute
      box-shadow: #00582680 0 0 5px
      border-radius: 10px
      .hea_con_hr
        position: absolute
        top: 25%
        width: 90%
        left: 50%
        transform: translateX(-50%)
      .fullname
        position: absolute
        top: -13%
        left: 22%
        font-size: 18px
      .email
        position:  absolute
        left: 22%
        display: flex
        align-items: center
        top: 2%
      .professional
        position: absolute
        left: 22%
        top: 12%
      .detail_box
        position: absolute
        top: 30%
        left: 5%
      .avatar
        z-index: -1
        width: 100%
        height: 100%
        overflow: hidden
        position: relative
        border-radius: 10px
        img
          position: absolute
          top: 50%
          left: 50%
          transform: translate(-50%, -50%)
          width: 100%
          opacity: .2
    .photo
      box-shadow: #00000030 0 0 5px
      background-color: white
      border-radius: 10px
      z-index: 1
      left: 5%
      width: 75px
      height: 105px
      overflow: hidden
      position: absolute
      img
        width: 100%


</style>