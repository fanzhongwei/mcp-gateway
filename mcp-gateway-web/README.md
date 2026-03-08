<!--
 * @FilePath: /mcp-gateway-web/README.md
 * @Author: teddy
 * @Date: 2024-02-12 10:52:52
 * @Description: README
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-05 21:02:45
-->

# Nuxt 3 Minimal Starter

Look at the [Nuxt 3 documentation](https://nuxt.com/docs/getting-started/introduction) to learn more.

## Setup

Make sure to install the dependencies:

```bash
# npm
npm install

# pnpm
pnpm install

# yarn
yarn install

# bun
bun install
```

## Development Server

Start the development server on `http://localhost:3000`:

```bash
# npm
npm run dev

# pnpm
pnpm run dev

# yarn
yarn dev

# bun
bun run dev
```

## Production

Build the application for production:

```bash
# npm
npm run build

# pnpm
pnpm run build

# yarn
yarn build

# bun
bun run build
```

Locally preview production build:

```bash
# npm
npm run preview

# pnpm
pnpm run preview

# yarn
yarn preview

# bun
bun run preview
```

Check out the [deployment documentation](https://nuxt.com/docs/getting-started/deployment) for more information.

## develop remark

- {"username": "mcpAdmin","password": "123456"}
- [安全色](https://www.bootcss.com/p/websafecolors/)
- [git 地址](http://223.243.124.76:36323/teddy/mcp-gateway)
- [icon 地址](https://remixicon.com/)
- [UI 组件库地址](https://antdv.com/components/overview-cn)
- [Tailwind CSS](https://tailwind.nodejs.cn/)
- [web 测试服地址](https://localhost:8888/mcp-gateway)
- [swagger](https://localhost:8888/mcp-gateway/swagger-ui.html)
- [API 地址](https://localhost:8888/mcp-gateway)
- [测试服地址](https://localhost:8888/mcp-gateway/login)
- 开发账号：mcpAdmin
- 开发密码：123456

- 只输入整数

```
onkeyup="(value)=value.replace(/^\D*([1-9]\d*)?.*$/,'$1')"
```

- 可输入4为小数

```
:maxlength="10"
onkeyup="(value)=value.replace(/^\D*(\d*(?:\.\d{0,4})?).*$/g, '$1')"
suffix="元"
```

modal title

```
<template #title>
  <i class="ri-pass-valid-line"></i>
  审核
</template>
```
