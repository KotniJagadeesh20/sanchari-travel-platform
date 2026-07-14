package com.travelplatform.busbooking.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.travelplatform.busbooking.entity.Bus;
import com.travelplatform.busbooking.exception.GlobalExceptionHandler;
import com.travelplatform.busbooking.repository.BusRepository;
import com.travelplatform.busbooking.repository.DriverRepository;
import com.travelplatform.busbooking.service.BusService;
import com.travelplatform.busbooking.service.DriverService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private BusService busService;
    @Mock private DriverService driverService;
    @Mock private BusRepository busRepo;
    @Mock private DriverRepository driverRepo;
    @InjectMocks private AdminController adminController;

    private MockMvc mockMvc;

    private static final String BUS_JSON =
        "{\"busno\":\"AP01\",\"source\":\"Hyderabad\",\"destination\":\"Bangalore\"," +
        "\"busType\":\"AC Sleeper\",\"date\":\"2026-06-20\",\"time\":\"22:00:00\",\"price\":800}";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addBus_returns202_whenBusNumberNew() throws Exception {
        when(busRepo.findBusByBusNo("AP01")).thenReturn(null);
        Bus saved = new Bus();
        saved.setId(UUID.randomUUID());
        saved.setBusno("AP01");
        when(busService.addBus(any(Bus.class))).thenReturn(saved);

        mockMvc.perform(post("/admin/addbus")
                .contentType("application/json").content(BUS_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void addBus_returns400_whenDuplicateBusNumber() throws Exception {
        Bus existing = new Bus();
        existing.setBusno("AP01");
        when(busRepo.findBusByBusNo("AP01")).thenReturn(existing);

        mockMvc.perform(post("/admin/addbus")
                .contentType("application/json").content(BUS_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        verify(busService, never()).addBus(any());
    }

    @Test
    void deleteBus_returns200_whenDeleted() throws Exception {
        when(busService.deletebus("AP01")).thenReturn(true);
        mockMvc.perform(delete("/admin/deletebus/AP01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void deleteBus_returns404_whenNotFound() throws Exception {
        when(busService.deletebus("ZZ99")).thenReturn(false);
        mockMvc.perform(delete("/admin/deletebus/ZZ99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void getAllBusses_returnsListWithOk() throws Exception {
        Bus bus = new Bus();
        bus.setId(UUID.randomUUID());
        bus.setBusno("AP01");
        when(busService.getAllBusses()).thenReturn(List.of(bus));

        mockMvc.perform(get("/admin/allbusses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busses[0].busno", is("AP01")));
    }
}
