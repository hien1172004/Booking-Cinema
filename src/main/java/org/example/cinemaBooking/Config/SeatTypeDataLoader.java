package org.example.cinemaBooking.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.Entity.SeatType;
import org.example.cinemaBooking.Repository.SeatTypeRepository;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatTypeDataLoader implements CommandLineRunner {

    private final SeatTypeRepository seatTypeRepository;

    @Override
    public void run(String... args) throws Exception {
        loadSeatTypes();
    }

    private void loadSeatTypes() {
        if (seatTypeRepository.count() == 0) {
            log.info("Loading seat types...");
            
            SeatType[] seatTypes = {
                SeatType.builder()
                    .name(SeatTypeEnum.STANDARD)
                    .priceModifier(BigDecimal.ZERO)
                    .build(),
                SeatType.builder()
                    .name(SeatTypeEnum.VIP)
                    .priceModifier(new BigDecimal("30000"))
                    .build(),
                SeatType.builder()
                    .name(SeatTypeEnum.COUPLE)
                    .priceModifier(new BigDecimal("80000"))
                    .build()
            };

            seatTypeRepository.saveAll(Arrays.asList(seatTypes));
            
            log.info("Created {} seat types", seatTypeRepository.count());
        } else {
            log.info("Seat types already exist, skipping...");
        }
    }
}