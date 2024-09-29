import {defineStore} from "pinia";

export const useUploadCoverStore = defineStore('UploadCoverStore', {
    state: () => ({
        isCoverShow: false,
        numberOfPeople: -1,
        nowNumberOfPeople: -1,
        nowPeopleName: '',
        updateFalStatus: 0,

        statusStore: {
            numberOfPeople: -1,
            nowNumberOfPeople: -1,
            nowPeopleName: '',
            updateFalStatus: 0
        }
    }),
    actions: {
        triggerCoverShow(target: boolean) {
            if (this.isCoverShow !== target) this.isCoverShow = target;
        },
        recordNumberOfPeople(target: number) {
            this.statusStore.numberOfPeople = target;
        },
        recordNowNumberOfPeople(target: number) {
            this.statusStore.nowNumberOfPeople = target;
        },
        recordNowPeopleName(target: string) {
            this.statusStore.nowPeopleName = target;
        },
        recordUpdateFalStatus(target: number) {
            this.statusStore.updateFalStatus = target;
            console.log('pinia接受', this.statusStore.updateFalStatus);
        },
        recordStatus(nop: number, nnop: number, npn: string, ufs: number){
            this.statusStore.numberOfPeople = nop;
            this.statusStore.nowNumberOfPeople = nnop;
            this.nowPeopleName = npn;
            this.statusStore.updateFalStatus = ufs;
        }
    }
})