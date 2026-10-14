
package com.findloop.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(
            String userOneId, String userTwoId);

    Optional<Conversation> findByContactRequestId(Long contactRequestId);
}
