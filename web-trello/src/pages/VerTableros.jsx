import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Button from "../components/ui/Button.jsx";
import Card from "../components/ui/Card.jsx";
import { Trash2, Info, Loader2 } from 'lucide-react'; // Iconos


const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/trello/v1";

export default function VerTableros() {
    const navigate = useNavigate();
    const location = useLocation();

    const urlQuery = new URLSearchParams(location.search).get("q") || "";

    const [boards, setBoards] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [query, setQuery] = useState(urlQuery);
    const [deletingId, setDeletingId] = useState(null); 

    useEffect(() => {
        if (boards.length === 0) { 
    }
}, []);

    useEffect(() => {
        setQuery(urlQuery);
    }, [urlQuery]);

    // Función para recargar la lista de tableros
    const fetchBoards = async (isCancelled) => {
        try {
            setLoading(true);
            setError("");

            const API_URL = `${API_BASE_URL}/tableros`;

            const res = await fetch(API_URL, {
                credentials: "include",
                headers: { Accept: "application/json" },
            });
            if (!res.ok) throw new Error("No se pudieron cargar los tableros");
            const data = await res.json();
            if (!isCancelled) setBoards(Array.isArray(data) ? data : []);
        } catch (e) {
            if (!isCancelled) setError(e.message || "Error al cargar los tableros");
        } finally {
            if (!isCancelled) setLoading(false);
        }
    };

    // useEffect inicial
    useEffect(() => {
        let cancelled = false;
        fetchBoards(cancelled);
        return () => { cancelled = true; };
    }, []);

    // NUEVA FUNCIÓN: Manejar la eliminación del tablero
    const handleDeleteBoard = async (id) => {
        if (!confirm(`¿Estás seguro de que quieres eliminar este tablero (ID: ${id})?`)) {
            return;
        }

        try {
            setDeletingId(id);
            const API_URL = `${API_BASE_URL}/trello/v1/tableros/${id}`;

            const res = await fetch(API_URL, {
                method: 'DELETE',
                credentials: "include",
                headers: { 'Content-Type': 'application/json' },
            });

            if (!res.ok) {
                // Asumo que si el DELETE es exitoso, devuelve 204 No Content.
                // Si devuelve un 200/202, lo ignoramos. Si es un error, lanzamos excepción.
                throw new Error(`Fallo al eliminar el tablero: ${res.status}`);
            }

            // Actualizar la UI: remover el tablero de la lista
            setBoards(boards.filter(b => b.id !== id));
            console.log(`Tablero con ID ${id} eliminado correctamente.`);

        } catch (e) {
            alert(`Error al eliminar: ${e.message}`);
            setError(`Error al intentar eliminar el tablero ${id}: ${e.message}`);
        } finally {
            setDeletingId(null);
        }
    };

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return boards;
        return boards.filter((b) => (b.name || "").toLowerCase().includes(q));
    }, [boards, query]);

    function applyQueryToUrl(next) {
        const params = new URLSearchParams(location.search);
        if (next.trim()) params.set("q", next.trim());
        else params.delete("q");
        navigate({ pathname: "/tableros", search: params.toString() ? `?${params}` : "" }, { replace: true });
    }
    

    return (
        <section className="min-h-screen bg-gradient-to-b from-neutral-50 to-white px-4 py-8">
            <div className="mx-auto w-full max-w-5xl">
                <div className="mb-4 flex items-center justify-between">
                    <h1 className="text-3xl font-bold text-neutral-800">Tus Tableros</h1>
                    <div className="flex gap-2">
                        <Button onClick={() => navigate("/tableros/nuevo")}>Crear tablero</Button>
                        <Button variant="secondary" onClick={() => navigate("/dashboard")}>Volver</Button>
                    </div>
                </div>

                <Card className="p-5">
                    <label className="mb-1 block text-sm font-medium text-neutral-800">
                        Buscar tableros
                    </label>
                    <div className="flex gap-2">
                        <input
                            value={query}
                            onChange={(e) => {
                                setQuery(e.target.value);
                                applyQueryToUrl(e.target.value);
                            }}
                            placeholder="Escribe un nombre…"
                            className="w-full rounded-xl border border-neutral-200 bg-white px-3 py-2 text-sm outline-none focus:border-violet-600 focus:ring-4 focus:ring-violet-100"
                        />
                        {query && (
                            <Button variant="secondary" onClick={() => { setQuery(""); applyQueryToUrl(""); }}>
                                Limpiar
                            </Button>
                        )}
                    </div>
                </Card>

                <div className="mt-6">
                    {loading && (
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                            <SkeletonCard /><SkeletonCard /><SkeletonCard />
                        </div>
                    )}

                    {!loading && error && (
                        <div className="rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700">
                            {error}
                        </div>
                    )}

                    {!loading && !error && filtered.length === 0 && (
                        <Card className="p-8 text-center">
                            <h3 className="text-base font-semibold text-neutral-900">No hay tableros</h3>
                            <p className="mt-1 text-sm text-neutral-600">
                                Crea tu primer tablero para empezar a organizar tus tareas.
                            </p>
                            <Button className="mt-4" onClick={() => navigate("/tableros/nuevo")}>
                                Crear tablero
                            </Button>
                        </Card>
                    )}

                    {!loading && !error && filtered.length > 0 && (
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                            {filtered.map((b) => (
                                <BoardCard
                                    key={b.id}
                                    id={b.id}
                                    name={b.name}
                                    lists={b.listsCount || 0}
                                    cards={b.cardsCount || 0}
                                    onOpen={() => navigate(`/tableros/${b.id}`)}
                                    onDelete={() => handleDeleteBoard(b.id)}
                                    isDeleting={deletingId === b.id}
                                />
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </section>
    );
}

// Componente de tarjeta de tablero
function BoardCard({ id, name, lists, cards, onOpen, onDelete, isDeleting }) {
    return (
        <Card className="p-5 transition hover:shadow-lg">
            <div className="flex justify-between items-start">
                {/* Contenido del Tablero (Título y Estadísticas) */}
                <div className="flex-1 min-w-0">
                    <div className="line-clamp-2 text-base font-semibold text-neutral-900 break-words">{name}</div>
                    <div className="mt-1 text-xs text-neutral-500">
                        {lists} listas · {cards} tarjetas
                    </div>
                </div>

                {/* Botón de Eliminar */}
                <Button
                    variant="danger"
                    size="icon"
                    onClick={(e) => {
                        e.stopPropagation(); // Evita que se dispare el onOpen de la tarjeta
                        onDelete();
                    }}
                    disabled={isDeleting}
                    className="ml-4 flex-shrink-0"
                >
                    {isDeleting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                </Button>
            </div>

            {/* Acciones de la Tarjeta */}
            <div className="mt-4 flex justify-between items-center border-t pt-3">
                <span className="text-xs text-neutral-400">ID: {id}</span>
                <Button
                    variant="link"
                    onClick={onOpen}
                    disabled={isDeleting}
                    className="text-violet-700 hover:text-violet-900 transition p-0 h-auto"
                >
                    <Info className="h-4 w-4 mr-1" /> Abrir Tablero
                </Button>
            </div>
        </Card>
    );
}

// Componente de esqueleto de carga
function SkeletonCard() {
    return (
        <Card className="h-32 animate-pulse p-5">
            <div className="h-4 w-2/3 rounded bg-neutral-200" />
            <div className="mt-3 h-3 w-24 rounded bg-neutral-200" />
            <div className="mt-2 h-3 w-16 rounded bg-neutral-200" />
        </Card>
    );
}
