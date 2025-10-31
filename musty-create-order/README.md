

                // Circuit Breaker to handle Solr failures
                .circuitBreaker()
                .resilience4jConfiguration()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(5000)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .end()
                .process("solrReindexProcessor")
                .onFallback()
                .log(LoggingLevel.ERROR, "Fallback triggered: Solr is unavailable!")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
                .setBody(simple("{\"error\": \"Solr is temporarily unavailable, retry later\"}"))
                .end()
                // End of Circuit Breaker