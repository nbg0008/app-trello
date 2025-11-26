// src/components/layout/PageShell.jsx
import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../modules/auth/AuthContext.jsx";
import logo from "../../assets/Logo dashboard2.png";
import BotonModo from "../ui/BotonModo.jsx";
import NotificationBell from "./NotificationBell.jsx";


export default function PageShell({ title, actions, children }) {
  return (
    <div className="min-h-screen bg-[var(--color-brand-25)] text-[var(--color-neutral-950)] dark:bg-[var(--color-brand-25)] dark:text-[var(--color-neutral-950)]">
      <Header />
      <main className="mx-auto max-w-7xl px-6 pt-2 pb-5">
        <div className="flex items-center justify-between gap-4">
          {title ? (
            <h1 className="text-3xl font-extrabold tracking-tight text-[var(--color-neutral-950)] dark:text-[var(--color-brand-500)]">  
              {title}
            </h1>
          ) : (
            <div />
          )}
          {actions}
        </div>
        <div className="mt-3">{children}</div>
      </main>
    </div>
  );
}

function Header() {
  const navigate = useNavigate();
  return (
  <header className="sticky top-0 z-40 bg-brand-600 text-white shadow-lg">
  <div className="mx-auto max-w-7xl px-6 h-16 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <span className="text-lg font-semibold tracking-wide">
          <img src={logo} alt="flomind" className="h-11 w-auto" />
        </span>
      </div>

      <div className="hidden md:flex flex-1 justify-center">
        <input
          type="search"
          placeholder="Buscar tableros, listas o tareas..."
          onChange={(e) => {
            const q = e.target.value.trim();
            navigate(`/dashboard?q=${encodeURIComponent(q)}`);
          }}
          className="w-full max-w-md rounded-xl bg-brand-500/40 placeholder-white/70 text-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-white/60"
        />
      </div>

      <div className="flex items-center gap-4">
        <NotificationBell />
        <AvatarArea />
      </div>
    </div>
  </header>
);
}

function AvatarArea() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  const initial = (user?.email || "U").charAt(0).toUpperCase();
  const storageKey = user ? `profilePhoto:${user.id}` : null;
  const [profilePhoto, setProfilePhoto] = useState(null);

  useEffect(() => {
    if (!storageKey) {
      setProfilePhoto(null);
      return;
    }
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) setProfilePhoto(saved);
      else setProfilePhoto(null);
    } catch {
      setProfilePhoto(null);
    }
  }, [storageKey]);

  useEffect(() => {
    function onDocClick(e) {
      if (!ref.current) return;
      if (!ref.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener("click", onDocClick);
    return () => document.removeEventListener("click", onDocClick);
  }, []);

  const go = (path) => {
    setOpen(false);
    navigate(path);
  };

  const logout = async () => {
    setOpen(false);
    try {
      await signOut();
    } catch { /* empty */ }
    navigate("/login", { replace: true });
  };

  return (
    <div className="flex items-center gap-3" ref={ref}>
      <span className="text-sm text-white/90 hidden sm:inline">
        Mi cuenta
      </span>

      <div className="relative">
  <button
  className="flex h-10 w-9 items-center justify-center rounded-full bg-white/30 border border-white/40 font-semibold overflow-hidden"
  onClick={() => setOpen((v) => !v)}
  aria-haspopup="menu"
  aria-expanded={open}
  title={user?.email || "Mi cuenta"}
>
  {profilePhoto ? (
    <img
      src={profilePhoto}
      alt={user?.email || "Foto de perfil"}
      className="h-full w-full object-cover rounded-full"
    />
  ) : (
    initial
  )}
</button>

        {open && (
  <div
    role="menu"
    className={[
      "absolute right-0 mt-2 w-44 rounded-xl border p-1 shadow-lg transition-colors duration-300",
      "bg-white/95 text-neutral-900 border-white/20",
      "dark:bg-[#1c1429]/95 dark:text-white dark:border-white/10",
    ].join(" ")}
  >
    <MenuItem onClick={() => go('/perfil')}>
      Mi cuenta
    </MenuItem>

    <MenuItem onClick={() => go('/ajustes')}>
      Ajustes
    </MenuItem>

    <MenuItem onClick={() => go('/suscripcion')}>
      Flomind pro ⚡
    </MenuItem>

    <MenuItem danger onClick={logout}>
      Cerrar sesi{'\u00f3'}n
    </MenuItem>
  </div>
)}
<BotonModo />


      </div>
    </div>
  );
}

function MenuItem({ children, onClick, danger }) {
  return (
    <button
      role="menuitem"
      onClick={onClick}
      className={`w-full rounded-lg px-3 py-2 text-left text-sm transition-colors
        ${
          danger
            ? `
              text-[var(--btn-danger-text)] 
              hover:bg-[var(--btn-danger-hover-bg)]
            `
            : `
              text-[var(--color-neutral-950)] 
              dark:text-[var(--color-neutral-950)] 
              hover:bg-[var(--color-surface-hover)] 
              hover:text-[var(--color-brand-500)]
              dark:hover:text-[var(--color-brand-500)]
            `
        }`}
    >
      {children}
    </button>
  );
}

