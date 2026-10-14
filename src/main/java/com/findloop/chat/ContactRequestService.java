package com.findloop.chat;

import com.findloop.item.ItemReport;
import com.findloop.item.ItemReportRepository;
import com.findloop.item.OwnershipVerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContactRequestService {

        private final ContactRequestRepository requestRepository;
        private final ItemReportRepository reportRepository;
        private final OwnershipVerificationRepository ownershipVerificationRepository;

        public ContactRequestService(
                        ContactRequestRepository requestRepository,
                        ItemReportRepository reportRepository,
                        OwnershipVerificationRepository ownershipVerificationRepository) {
                this.requestRepository = requestRepository;
                this.reportRepository = reportRepository;
                this.ownershipVerificationRepository = ownershipVerificationRepository;
        }

        @Transactional
        public ContactRequest send(Long reportId, String senderId, Long ownershipReportId) {
                ItemReport report = reportRepository.findById(reportId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Report not found"));

                if (report.getOwnerId() == null || report.getOwnerId().isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This older report has no registered owner yet");
                }

                if (report.getOwnerId().equals(senderId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "You cannot contact yourself");
                }

                if (!"OPEN".equalsIgnoreCase(report.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT, "This report is no longer open");
                }

                Long checkReportId = ownershipReportId;

                if (checkReportId == null
                                && report.getType() == com.findloop.item.ItemType.FOUND) {
                        checkReportId = reportId;
                }

                ItemReport checkReport = checkReportId == null
                                ? null
                                : reportRepository.findById(checkReportId)
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                "Ownership check report not found"));

                if (checkReport != null
                                && (checkReport.getType() != com.findloop.item.ItemType.FOUND
                                                || !"OPEN".equalsIgnoreCase(checkReport.getStatus())
                                                || (!checkReportId.equals(reportId)
                                                                && checkReport.getType() == report.getType()))) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Ownership check must use an open found report paired with this request.");
                }

                if (checkReport != null
                                && checkReport.getOwnershipAnswerHash() != null
                                && !ownershipVerificationRepository
                                                .existsByReportIdAndUserIdAndVerifiedAtAfter(
                                                                checkReportId,
                                                                senderId,
                                                                java.time.LocalDateTime.now().minusMinutes(30))) {
                        throw new ResponseStatusException(
                                        HttpStatus.PRECONDITION_REQUIRED,
                                        "Answer the ownership check before contacting the finder.");
                }

                if (requestRepository.existsByReportIdAndSenderIdAndStatus(
                                reportId, senderId, "PENDING")
                                || requestRepository.existsByReportIdAndSenderIdAndStatus(
                                                reportId, senderId, "ACCEPTED")) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "You already have a pending or accepted request for this report");
                }

                return requestRepository.save(
                                new ContactRequest(reportId, senderId, report.getOwnerId()));
        }

        public List<ContactRequest> sentBy(String userId) {
                return requestRepository.findBySenderIdOrderByCreatedAtDesc(userId);
        }

        public List<ContactRequest> receivedBy(String userId) {
                return requestRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
        }

        @Transactional
        public ContactRequest respond(Long requestId, String userId, String decision) {
                ContactRequest request = requestRepository.findById(requestId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Contact request not found"));

                if (!request.getReceiverId().equals(userId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Only the recipient can respond to this request");
                }

                if (!"PENDING".equals(request.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This request has already been answered");
                }

                String normalized = decision == null
                                ? ""
                                : decision.trim().toUpperCase();

                if (!normalized.equals("ACCEPTED") && !normalized.equals("DECLINED")) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Decision must be ACCEPTED or DECLINED");
                }

                request.setStatus(normalized);
                return requestRepository.save(request);
        }
}
