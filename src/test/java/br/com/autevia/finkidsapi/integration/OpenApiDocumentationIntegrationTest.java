package br.com.autevia.finkidsapi.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiSpecWithMainPathsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.info.title").value("Fin Kids API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/transactions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/automation/transactions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transactions'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/automation/transactions'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/balance']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/monthly-summary']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/child-view']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/parent-view']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/goals']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/bonus-rule']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/user-links']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/user-links/{userId}']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.AutomationBearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.UserBearerAuth").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists());
    }

    @Test
    void shouldExposeSwaggerUiPage() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
