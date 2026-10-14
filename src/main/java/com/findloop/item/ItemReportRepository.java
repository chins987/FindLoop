package com.findloop.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemReportRepository extends JpaRepository<ItemReport, Long> {
    List<ItemReport> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);
    List<ItemReport> findByTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(ItemType type, String status);

    @Query("""
            select r from ItemReport r
            where upper(r.status) = 'OPEN'
              and (:type is null or r.type = :type)
              and (:category is null or lower(r.category) = lower(:category))
              and (:query is null or lower(concat(concat(concat(concat(r.title, ' '), concat(r.description, ' ')), concat(r.category, ' ')), concat(r.location, coalesce(r.locationDetails, '')))) like lower(concat('%', :query, '%')))
              and (:minLatitude is null or (r.latitude between :minLatitude and :maxLatitude and r.longitude between :minLongitude and :maxLongitude))
            order by r.createdAt desc
            """)
    List<ItemReport> searchOpenReports(@Param("type") ItemType type,
            @Param("category") String category,
            @Param("query") String query,
            @Param("minLatitude") Double minLatitude,
            @Param("maxLatitude") Double maxLatitude,
            @Param("minLongitude") Double minLongitude,
            @Param("maxLongitude") Double maxLongitude);
}
