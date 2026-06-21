import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/suggest": {
        target: "http://localhost:8082",
        changeOrigin: true
      },
      "/search": {
        target: "http://localhost:8082",
        changeOrigin: true
      }
    }
  }
});
