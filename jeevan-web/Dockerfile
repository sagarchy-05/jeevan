# Dev-server image for the demo harness. A production (build + static serve)
# image can replace this later; the dev server keeps live reload for the demo
# and reads VITE_API_BASE_URL from the environment at runtime.
FROM node:24-alpine
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host"]
