<script setup lang="ts">
import {onMounted, ref} from "vue";

const copyrightYear = ref<string | null>(null);

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
  display: flex
  flex-direction: column
  gap: 2px
  p
    font-size: 12px
    color: var(--app-menu-text)
    margin: 0
    padding: 0
    line-height: 1.4
  a
    text-decoration: none
    color: var(--app-menu-text)
    font-size: 12px
    margin: 0
    padding: 0

@media (max-width: 1023px)
  .footer_box
    p,
    a
      font-size: 11px
</style>
