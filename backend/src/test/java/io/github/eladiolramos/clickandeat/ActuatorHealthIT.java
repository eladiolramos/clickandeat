package io.github.eladiolramos.clickandeat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public class ActuatorHealthIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointREportsUp() throws Exception{
        // When: pido el estado de la aplicación
        // Then: responde 200 y el estado global es UP
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

}
