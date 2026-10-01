package com.agrogestao.livestock.dto;

import com.agrogestao.finance.dto.InstallmentView;
import java.util.List;

public record TradeDetailResponse(TradeResponse trade, List<AnimalResponse> animals, List<InstallmentView> installments) {
}
