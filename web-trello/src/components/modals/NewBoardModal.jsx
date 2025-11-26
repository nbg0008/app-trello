import React, { useEffect, useMemo, useState } from "react";
import Button from "../ui/Button.jsx";
import {
  BOARD_BACKGROUND_OPTIONS,
  resolveBoardBackground,
} from "../../constants/boardBackgrounds.js";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../modules/auth/AuthContext.jsx";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/trello/v1";
const API_URL = `https://${API_BASE_URL}/tableros`; 


const TEMPLATES = {
  "Sin plantilla": {
    description: "Empieza desde cero, sin listas iniciales.",
    lists: [],
  },
  "Plantilla básica": {
    description: "3 listas para arrancar rápido.",
    lists: [
      { name: "Pendiente", tasks: ["Tarea 1", "Tarea 2"] },
      { name: "En progreso", tasks: [] },
      { name: "Hecho", tasks: [] },
    ],
  },
  "Proyecto simple": {
    description: "Flujo compacto para proyectos pequeños.",
    lists: [
      { name: "Backlog", tasks: [] },
      { name: "En progreso", tasks: [] },
      { name: "Revision", tasks: [] },
      { name: "Hecho", tasks: [] },
    ],
  },
  Estudios: {
    description: "Organiza clases, tareas y exámenes.",
    lists: [
      { name: "Asignaturas", tasks: [] },
      { name: "Tareas", tasks: [] },
      { name: "Exámenes", tasks: [] },
      { name: "Hecho", tasks: [] },
    ],
  },
};

