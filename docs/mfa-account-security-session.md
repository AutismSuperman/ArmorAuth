# MFA Account Security Session

## Goal

Implement a logged-in account security experience where a user can manage multi-factor authentication from the hosted account home page after sign-in.

## Current State

- `auth_factor` already stores MFA factors.
- TOTP setup, verification, recovery-code generation, and factor deletion already exist under `/api/account/v1`.
- Passkey registration metadata endpoints already exist, but the browser-side ceremony is not wired into the hosted account home page.
- Login can already redirect to `/login/mfa` when MFA is required.
- Application and role policy MFA already exist through `oauth2_client.mfa_required` and `role_mfa_required`.
- Current login behavior treats any verified enabled user factor as a reason to challenge MFA. This couples "factor exists" with "user wants MFA at login".

## First Iteration Scope

- Add an explicit user-level MFA preference.
- Let users turn MFA on only after they have at least one verified enabled runtime factor.
- Support Authenticator app / TOTP setup from the logged-in account home page (`/`).
- Show existing factors, last used time, status, and allow deletion.
- Keep app/role policy-enforced MFA authoritative in the login flow.
- Keep SMS/Text message out of scope for now.

## Backend Design

- Add `user_info.mfa_enabled` with default `false`.
- Include MFA preference and factor summary in account security response.
- Add an account self-service endpoint for updating the user MFA preference.
- Change login MFA decision to:
  - app/role policy requires MFA: challenge is required;
  - user has `mfa_enabled=true` and a verified enabled factor: challenge is required;
  - policy requires MFA and user has no factor: send to MFA binding flow;
  - factor exists but `mfa_enabled=false`: no user-triggered challenge.

## Account Home UI Design

- Extend the hosted login success page (`home.ftlh`) with an `账号安全` section.
- Present user-managed MFA status as operational controls, not a marketing panel.
- Provide:
  - main MFA switch;
  - inline Authenticator app setup panel;
  - verification input;
  - recovery code display after setup starts;
  - current factor list;
  - clear warning when the user has no runtime MFA factor.
- Keep the admin console focused on management configuration and permission probing; do not place personal MFA self-service in `/main/settings`.

## Acceptance

- A logged-in user can bind TOTP, verify it, enable MFA, disable MFA, and delete the factor from the hosted account home page.
- Enabling MFA without a verified factor is blocked with a clear message.
- Existing app/role MFA policy remains authoritative in the login flow.
- Relevant server tests pass.
- Admin UI builds successfully.
