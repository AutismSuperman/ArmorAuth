import axios from 'axios';

axios.create({
  withCredentials: true,
  timeout: 30,
});

axios.interceptors.response.use(
  (response) => {
    const { data } = response;
    return data;
  },
  (error) => {
    return Promise.reject(error);
  },
);
export default axios;
