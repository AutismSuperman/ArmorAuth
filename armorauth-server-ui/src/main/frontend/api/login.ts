import axios from '@/util/axios';
import * as qs from 'qs';

export interface LoginParams {
  username: string;
  password: string;
  'remember-me': string;
}

export interface LoginCaptchaParams {
  account: string;
  captcha: string;
  'remember-me': string;
}

export const login = (params: LoginParams) => axios.post('/login', qs.stringify(params));

export const loginCaptcha = (params: LoginCaptchaParams) => axios.post('/login/captcha', qs.stringify(params));
