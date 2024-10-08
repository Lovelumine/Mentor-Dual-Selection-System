import {defineStore} from "pinia";
import {http} from "@/utils/http";

// interface UserInfo {
//     uid: number,
//     role: string
//     avatarUrl: string,
//     fullName: string,
//     email: string,
//     username: string,
// }

export const useUserInfoStore = defineStore('userInfoStore', {
    state: () => ({
        userInfo: {
            uid: -1 as undefined | number,
            role: null as null | string,
            avatarUrl: null as null | string,
            fullName: null as null | string,
            email: null as null | string,
            username: null as null | string,
        }
    }),
    actions: {
         fetchUserInfo() {
            http({
                url: '/user/me',
                method: "GET",
                headers: {
                    "Accept": "*/*",
                    "Authorization": `Bearer ${localStorage.getItem('token')}`
                }
            }).then(res => {
                console.log(res);
                if (res.data.code === 200) {
                    this.userInfo = res.data.data;
                }
            }).catch(err => {
                console.error(err);
                localStorage.removeItem('token');
                window.location.reload();
            })
        }
    }
})