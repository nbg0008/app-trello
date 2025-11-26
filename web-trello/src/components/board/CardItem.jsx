import { useState, useRef, useCallback } from "react";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { CheckCircle2, Circle, MoreHorizontal, Clock3 } from "lucide-react";
import CardMenu from "./CardMenu.jsx";

const CARD_PREFIX = "card-";
const dateFormatter = new Intl.DateTimeFormat("es-ES", {
  day: "numeric",
  month: "short",
});

const formatDateRange = (startsOn, expiresOn) => {
  const normalize = (value) => {
    if (!value) return null;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    return dateFormatter.format(date).toLowerCase().replace(/\./g, "");
  };

  const start = normalize(startsOn);
  const end = normalize(expiresOn);
  if (start && end) return `${start} - ${end}`;
  return start || end || "";
};

const getDeadlineStatus = (expiresOn) => {
  if (!expiresOn) return "none";
  const now = new Date();
  const due = new Date(expiresOn);
  const diff = due - now;
  if (diff < 0) return "expired";
  if (diff < 24 * 60 * 60 * 1000) return "soon";
  return "ok";
};

export default function CardItem({
  card,
  listId,
  isComplete,
  onToggleComplete,
  onMenuAction,
  canEditContent = true,
}) {
  const cardId = String(card.id);
  const cardSortableId = `${CARD_PREFIX}${cardId}`;
  const listKey = String(listId);
  const labelColor = card?.label?.color ?? null;

  const [menuOpen, setMenuOpen] = useState(false);
  const [anchorRect, setAnchorRect] = useState(null);
  const itemRef = useRef(null);
  const dateLabel = formatDateRange(card.startsOn, card.expiresOn);
  const hasDateBadge = dateLabel.length > 0;
  const deadlineStatus = getDeadlineStatus(card.expiresOn);

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: cardSortableId,
    data: { type: "card", listId: listKey, cardId },
    disabled: !canEditContent,
  });

  const setRefs = useCallback(
    (node) => {
      setNodeRef(node);
      itemRef.current = node;
    },
    [setNodeRef]
  );

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 60 : undefined,
    opacity: isDragging ? 0 : 1,
  };

  const handleMenuToggle = (event) => {
    if (!canEditContent) return;
    event.stopPropagation();
    event.preventDefault();
    if (!menuOpen && itemRef.current) {
      const rect = itemRef.current.getBoundingClientRect();
      setAnchorRect({
        top: rect.top,
        right: rect.right,
        bottom: rect.bottom,
        left: rect.left,
        width: rect.width,
        height: rect.height,
      });
    }
    setMenuOpen((value) => !value);
  };
   const now = new Date();
  const expiresOn = card.expiresOn ? new Date(card.expiresOn) : null;
  let dateStatus = "";

  if (expiresOn && !isComplete) {
    const diff = expiresOn - now;
    const oneDay = 24 * 60 * 60 * 1000;

    if (diff < 0) dateStatus = "expired"; 
    else if (diff < oneDay) dateStatus = "near"; 
  }

  const deadlineClass =
    dateStatus === "expired"
      ? "border-red-500 animate-pulse bg-red-50 dark:bg-red-900/30"
      : dateStatus === "near"
      ? "border-yellow-400 animate-pulse bg-yellow-50 dark:bg-yellow-900/30"
      : "";

  return (
    <div
  ref={setRefs}
  data-draggable="card"
  style={style}
  className={`group relative overflow-hidden rounded-2xl border border-transparent 
    text-gray-800 dark:text-gray-100 px-3 py-3 shadow-md transition
    hover:border-[#4b3acd]/40 hover:shadow-lg
    ${isDragging ? "border-[#7f6dff]/60 shadow-[#6b4dff]/50" : ""}
    ${deadlineClass}`}
  {...attributes}
  {...listeners}
>

      {labelColor ? (
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-1"
          style={{ backgroundColor: labelColor }}
        />
      ) : null}

      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={(event) => {
            if (!canEditContent) return;
            event.stopPropagation();
            onToggleComplete?.();
          }}
          aria-label={
            isComplete ? "Marcar como pendiente" : "Marcar como completada"
          }
          disabled={!canEditContent}
          className={`rounded-full text-[#7f6dff] dark:text-[#cdbfff] transition ${
            canEditContent ? "hover:text-[#a493ff]" : "cursor-not-allowed opacity-60"
          }`}
        >
          {isComplete ? (
            <CheckCircle2 className="h-4 w-4 text-emerald-500" />
          ) : (
            <Circle className="h-4 w-4" />
          )}
        </button>

        <div className="flex-1 pr-6 text-sm font-medium">
          {card.title}
          {card.description && (
            <p className="mt-1 text-xs font-normal text-gray-600 dark:text-neutral-300 line-clamp-2">
              {card.description}
            </p>
          )}
        </div>

        {canEditContent ? (
          <button
            type="button"
            onPointerDown={(event) => event.stopPropagation()}
            onClick={handleMenuToggle}
            className="rounded-full border border-transparent bg-gray-100 dark:bg-[#2d2d38]
                     p-1.5 text-gray-600 dark:text-neutral-300 opacity-0
                     transition group-hover:translate-x-1 group-hover:opacity-100
                     hover:border-[#4b3acd]/40 hover:bg-gray-200 dark:hover:bg-[#383846]"
            aria-label="Abrir menu de tarjeta"
          >
            <MoreHorizontal className="h-3.5 w-3.5" />
          </button>
        ) : null}
      </div>

      {hasDateBadge && (
        <div className="mt-3 flex items-center gap-2">
  <div
    className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold shadow
      ${
        deadlineStatus === "expired"
          ? "bg-red-500/90 text-white animate-pulse"
          : deadlineStatus === "soon"
          ? "bg-amber-400/90 text-neutral-900"
          : "bg-amber-300/90 text-neutral-900 dark:text-neutral-800"
      }`}
  >

            <Clock3 className="h-3.5 w-3.5" />
            <span>{dateLabel}</span>
          </div>
        </div>
      )}

      {menuOpen && anchorRect && canEditContent ? (
        <CardMenu
          anchorRect={anchorRect}
          onClose={() => setMenuOpen(false)}
          onAction={(action) => {
            onMenuAction?.(action, card);
            setMenuOpen(false);
          }}
        />
      ) : null}
    </div>
  );
}
