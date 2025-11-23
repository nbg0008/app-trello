import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import mkcert from "vite-plugin-mkcert";


const environment = process.env.NODE_ENV || "development";
const host = environment == "production" ? "0.0.0.0" : "localhost";
const port = process.env.PORT || 3000;

console.info("Config for %s environment. Host: %s, Port: %s", environment, host, port);

export default defineConfig({
  plugins: [tailwindcss(), mkcert()],
  server: { 
    host: host,
    port: port 
  } // 👈 correrá en http://localhost:3000
})
