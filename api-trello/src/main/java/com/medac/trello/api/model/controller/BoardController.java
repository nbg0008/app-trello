package com.medac.trello.api.model.controller;

import com.medac.trello.api.dto.BoardMemberDTO;
import com.medac.trello.api.dto.BoardRequestDTO;
import com.medac.trello.api.dto.BoardResponseDTO;
import com.medac.trello.api.dto.InviteRequestDTO;
import com.medac.trello.api.dto.UpdateMemberRoleRequest;
import com.medac.trello.api.exception.ResourceNotFoundException;
import com.medac.trello.api.model.Board;
import com.medac.trello.api.model.Invitation;
import com.medac.trello.api.model.User;
import com.medac.trello.api.resources.TrelloApi;
import com.medac.trello.api.service.BoardService;
import com.medac.trello.api.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.Map;

import static java.util.stream.Collectors.toSet;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;

// Implementamos la interfaz TrelloApi
@RestController
@RequestMapping(value = "/tableros", produces = APPLICATION_JSON_VALUE)
public class BoardController implements TrelloApi {
    private final BoardService boardService;
    private final InvitationService invitationService;

    @Autowired
    public BoardController(BoardService boardService, InvitationService invitationService) {
        this.boardService = boardService;
        this.invitationService = invitationService;

    }

    //CREAR
    @PostMapping
    public ResponseEntity<BoardResponseDTO> crearTablero(
            @RequestBody BoardRequestDTO board,
            @AuthenticationPrincipal User authenticatedUser) {

        var savedBoard = boardService.guardarBoard(board, authenticatedUser);
        BoardResponseDTO nuevoBoard = boardService.mapToBoardResponse(
                savedBoard,
                authenticatedUser != null ? authenticatedUser.getId() : null
        );
        return new ResponseEntity<>(nuevoBoard, HttpStatus.CREATED);
    }

    //LEER TODOS
    @GetMapping
    public Set<BoardResponseDTO> listarTodosLosTableros(@AuthenticationPrincipal User authenticatedUser) {
        Long userId = authenticatedUser != null ? authenticatedUser.getId() : null;
        return boardService.obtenerTablerosPorUsuario(userId).stream()
                .map(board -> boardService.mapToBoardResponse(board, userId))
                .collect(toSet());
    }

    //LEER UNO
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> obtenerTableroPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal User authenticatedUser) {
        var board = boardService.obtenerBoardConRol(
                id,
                authenticatedUser != null ? authenticatedUser.getId() : null
        );
        return ResponseEntity.ok(board);
    }

    //ACTUALIZAR
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> actualizarTablero(
            @PathVariable Long id,
            @RequestBody Board boardDetalles,
            @AuthenticationPrincipal User authenticatedUser) {
        var boardActualizado = boardService.mapToBoardResponse(
                boardService.actualizarBoard(id, boardDetalles),
                authenticatedUser != null ? authenticatedUser.getId() : null
        );
        return ResponseEntity.ok(boardActualizado);
    }

    //ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> eliminarTablero(@PathVariable Long id) {
        boardService.eliminarBoard(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @GetMapping("/by-user/{userId}")
    public Set<BoardResponseDTO> listarTablerosPorUsuario(@PathVariable Long userId) {
        // Llama al nuevo método del servicio
        return boardService.obtenerTablerosPorUsuario(userId).stream()
                .map(board -> boardService.mapToBoardResponse(board, userId))
                .collect(toSet());
    }

    @GetMapping("/{boardId}/miembros")
    public ResponseEntity<List<BoardMemberDTO>> listarMiembrosDelTablero(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var miembros = boardService.obtenerMiembros(
                boardId,
                authenticatedUser != null ? authenticatedUser.getId() : null
        );
        return ResponseEntity.ok(miembros);
    }

    @PutMapping("/{boardId}/miembros/{memberId}")
    public ResponseEntity<BoardMemberDTO> actualizarRolMiembro(
            @PathVariable Long boardId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var actualizado = boardService.actualizarRolMiembro(
                boardId,
                authenticatedUser != null ? authenticatedUser.getId() : null,
                memberId,
                request.getRole()
        );
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{boardId}/miembros/{memberId}")
    public ResponseEntity<Void> eliminarMiembro(
            @PathVariable Long boardId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        boardService.eliminarMiembro(
                boardId,
                authenticatedUser != null ? authenticatedUser.getId() : null,
                memberId
        );
        return ResponseEntity.noContent().build();
    }

    //-----------------------------endpoint de invitacion a tablero------------


    @PostMapping("/{boardId}/invitaciones")
    public ResponseEntity<Map<String, String>> inviteUserToBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody InviteRequestDTO request,
            @AuthenticationPrincipal User authenticatedUser) {

    try {
        Long inviterId = authenticatedUser.getId();
        Board board = boardService.obtenerBoardPorId(boardId);

        if (!board.getOwnerId().equals(authenticatedUser.getId()) && !board.getMembers().contains(authenticatedUser)) {
            throw new AccessDeniedException("Solo el dueno o miembros del tablero pueden invitar.");
        }

        invitationService.createAndSendInvitation(
                board,
                request.email(),
                inviterId,
                request.role()
        );

        return ResponseEntity.ok(Map.of("message", "Invitacion enviada con exito a " + request.email()));

    } catch (AccessDeniedException e) {
        return status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (ResourceNotFoundException e) {
        return status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    } catch (MailException e) {
        return ResponseEntity.ok(Map.of("message", "Invitacion creada, pero el correo no pudo enviarse: " + e.getMessage()));
    } catch (Exception e) {
        return status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno al procesar la invitacion: " + e.getMessage()));
    }
}

@GetMapping("/invitations/received")
    public ResponseEntity<List<Invitation>> getReceivedInvitations(
            @AuthenticationPrincipal User authenticatedUser) { // Obtiene el usuario autenticado del JWT

        // Obtener el email del usuario autenticado
        String userEmail = authenticatedUser.getEmail();

        // Llamar al servicio
        List<Invitation> invitations = invitationService.getReceivedInvitations(userEmail);

        // Devolver la lista
        return ResponseEntity.ok(invitations);
    }
}

