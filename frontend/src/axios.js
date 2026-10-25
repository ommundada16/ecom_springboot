import axios from "axios";

export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

const API = axios.create({ baseURL: API_URL });

API.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

API.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthCall = error.config?.url?.startsWith("/auth/");
    // A 401 on a normal call while a token is stored means the token expired or is invalid.
    if (error.response?.status === 401 && !isAuthCall && localStorage.getItem("token")) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.assign("/login");
    }
    return Promise.reject(error);
  }
);

export const errorMessage = (error, fallback = "Something went wrong. Please try again.") => {
  if (!error.response) {
    return "Cannot reach the server. Is the backend running?";
  }
  return error.response.data?.message || fallback;
};

export default API;
