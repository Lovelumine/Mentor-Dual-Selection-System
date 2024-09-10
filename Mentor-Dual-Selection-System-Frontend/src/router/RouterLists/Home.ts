import HomeView from "@/views/HomeView.vue";

export default {
    path: '/',
    name: 'home',
    component: HomeView,
    children: [
        {
            path: 'teachers',
            name: 'teachers',
            component: () => import('@/components/Home/TeachersComp.vue')
        },
        {
            path: 'notice',
            name: 'notice',
            component: () => import('@/components/Home/Notice/NoticeComp.vue')
        }
    ]
}