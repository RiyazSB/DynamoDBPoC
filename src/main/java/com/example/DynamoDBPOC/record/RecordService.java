package com.example.DynamoDBPOC.record;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Service
public class RecordService {

    private final DynamoDbTable<RecordItem> recordTable;

    public RecordService(DynamoDbTable<RecordItem> recordTable) {
        this.recordTable = recordTable;
    }

    @Caching(
            put = @CachePut(cacheNames = "recordsById", key = "#result.id"),
            evict = @CacheEvict(cacheNames = "recordsAll", allEntries = true)
    )
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

    @Cacheable(cacheNames = "recordsById", key = "#id")
    public RecordItem getRecord(String id) {
        RecordItem item = recordTable.getItem(Key.builder().partitionValue(id).build());
        if (item == null) {
            throw new RecordNotFoundException(id);
        }
        return item;
    }

    @Cacheable(cacheNames = "recordsAll")
    public List<RecordItem> getAllRecords() {
        List<RecordItem> records = new ArrayList<>();
        recordTable.scan().items().forEach(records::add);
        return records;
    }

    @Caching(
            put = @CachePut(cacheNames = "recordsById", key = "#id"),
            evict = @CacheEvict(cacheNames = "recordsAll", allEntries = true)
    )
    public RecordItem updateRecord(String id, RecordRequest request) {
        RecordItem existing = getRecord(id);
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(Instant.now().toString());

        recordTable.updateItem(existing);
        return existing;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "recordsById", key = "#id"),
            @CacheEvict(cacheNames = "recordsAll", allEntries = true)
    })
    public void deleteRecord(String id) {
        RecordItem existing = getRecord(id);
        recordTable.deleteItem(existing);
    }
}


