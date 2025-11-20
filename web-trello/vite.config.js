import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

const environment = process.env.NODE_ENV || "development";
const host = environment == "production" ? "0.0.0.0" : "localhost";
const port = process.env.PORT || 3000;

console.log("Config for %s environment. Host: %s, Port: %s", environment, host, port);

export default defineConfig({
  plugins: [tailwindcss()],
  server: { 
    host: host,
    port: port ,

  } // 👈 correrá en http://localhost:3000
})
