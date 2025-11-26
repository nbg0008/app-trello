/*package com.medac.trello.api.service; // Asume tu paquete de servicio

import com.medac.trello.api.exception.ResourceNotFoundException;
import com.medac.trello.api.model.Lista;
import com.medac.trello.api.model.repository.ListaRepository; // O ListaRepository, asegúrate del nombre
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List; // <<-- ¡IMPORTANTE! Añade esta importación para java.util.List
import java.util.Optional;

@Service
public class ListasService {

    @Autowired
    private ListaRepository listaRepository; // Asumo ListRepository existe

    // C - Crear / Actualizar
    public Lista guardarLista(Lista lista) {
        return listaRepository.save(lista);
    }

    // R - Listar (Todos)
    // CORREGIDO: El tipo de retorno debe ser java.util.List<Lista>
    public List<Lista> obtenerTodasLasListas() {
        return listaRepository.findAll();
    }

    // R - Listar (Por ID)
    //public Optional<Lista> obtenerListaPorId(Long idLista) {
        // CORREGIDO: Usar el parámetro idLista
        //return listaRepository.findById(idLista);
    //}
    public Lista obtenerListaPorId(Long idLista) {
        // Reemplaza ResourceNotFoundException por tu clase de excepción real
        return listaRepository.findById(idLista)
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + idLista));
    }
    // U - Actualizar
    public Lista actualizarLista(Long idLista, Lista listaDetalles) {
        Optional<Lista> listaOptional = listaRepository.findById(idLista);

        if (listaOptional.isPresent()) {
            Lista listaExistente = listaOptional.get();

            // Usando los nombres de variables de tu última entidad (nombre, orden, idTablero)
            listaExistente.setNombre(listaDetalles.getNombre());
            listaExistente.setOrden(listaDetalles.getOrden());
            // CORREGIDO: Asumo que usas el ID, no el objeto Tablero, según tu última entidad.
            listaExistente.setIdTablero(listaDetalles.getIdTablero());

            return listaRepository.save(listaExistente);
        } else {
            // Esto se mapeará a un HTTP 404 en tu controlador
            throw new RuntimeException("Lista no encontrada con id: " + idLista);
        }
    }

    // D - Eliminar
    public void eliminarLista(Long idLista) {
        listaRepository.deleteById(idLista);
    }
}


 */
/*package com.medac.trello.api.service;

import com.medac.trello.api.exception.ResourceNotFoundException;
import com.medac.trello.api.model.Lista;
import com.medac.trello.api.model.repository.ListaRepository;
import com.medac.trello.api.model.Board; // Necesario para la corrección (la entidad Board)
import com.medac.trello.api.model.repository.BoardRepository; // Repositorio de Board
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ListasService {

    @Autowired
    private ListaRepository listaRepository;

    // Necesitamos el repositorio de Board para obtener la entidad completa
    // y asignarla a la relación Many-to-One de Lista.
    @Autowired
    private BoardRepository boardRepository;

    // C - Crear / Actualizar
    public Lista guardarLista(Lista lista) {
        return listaRepository.save(lista);
    }

    // R - Listar (Todos)
    public List<Lista> obtenerTodasLasListas() {
        return listaRepository.findAll();
    }

    // R - Listar (Por ID)
    public Lista obtenerListaPorId(Long idLista) {
        // Usa ResourceNotFoundException para manejar el caso de no encontrar el recurso
        return listaRepository.findById(idLista)
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + idLista));
    }

    // U - Actualizar
    public Lista actualizarLista(Long idLista, Lista listaDetalles) {
        // 1. Obtener la lista existente o lanzar excepción
        Lista listaExistente = listaRepository.findById(idLista)
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + idLista));

        // 2. Actualizar campos simples
        listaExistente.setNombre(listaDetalles.getNombre());
        listaExistente.setOrden(listaDetalles.getOrden());

        // 3. CORRECCIÓN CLAVE: Actualizar la relación Board
        // Asumimos que listaDetalles.getBoard() contiene el objeto Board
        // con al menos el ID del tablero destino.
        Board nuevoBoardDetalles = listaDetalles.getBoard();

        if (nuevoBoardDetalles != null && nuevoBoardDetalles.getId() > 0) {
            Long nuevoBoardId = nuevoBoardDetalles.getId();

            // Buscar la entidad Board completa en la base de datos
            Board nuevoBoard = boardRepository.findById(nuevoBoardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tablero destino no encontrado con id: " + nuevoBoardId));

            // Usar el setter de la relación JPA
            listaExistente.setBoard(nuevoBoard);
        }
        // Nota: Si el board es null o no tiene ID, mantenemos la lista en su board actual.


        // 4. Guardar y retornar la entidad actualizada
        return listaRepository.save(listaExistente);
    }

    // D - Eliminar
    public void eliminarLista(Long idLista) {
        // Verificar si existe antes de intentar eliminar (opcional, pero buena práctica)
        if (!listaRepository.existsById(idLista)) {
            throw new ResourceNotFoundException("Lista no encontrada con id: " + idLista);
        }
        listaRepository.deleteById(idLista);
    }
}


 */

