# ai-coding-station-frontend

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Compile and Minify for Production

```sh
npm run build-only
```

## Environment Variables

- `.env.development`
  - `VITE_APP_DEPLOY_BASE_URL`: deploy preview domain used by `getDeployUrl` (usually local nginx/static host in dev).
  - `VITE_DEV_PROXY_TARGET`: Vite dev server `/api` proxy target (local debug backend).
  - `VITE_APP_API_BASE_URL=/api`, `VITE_APP_PREVIEW_BASE_URL=/api`.
  - `OPENAPI_SCHEMA_URL`: OpenAPI schema URL used by `npm run openapi2ts`.
- `.env.production`
  - `VITE_APP_DEPLOY_BASE_URL`: public deploy domain used for generated deploy links.
  - Keep frontend API base as same-origin path: `VITE_APP_API_BASE_URL=/api`.
  - `OPENAPI_SCHEMA_URL` can point to production backend swagger endpoint.

Notes:
- Do not hardcode `localhost` / LAN IP in `vite.config.ts` or runtime config.
- Do not use `http://backend:8142` in browser runtime; use `/api` + reverse proxy in production.
