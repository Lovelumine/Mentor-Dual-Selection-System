<script setup lang="ts">

import {Paperclip} from "@element-plus/icons-vue";
</script>

<template>
  <div class="card_box">
    <div class="notice_title_box">
      <h3 class="notice_title">
        关于个性化培养方案的说明
      </h3>
      <span class="release_date">
        发布时间：2024年9月10日
      </span>
    </div>
    <hr/>
    <div class="notice_content">
      本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    </div>
    <div class="annex">
      <a href="#"><el-icon size="20"><Paperclip /></el-icon>下载文件.docx</a>
      <div class="button_box">
        <button class="button" @click="handleEdit(scope.$index, scope.row)">修改</button>
        <button class="button" @click="handleDelete(scope.$index, scope.row)">删除</button>
      </div>
    </div>
  </div>
  <div class="card_box">
    <div class="notice_title_box">
      <h3 class="notice_title">
        关于个性化培养方案的说明
      </h3>
      <span class="release_date">
        发布时间：2024年9月10日
      </span>
    </div>
    <hr/>
    <div class="notice_content">
      本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
    本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
      本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。
      本学期增加了个性化培养方案的填写，请老师和同学们共同完成，请各位老师依据学生个性化培养方案的完成情况进行评分，并将电子版培养方案上传至系统，或由学生签名后提交给教务老师，谢谢。

    </div>
    <div class="annex">
      <a href="#"><el-icon size="20"><Paperclip /></el-icon>下载文件.docx</a>
      <div class="button_box">
        <button class="button" @click="handleEdit(scope.$index, scope.row)">修改</button>
        <button class="button" @click="handleDelete(scope.$index, scope.row)">删除</button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.card_box
  margin: 0 auto 30px auto
  width: 97%
  height: 250px
  background-color: white
  padding: 20px
  border-radius: 10px
  transition: .3s ease
  .notice_title_box
    display: flex
    h3
      font-size: 23px
    .release_date
      margin: auto 0 0 auto
  hr
    margin: 10px auto
  .notice_content
    height: 100px
    margin-bottom: 20px
    display: -webkit-box
    -webkit-line-clamp: 4
    -webkit-box-orient: vertical
    overflow: hidden
    text-overflow: ellipsis
  .annex
    display: flex
    a
      text-decoration: none
      display: inline-flex
      align-items: center

    .button_box
      margin: 0 0 0 auto
      .button
        background-color: #005826
        border: 1px solid #005826
        color: white
        width: 60px
        height: 32px
        border-radius: 5px
        font-size: 15px
        transition: .3s ease
      .button:hover
        cursor: pointer
        background-color: #0f7e3f
        border: 1px solid #0f7e3f
      .button:last-child
        margin-left: 10px
        border: 1px solid #bd0000
        background-color: #bd000000
        color: #bd0000
      .button:last-child:hover
        background-color: #bd0000
        color: white

.card_box:hover
  box-shadow: 0 10px 20px 0 rgba(0,0,0,0.2)
  transform: translateY(-1%)
</style>