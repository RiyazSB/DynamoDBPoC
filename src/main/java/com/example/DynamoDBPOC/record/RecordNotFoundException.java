package com.example.DynamoDBPOC.record;

public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(String id) {
        super("Record not found with id: " + id);
    }
}