export default function NewBoardModal({
  onCreated,
  open,
  onOpenChange,
  initialTemplate = "Sin plantilla",
  showTriggerButton = true,
  // 👇 NUEVO: cuando se crea desde WorkspaceDetail, te pasarán el id
  workspaceId,
}) {
  const navigate = useNavigate();
  const { user } = useAuth();

  const isControlled = typeof open === "boolean";
  const [internalOpen, setInternalOpen] = useState(false);
  const isOpen = isControlled ? open : internalOpen;

  const [boardName, setBoardName] = useState("");
  const [template, setTemplate] = useState(initialTemplate);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [background, setBackground] = useState(
    BOARD_BACKGROUND_OPTIONS[0].value
  );

  const selectedBackground = resolveBoardBackground(background);

  const setOpenState = (value) => {
    if (!isControlled) {
      setInternalOpen(value);
    }
    onOpenChange?.(value);
  };

  useEffect(() => {
    if (isOpen) {
      setTemplate(initialTemplate);
      setBoardName("");
      setError(null);
      setLoading(false);
      setBackground(BOARD_BACKGROUND_OPTIONS[0].value);
    }
  }, [initialTemplate, isOpen]);

  const resetForm = (nextTemplate = initialTemplate) => {
    setBoardName("");
    setTemplate(nextTemplate);
    setError(null);
    setLoading(false);
    setBackground(BOARD_BACKGROUND_OPTIONS[0].value);
  };

  const openModal = () => {
    resetForm(initialTemplate);
    setOpenState(true);
  };

  const forceClose = () => {
    resetForm(initialTemplate);
    setOpenState(false);
  };

  const handleCancel = () => {
    if (!loading) {
      forceClose();
    }
  };

  const selected = useMemo(
    () => TEMPLATES[template] || TEMPLATES["Sin plantilla"],
    [template]
  );
  const canCreate = boardName.trim().length > 0 && !loading;

  async function handleCreate() {
    if (!canCreate) return;
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem("token");
      if (!token) {
        throw new Error("Sesión no válida. Inicia sesión nuevamente.");
      }

      const payload = {
        name: boardName.trim(),
        description: "",
        background,
        // 👇 MUY IMPORTANTE: enviar id del espacio
        workspaceId: workspaceId ?? null,
      };

      const creatorId = Number(user?.id);
      if (!Number.isNaN(creatorId)) {
        payload.createdBy = creatorId;
      }

      const res = await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        let message = `Error del servidor: ${res.status}`;
        try {
          const errorBody = await res.json();
          message = errorBody.message || message;
        } catch { /* noop */ }
        throw new Error(message);
      }

      const created = await res.json();

      // crea listas de plantilla
      if (selected?.lists?.length) {
        const boardId = created.id;
        const boardNumericId = Number(boardId);
        const boardRefId = Number.isNaN(boardNumericId) ? boardId : boardNumericId;

        for (let index = 0; index < selected.lists.length; index += 1) {
          const listDefinition = selected.lists[index];
          const listPayload = {
            nombre: listDefinition.name,
            orden: index + 1,
            board: { id: boardRefId },
          };

          const listRes = await fetch(`${API_URL}/${boardId}/listas`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(listPayload),
          });

          if (!listRes.ok) {
            let message = `Error creando la lista "${listDefinition.name}"`;
            try {
              const errorBody = await listRes.json();
              message = errorBody.message || message;
            } catch { /* noop */ }
            throw new Error(message);
          }
        }
      }

      onCreated?.(created);
      forceClose();
      // redirige al tablero creado
      navigate(`/tableros/${created.id}`);
    } catch (err) {
      console.error("No se pudo crear el tablero:", err);
      setError(err.message || "No se pudo crear el tablero");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      {showTriggerButton ? (
        <Button
          variant="primary"
          onClick={openModal}
          className="self-start sm:self-auto"
        >
          + Nuevo tablero
        </Button>
      ) : null}

      {isOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          aria-modal="true"
          role="dialog"
          aria-labelledby="new-board-title"
          onKeyDown={(event) => event.key === "Escape" && handleCancel()}
        >
          <div
            className="absolute inset-0 bg-black/50 backdrop-blur-md transition-opacity"
            onClick={handleCancel}
          />

          <div
            className="
              relative z-10 w-full max-w-xl rounded-2xl
              bg-[var(--color-surface)] dark:bg-[var(--color-surface-hover)]
              text-[var(--color-neutral-950)] dark:text-[var(--color-neutral-950)]
              border border-[rgba(0,0,0,0.08)] dark:border-[rgba(255,255,255,0.12)]
              shadow-xl dark:shadow-[0_0_20px_rgba(177,151,249,0.25)]
              transition-colors duration-300
            "
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 border-b px-5 py-4">
              <div>
                <h2
                  id="new-board-title"
                  className="
                    text-xl font-bold tracking-tight
                    text-[var(--color-brand-700)]
                    dark:text-[var(--color-brand-300)]
                    transition-colors duration-300
                  "
                >
                  Creación de tableros
                </h2>
                <p
                  className="
                    text-sm font-medium mt-1
                    text-[var(--color-brand-500)]
                    dark:text-[var(--color-brand-400)]
                    transition-colors duration-300
                  "
                >
                  Configura un tablero nuevo
                </p>
              </div>

              <button
                className="
                  rounded-lg px-2 py-1
                  text-neutral-600 hover:bg-neutral-100
                  dark:text-neutral-300 dark:hover:bg-[var(--color-surface-hover)]
                  transition-colors duration-300
                "
                onClick={handleCancel}
                aria-label="Cerrar"
                disabled={loading}
              >
                ✕
              </button>
            </div>

            <div className="space-y-5 p-5">
              {error ? (
                <div className="rounded-lg border border-red-400/50 bg-red-100/60 dark:bg-red-900/40 px-3 py-2 text-sm text-red-800 dark:text-red-200">
                  {error}
                </div>
              ) : null}

              <label className="flex flex-col gap-2 text-sm">
                <span className="font-medium text-neutral-900 dark:text-neutral-200">
                  Nombre de tablero
                </span>
                <input
                  value={boardName}
                  onChange={(event) => setBoardName(event.target.value)}
                  placeholder="Ej. Campaña Q4 / Estudio DAW / Personal"
                  className="
                    rounded-xl border border-[rgba(0,0,0,0.1)] dark:border-[rgba(255,255,255,0.15)]
                    bg-[var(--color-surface)] dark:bg-[var(--color-surface-hover)]
                    px-3 py-2 outline-none
                    focus:border-[var(--color-brand-500)]
                    transition-colors
                  "
                  disabled={loading}
                />
              </label>

              <label className="flex flex-col gap-2 text-sm">
                <span className="font-medium text-neutral-900 dark:text-neutral-200">
                  Plantilla seleccionada
                </span>
                <select
                  value={template}
                  onChange={(event) => setTemplate(event.target.value)}
                  className="
                    rounded-xl border border-[rgba(0,0,0,0.1)] dark:border-[rgba(255,255,255,0.15)]
                    bg-[var(--color-surface)] dark:bg-[var(--color-surface-hover)]
                    px-3 py-2 outline-none focus:border-[var(--color-brand-500)]
                    transition-colors
                  "
                  disabled={loading}
                >
                  {Object.keys(TEMPLATES).map((key) => (
                    <option key={key} value={key}>
                      {key}
                    </option>
                  ))}
                </select>
                <span className="text-xs text-neutral-600 dark:text-neutral-400">
                  {selected?.description}
                </span>
              </label>

              <div className="space-y-2">
                <span className="text-sm font-medium text-neutral-900 dark:text-neutral-200">
                  Fondo del tablero
                </span>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  {BOARD_BACKGROUND_OPTIONS.map((option) => {
                    const isActive =
                      option.value === background || option.id === background;
                    const isColor = option.type === "color";
                    const previewStyle = isColor
                      ? { background: option.value }
                      : {
                          backgroundImage: `url(${option.value})`,
                          backgroundSize: "cover",
                          backgroundPosition: "center",
                        };
                    return (
                      <button
                        type="button"
                        key={option.id}
                        onClick={() => setBackground(option.value)}
                        className={[
                          "flex h-16 items-end rounded-xl border p-2 text-xs font-semibold text-white transition-all",
                          isActive
                            ? "border-[var(--color-brand-500)] shadow-lg"
                            : "border-transparent opacity-85 hover:opacity-100",
                        ].join(" ")}
                        style={previewStyle}
                        disabled={loading}
                        aria-pressed={isActive}
                      >
                        <span className="drop-shadow-md bg-black/30 px-2 py-0.5 rounded">
                          {option.label}
                        </span>
                      </button>
                    );
                  })}
                </div>
                <p className="text-xs text-neutral-600 dark:text-neutral-400">
                  Selección actual: {selectedBackground.label}
                </p>
              </div>

              <div className="space-y-3">
                <p className="text-sm font-medium text-neutral-900 dark:text-neutral-200">
                  Se crearán estas listas:
                </p>

                {selected?.lists?.length ? (
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    {selected.lists.map((list, index) => (
                      <div
                        key={`${list.name}-${index}`}
                        className="
                          rounded-xl p-3 shadow-sm
                          bg-[var(--color-surface)] dark:bg-[var(--color-surface-hover)]
                          border border-[rgba(0,0,0,0.05)] dark:border-[rgba(255,255,255,0.08)]
                          transition-colors
                        "
                      >
                        <p className="mb-2 text-sm font-semibold text-neutral-800 dark:text-neutral-100">
                          {list.name}
                        </p>
                        {list.tasks?.length ? (
                          <ul className="space-y-1 text-xs text-neutral-600 dark:text-neutral-300">
                            {list.tasks.map((task) => (
                              <li key={task} className="flex items-center gap-2">
                                <span className="inline-block h-1.5 w-1.5 rounded-full bg-neutral-400 dark:bg-neutral-300" />
                                {task}
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <p className="text-xs text-neutral-500 dark:text-neutral-400">
                            (Sin tareas iniciales)
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-neutral-500 dark:text-neutral-400">
                    (No se crearán listas automáticamente)
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center justify-between border-t border-[rgba(0,0,0,0.08)] dark:border-[rgba(255,255,255,0.12)] px-5 py-4">
              <Button variant="secondary" onClick={handleCancel} disabled={loading}>
                Cancelar
              </Button>
              <Button variant="primary" onClick={handleCreate} disabled={!canCreate}>
                {loading ? "Creando..." : "Crear tablero"}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
