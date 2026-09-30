package com.agrogestao.finance;

import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.finance.dto.InstallmentInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Regras do parcelamento de uma compra/venda, comuns a contas a pagar e a receber. */
public final class InstallmentPlan {

    public static final int MAX_INSTALLMENTS = 48;

    private InstallmentPlan() {
    }

    /** Valida forma de pagamento e parcelas; devolve a forma efetiva (padrão: à vista). */
    public static PaymentType validate(PaymentType paymentType, List<InstallmentInput> installments,
            BigDecimal total, LocalDate operationDate) {
        PaymentType effective = paymentType != null ? paymentType : PaymentType.CASH;
        boolean hasInstallments = installments != null && !installments.isEmpty();
        if (effective == PaymentType.CASH) {
            if (hasInstallments) {
                throw new BusinessException("Operação à vista não tem parcelas.");
            }
            return effective;
        }
        if (!hasInstallments) {
            throw new BusinessException("Informe ao menos uma parcela para a operação a prazo.");
        }
        if (installments.size() > MAX_INSTALLMENTS) {
            throw new BusinessException("No máximo " + MAX_INSTALLMENTS + " parcelas.");
        }
        BigDecimal sum = installments.stream().map(InstallmentInput::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(total) != 0) {
            throw new BusinessException("A soma das parcelas (R$ " + sum.toPlainString()
                    + ") precisa ser igual ao total da operação (R$ " + total.toPlainString() + ").");
        }
        if (installments.stream().anyMatch(i -> i.dueDate().isBefore(operationDate))) {
            throw new BusinessException("O vencimento das parcelas não pode ser anterior à data da operação.");
        }
        return effective;
    }
}
