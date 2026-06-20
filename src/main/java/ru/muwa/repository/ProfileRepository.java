package ru.muwa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.muwa.entity.Profile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserId(UUID userId);

    @Query(value = """
    SELECT p.*, 
    (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(p.latitude)))) AS distance 
    FROM profile.profiles p 
    LEFT JOIN profile.profile_instruments pi ON p.id = pi.profile_id
    WHERE p.user_id != :currentUserId
    AND (:gender IS NULL OR p.gender = :gender)
    AND (:minAge IS NULL OR p.age >= :minAge)
    AND (:maxAge IS NULL OR p.age <= :maxAge)
    AND (:insCount = 0 OR pi.instrument_id IN (:insIds))
    GROUP BY p.id
    HAVING (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(p.latitude)))) <= :radius
    ORDER BY distance ASC
    """, nativeQuery = true)
    List<Profile> findNearby(@Param("lat") Double lat,
                               @Param("lon") Double lon,
                               @Param("radius") Double radius,
                               @Param("gender") String gender,
                               @Param("minAge") Integer minAge,
                               @Param("maxAge") Integer maxAge,
                               @Param("insIds") Set<Integer> insIds,
                               @Param("insCount") Integer insCount,
                               @Param("currentUserId") UUID currentUserId);

}
