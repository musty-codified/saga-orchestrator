package com.mustycodified.musty_process_payment.commonlib.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductResponse {
    private VariantProduct data;

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VariantProduct {
        private Integer id;
        private String uid;
        private BigDecimal price;
        private String productName;
        private UnitOfMeasurement unitOfMeasurement;
        private Stock stock;
    }

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnitOfMeasurement {
        private Integer id;
        private String label;
    }

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stock {
        private Integer id;
        private Long quantity;
    }
}


