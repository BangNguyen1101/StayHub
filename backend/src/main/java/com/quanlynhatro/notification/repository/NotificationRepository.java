package com.quanlynhatro.notification.repository;

import com.quanlynhatro.notification.domain.NotificationEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    @Query(
            """
            select n from NotificationEntity n
            where n.recipientUser.id = :recipientId
              and (:unread is null or n.read = false)
              and (:keyword is null or :keyword = ''
                   or lower(n.title) like lower(concat('%', :keyword, '%'))
                   or lower(n.content) like lower(concat('%', :keyword, '%')))
            order by n.createdAt desc
            """)
    Page<NotificationEntity> searchForRecipient(
            @Param("recipientId") Long recipientId,
            @Param("unread") Boolean unread,
            @Param("keyword") String keyword,
            Pageable pageable);

    Page<NotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<NotificationEntity> findByRecipientUser_IdOrderByCreatedAtDesc(
            Long recipientId, Pageable pageable);

    Optional<NotificationEntity> findByIdAndRecipientUser_Id(Long id, Long recipientId);

    long countByRecipientUser_IdAndReadFalse(Long recipientId);

    boolean existsByRecipientUser_IdAndNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
            Long recipientId,
            String notificationType,
            String relatedEntityType,
            Long relatedEntityId);
}