package com.medac.trello.api.service;

import com.medac.trello.api.exception.ResourceNotFoundException;
import com.medac.trello.api.model.Lista;
import com.medac.trello.api.model.User;
import com.medac.trello.api.model.notification.ListaAddedNotificationDetails;
import com.medac.trello.api.model.notification.ListaDeletedNotificationDetails;
import com.medac.trello.api.model.notification.ListaUpdatedNotificationDetails;
import com.medac.trello.api.model.notification.NotificationDetails;
import com.medac.trello.api.model.repository.ListaRepository;
import com.medac.trello.api.model.repository.CardRepository;
import com.medac.trello.api.model.repository.HistorialMovimientoRepository;
import com.medac.trello.api.model.Board;
import com.medac.trello.api.model.repository.BoardRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set; // Importado para el nuevo método obtenerListasPorTablero

import static com.medac.trello.api.model.notification.ListaUpdatedNotificationDetails.ListaDetail.*;


@Service
public class ListasService {

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private HistorialMovimientoRepository historialMovimientoRepository;

    @Autowired
    private NotificationService notificationService;

    // ----------------------CREAR LISTA-----------------
    @Transactional
    public Lista guardarLista(User authenticatedUser, Long boardId, Lista lista) {
        // 1. Obtener el Board padre o lanzar excepción si no existe
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Tablero no encontrado con id: " + boardId));

        // 2. Asignar la entidad Board completa a la lista
        lista.setBoard(board);

        // 3. Guardar la lista
        final var listaNueva =  listaRepository.save(lista);
        boardMembersWithOwner(board).forEach(member ->
                notificationService.addNotification(authenticatedUser.getId(), member.getId(),
                        new ListaAddedNotificationDetails(
                                listaNueva.getNombre(),
                                listaNueva.getBoard().getName())
                ));
        return listaNueva;
    }
 //------------------------------LEER LITAS-----------------------------

    // R - Listar (Todos)
    public List<Lista> obtenerTodasLasListas() {
        return listaRepository.findAll();
    }

    // LISTAR POR TABLERO
    public Set<Lista> obtenerListasPorTablero(Long boardId) {
        // Primero, aseguramos que el tablero padre exista.
        /*if (!boardRepository.existsById(boardId)) {
            throw new ResourceNotFoundException("Tablero no encontrado con id: " + boardId);

         */
        //}

        // 'Set<Lista> findAllByBoard_Id(Long boardId);'
        return listaRepository.findAllByBoardId(boardId);
    }


