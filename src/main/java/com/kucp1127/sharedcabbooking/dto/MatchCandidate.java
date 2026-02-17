package com.kucp1127.sharedcabbooking.dto;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.domain.entity.RideGroup;
import lombok.*;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchCandidate {

    private RideGroup rideGroup;
    private Double matchScore;
    private Double estimatedDetour;
    private Double additionalDistance;
    private Boolean meetsAllConstraints;
    private List<String> violatedConstraints;

    public int compareTo(MatchCandidate other) {
        return Double.compare(other.matchScore, this.matchScore);
    }
}
