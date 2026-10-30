<script setup lang="ts">
import FooterComp from "@/components/HeaderFooterMenu/FooterComp.vue";
import {ref} from "vue";
import {http} from "@/utils/http";
import {useRouter} from "vue-router";
import { ElMessage } from "element-plus";
const router = useRouter();

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
        ElMessage.success('邮箱令牌已发送，请注意查收');
      } else {
        ElMessage.error('邮箱令牌发送失败');
      }
    }).catch((err) => {
      const message = err?.response?.data?.data || err?.response?.data?.message || '邮箱令牌发送失败';
      ElMessage.error(message);
    })
  } else {
    ElMessage.warning('请填写邮箱地址');
  }
}

const repwdClicked = () => {
  if (repwdForm.value.password === null || repwdForm.value.password === '') {
    ElMessage.warning('请填写新密码');
    return;
  }
  if (repwdForm.value.token === null || repwdForm.value.token === '') {
    ElMessage.warning('请填写邮箱令牌');
    return;
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
      ElMessage.success('密码修改成功');
    } else ElMessage.error('密码修改失败');
  }).catch(() => {
    const message = '密码修改失败';
    ElMessage.error(message);
  })
}
const returnClicked = () => {
  router.push('/');
}
</script>

<template>
  <div class="forget_page">
    <h1>重置密码</h1>
    <el-form class="forget_form" label-width="auto" :model="repwdForm" @submit.prevent="repwdClicked">
      <el-form-item label="电子邮箱：">
        <div class="email_group">
          <el-input type="text" placeholder="请输入" v-model="repwdForm.email" />
          <el-button type="primary" native-type="button" @click="getEmailToken">发送验证码</el-button>
        </div>
      </el-form-item>
      <el-form-item label="邮箱令牌：">
        <el-input type="text" placeholder="请输入" v-model="repwdForm.token" required />
      </el-form-item>
      <el-form-item label="新密码：">
        <el-input type="password" placeholder="请输入" v-model="repwdForm.password" required />
      </el-form-item>
      <div class="form_actions">
        <el-button native-type="submit" type="danger">重置密码</el-button>
        <el-button native-type="button" type="primary" @click="returnClicked">返回登录</el-button>
      </div>
    </el-form>
    <FooterComp />
  </div>
</template>

<style scoped lang="sass">
.forget_page
  min-height: calc(100vh - 60px)
  padding: 28px 16px 48px
  display: flex
  flex-direction: column
  align-items: center
  box-sizing: border-box
  h1
    text-align: center
    margin: 0
    color: var(--app-text-primary)

.forget_form
  width: min(500px, calc(100vw - 32px))
  margin: 40px auto 0
  padding: 28px 24px
  border-radius: 20px
  border: 1px solid var(--app-border)
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  box-shadow: 0 20px 40px var(--app-shadow)

.email_group
  display: flex
  flex-direction: column
  gap: 10px
  width: 100%

.form_actions
  display: flex
  flex-wrap: wrap
  gap: 10px
  justify-content: flex-start

@media (max-width: 767px)
  .forget_page
    padding: 24px 12px 40px

  .forget_form
    width: min(100%, calc(100vw - 24px))
    padding: 22px 16px

  .form_actions
    flex-direction: column
    :deep(.el-button)
      margin-left: 0
</style>
