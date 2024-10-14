import {defineStore} from "pinia";

export const useTeacherListStore = defineStore('TeacherListStore', {
    state: () => ({
        teacherListSt: null as any
    }),
    actions: {
        updateTeacherList(teacherList: any) {
            this.teacherListSt = teacherList;
        }
    }
})
