
package com.findloop.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRequestRepository
        extends JpaRepository<ContactRequest, Long> {

    List<ContactRequest> findBySenderIdOrderByCreatedAtDesc(String senderId);

    List<ContactRequest> findByReceiverIdOrderByCreatedAtDesc(String receiverId);

    boolean existsByReportIdAndSenderIdAndStatus(
            Long reportId, String senderId, String status);
}
