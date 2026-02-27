package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.TransactionService;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionCommand;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.service.dto.TransactionItemResult;
import br.com.autevia.finkidsapi.service.dto.TransactionListResult;
import br.com.autevia.finkidsapi.web.dto.CreateTransactionRequest;
import br.com.autevia.finkidsapi.web.dto.CreateTransactionResponse;
import br.com.autevia.finkidsapi.web.dto.TransactionItemResponse;
import br.com.autevia.finkidsapi.web.dto.TransactionListResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTransactionResponse create(@RequestBody CreateTransactionRequest request) {
        CreateTransactionResult result = transactionService.createTransaction(
                new CreateTransactionCommand(
                        request.accountId(),
                        request.type(),
                        request.origin(),
                        request.amount(),
                        request.description(),
                        request.evidenceReference(),
                        request.occurredAt()
                )
        );

        return new CreateTransactionResponse(result.transactionId(), result.updatedBalance());
    }

    @GetMapping
    public TransactionListResponse list(
            @RequestParam Long accountId,
            @RequestParam Instant start,
            @RequestParam Instant end
    ) {
        TransactionListResult result = transactionService.listTransactions(accountId, start, end);
        List<TransactionItemResponse> items = result.transactions().stream()
                .map(this::toResponse)
                .toList();

        return new TransactionListResponse(result.currentBalance(), items);
    }

    private TransactionItemResponse toResponse(TransactionItemResult item) {
        return new TransactionItemResponse(
                item.transactionId(),
                item.accountId(),
                item.type(),
                item.origin(),
                item.amount(),
                item.description(),
                item.evidenceReference(),
                item.occurredAt()
        );
    }
}
