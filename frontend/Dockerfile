# Build stage
FROM node:18 AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (use install instead of ci for optional deps)
RUN npm install --legacy-peer-deps

# Explicitly install rollup native bindings
RUN npm install @rollup/rollup-linux-x64-gnu --save-optional

# Copy source code
COPY . .

# Build argument for API URL - NOTE: must include /api suffix as client expects full base URL
ARG VITE_API_BASE_URL=http://localhost:8080/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Build the app
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
