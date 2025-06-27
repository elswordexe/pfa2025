import axios from "axios";


axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    console.log("[Axios] current JWT token:", token);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      console.warn("[Axios] 401 Unauthorized — Token peut être expiré.");
      localStorage.removeItem("token");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);
export default axios;
