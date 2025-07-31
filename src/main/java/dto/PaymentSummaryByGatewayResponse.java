package dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public record PaymentSummaryByGatewayResponse(Map<String, PaymentSummary> paymentSummaryByGateway) {
    public String toJson() {
        Map<String, Object> gatewaysJson = new LinkedHashMap<>();
        paymentSummaryByGateway.forEach((gateway, summary) -> {
            var summaryJson = new LinkedHashMap<String, Object>();
            summaryJson.put("totalRequests", summary.totalRequests());
            summaryJson.put("totalAmount", summary.totalAmount());
            gatewaysJson.put(gateway, summaryJson);
        });
        var gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(gatewaysJson);
    }
}
