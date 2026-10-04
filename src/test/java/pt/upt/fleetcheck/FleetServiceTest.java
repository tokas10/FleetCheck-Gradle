package pt.upt.fleetcheck;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FleetServiceTest {
    private final FleetService service = new FleetService();

    @Test
    void vehicleExactlyAtIntervalNeedsService() {
        Vehicle v = new Vehicle("V3", "Diesel", 65000, 55000, 10000);
        assertTrue(service.needsService(v));
    }

    @Test
    void vehicleBelowIntervalDoesNotNeedService() {
        Vehicle v = new Vehicle("V2", "Hybrid", 22000, 15000, 10000);
        assertFalse(service.needsService(v));
    }
}