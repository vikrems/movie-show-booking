package com.ticket.booking.persistence.repository;

import com.ticket.booking.domain.entity.Booking;
import com.ticket.booking.persistence.entity.BookingEntity;

import java.util.List;

public interface IBookingRepository {

    Booking getBookingById(String showId);
    List<BookingEntity> getBookingByShowId(String showId);
}
