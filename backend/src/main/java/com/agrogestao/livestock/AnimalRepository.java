package com.agrogestao.livestock;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    boolean existsByPropertyIdAndStatusAndTagIgnoreCase(Long propertyId, AnimalStatus status, String tag);

    boolean existsByPropertyIdAndStatusAndTagIgnoreCaseAndIdNot(Long propertyId, AnimalStatus status, String tag, Long id);

    boolean existsByBatchId(Long batchId);

    boolean existsByMotherId(Long motherId);

    List<Animal> findByMotherIdOrderByBirthDateAsc(Long motherId);

    List<Animal> findByPropertyIdAndIdIn(Long propertyId, List<Long> ids);

    /** [batchId, cabeças ativas] dos lotes individuais. */
    @Query("SELECT a.batchId, COUNT(a) FROM Animal a WHERE a.propertyId = :propertyId "
            + "AND a.status = com.agrogestao.livestock.AnimalStatus.ATIVO AND a.batchId IS NOT NULL GROUP BY a.batchId")
    List<Object[]> countActiveByBatch(@Param("propertyId") Long propertyId);

    /** [espécie, cabeças ativas] dos animais individuais. */
    @Query("SELECT a.species, COUNT(a) FROM Animal a WHERE a.propertyId = :propertyId "
            + "AND a.status = com.agrogestao.livestock.AnimalStatus.ATIVO GROUP BY a.species")
    List<Object[]> countActiveBySpecies(@Param("propertyId") Long propertyId);

    @Query("SELECT a FROM Animal a WHERE a.propertyId = :propertyId "
            + "AND (:status IS NULL OR a.status = :status) "
            + "AND (:batchId IS NULL OR a.batchId = :batchId) "
            + "AND (:species IS NULL OR a.species = :species) "
            + "AND (:q IS NULL OR LOWER(a.tag) LIKE :q ESCAPE '\\' OR LOWER(a.name) LIKE :q ESCAPE '\\') "
            + "ORDER BY a.tag")
    Page<Animal> search(@Param("propertyId") Long propertyId, @Param("status") AnimalStatus status,
            @Param("batchId") Long batchId, @Param("species") Species species, @Param("q") String q,
            Pageable pageable);
}
