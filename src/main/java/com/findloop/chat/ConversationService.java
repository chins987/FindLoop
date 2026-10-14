
package com.findloop.chat;

import com.findloop.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ConversationService {
        private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

        private final ConversationRepository conversationRepository;
        private final ContactRequestRepository requestRepository;
        private final ChatMessageRepository messageRepository;
        private final NotificationService notificationService;

        public ConversationService(
                        ConversationRepository conversationRepository,
                        ContactRequestRepository requestRepository,
                        ChatMessageRepository messageRepository,
                        NotificationService notificationService) {
                this.conversationRepository = conversationRepository;
                this.requestRepository = requestRepository;
                this.messageRepository = messageRepository;
                this.notificationService = notificationService;
        }

        public List<Conversation> listConversations(String userId) {
                return conversationRepository
                                .findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(userId, userId);
        }

        @Transactional
        public Conversation openConversation(Long requestId, String userId) {
                ContactRequest request = requestRepository.findById(requestId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Contact request not found"));

                if (!"ACCEPTED".equals(request.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "The contact request must be accepted before chatting");
                }

                if (!request.getSenderId().equals(userId)
                                && !request.getReceiverId().equals(userId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You are not a participant in this request");
                }

                return conversationRepository.findByContactRequestId(requestId)
                                .orElseGet(() -> conversationRepository.save(
                                                new Conversation(
                                                                requestId,
                                                                request.getSenderId(),
                                                                request.getReceiverId())));
        }

        public List<ChatMessage> getMessages(Long conversationId, String userId) {
                requireParticipant(conversationId, userId);
                return messageRepository
                                .findByConversationIdOrderBySentAtAsc(conversationId);
        }

        @Transactional
        public ChatMessage sendMessage(
                        Long conversationId, String userId, String content) {
                Conversation conversation = requireParticipant(conversationId, userId);

                if (content == null || content.isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Message cannot be empty");
                }

                String trimmed = content.trim();

                if (trimmed.length() > 2000) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Message cannot exceed 2000 characters");
                }

                ChatMessage saved = messageRepository.save(new ChatMessage(conversationId, userId, trimmed));
                String recipientId = conversation.getUserOneId().equals(userId)
                        ? conversation.getUserTwoId() : conversation.getUserOneId();
                try {
                        notificationService.notifyReply(recipientId, conversationId, trimmed);
                } catch (RuntimeException ex) {
                        log.warn("Message was saved, but its notification could not be created", ex);
                }
                return saved;
        }

        private Conversation requireParticipant(Long conversationId, String userId) {
                Conversation conversation = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Conversation not found"));

                if (!conversation.includes(userId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You are not a participant in this conversation");
                }

                return conversation;
        }
}
