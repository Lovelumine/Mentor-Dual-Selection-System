import {defineStore} from "pinia";

export const useStuListGrade = defineStore('useStudentListGrade', {
    state: () => ({
        gradeStatus: 0
    }),
    actions: {
        changeGrade (target: number) {
            if (this.gradeStatus !== target) {
                this.gradeStatus = target;
            }
        }
    }
})