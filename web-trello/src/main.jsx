import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App.jsx";
import { AuthProvider } from "./modules/auth/AuthContext.jsx";
import { GoogleOAuthProvider } from "@react-oauth/google";
import "./App.css";


const currentOrigin = window.location.origin;

const cleanedOrigin = currentOrigin.endsWith('/')
  ? currentOrigin.slice(0, -1)
  : currentOrigin;


const CLIENT_ID = "206192123803-tqklggssabfakgg3kr9mrdll1tm282g6.apps.googleusercontent.com";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <GoogleOAuthProvider 
      clientId={CLIENT_ID}>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </GoogleOAuthProvider>
  </React.StrictMode>
);