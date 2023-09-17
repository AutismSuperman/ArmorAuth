import { rest } from 'msw';

export const handlers = [
  rest.post('/login', (req, res, ctx) => {
    return res(
      ctx.json({
        redirectUri: 'http://localhost:5173/consent',
        description: '请在授权页面点击同意，完成登录。',
      }),
    );
  }),
  rest.post('/login/captcha', (req, res, ctx) => {
    const { code } = req.params;
    if (code == '123456') {
      return res(
        ctx.json({
          redirectUri: 'http://localhost:5173/consent',
          description: '请在授权页面点击同意，完成登录。',
        }),
      );
    } else {
      return res(
        ctx.status(403),
        ctx.json({
          description: '验证码不对！',
        }),
      );
    }
  }),
];
export const defaultHandlers = [];
