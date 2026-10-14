package com.findloop.item;

import java.time.LocalDateTime;

public record ItemReportView(
        Long id,
        ItemType type,
        String title,
        String description,
        String category,
        String location,
        String locationDetails,
        LocalDateTime eventDateTime,
        String status,
        boolean ownedByViewer) {

    public static ItemReportView from(ItemReport report, String viewerId) {
        boolean owner = viewerId != null && viewerId.equals(report.getOwnerId());
        return new ItemReportView(report.getId(), report.getType(), report.getTitle(), report.getDescription(),
                report.getCategory(), report.getLocation(), report.getLocationDetails(), report.getEventDateTime(),
                report.getStatus(), owner);
    }
}
