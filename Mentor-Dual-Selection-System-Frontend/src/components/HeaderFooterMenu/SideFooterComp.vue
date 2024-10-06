<script setup lang="ts">
import {onMounted, ref} from "vue";

const copyrightYear = ref(null);

onMounted(() => {
  const date = new Date();
  let year = date.getFullYear();
  copyrightYear.value = year === 2024? '2024': `2024-${year}`;
})
</script>

<template>
  <div class="footer_box">
    <p>Copyright ©{{copyrightYear}}</p>
    <p>永新县爱荧科技有限责任公司</p>
  </div>
</template>

<style scoped lang="sass">
.footer_box
  width: 100%
  text-align: center
  position: absolute
  bottom: 10%
  z-index: -10
  p
    font-size: 12px
    color: white
    margin: 0
    padding: 0
  a
    text-decoration: none
    color: white
    font-size: 12px
    margin: 0
    padding: 0
</style>