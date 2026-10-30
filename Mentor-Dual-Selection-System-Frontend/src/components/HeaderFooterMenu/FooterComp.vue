<script setup lang="ts">
import { onMounted, ref } from "vue";

const copyrightYear = ref<string | null>(null);

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
  width: 100%
  padding: 18px 16px
  box-sizing: border-box
  border-top: 1px solid var(--app-border)
  background-color: var(--app-surface-muted)
  margin-top: auto
  .container
    display: flex
    justify-content: center
    align-items: center
    text-align: center
    min-height: 24px
    p
      font-size: 12px
      color: var(--app-text-secondary)
      font-weight: bold
      margin: 0
      padding: 0
      line-height: 1.5
    a
      text-decoration: none
      color: var(--app-text-secondary)
      font-size: 12px
      margin: 0
      padding: 0

@media (max-width: 767px)
  footer
    padding: 14px 12px
    .container p
      font-size: 11px
</style>
