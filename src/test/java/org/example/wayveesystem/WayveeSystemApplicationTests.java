package org.example.wayveesystem;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Disabled in CI/CD build when local PostgreSQL database instance is not running")
class WayveeSystemApplicationTests {

    @Test
    void contextLoads() {
    }

}
