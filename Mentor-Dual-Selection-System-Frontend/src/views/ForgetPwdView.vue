<script setup lang="ts">
import FooterComp from "@/components/HeaderFooterMenu/FooterComp.vue";
import {ref} from "vue";
import {http} from "@/utils/http";

interface RepwdImpl {
  email: string | null;
  token: string | null;
  password: string | null;
}

const repwdForm = ref<RepwdImpl>({
  email: null,
  token: null,
  password: null,
});

const getEmailToken = () => {
  if (repwdForm.value.email) {
    http({
      url: '/auth/forgot-password',
      method: 'POST',
      headers: {
        Accept: '*/*',
      },
      params: {
        email: repwdForm.value.email,
      }
    }).then(res => {
      if (res.status === 200) {
        alert('邮箱令牌已发送！请注意查收！');
      } else {
        alert('邮箱令牌发送失败！');
      }
    }).catch((err) => {
      if (err.request.status === 404) {
        alert(JSON.parse(err.request.responseText).data);
      } else {
        alert('邮箱令牌发送失败');
        // console.log(err);
      }

    })
  } else {
    alert('请填写邮箱地址！');
  }
}

const repwdClicked = () => {
  if (repwdForm.value.password === null || repwdForm.value.password === '') {
    alert('请填写新密码！');
  }
  if (repwdForm.value.token === null || repwdForm.value.token === '') {
    alert('请填写邮箱令牌！');
  }
  http({
    url: '/auth/reset-password-via-token',
    method: 'POST',
    headers: {
      Accept: '*/*',
    },
    params: {
      token: repwdForm.value.token,
      newPassword: repwdForm.value.password,
    }
  }).then(res => {
    if (res.status === 200) {
      alert('密码修改成功！');
    } else alert('密码修改失败！');
  }).catch(() => {
    alert('密码修改失败！');
  })
}
</script>

<template>
  <h1 style="text-align: center; margin-top: 20px">重置密码</h1>
  <el-form label-width="auto" style="width: 500px; margin: 5% auto 0 auto;" :model="repwdForm" @submit.prevent="repwdClicked">
    <el-form-item label="电子邮箱：">
      <el-input type="text" placeholder="请输入" v-model="repwdForm.email"/>
      <el-button type="primary" native-type="button" @click="getEmailToken">发送验证码</el-button>
    </el-form-item>
    <el-form-item label="邮箱令牌：">
      <el-input type="text" placeholder="请输入" v-model="repwdForm.token" required/>
    </el-form-item>
    <el-form-item label="新密码：">
      <el-input type="password" placeholder="请输入" v-model="repwdForm.password" required/>
    </el-form-item>
    <el-button native-type="submit" type="primary">重置密码</el-button>
  </el-form>
  <FooterComp/>
</template>

<style scoped lang="sass">

</style>