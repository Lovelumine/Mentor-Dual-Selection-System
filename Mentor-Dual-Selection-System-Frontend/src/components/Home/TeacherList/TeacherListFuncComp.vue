<script setup lang="ts">

</script>

<template>
  <div class="button_box">
    <button>添加单条导师信息</button>
    <button>下载当前导师状态</button>
    <button>下载导师表格模板</button>
    <button>批量上传导师信息</button>
  </div>
</template>

<style scoped lang="sass">
.button_box
  width: 100%
  height: 80px
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