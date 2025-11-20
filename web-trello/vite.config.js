import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

const host = import.meta.env.PROD ? "0.0.0.0" : "localhost";
const port = import.meta.env.PORT || 3000;

console.log("Config for %s environment. Host: %s, Port: %s", 
  import.meta.env.PROD ? "PROD":"DEV", host, port);

export default defineConfig({
  plugins: [tailwindcss()],
  server: { 
    host: host,
    port: port ,

  } // 👈 correrá en http://localhost:3000
})
