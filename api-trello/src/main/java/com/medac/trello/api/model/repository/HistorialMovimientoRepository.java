package com.medac.trello.api.model.repository;

import com.medac.trello.api.model.HistorialMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface HistorialMovimientoRepository extends JpaRepository<HistorialMovimiento, Long> {

    void deleteAllByTarjetaIdIn(Collection<Long> cardIds);

    void deleteAllByTarjeta_Lista_IdLista(Long listId);

    @Modifying
    @Query("delete from HistorialMovimiento hm where hm.tarjeta.lista.idLista = :listId")
    void deleteByListId(@Param("listId") Long listId);

    @Modifying
    @Query("delete from HistorialMovimiento hm where hm.tarjeta.id in :cardIds")
    void deleteByCardIds(@Param("cardIds") Collection<Long> cardIds);
}
