
import axios from "axios";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const apiClient = axios.create({
    baseURL: API_BASE,
    headers: {
        "Content-Type": "application/json",
    },
});


apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("jwt_token");
        if(token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default apiClient;