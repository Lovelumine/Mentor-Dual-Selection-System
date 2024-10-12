<script setup lang="ts">
import { onMounted, ref } from "vue";

const copyrightYear = ref<string | null>(null); // 将类型声明为 string | null

onMounted(() => {
  const date = new Date();
  const year = date.getFullYear();
  copyrightYear.value = year === 2024 ? '2024' : `2024-${year}`;
});
</script>



<template>
  <footer>
    <div class="container">
      <p>Copyright ©{{copyrightYear}} 永新县爱荧科技有限责任公司</p>
    </div>
  </footer>
</template>

<style scoped lang="sass">
footer
  height: 30px
  width: 100%
  background-color: #e0e0e0
  position: absolute
  bottom: 0
  z-index: 10
  .container
    p
      position: absolute
      text-align: center
      font-size: 12px
      color: #003c1a
      font-weight: bold
      top: 50%
      left: 4%
      transform: translateY(-50%)
      margin: 0
      padding: 0
    a
      position: absolute
      text-decoration: none
      font-size: 12px
      font-weight: bold
      margin: 0
      padding: 0
      color: #003c1a
      top: 50%
      transform: translateY(-50%)
      right: 4%
</style>