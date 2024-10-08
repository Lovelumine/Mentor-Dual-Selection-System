<script setup lang="ts">
import {ref} from "vue";
import {http} from "@/utils/http";

const pendingUtilForm = ref({
  applicationId: null as null | number,
  approved: null as null | boolean,
  rejectionReason: null as null | string,
  reason: null as null | string,
});

function approveClicked (){
  console.log(pendingUtilForm.value);
  if (pendingUtilForm.value.approved === null) {
    alert('请选择审批状态后再提交');
  } else {
    http({
      url: '/application/approve',
      method: 'POST',
      headers: {
        'Accept': '*/*',
        'Authorization': `Bearer ${localStorage.getItem("token")}`
      },
      params: pendingUtilForm.value,
    }).then(res => {
      if (res.status === 200) {
        alert('该申请处理完成！');
      } else {
        alert(res.data.error);
      }
    }).catch((err) => {
      alert(JSON.parse(err.request.responseText).data.error);
    })
  }
}
</script>

<template>
  <form @submit.prevent="approveClicked">
    <span class="input_item">申请编号：<input type="text" placeholder="请输入" required v-model="pendingUtilForm.applicationId"/></span>
    <span class="input_item">
        <label>是否同意申请：</label>
        <select id="is_accept" required v-model="pendingUtilForm.approved">
          <option :value="null">未选择</option>
          <option :value="true">通过</option>
          <option :value="false">拒绝</option>
        </select>
      </span>
    <span class="input_item">
      拒绝理由：
      <input type="text" placeholder="请输入"
             :disabled="pendingUtilForm.approved !== false" v-model="pendingUtilForm.reason"/>
    </span>
    <button class="button" type="submit">审批</button>
  </form>
</template>

<style scoped lang="sass">
form
  margin: 20px auto 20px auto
  .input_item
    font-size: 16px
    margin-left: 20px
    input
      width: 220px
      height: 32px
      transition: .3s ease
      border: 1px solid #989898
      border-radius: 5px
    input:hover
      border: 1px solid #005826
    input:focus
      outline: none
      box-shadow: #005826 0 0 5px
      border: 1px #005826 solid
    input:disabled
      background-color: #c6c6c6
    select
      width: 220px
      height: 32px
    select
      width: 220px
      height: 32px
      transition: .3s ease
      border: 1px solid #989898
      border-radius: 5px
    select:hover
      border: 1px solid #005826
    select:focus
      outline: none
      box-shadow: #005826 0 0 5px
      border: 1px #005826 solid
.button
  border-radius: 5px
  width: 60px
  height: 32px
  background-color: #005826
  color: white
  margin-left: 20px
  border: 1px solid #005826
  font-size: 15px
  transition: .3s ease
.button:hover
  background-color: #0f7e3f
  border: 1px solid #0f7e3f
</style>