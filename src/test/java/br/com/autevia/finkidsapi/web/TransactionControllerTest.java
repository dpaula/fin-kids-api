package br.com.autevia.finkidsapi.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.BusinessRuleException;
import br.com.autevia.finkidsapi.domain.exception.DuplicateTransactionException;
import br.com.autevia.finkidsapi.service.TransactionService;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.service.dto.TransactionItemResult;
import br.com.autevia.finkidsapi.service.dto.TransactionListResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import(ApiExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldCreateTransaction() throws Exception {
        when(transactionService.createTransaction(any()))
                .thenReturn(new CreateTransactionResult(100L, new BigDecimal("150.00")));

        String payload = """
                {
                  "accountId": 1,
                  "type": "DEPOSIT",
                  "origin": "MANUAL",
                  "amount": 100.00,
                  "description": "Mesada",
                  "occurredAt": "2026-02-27T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(100))
                .andExpect(jsonPath("$.updatedBalance").value(150.00));
    }

    @Test
    void shouldReturnUnprocessableWhenBusinessRuleFails() throws Exception {
        when(transactionService.createTransaction(any()))
                .thenThrow(new BusinessRuleException("Saldo insuficiente para realizar saque."));

        String payload = """
                {
                  "accountId": 1,
                  "type": "WITHDRAW",
                  "origin": "MANUAL",
                  "amount": 100.00,
                  "description": "Compra",
                  "occurredAt": "2026-02-27T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente para realizar saque."));
    }

    @Test
    void shouldReturnConflictWhenEvidenceReferenceIsDuplicated() throws Exception {
        when(transactionService.createTransaction(any()))
                .thenThrow(new DuplicateTransactionException("Transacao duplicada para a mesma evidencia informada."));

        String payload = """
                {
                  "accountId": 1,
                  "type": "DEPOSIT",
                  "origin": "WHATSAPP",
                  "amount": 80.00,
                  "description": "Deposito via comprovante",
                  "evidenceReference": "wa-media-001"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Transacao duplicada para a mesma evidencia informada."));
    }

    @Test
    void shouldListTransactions() throws Exception {
        TransactionItemResult item = new TransactionItemResult(
                77L,
                1L,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                new BigDecimal("40.00"),
                "Mesada",
                null,
                Instant.parse("2026-02-20T10:00:00Z")
        );

        when(transactionService.listTransactions(any(), any(), any()))
                .thenReturn(new TransactionListResult(new BigDecimal("90.00"), List.of(item)));

        mockMvc.perform(get("/api/v1/transactions")
                        .param("accountId", "1")
                        .param("start", "2026-02-01T00:00:00Z")
                        .param("end", "2026-02-28T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(90.00))
                .andExpect(jsonPath("$.transactions[0].transactionId").value(77))
                .andExpect(jsonPath("$.transactions[0].type").value("DEPOSIT"));
    }

    @Test
    void shouldReturnBadRequestWhenCreatePayloadIsInvalid() throws Exception {
        String payload = """
                {
                  "accountId": 1,
                  "type": "DEPOSIT",
                  "origin": "MANUAL",
                  "amount": 0,
                  "description": ""
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
