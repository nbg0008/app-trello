import { useState, useRef, useEffect } from "react";
import { useDroppable } from "@dnd-kit/core";
import {
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import Button from "../ui/Button.jsx";
import { Plus, Trash2 } from "lucide-react";
import CardItem from "./CardItem.jsx";

const LIST_SORTABLE_PREFIX = "list-";
const LIST_DROPPABLE_PREFIX = "list-droppable-";
const CARD_PREFIX = "card-";

export default function ListColumn({
  list,
  cards,
  onAddCard,
  isSavingCard,
  completedCards,
  onToggleCardComplete,
  onCardMenuAction,
  onDeleteList,
  canEditContent = true,
  enableDrag = true,
}) {
  const listId = String(list.idLista);
  const sortableId = `${LIST_SORTABLE_PREFIX}${listId}`;
  const droppableId = `${LIST_DROPPABLE_PREFIX}${listId}`;

  const completedSet =
    completedCards instanceof Set
      ? completedCards
      : new Set(completedCards ?? []);

  const [isComposing, setIsComposing] = useState(false);
  const [title, setTitle] = useState("");
  const textareaRef = useRef(null);

  useEffect(() => {
    if (isComposing && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [isComposing]);
  
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: sortableId,
    data: { type: "list", listId },
    disabled: !enableDrag,
  });

  const { isOver, setNodeRef: setDroppableRef } = useDroppable({
    id: droppableId,
    data: { type: "list-droppable", listId },
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.85 : undefined,
  };

  const tarjetas = Array.isArray(cards) ? cards : [];

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!canEditContent) return;
    const value = title.trim();
    if (!value) return;
    onAddCard(list.idLista, value);
    setTitle("");
    setIsComposing(false);
  };

  const handleCancel = () => {
    setTitle("");
    setIsComposing(false);
  };

  return (
    <div
      ref={setNodeRef}
      data-draggable="list"
      style={style}
      className={[
        "flex w-72 flex-shrink-0 flex-col rounded-2xl transition-all duration-300 overflow-visible",
        "bg-[var(--color-surface-hover)] text-[var(--color-neutral-950)]",
        "dark:bg-[var(--color-surface-hover)] dark:text-[var(--color-neutral-950)]",
        "shadow-[0_6px_18px_-6px_rgba(0,0,0,0.25)] dark:shadow-[0_6px_18px_-6px_rgba(255,255,255,0.05)]",
      ].join(" ")}
    >
      <header
        className={`mb-3 flex items-start justify-between px-4 pt-3 ${enableDrag ? "cursor-grab active:cursor-grabbing" : "cursor-default"}`}
        {...attributes}
        {...listeners}
      >
        <h4
          className="text-sm font-semibold tracking-wide transition-colors"
          style={{
            color: "var(--color-brand-700)",
          }}
        >
          {list.nombre}
        </h4>

        {canEditContent ? (
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              onDeleteList?.(list.idLista);
            }}
            className="rounded-full p-1 text-neutral-400 transition hover:text-red-500 hover:bg-red-50"
            aria-label={`Eliminar lista ${list.nombre ?? ""}`.trim()}
          >
            <Trash2 className="h-4 w-4" />
          </button>
        ) : null}
      </header>

      <SortableContext
        id={droppableId}
        items={tarjetas.map((card) => `${CARD_PREFIX}${card.id}`)}
        strategy={verticalListSortingStrategy}
      >
        <div ref={setDroppableRef} className="relative flex flex-col flex-1">
          <div
            className={`flex flex-col justify-center space-y-3 px-4 pb-1 border rounded-xl shadow-sm transition-all duration-300 ${
              isOver
                ? "bg-transparent border-transparent"
                : "bg-[var(--color-surface)] border-[rgba(0,0,0,0.08)]"
            }`}
            style={{
              color: "var(--color-neutral-950)",
              minHeight: tarjetas.length === 0 ? "6rem" : "100%",
            }}
          >
            {tarjetas.length === 0 ? (
              <div
                className="flex h-16 items-center justify-center rounded-xl border border-dashed text-sm font-medium transition-all duration-300"
                style={{
                  backgroundColor: isOver
                    ? "transparent"
                    : "var(--color-surface-hover)",
                  color: "var(--color-neutral-950)",
                  borderColor: "rgba(0, 0, 0, 0.2)",
                }}
              >
                No hay tarjetas todavía
              </div>
            ) : (
              tarjetas.map((card) => (
                <CardItem
                  key={card.id}
                  card={card}
                  listId={list.idLista}
                  isComplete={completedSet.has(card.id)}
                  onToggleComplete={() => onToggleCardComplete?.(card.id)}
                  onMenuAction={(action) => onCardMenuAction?.(action, card)}
                  canEditContent={canEditContent}
                />
              ))
            )}
          </div>
        </div>
      </SortableContext>

      <div className="px-4 pb-4 pt-1">
        {canEditContent ? (
          isComposing ? (
            <form onSubmit={handleSubmit} className="space-y-2">
              <textarea
                ref={textareaRef}
                rows={3}
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="Introduce un titulo o pega un enlace"
                className={[
                  "w-full resize-none rounded-xl border px-3 py-2 text-sm placeholder:text-neutral-400 focus:outline-none focus:ring-2 transition-colors duration-300",
                  "bg-[var(--color-surface-hover)] text-[var(--color-neutral-950)] border-[rgba(0,0,0,0.1)]",
                  "focus:border-[var(--color-brand-500)] focus:ring-[var(--color-brand-500)]/40",
                  "dark:bg-[var(--color-brand-50)] dark:text-[var(--color-neutral-950)] dark:border-[rgba(255,255,255,0.15)]",
                  "dark:focus:border-[var(--color-brand-500)] dark:focus:ring-[var(--color-brand-500)]/40",
                ].join(" ")}
                disabled={isSavingCard}
              />
              <div className="flex items-center gap-2">
                <Button type="submit" disabled={!title.trim() || isSavingCard}>
                  Anadir tarjeta
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handleCancel}
                  disabled={isSavingCard}
                >
                  Cancelar
                </Button>
              </div>
            </form>
          ) : (
            <button
              type="button"
              onClick={() => setIsComposing(true)}
              className="inline-flex items-center gap-2 text-sm font-medium text-[var(--color-brand-600)] transition hover:text-[var(--color-brand-500)] dark:text-[var(--color-brand-400)] dark:hover:text-[var(--color-brand-200)]"
            >
              <Plus className="h-4 w-4" />
              <span>Añade una tarjeta</span>
            </button>
          )
        ) : (
          <p className="text-xs font-medium text-neutral-500 dark:text-neutral-400">
            Solo lectura
          </p>
        )}
      </div>
    </div>
  );
}
