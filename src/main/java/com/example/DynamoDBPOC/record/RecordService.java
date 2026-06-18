package com.example.DynamoDBPOC.record;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Service
public class RecordService {

    private final DynamoDbTable<RecordItem> recordTable;

    public RecordService(DynamoDbTable<RecordItem> recordTable) {
        this.recordTable = recordTable;
    }

    public RecordItem createRecord(RecordRequest request) {
        String now = Instant.now().toString();

        RecordItem item = new RecordItem();
        item.setId(UUID.randomUUID().toString());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        recordTable.putItem(item);
        return item;
    }

    public RecordItem getRecord(String id) {
        RecordItem item = recordTable.getItem(Key.builder().partitionValue(id).build());
        if (item == null) {
            throw new RecordNotFoundException(id);
        }
        return item;
    }

    public List<RecordItem> getAllRecords() {
        List<RecordItem> records = new ArrayList<>();
        recordTable.scan().items().forEach(records::add);
        return records;
    }

    public RecordItem updateRecord(String id, RecordRequest request) {
        RecordItem existing = getRecord(id);
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(Instant.now().toString());

        recordTable.updateItem(existing);
        return existing;
    }

    public void deleteRecord(String id) {
        RecordItem existing = getRecord(id);
        recordTable.deleteItem(existing);
    }
}


