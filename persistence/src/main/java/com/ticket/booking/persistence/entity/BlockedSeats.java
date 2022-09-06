package com.ticket.booking.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlockedSeats implements Serializable {

    private String userId;
    private List<String> seats;
}
