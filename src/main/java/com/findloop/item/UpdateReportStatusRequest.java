package com.findloop.item;

import jakarta.validation.constraints.NotNull;

public record UpdateReportStatusRequest(@NotNull ItemStatus status) {}
