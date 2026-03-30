import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/main.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const themeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
const legacyThemeMediaQuery = themeMediaQuery as MediaQueryList & {
    addListener?: (listener: (event: MediaQueryListEvent) => void) => void
}

const applySystemTheme = (isDark: boolean) => {
    document.documentElement.classList.toggle('dark', isDark)
}

const handleThemeChange = (event: MediaQueryListEvent) => {
    applySystemTheme(event.matches)
}

applySystemTheme(themeMediaQuery.matches)

if ('addEventListener' in themeMediaQuery) {
    themeMediaQuery.addEventListener('change', handleThemeChange)
} else if (typeof legacyThemeMediaQuery.addListener === 'function') {
    legacyThemeMediaQuery.addListener(handleThemeChange)
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.mount('#app')
