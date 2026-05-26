package com.school.app.dto.response;

import java.util.List;

public record DashboardResponse(
        List<SalesMetricResponse> metrics,
        List<PaymentMethodResponse> paymentMethods
) {
}
