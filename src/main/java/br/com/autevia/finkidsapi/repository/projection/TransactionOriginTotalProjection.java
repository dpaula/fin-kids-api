package br.com.autevia.finkidsapi.repository.projection;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import java.math.BigDecimal;

public interface TransactionOriginTotalProjection {

    TransactionOrigin getOrigin();

    BigDecimal getTotal();
}
