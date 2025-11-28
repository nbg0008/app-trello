package com.medac.trello.api.service;

import com.medac.trello.api.model.Invitation;
import com.medac.trello.api.model.User;
import com.medac.trello.api.model.Board;
import com.medac.trello.api.model.Workspace;
import com.medac.trello.api.model.WorkspaceBoardLink;
import com.medac.trello.api.model.repository.BoardRepository;
import com.medac.trello.api.model.repository.InvitationRepository;
import com.medac.trello.api.model.repository.UserRepository;
import com.medac.trello.api.model.repository.WorkspaceBoardLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // â¬…ï¸ Necesario para inyectar baseUrl
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.medac.trello.api.model.Invitation.Estado.ACEPTADA;
import static com.medac.trello.api.model.Invitation.Estado.PENDIENTE;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class InvitationService {

    // DEFINICION DE DEPENDENCIAS
    private final BoardRepository boardRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final WorkspaceService workspaceService;
    private final WorkspaceBoardLinkRepository workspaceBoardLinkRepository;

    //Inyectar la URL base de tu frontend/aplicaciÃ³n
    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    public InvitationService(
            BoardRepository boardRepository,
            InvitationRepository invitationRepository,
            UserRepository userRepository,
            EmailService emailService,
            WorkspaceService workspaceService,
            WorkspaceBoardLinkRepository workspaceBoardLinkRepository) {
        this.boardRepository = boardRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.workspaceService = workspaceService;
        this.workspaceBoardLinkRepository = workspaceBoardLinkRepository;
    }

    // -------------------------------------------------------------

    @Transactional
    public void createAndSendInvitation(Board board, String inviteeEmail, Long inviterId, String requestedRole) {

        // 1. GENERAR TOKEN ÚNICO
        String token = UUID.randomUUID().toString();
        String normalizedRole = normalizeRole(requestedRole);

        // 2. CONSTRUIR OBJETO INVITATION Y GUARDAR
        Invitation newInvitation = new Invitation();
        newInvitation.setToken(token);
        newInvitation.setBoard(board);
        newInvitation.setInviteeEmail(inviteeEmail);
        newInvitation.setRole(normalizedRole);

        // Establecer fecha de expiración
        newInvitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        newInvitation.setInviterId(inviterId);

        invitationRepository.save(newInvitation);

        // 3. CONSTRUIR EL ENLACE COMPLETO
        // Usa 'baseUrl' (inyectado) y 'token'
        String path = String.format("/invitations/accept?email=%s&token=%s", URLEncoder.encode(inviteeEmail, UTF_8), token);
        String acceptanceLink = baseUrl + path;


        // 4. PREPARAR Y ENVIAR EL EMAIL
        String subject = "Has sido invitado a un tablero de Flomind!";

        // El cuerpo del email se construye en el EmailService, solo necesitas pasar los parámetros:
        emailService.sendBoardInvitation(inviteeEmail, board.getName(), acceptanceLink);
    }
    //-------------------------------------BUSCAR, VALIDAR Y ELIMINAR INVITACIÓN-----------------

    @Transactional
    public void acceptInvitation(String token, String userEmail) {

        // 1. Buscar la invitación por token
        Invitation invitation = invitationRepository.findByTokenAndStatus(token, PENDIENTE)
                .orElseThrow(() -> new RuntimeException("No se encontrdo la invitación o es inválida"));

        // 2. Validar que la invitaciÃ³n es para el usuario actual
        if (!invitation.getInviteeEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("La invitación no es para el usuario.");
        }

        // 2b. Opcional: Validar si ha expirado
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitationRepository.delete(invitation);
            throw new RuntimeException("La invitación ha caducado.");
        }

        // 3. Buscar el usuario (asumiendo que ya estÃ¡ autenticado)
        User invitingUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado."));

        // 4. Buscar el tablero
        Board board = invitation.getBoard();
        if (board == null) {
            throw new RuntimeException("No se ha encontrado la invitación.");
        }

        // 4b. Opcional: Verificar si ya es miembro
        if (board.getMembers().contains(invitingUser)) {
            invitationRepository.delete(invitation);
            throw new RuntimeException("El usuario ya es miembro de este tablero.");
        }


        // 5. AÃ±adir el usuario al tablero
        board.addMember(invitingUser);
        boardRepository.save(board);
        String roleToAssign = normalizeRole(invitation.getRole());
        boardRepository.updateMemberRole(board.getId(), invitingUser.getId(), roleToAssign);

        Workspace defaultWorkspace = workspaceService.ensureDefaultWorkspace(invitingUser);
        workspaceBoardLinkRepository.findByWorkspace_IdAndBoard_Id(defaultWorkspace.getId(), board.getId())
                .orElseGet(() -> workspaceBoardLinkRepository.save(new WorkspaceBoardLink(defaultWorkspace, board)));

        // 6. Actualizar la invitaciÃ³n a ACEPTADA
        invitation.setStatus(ACEPTADA);
        invitationRepository.save(invitation);
    }

    //------------------------------------CONSULTAR INVITACIONES----------------------
    public List<Invitation> getReceivedInvitations(String userEmail) {
        return invitationRepository.findByInviteeEmail(userEmail);
    }

    private String normalizeRole(String requestedRole) {
        final Set<String> allowedRoles = Set.of("lector", "editor", "admin");
        if (requestedRole == null) {
            return "lector";
        }
        String normalized = requestedRole.trim().toLowerCase();
        return allowedRoles.contains(normalized) ? normalized : "lector";
    }
}


