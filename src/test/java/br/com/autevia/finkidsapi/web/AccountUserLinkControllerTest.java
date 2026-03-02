package br.com.autevia.finkidsapi.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.security.AccountAuthorizationService;
import br.com.autevia.finkidsapi.service.AccountUserLinkService;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkListResult;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AccountUserLinkController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@Import(ApiExceptionHandler.class)
class AccountUserLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountUserLinkService accountUserLinkService;

    @MockitoBean
    private AccountAuthorizationService accountAuthorizationService;

    @BeforeEach
    void setupAuthorization() {
        when(accountAuthorizationService.canWrite(anyLong())).thenReturn(true);
    }

    @Test
    void shouldListLinks() throws Exception {
        AccountUserLinkResult link = new AccountUserLinkResult(
                10L,
                1L,
                2L,
                "Maria Silva",
                "maria@email.com",
                UserRole.PARENT,
                Instant.parse("2026-03-02T14:00:00Z")
        );
        when(accountUserLinkService.listLinks(1L))
                .thenReturn(new AccountUserLinkListResult(1L, List.of(link)));

        mockMvc.perform(get("/api/v1/accounts/1/user-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.links[0].userId").value(2))
                .andExpect(jsonPath("$.links[0].profileRole").value("PARENT"));
    }

    @Test
    void shouldUpsertLink() throws Exception {
        when(accountUserLinkService.upsertLink(1L, 2L, UserRole.CHILD))
                .thenReturn(new AccountUserLinkResult(
                        10L,
                        1L,
                        2L,
                        "Lucas",
                        "lucas@email.com",
                        UserRole.CHILD,
                        Instant.parse("2026-03-02T14:00:00Z")
                ));

        mockMvc.perform(put("/api/v1/accounts/1/user-links/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileRole": "CHILD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkId").value(10))
                .andExpect(jsonPath("$.profileRole").value("CHILD"));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/accounts/1/user-links/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenServiceThrowsNotFound() throws Exception {
        when(accountUserLinkService.upsertLink(1L, 999L, UserRole.PARENT))
                .thenThrow(new ResourceNotFoundException("Usuario nao encontrado para id=999"));

        mockMvc.perform(put("/api/v1/accounts/1/user-links/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileRole": "PARENT"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario nao encontrado para id=999"));
    }
}
