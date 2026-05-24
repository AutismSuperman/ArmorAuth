import { UserManager, UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts';

const authority = 'http://localhost:9000';

const settings: UserManagerSettings = {
  authority,
  client_id: 'react-spa-pkce',
  redirect_uri: 'http://localhost:3000/callback',
  post_logout_redirect_uri: 'http://localhost:3000/',
  response_type: 'code',
  scope: 'openid profile email',
  automaticSilentRenew: true,
  silent_redirect_uri: 'http://localhost:3000/silent-renew',
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  metadata: {
    authorization_endpoint: `${authority}/oauth2/authorize`,
    token_endpoint: `${authority}/oauth2/token`,
    userinfo_endpoint: `${authority}/userinfo`,
    end_session_endpoint: `${authority}/connect/logout`,
    jwks_uri: `${authority}/oauth2/jwks`,
    issuer: authority,
  },
};

export const userManager = new UserManager(settings);

export async function login() {
  await userManager.signinRedirect();
}

export async function loginCallback() {
  return await userManager.signinRedirectCallback();
}

export async function logout() {
  await userManager.signoutRedirect();
}

export async function getUser() {
  return await userManager.getUser();
}

export async function getAccessToken() {
  const user = await userManager.getUser();
  return user?.access_token;
}
