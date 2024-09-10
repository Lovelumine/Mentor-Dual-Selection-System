import axios from 'axios';

export const http = axios.create({
    baseURL: 'http://223.82.75.76:50000/',
    timeout: 5000,
    withCredentials: true
})