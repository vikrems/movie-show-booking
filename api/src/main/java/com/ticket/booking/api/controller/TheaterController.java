package com.ticket.booking.api.controller;

import com.ticket.booking.api.dto.AllBookings;
import com.ticket.booking.api.dto.SeatAllocation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("theater/{theaterId}")
public class TheaterController {

    @GetMapping("seats")
    public ResponseEntity<SeatAllocation> listAllSeats(@PathVariable("theaterId") String theaterId) {
        return null;
    }

    @GetMapping("booking")
    public ResponseEntity<AllBookings> listBookings(@PathVariable("theaterId") String theaterId) {
        return null;
    }
}
