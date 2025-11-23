const BASE_URL = import.meta.env.VITE_API_BASE_URL;
const API_BASE_URL = `http://${BASE_URL}` || "http://localhost:8080/trello/v1"; 

const TOKEN_STORAGE_KEY = "token";
const REFRESH_TOKEN_STORAGE_KEY = "refreshToken";
const SESSION_STORAGE_KEY = "trello_auth_session";
const LEGACY_SESSION_KEY = "demo_auth_session";

function persistSession(token, refreshToken, user) {
  if (!token || !refreshToken || !user) {
    throw new Error("Invalid authentication payload received.");
  }

  const session = JSON.stringify({ token, refreshToken, user, storedAt: Date.now() });
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
  localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
  localStorage.setItem(SESSION_STORAGE_KEY, session);
  localStorage.removeItem(LEGACY_SESSION_KEY);
  return user;
}

function clearSession() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
  localStorage.removeItem(SESSION_STORAGE_KEY);
  localStorage.removeItem(LEGACY_SESSION_KEY);
}

function parseJson(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

// Completamos los campos de userName y name con la parte del email antes de la @
export async function register(email, password) {
  if (!email || !password) {
    throw new Error("Email and password are required.");
  }
  if (password.length < 8) throw new Error("La contrasena debe tener al menos 8 caracteres.");

  const base = (email.split("@")[0] || "user").trim();

  const body = {
    userName: base,
    name: base.charAt(0).toUpperCase() + base.slice(1),
    email,
    password,
  };

  try {
    const res = await fetch(`${API_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const errorText = await res.text();
      const details = parseJson(errorText);
      throw new Error(details?.message || errorText || `HTTP ${res.status}`);
    }

    const payload = await res.text();
    return parseJson(payload);
  } catch (err) {
    console.error("REGISTER ERROR:", err);
    throw err;
  }
}

export async function login(email, password, _remember) {
  if (!email || !password) {
    throw new Error("Email and password are required.");
  }

  try {
    const res = await fetch(`${API_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });

    const bodyText = await res.text();

    if (!res.ok) {
      const details = parseJson(bodyText);
      const message = details?.message || "Credenciales invalidas.";
      throw new Error(message);
    }

    const data = parseJson(bodyText) || {};
    const { accessToken, refreshToken, user } = data;

    return persistSession(accessToken, refreshToken, user);
  } catch (err) {
    console.error("LOGIN ERROR:", err);
    clearSession();
    throw err;
  }
}

export function getCurrentUser() {
  const storedSession = localStorage.getItem(SESSION_STORAGE_KEY);
  if (storedSession) {
    try {
      const session = JSON.parse(storedSession);
      if (session?.token && session?.refreshToken && session?.user) {
        localStorage.setItem(TOKEN_STORAGE_KEY, session.token);
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, session.refreshToken);
        return session.user;
      }
    } catch (err) {
      console.warn("Failed to parse stored auth session.", err);
    }
  }

  const legacy = localStorage.getItem(LEGACY_SESSION_KEY);
  if (legacy) {
    try {
      const { email } = JSON.parse(legacy);
      if (email) return { email };
    } catch {
      /* ignore legacy parse errors */
    }
  }

  return null;
}

export async function signOut() {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  if (token && refreshToken) {
    try {
      await fetch(`${API_URL}/auth/logout`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ refreshToken }),
      });
    } catch (err) {
      console.warn("Error during sign out request:", err);
    }
  }
  clearSession();
}

// A la espera de que el backend lo implemente.
export async function resetPassword(email, newPassword) {
  console.warn("resetPassword called but not implemented on server yet.");
  throw new Error("Password reset not available yet.");
}
