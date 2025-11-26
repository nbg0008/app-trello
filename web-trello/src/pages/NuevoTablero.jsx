import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/ui/Button.jsx';
// Asume que API_BASE_URL es 'http://localhost:3000' (o el puerto correcto de tu backend)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/trello/v1";

export default function CrearTablero() {
    const navigate = useNavigate();
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const API_URL = `https://${API_BASE_URL}/tableros`; 

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        // Simple validación
        if (!name) {
            setError('El nombre del tablero es obligatorio.');
            setLoading(false);
            return;
        }

        const nuevoTablero = {
            // Asegúrate de que los nombres de las propiedades coincidan con tu clase Board.java
            name: name,
            description: description,
            createdBy: 1,
            //createdOn: now.toISOString(), lo maneja el backend
        };

        try {
            const res = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // Si usas autenticación (ej. JWT), deberías incluir el 'Authorization' aquí.
                },
                body: JSON.stringify(nuevoTablero),
            });

            if (!res.ok) {
                // Maneja errores como 400 (Bad Request) o 500 (Internal Server Error)
                const errorData = await res.json();
                throw new Error(errorData.message || `Error del servidor: ${res.status}`);
            }

            const tableroCreado = await res.json();
            
            // Éxito: Navegar al dashboard o a la vista del nuevo tablero
            alert(`Tablero "${tableroCreado.name}" creado con éxito!`);
            navigate(`/tableros/${tableroCreado.id}`); 

        } catch (e) {
            console.error('Fallo al crear el tablero:', e);
            setError(e.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <section className="min-h-screen bg-neutral-50 p-8">
            <div className="mx-auto w-full max-w-lg bg-white p-8 rounded-xl shadow-2xl">
                <h1 className="text-3xl font-bold text-neutral-800 mb-6 border-b pb-2">
                    Crear Nuevo Tablero
                </h1>
                
                {error && (
                    <div className="p-3 mb-4 bg-red-100 border border-red-400 text-red-700 rounded">
                        <p>{error}</p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label htmlFor="name" className="block text-sm font-medium text-neutral-700">
                            Nombre del Tablero <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            id="name"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                            className="mt-1 block w-full border border-neutral-300 rounded-md shadow-sm p-2"
                            disabled={loading}
                        />
                    </div>
                    
                    <div>
                        <label htmlFor="description" className="block text-sm font-medium text-neutral-700">
                            Descripción (Opcional)
                        </label>
                        <textarea
                            id="description"
                            rows="3"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            className="mt-1 block w-full border border-neutral-300 rounded-md shadow-sm p-2"
                            disabled={loading}
                        />
                    </div>

                    <div className="flex justify-end space-x-3 pt-4">
                        <Button 
                            variant="secondary" 
                            type="button" 
                            onClick={() => navigate(-1)}
                            disabled={loading}
                        >
                            Cancelar
                        </Button>
                        <Button 
                            type="submit" 
                            disabled={loading}
                            className="bg-blue-600 hover:bg-blue-700 text-white"
                        >
                            {loading ? 'Creando...' : 'Crear Tablero'}
                        </Button>
                    </div>
                </form>
            </div>
        </section>
    );
}