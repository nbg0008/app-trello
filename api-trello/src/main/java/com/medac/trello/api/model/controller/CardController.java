package com.medac.trello.api.model.controller;

import com.medac.trello.api.dto.CardRequestDTO;
import com.medac.trello.api.dto.CardResponseDTO;
import com.medac.trello.api.model.Card;
import com.medac.trello.api.model.Lista;
import com.medac.trello.api.model.User;
import com.medac.trello.api.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
//@RequestMapping("/trello/v1/tarjetas") // Usar un prefijo de versión y base más claro
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class CardController {

    @Autowired
    private CardService cardService;

    // --------------------------------CREAR TARJETA -----------------
    // POST /trello/v1/listas/{listId}/tarjetas
    @PostMapping("/listas/{listId}/tarjetas")
    public ResponseEntity<CardResponseDTO> crearTarjeta(
            @PathVariable Long listId,
            @RequestBody CardRequestDTO cardDto,
            @AuthenticationPrincipal User authenticatedUser

    ) {
        // 1. Mapeo DTO -> Entidad (Solo los campos de datos)
        Card cardParaGuardar = new Card();
        cardParaGuardar.setTitle(cardDto.getTitle());
        cardParaGuardar.setDescription(cardDto.getDescription());
        cardParaGuardar.setCardOrder(cardDto.getCardOrder());
        if (cardDto.isStartsOnPresent()) {
            cardParaGuardar.setStartsOn(cardDto.getStartsOn());
        }
        if (cardDto.isExpiresOnPresent()) {
            cardParaGuardar.setExpiresOn(cardDto.getExpiresOn());
        }

        // 2. Llamada al servicio con la entidad y el ID de la lista padre
        Card cardGuardada = cardService.guardarCard(authenticatedUser, listId, cardParaGuardar, cardDto.getLabelId());

        // 3. Mapeo Entidad -> DTO de Respuesta
        CardResponseDTO responseDto = new CardResponseDTO(cardGuardada);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED); // 201
    }

    /*// ---------------------- LEER POR TABLERO / GET ----------------------
    // GET /trello/v1/tableros/{tableroId}/tarjetas
    @GetMapping("/tableros/{tableroId}/tarjetas")
    public ResponseEntity<List<Card>> obtenerTarjetasPorTablero(@PathVariable Long tableroId) {
        List<Card> tarjetas = cardService.encontrarTarjetasPorTableroId(tableroId);
        return ResponseEntity.ok(tarjetas); // 200
    }

     */

    // ---------------------- R - LEER TODAS LAS TARJETAS DE UNA LISTA ----------------------
    // URI: /listas/{listId}/tarjetas
    @GetMapping("/listas/{listId}/tarjetas")
    public List<CardResponseDTO> listarTarjetasPorLista(@PathVariable Long listId) {

        // 1. Llamada al servicio, que devuelve Entidades JPA
        List<Card> cards = cardService.obtenerCardsPorLista(listId);

        // 2. Mapeo de la colección de Entidades a colección de DTO de Respuesta
        return cards.stream()
                .map(CardResponseDTO::new) // Usando el constructor de mapeo
                .collect(Collectors.toList());
    }


    // ---------------------- R - LEER UNA TARJETA ----------------------
    // URI: /tarjetas/{cardId}
    @GetMapping("/tarjetas/{cardId}")
    public ResponseEntity<CardResponseDTO> obtenerTarjetaPorId(@PathVariable Long cardId) {
        Card card = cardService.obtenerCardPorId(cardId);

        CardResponseDTO responseDto = new CardResponseDTO(card);
        return ResponseEntity.ok(responseDto); // 200
    }




   /* // --- EDITAR (Contenido) y MOVER (Lista) ---
    // PUT /trello/v1/tarjetas/{idTarjeta}
    @PutMapping("/tarjetas/{idTarjeta}")
    public ResponseEntity<Card> actualizarTarjeta(@PathVariable Long idTarjeta, @RequestBody Card cardDetails){
        // El servicio maneja la excepción ResourceNotFoundException (404)
        Card cardActualizada  = cardService.actualizarCard(idTarjeta, cardDetails);
        return ResponseEntity.ok(cardActualizada); // 200
    }

    */
// ---------------------- U - ACTUALIZAR TARJETA (Incluye movimiento entre listas) ----------------------
   // URI: /tarjetas/{cardId}
   @PutMapping("/tarjetas/{cardId}")
   public ResponseEntity<CardResponseDTO> actualizarTarjeta(
           @PathVariable Long cardId,
           @RequestBody CardRequestDTO cardDto,
           @AuthenticationPrincipal User authenticatedUser

   ) {
       // 1. Mapeo DTO
       Card cardParaActualizar = new Card();
       cardParaActualizar.setTitle(cardDto.getTitle());
       cardParaActualizar.setDescription(cardDto.getDescription());
       cardParaActualizar.setCardOrder(cardDto.getCardOrder());
       if (cardDto.isStartsOnPresent()) {
           cardParaActualizar.setStartsOn(cardDto.getStartsOn());
       }
       if (cardDto.isExpiresOnPresent()) {
           cardParaActualizar.setExpiresOn(cardDto.getExpiresOn());
       }

       // Lógica de MOVIMIENTO:
       if (cardDto.getIdLista() != null) {
           Lista listaStub = new Lista();
           // Asume que la entidad Lista tiene public void setIdLista(Long idLista)
           listaStub.setIdLista(cardDto.getIdLista());
           cardParaActualizar.setLista(listaStub);
       }

       // 2. Llamada al servicio
       Card cardActualizada = cardService.actualizarCard(
               authenticatedUser,
               cardId,
               cardParaActualizar,
               cardDto.getLabelId(),
               cardDto.isStartsOnPresent(),
               cardDto.isExpiresOnPresent()
       );

       // 3. Mapeo Entidad -> DTO de Respuesta
       CardResponseDTO responseDto = new CardResponseDTO(cardActualizada);

       return ResponseEntity.ok(responseDto); // 200
   }


    //------------------------------------ ELIMINAR ----------------------
    /*// DELETE /trello/v1/tarjetas/{idTarjeta}
    @DeleteMapping("/tarjetas/{idTarjeta}")
    public ResponseEntity<HttpStatus> eliminarTarjeta(@PathVariable Long idTarjeta){
        // El servicio maneja la excepción ResourceNotFoundException (404)
        cardService.eliminarTarjeta(idTarjeta);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }

     */

    // ---------------------- D - ELIMINAR ----------------------
    // URI: /tarjetas/{cardId}
    @DeleteMapping("/tarjetas/{cardId}")
    public ResponseEntity<HttpStatus> eliminarTarjeta(@PathVariable Long cardId,
                                                      @AuthenticationPrincipal User authenticatedUser) {
        cardService.eliminarTarjeta(authenticatedUser, cardId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }
}



