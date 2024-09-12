import axios from 'axios';

export const http = axios.create({
    baseURL: '/api',
    timeout: 5000,
    withCredentials: true
})