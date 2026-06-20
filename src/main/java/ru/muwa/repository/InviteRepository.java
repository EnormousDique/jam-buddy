package ru.muwa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.muwa.entity.Invite;
import ru.muwa.entity.Profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    @Query("SELECT COUNT(i) FROM Invite i WHERE i.senderId = :senderId AND i.createdAt >= :startOfDay")
    long countDailyInvites(@Param("senderId") UUID senderId, @Param("startOfDay") LocalDateTime startOfDay);

    // boolean existsBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    List<Invite> findByReceiverId(UUID receiverId);

    List<Invite> findBySenderId(UUID senderId);

}
