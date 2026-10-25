# ShopEase — Frontend

React + Vite client for ShopEase. See the [repo root README](../README.md) for the full project overview, Docker setup and API reference.

## Development

```bash
npm install
npm run dev
```

Requires the [backend](../backend) to be running. The API address comes from `VITE_API_URL` and defaults to `http://localhost:8080/api`; override it in a `.env.local` file.

## Scripts

- `npm run dev` — start the Vite dev server
- `npm run build` — production build
- `npm run preview` — preview the production build
- `npm run lint` — run ESLint
