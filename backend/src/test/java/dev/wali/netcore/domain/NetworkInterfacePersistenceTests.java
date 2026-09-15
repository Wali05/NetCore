package dev.wali.netcore.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class NetworkInterfacePersistenceTests {

    @Autowired
    private EntityManager entityManager;

    @Test
    void rejectsDuplicateInterfaceNameOnSameDevice() {
        Device device = new Device("server-01", DeviceType.SERVER);
        entityManager.persist(device);

        entityManager.persist(new NetworkInterface(
                device,
                "eth0",
                "AA:BB:CC:DD:EE:01"
        ));
        entityManager.flush();

        assertThrows(PersistenceException.class, () -> {
            entityManager.persist(new NetworkInterface(
                    device,
                    "eth0",
                    "AA:BB:CC:DD:EE:02"
            ));
            entityManager.flush();
        });
    }
}
