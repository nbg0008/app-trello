package com.medac.trello.api.model.repository;

import com.medac.trello.api.model.Card;
import org.springframework.data.jpa.repository.JpaRepository; // Cambiamos a JpaRepository
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByLista_IdListaOrderByCardOrderAsc(Long listId);

    List<Card> findByLista_Board_Id(Long boardId);

    List<Card> findByLabels_Id(Long labelId);

    void deleteAllByLista_IdLista(Long listId);

    @Modifying
    @Query("delete from Card c where c.lista.idLista = :listId")
    void deleteByListId(@Param("listId") Long listId);
}
