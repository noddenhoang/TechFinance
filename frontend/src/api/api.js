import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Tạo instance axios với config mặc định
const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Interceptor để thêm token vào header mỗi request
api.interceptors.request.use(config => {
  const auth = useAuthStore();
  
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  
  return config;
});

export { api, BASE_URL }; 