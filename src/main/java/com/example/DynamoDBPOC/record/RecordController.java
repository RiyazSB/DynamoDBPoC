package com.example.DynamoDBPOC.record;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecordItem createRecord(@Valid @RequestBody RecordRequest request) {
        return recordService.createRecord(request);
    }

    @GetMapping
    public List<RecordItem> getAllRecords() {
        return recordService.getAllRecords();
    }

    @GetMapping("/{id}")
    public RecordItem getRecord(@PathVariable String id) {
        return recordService.getRecord(id);
    }

    @PutMapping("/{id}")
    public RecordItem updateRecord(@PathVariable String id, @Valid @RequestBody RecordRequest request) {
        return recordService.updateRecord(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecord(@PathVariable String id) {
        recordService.deleteRecord(id);
    }
}


