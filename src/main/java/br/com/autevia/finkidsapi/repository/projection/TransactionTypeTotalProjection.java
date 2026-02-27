package br.com.autevia.finkidsapi.repository.projection;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import java.math.BigDecimal;

public interface TransactionTypeTotalProjection {

    TransactionType getType();

    BigDecimal getTotal();
}