    // R - Listar (Por ID)
    public Lista obtenerListaPorId(Long idLista) {
        // Usa ResourceNotFoundException para manejar el caso de no encontrar el recurso
        return listaRepository.findById(idLista)
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + idLista));
    }

    // -----------------------U - ACTUALIZAR LISTAS---------------------------
    @Transactional
    public Lista actualizarLista(User usuario, Long idLista, Lista listaDetalles) {
        Lista listaExistente = listaRepository.findById(idLista)
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + idLista));

        List<NotificationDetails> notificaciones = new ArrayList<>();

        // 1. Actualizar campos simples (nombre y orden)
        if (listaDetalles.getNombre() != null) {
            listaExistente.setNombre(listaDetalles.getNombre());
            notificaciones.add(new ListaUpdatedNotificationDetails<>(
                    listaExistente.getNombre(),
                    listaExistente.getNombre(),
                    listaDetalles.getNombre(), NAME));
        }

        if (listaDetalles.getOrden() != listaExistente.getOrden()) {
            listaExistente.setOrden(listaDetalles.getOrden());
            notificaciones.add(new ListaUpdatedNotificationDetails<>(
                    listaExistente.getNombre(),
                    listaExistente.getOrden().toString(),
                    listaDetalles.getOrden().toString(), ORDER));
        }

        // 2. Lógica para mover la lista a otro tablero
        Board nuevoBoardDetalles = listaDetalles.getBoard();

        if (nuevoBoardDetalles != null && nuevoBoardDetalles.getId() != null) {
            Long nuevoBoardId = nuevoBoardDetalles.getId();

            Long currentBoardId = listaExistente.getBoard() != null ? listaExistente.getBoard().getId() : null;

            // Solo actualizar si el ID del tablero es diferente al actual
            if (!Objects.equals(listaExistente.getBoard().getId(), nuevoBoardId)) {

                Board nuevoBoard = boardRepository.findById(nuevoBoardId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tablero destino no encontrado con id: " + nuevoBoardId));

                listaExistente.setBoard(nuevoBoard);
                notificaciones.add(new ListaUpdatedNotificationDetails<>(
                        listaExistente.getNombre(),
                        listaExistente.getBoard().getName(),
                        nuevoBoard.getName(), BOARD));
            }
        }

        final var listaActualizada = listaRepository.save(listaExistente);
        boardMembersWithOwner(listaExistente.getBoard()).forEach(member ->
                notificationService.addNotifications(usuario.getId(), member.getId(), notificaciones));

        return listaActualizada;
    }
    // -----------------------D - EÑLLIMINAR LISTA-------------------------
    @Transactional
        public void eliminarLista(User user, Long idLista) {
        final var listaParaBorrar = listaRepository.findById(idLista)
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + idLista));

        // Eliminar historiales y tarjetas asociadas para evitar violaciones de FK
        historialMovimientoRepository.deleteByListId(idLista);
        historialMovimientoRepository.deleteAllByTarjeta_Lista_IdLista(idLista);
        final var cards = cardRepository.findByLista_IdListaOrderByCardOrderAsc(idLista);
        if (!cards.isEmpty()) {
            final var cardIds = cards.stream().map(com.medac.trello.api.model.Card::getId).toList();
            historialMovimientoRepository.deleteByCardIds(cardIds);
            cardRepository.deleteAllById(cardIds);
        }
        cardRepository.deleteByListId(idLista);

        Long actorId = (user != null) ? user.getId() : null;
        if (actorId == null && listaParaBorrar.getBoard() != null && listaParaBorrar.getBoard().getCreatedBy() != null) {
            actorId = listaParaBorrar.getBoard().getCreatedBy().getId();
        }

        if (actorId != null) {
            final Long finalActorId = actorId;
            boardMembersWithOwner(listaParaBorrar.getBoard()).forEach(member ->
                    notificationService.addNotification(finalActorId, member.getId(),
                            new ListaDeletedNotificationDetails(
                                    listaParaBorrar.getNombre(),
                                    listaParaBorrar.getBoard().getName())
                    ));
        }

        listaRepository.deleteById(idLista);
    }

    /**
     * Devuelve todos los miembros del tablero, incluyendo al propietario como destinatario.
     */
    private Set<User> boardMembersWithOwner(Board board) {
        Set<User> recipients = new HashSet<>(board.getMembers());
        if (board.getCreatedBy() != null) {
            recipients.add(board.getCreatedBy());
        }
        return recipients;
    }
}



