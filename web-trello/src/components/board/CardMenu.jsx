import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";

const MENU_OPTIONS = [
  { id: "edit-labels", label: "Editar etiquetas" },
  {
    id: "change-members",
    label: "Cambiar miembros",
    disabled: true,
    hint: "En desarrollo",
  },
  { id: "edit-dates", label: "Editar fechas" },
  { id: "move", label: "Mover", disabled: true, hint: "En desarrollo" },
  { id: "copy-card", label: "Copiar tarjeta", disabled: true, hint: "En desarrollo" },
  { id: "copy-link", label: "Copiar enlace", disabled: true, hint: "En desarrollo" },
  { id: "archive", label: "Archivar", disabled: true, hint: "En desarrollo" },
  { id: "delete-card", label: "Eliminar", danger: true },
];

export default function CardMenu({ anchorRect, onAction, onClose }) {
  const menuRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        onClose?.();
      }
    }
    function handleEsc(event) {
      if (event.key === "Escape") onClose?.();
    }

    document.addEventListener("pointerdown", handleClickOutside, true);
    window.addEventListener("keydown", handleEsc);
    return () => {
      document.removeEventListener("pointerdown", handleClickOutside, true);
      window.removeEventListener("keydown", handleEsc);
    };
  }, [onClose]);

  if (!anchorRect) return null;

  const top = anchorRect.top + window.scrollY;
  const left = anchorRect.right + 12 + window.scrollX;
  const isDark = document.documentElement.classList.contains("dark");

  return createPortal(
    <div className={isDark ? "dark" : ""}>
      <div
        ref={menuRef}
        className="
          fixed z-50 w-52 rounded-3xl overflow-hidden border shadow-2xl
          bg-[var(--color-surface)] text-[var(--color-neutral-950)] border-[rgba(0,0,0,0.1)]
          dark:bg-[var(--color-surface-hover)] dark:text-[var(--color-neutral-950)] dark:border-[rgba(255,255,255,0.1)]
        "
        style={{ top, left }}
      >
        <ul className="py-2">
          {MENU_OPTIONS.map((option) => {
            const isDisabled = Boolean(option.disabled);
            return (
              <li key={option.id}>
                <button
                  type="button"
                  onClick={() => {
                    if (!isDisabled) onAction?.(option.id);
                  }}
                  disabled={isDisabled}
                  className={[
                    "w-full px-4 py-2 text-left transition",
                    isDisabled
                      ? "cursor-not-allowed bg-transparent text-neutral-500 dark:text-neutral-500"
                      : option.danger
                        ? "text-red-500 hover:bg-red-500/10 hover:text-red-600 dark:hover:bg-red-400/15 dark:hover:text-red-300"
                        : "hover:bg-[var(--color-brand-500)]/15 hover:text-[var(--color-brand-700)] dark:hover:bg-[var(--color-brand-500)]/25 dark:hover:text-[var(--color-brand-300)]",
                  ].join(" ")}
                >
                  <span className="block text-sm font-medium">
                    {option.label}
                  </span>
                  {option.hint ? (
                    <span className="text-xs text-neutral-400 dark:text-neutral-500">
                      {option.hint}
                    </span>
                  ) : null}
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </div>,
    document.body
  );
}
