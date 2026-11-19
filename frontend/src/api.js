import axios from "axios";

export const API_ORIGIN = "http://localhost:8080";

const api = axios.create({ baseURL: `${API_ORIGIN}/api` });

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Log out automatically if the token is rejected
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && localStorage.getItem("token")) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

export const errMsg = (e) => e.response?.data?.message || "Something went wrong. Is the backend running?";

// Room photos are either an uploaded file (/uploads/...) served by the backend, or a full web link.
export const imgSrc = (url) => (!url ? null : url.startsWith("/uploads/") ? API_ORIGIN + url : url);

// Download a file (such as a CSV) from a protected admin endpoint.
export async function downloadFile(path, filename) {
  const res = await api.get(path, { responseType: "blob" });
  const url = URL.createObjectURL(res.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export default api;
