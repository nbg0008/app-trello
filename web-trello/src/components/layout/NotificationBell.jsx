import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Bell, Check } from "lucide-react";
import { deleteNotification, fetchNotifications } from "../../modules/apiClient.js";

function formatDate(isoString) {
  if (!isoString) return "";
  try {
    const date = new Date(isoString);
    return date.toLocaleString();
  } catch {
    return "";
  }
}

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [items, setItems] = useState([]);
  const ref = useRef(null);

  const unreadCount = useMemo(() => items.length, [items]);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await fetchNotifications();
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.message || "No se pudieron cargar las notificaciones");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  useEffect(() => {
    function onDocClick(e) {
      if (!ref.current) return;
      if (!ref.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener("click", onDocClick);
    return () => document.removeEventListener("click", onDocClick);
  }, []);

  const onToggle = () => {
    const next = !open;
    setOpen(next);
    if (next) {
      // refrescamos al abrir para ver nuevas
      loadNotifications();
    }
  };

  const onDelete = async (id) => {
    try {
      await deleteNotification(id);
      setItems((prev) => prev.filter((n) => n.id !== id));
    } catch (err) {
      setError(err?.message || "No se pudo borrar la notificaci\u00f3n");
    }
  };

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={onToggle}
        className="relative flex h-10 w-10 items-center justify-center rounded-full border border-white/30 bg-white/15 text-white transition hover:bg-white/25"
        aria-label="Notificaciones"
        aria-expanded={open}
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-[#ff7a59] px-1 text-[11px] font-semibold leading-none text-white shadow-lg">
            {unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          className="absolute right-0 mt-3 w-96 max-h-[70vh] overflow-hidden rounded-2xl border border-white/15 bg-white/95 text-neutral-900 shadow-xl backdrop-blur dark:border-white/20 dark:bg-[#1c1429]/95 dark:text-white"
          role="menu"
        >
          <div className="flex items-center justify-between px-4 py-3 border-b border-white/30 dark:border-white/10">
            <div>
              <p className="text-sm font-semibold">Notificaciones</p>
              <p className="text-xs text-neutral-500 dark:text-white/60">
                {loading ? "Cargando..." : `${items.length} pendientes`}
              </p>
            </div>
            <button
              onClick={loadNotifications}
              className="text-xs font-medium text-[var(--color-brand-600)] hover:text-[var(--color-brand-700)] dark:text-[var(--color-brand-600)]"
            >
              Actualizar
            </button>
          </div>

          <div className="max-h-[60vh] overflow-y-auto p-3 space-y-3">
            {error && (
              <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700 dark:border-red-500/40 dark:bg-red-500/10 dark:text-red-100">
                {error}
              </div>
            )}

            {!loading && items.length === 0 && !error && (
              <div className="rounded-xl border border-white/30 bg-white/70 px-3 py-4 text-sm text-neutral-600 shadow-sm dark:border-white/10 dark:bg-white/5 dark:text-white/70">
                No hay notificaciones pendientes.
              </div>
            )}

            {items.map((n) => (
              <article
                key={n.id}
                className="rounded-xl border border-white/40 bg-white px-3 py-3 shadow-md transition hover:-translate-y-0.5 hover:shadow-lg dark:border-white/10 dark:bg-white/5"
              >
                <p className="text-sm font-semibold text-[var(--color-neutral-950)] dark:text-white">
                  {n.description}
                </p>
                <div className="mt-1 flex items-center justify-between text-xs text-neutral-500 dark:text-white/60">
                  <span>{formatDate(n.createdOn)}</span>
                  <button
                    onClick={() => onDelete(n.id)}
                    className="inline-flex items-center gap-1 rounded-full bg-[var(--color-brand-600)] px-2.5 py-1 text-[11px] font-semibold text-white transition hover:bg-[var(--color-brand-700)]"
                  >
                    <Check className="h-3.5 w-3.5" />
                    Visto
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
