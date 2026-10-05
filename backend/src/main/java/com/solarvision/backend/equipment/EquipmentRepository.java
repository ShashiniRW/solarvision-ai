package com.solarvision.backend.equipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.site s " +
            "LEFT JOIN FETCH s.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "WHERE e.site.id = :siteId")
    List<Equipment> findBySiteIdWithDetails(Long siteId);

    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.site s " +
            "LEFT JOIN FETCH s.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "WHERE e.id = :id")
    Optional<Equipment> findByIdWithDetails(Long id);

    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.site s " +
            "LEFT JOIN FETCH s.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "WHERE e.qrCode = :qrCode")
    Optional<Equipment> findByQrCode(String qrCode);

    boolean existsByQrCode(String qrCode);
}