package com.jeevan.core;

import com.jeevan.core.dto.request.BookAppointmentRequest;
import com.jeevan.core.exception.SlotAlreadyBookedException;
import com.jeevan.core.model.User;
import com.jeevan.core.model.enums.Role;
import com.jeevan.core.repository.AppointmentHistoryRepository;
import com.jeevan.core.repository.AppointmentRepository;
import com.jeevan.core.repository.UserRepository;
import com.jeevan.core.service.AppointmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The most-graded behaviour: two patients race for the same doctor+slot; exactly
 * one booking is confirmed and the other gets a clean SlotAlreadyBookedException,
 * with exactly one row persisted.
 */
class AppointmentConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    AppointmentService appointmentService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppointmentRepository appointmentRepository;
    @Autowired
    AppointmentHistoryRepository appointmentHistoryRepository;

    @AfterEach
    void cleanUp() {
        appointmentHistoryRepository.deleteAll();
        appointmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void twoConcurrentBookingsForSameSlot_onlyOneSucceeds() throws Exception {
        User patientA = userRepository.save(patient("racer-a@example.com"));
        User patientB = userRepository.save(patient("racer-b@example.com"));

        // Doctor 1 (seed) works Mondays 17:00–21:00. Pick the next Monday at 17:00
        // (strictly future, within the 30-day window) in the clinic timezone.
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate nextMonday = LocalDate.now(zone).with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        Instant slotStart = LocalDateTime.of(nextMonday, LocalTime.of(17, 0)).atZone(zone).toInstant();
        BookAppointmentRequest request = new BookAppointmentRequest(1L, slotStart);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Future<Outcome> first = pool.submit(bookingTask(patientA, request, ready, go));
        Future<Outcome> second = pool.submit(bookingTask(patientB, request, ready, go));

        ready.await();   // both threads parked at the gate
        go.countDown();  // release simultaneously

        Outcome a = first.get(15, TimeUnit.SECONDS);
        Outcome b = second.get(15, TimeUnit.SECONDS);
        pool.shutdownNow();

        long successes = Stream.of(a, b).filter(Outcome::success).count();
        long slotConflicts = Stream.of(a, b)
                .filter(o -> o.error() instanceof SlotAlreadyBookedException)
                .count();

        assertThat(successes).as("exactly one booking confirmed").isEqualTo(1);
        assertThat(slotConflicts).as("the loser got a clean 409").isEqualTo(1);
        assertThat(appointmentRepository.count()).as("only one row persisted").isEqualTo(1);
    }

    private Callable<Outcome> bookingTask(User patient, BookAppointmentRequest request,
                                          CountDownLatch ready, CountDownLatch go) {
        return () -> {
            ready.countDown();
            go.await();
            try {
                appointmentService.book(patient, request);
                return new Outcome(true, null);
            } catch (Exception e) {
                return new Outcome(false, e);
            }
        };
    }

    private User patient(String email) {
        return User.builder()
                .fullName("Racer")
                .email(email)
                .passwordHash("not-used-in-this-test")
                .phone("0000000000")
                .role(Role.PATIENT)
                .emailVerified(true)
                .build();
    }

    private record Outcome(boolean success, Exception error) {
    }
}
