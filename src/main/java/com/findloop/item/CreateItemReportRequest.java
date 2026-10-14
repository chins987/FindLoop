
package com.findloop.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDateTime;

public record CreateItemReportRequest(

                @NotNull ItemType type,

                @NotBlank @Size(max = 80) String title,

                @NotBlank @Size(max = 2000) String description,

                @NotBlank @Size(max = 80) String category,

                @NotBlank @Size(max = 160) String location,

                @Size(max = 500) String locationDetails,

                @NotNull(message = "Date and time are required") LocalDateTime eventDateTime,

                @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,

                @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,

                @Size(max = 240) String ownershipQuestion,

                @Size(max = 200) String ownershipAnswer

) {
        @AssertTrue(message = "Latitude and longitude must be provided together")
        public boolean isCoordinatePairValid() {
                return (latitude == null) == (longitude == null);
        }

        @AssertTrue(message = "Found reports need both ownership question and answer")
        public boolean isOwnershipCheckValid() {
                boolean question = ownershipQuestion != null && !ownershipQuestion.isBlank();
                boolean answer = ownershipAnswer != null && !ownershipAnswer.isBlank();
                return type == ItemType.FOUND ? question == answer : !question && !answer;
        }
}
