package com.typeahead.batch;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchWriteController {

    private final BatchWriteService batchWriteService;

    public BatchWriteController(BatchWriteService batchWriteService) {
        this.batchWriteService = batchWriteService;
    }

    @GetMapping("/batch/debug")
    public BatchDebugResponse debug() {
        return batchWriteService.debug();
    }

    @PostMapping("/batch/flush")
    public BatchWriteResponse flush() {
        BatchFlushResult result = batchWriteService.flushNow();
        return new BatchWriteResponse(
            result.status().name(),
            result.rawEventCount(),
            result.uniqueQueryCount(),
            result.dbWriteCount()
        );
    }
}
