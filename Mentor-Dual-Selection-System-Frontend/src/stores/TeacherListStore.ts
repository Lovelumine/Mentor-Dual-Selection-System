import {defineStore} from "pinia";

export const useTeacherListStore = defineStore('TeacherListStore', {
    state: () => ({
        teacherListSt: null
    }),
    actions: {
        updateTeacherList(teacherList) {
            this.teacherListSt = teacherList;
        }
    }
})