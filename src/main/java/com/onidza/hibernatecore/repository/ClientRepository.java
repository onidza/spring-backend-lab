package com.onidza.hibernatecore.repository;

import com.onidza.hibernatecore.model.entity.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    
    @EntityGraph(value = "client-full", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT c FROM Client c")
    List<Client> findAllWithDetails();

    boolean existsByEmail(String email);
}
