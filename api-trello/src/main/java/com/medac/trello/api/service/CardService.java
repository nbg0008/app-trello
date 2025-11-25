package com.medac.trello.api.service;

import com.medac.trello.api.exception.ResourceNotFoundException;
import com.medac.trello.api.model.*;
import com.medac.trello.api.model.notification.*;
import com.medac.trello.api.model.repository.CardRepository;
import com.medac.trello.api.model.repository.HistorialMovimientoRepository;
import com.medac.trello.api.model.repository.LabelRepository;
import com.medac.trello.api.model.repository.CommentRepository;
import com.medac.trello.api.model.repository.ListaRepository;
import jakarta.transaction.Transactional;
import org.antlr.v4.runtime.atn.SemanticContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.medac.trello.api.model.notification.CardUpdatedNotificationDetails.CardDetail.*;
import static com.medac.trello.api.model.notification.ListaUpdatedNotificationDetails.ListaDetail.BOARD;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private HistorialMovimientoRepository historialMovimientoRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    // ---------------------- C - CREAR TARJETA ----------------------

    @Transactional
    public Card guardarCard(User authenticatedUser, Long listId, Card card, Long labelId) {
        // 1. Obtener la lista (columna)
        Lista lista = listaRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + listId));

        // 2. Asignar propiedades de creacion
        card.setLista(lista);
        if (card.getCreatedOn() == null) {
            card.setCreatedOn(Instant.now());
        }
        validateCardDates(card.getStartsOn(), card.getExpiresOn());

        applyLabel(card, labelId, lista);

        // 3. Guardar
        final var updatedCard = cardRepository.save(card);
        boardMembersWithOwner(lista.getBoard()).forEach(member ->
                notificationService.addNotification(authenticatedUser.getId(), member.getId(),
                        new CardAddedNotificationDetails(
                                updatedCard.getTitle(),
                                lista.getNombre(),
                                lista.getBoard().getName())
                ));
        return updatedCard;
    }

    // ---------------------- R - LEER TARJETAS ----------------------

    /* // Listar todas las tarjetas (principalmente para debug)
    public List<Card> findAllCards() {
        return cardRepository.findAll();
    }
     */

    public List<Card> obtenerCardsPorLista(Long listaId) {
        return cardRepository.findByLista_IdListaOrderByCardOrderAsc(listaId);
    }

    // Obtener por ID
    public Card obtenerCardPorId(Long idTarjeta) {
        return cardRepository.findById(idTarjeta)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + idTarjeta));
    }

    public List<Card> encontrarTarjetasPorTableroId(Long tableroId) {
        // Llama al metodo de consulta derivada que debe existir en CardRepository.
        return cardRepository.findByLista_Board_Id(tableroId);
    }

    // ---------------------- U - ACTUALIZAR/MOVER TARJETAS ----------------------

    @Transactional
    public Card actualizarCard(
            User usuario,
            Long idTarjeta,
            Card cardDetails,
            Long labelId,
            boolean startsOnPresent,
            boolean expiresOnPresent
    ) {

        // 1. Obtener la tarjeta existente
        Card cardExistente = cardRepository.findById(idTarjeta)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + idTarjeta));
        Lista listaOriginal = cardExistente.getLista(); // Lista de origen
        Long listaOrigenId = listaOriginal != null ? listaOriginal.getIdLista() : null;

        List<NotificationDetails> notificaciones = new ArrayList<>();

        // 2. Actualizar campos simples
        if (cardDetails.getTitle() != null) {
            notificaciones.add(new CardUpdatedNotificationDetails<>(
                    cardExistente.getTitle(),
                    cardExistente.getTitle(),
                    cardDetails.getTitle(), NAME));
            cardExistente.setTitle(cardDetails.getTitle());
        }
        if (cardDetails.getDescription() != null) {
            notificaciones.add(new CardUpdatedNotificationDetails<>(
                    cardExistente.getTitle(),
                    cardExistente.getDescription(),
                    cardDetails.getDescription(), DESCRIPCION));
            cardExistente.setDescription(cardDetails.getDescription());
        }
        Instant candidateStartsOn = startsOnPresent ? cardDetails.getStartsOn() : cardExistente.getStartsOn();
        Instant candidateExpiresOn = expiresOnPresent ? cardDetails.getExpiresOn() : cardExistente.getExpiresOn();
        validateCardDates(candidateStartsOn, candidateExpiresOn);
        if (startsOnPresent) {
            notificaciones.add(new CardUpdatedNotificationDetails<>(
                    cardExistente.getTitle(),
                    cardExistente.getStartsOn(),
                    cardDetails.getStartsOn(), STARTS_ON));
            cardExistente.setStartsOn(candidateStartsOn);
        }
        if (expiresOnPresent) {
            notificaciones.add(new CardUpdatedNotificationDetails<>(
                    cardExistente.getTitle(),
                    cardExistente.getExpiresOn(),
                    cardDetails.getExpiresOn(), EXPIRES_ON));
            cardExistente.setExpiresOn(candidateExpiresOn);
        }

        // 3. Manejar movimiento (cambio de lista/columna)
        Long listaDestinoIdTmp = listaOrigenId;
        Lista nuevaListaStub = cardDetails.getLista();
        if (nuevaListaStub != null && nuevaListaStub.getIdLista() != null) {
            listaDestinoIdTmp = nuevaListaStub.getIdLista();
        }
        final Long listaDestinoId = listaDestinoIdTmp;
        if (listaDestinoId == null) {
            throw new ResourceNotFoundException("Lista destino no encontrada para la tarjeta con id: " + idTarjeta);
        }

        boolean cambioDeLista = !Objects.equals(listaOrigenId, listaDestinoId);
        Lista listaDestino = listaOriginal;

        if (cambioDeLista) {
            notificaciones.add(new CardUpdatedNotificationDetails<>(
                    cardExistente.getTitle(),
                    listaOriginal.getNombre(),
                    listaDestino.getNombre(), LISTA));
            listaDestino = listaRepository.findById(listaDestinoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lista destino no encontrada con id: " + listaDestinoId));

            cardExistente.setLista(listaDestino);

            HistorialMovimiento registro = new HistorialMovimiento(
                    cardExistente,
                    listaOriginal,
                    listaDestino,
                    Instant.now()
            );

            historialMovimientoRepository.save(registro);
        }

        applyLabel(cardExistente, labelId, listaDestino);

        // Persistimos cambios simples antes de recalcular el orden
        cardRepository.save(cardExistente);

        if (cambioDeLista && listaOrigenId != null) {
            reindexarTarjetas(listaOrigenId);
        }

        Integer posicionObjetivo = cardDetails.getCardOrder();
        if (posicionObjetivo == null) {
            posicionObjetivo = cambioDeLista ? Integer.MAX_VALUE : cardExistente.getCardOrder();
        }

        reubicarTarjeta(listaDestino.getIdLista(), cardExistente.getId(), posicionObjetivo);
        final var updatedCard = cardRepository.findById(cardExistente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + cardExistente.getId()));

        notificaciones.add(new CardUpdatedNotificationDetails<>(
                cardExistente.getTitle(),
                cardExistente.getCardOrder(),
                updatedCard.getCardOrder(), ORDER));
        boardMembersWithOwner(updatedCard.getLista().getBoard()).forEach(member ->
                notificationService.addNotifications(usuario.getId(), member.getId(), notificaciones));
        return updatedCard;
    }

    // ---------------------- D - ELIMINAR TARJETA ----------------------

    @Transactional
    public void eliminarTarjeta(User user, Long idTarjeta) {
        Card cardExistente = cardRepository.findById(idTarjeta)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + idTarjeta));

        cardExistente.getLabels().clear();
        cardRepository.save(cardExistente);

        historialMovimientoRepository.deleteAllByTarjetaIdIn(java.util.Collections.singletonList(idTarjeta));
        commentRepository.deleteAllByOwningCardId(idTarjeta);

        cardRepository.delete(cardExistente);
        boardMembersWithOwner(cardExistente.getLista().getBoard()).forEach(member ->
                notificationService.addNotification(user.getId(), member.getId(),
                        new CardDeletedNotificationDetails(
                                cardExistente.getTitle(),
                                cardExistente.getLista().getNombre())
                ));
    }

    private void validateCardDates(Instant startsOn, Instant expiresOn) {
        if (startsOn != null && expiresOn != null && startsOn.isAfter(expiresOn)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de finalización.");
        }
    }

    /**
     * Reindexa todas las tarjetas de una lista para que sus posiciones sean consecutivas.
     */
    private void reindexarTarjetas(Long listaId) {
        List<Card> tarjetas = cardRepository.findByLista_IdListaOrderByCardOrderAsc(listaId);
        for (int index = 0; index < tarjetas.size(); index++) {
            tarjetas.get(index).setCardOrder(index);
        }
        cardRepository.saveAll(tarjetas);
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

    /**
     * Inserta la tarjeta en la posicion solicitada dentro de la lista destino y normaliza los indices.
     * Cuando la posicion es null o Integer.MAX_VALUE se inserta al final.
     */
    private void reubicarTarjeta(Long listaId, Long tarjetaId, Integer posicionDeseada) {
        List<Card> tarjetas = cardRepository.findByLista_IdListaOrderByCardOrderAsc(listaId);
        Card tarjetaEnMovimiento = null;

        for (Iterator<Card> iterator = tarjetas.iterator(); iterator.hasNext(); ) {
            Card tarjeta = iterator.next();
            if (tarjeta.getId().equals(tarjetaId)) {
                tarjetaEnMovimiento = tarjeta;
                iterator.remove();
                break;
            }
        }

        if (tarjetaEnMovimiento == null) {
            tarjetaEnMovimiento = cardRepository.findById(tarjetaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + tarjetaId));
        }

        int indiceDestino = (posicionDeseada != null && posicionDeseada >= 0)
                ? Math.min(posicionDeseada, tarjetas.size())
                : tarjetas.size();

        tarjetas.add(indiceDestino, tarjetaEnMovimiento);

        for (int index = 0; index < tarjetas.size(); index++) {
            tarjetas.get(index).setCardOrder(index);
        }

        cardRepository.saveAll(tarjetas);
    }

    private void applyLabel(Card card, Long labelId, Lista lista) {
        card.setPrimaryLabel(null);
        if (labelId == null) {
            return;
        }

        Long boardId = resolveBoardId(lista);

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Etiqueta no encontrada con id: " + labelId));

        if (boardId != null && !Objects.equals(label.getOwningBoardId(), boardId)) {
            throw new ResourceNotFoundException("La etiqueta no pertenece al tablero indicado.");
        }

        card.setPrimaryLabel(label);
    }

    private Long resolveBoardId(Lista lista) {
        if (lista == null) {
            return null;
        }
        if (lista.getBoard() != null) {
            return lista.getBoard().getId();
        }
        if (lista.getIdTablero() != null) {
            return lista.getIdTablero();
        }
        return null;
    }
}



