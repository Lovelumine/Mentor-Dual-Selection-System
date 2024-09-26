<script setup lang="ts">

</script>

<template>
  <div class="button_box">
    <button>添加单条学生信息</button>
    <button>下载当前学生状态</button>
    <button>下载学生表格模板</button>
    <button>批量上传学生信息</button>
    <button>批量下载学生附件</button>
    <button>下载列表</button>
  </div>
</template>

<style scoped lang="sass">
.button_box
  background-color: #e2e2e2
  width: 100%
  height: 60px
  display: flex
  align-items: center
  button
    padding: 0 20px
    height: 32px
    width: 160px
    font-size: 14px
    border-radius: 5px
    border: none
    color: white
    margin-left: 32px
    background-color: #005826
    transition: .3s ease
  button:hover
    background-color: #0f7e3f
</style>