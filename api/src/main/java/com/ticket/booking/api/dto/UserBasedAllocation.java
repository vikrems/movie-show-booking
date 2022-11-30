package com.ticket.booking.api.dto;

import com.ticket.booking.domain.entity.enums.Occupancy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class UserBasedAllocation {

    private final String userId;
    private final Occupancy occupancy;
    private final List<String> seats;
}
