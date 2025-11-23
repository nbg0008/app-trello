const API_BASE_URL =
  "http://${process.env.VITE_API_BASE_URL}" || "http://localhost:8080/trello/v1"; 

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem("token");
  const headers = {
    Accept: "application/json",
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    ...options,
    headers,
  });

  const contentType = res.headers.get("content-type") || "";
  const raw = await res.text(); // leer una sola vez

  const tryParseJson = () => {
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  };

  if (!res.ok) {
    if (contentType.includes("application/json")) {
      const body = tryParseJson();
      // si el JSON viene roto, devolvemos texto
      const message =
        (body && body.message) ||
        (raw ? raw.slice(0, 500) : `Error ${res.status}`);
      throw new Error(message);
    } else {
      throw new Error(raw ? raw.slice(0, 500) : `Error ${res.status}`);
    }
  }

  if (!raw) return null;
  if (contentType.includes("application/json")) {
    const ok = tryParseJson();
    if (ok !== null) return ok;
    throw new Error(`Respuesta JSON inválida: ${raw.slice(0, 500)}`);
  }

  // Si la API devolvió éxito pero no-JSON:
  return raw;
}

// helpers
export function createCard(listId, payload) {
  return apiFetch(`/listas/${listId}/tarjetas`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
export function updateCard(cardId, payload) {
  return apiFetch(`/tarjetas/${cardId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
export function deleteCard(cardId) {
  return apiFetch(`/tarjetas/${cardId}`, {
    method: "DELETE",
  });
}
export function fetchBoardLabels(boardId) {
  return apiFetch(`/tableros/${boardId}/etiquetas`);
}
export function createBoardLabel(boardId, payload) {
  return apiFetch(`/tableros/${boardId}/etiquetas`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
export function updateBoardLabel(boardId, labelId, payload) {
  return apiFetch(`/tableros/${boardId}/etiquetas/${labelId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
export function deleteBoardLabel(boardId, labelId) {
  return apiFetch(`/tableros/${boardId}/etiquetas/${labelId}`, {
    method: "DELETE",
  });
}
export function fetchBoardMembers(boardId) {
  return apiFetch(`/tableros/${boardId}/miembros`);
}
export function updateBoardMemberRole(boardId, memberId, payload) {
  return apiFetch(`/tableros/${boardId}/miembros/${memberId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
export function removeBoardMember(boardId, memberId) {
  return apiFetch(`/tableros/${boardId}/miembros/${memberId}`, {
    method: "DELETE",
  });
}
