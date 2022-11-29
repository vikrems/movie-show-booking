package com.ticket.booking.api.dto;

import com.ticket.booking.domain.entity.enums.Occupancy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {

    private String number;
    private Occupancy occupancy;
}
