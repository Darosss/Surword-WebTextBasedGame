package com.textbasedgame.utils;

import org.bson.Document;

public class AggregationUtils {
    private AggregationUtils(){}

    public static Document cleanMongoDocument(Document doc) {
        if (doc.containsKey("_id")) {
            doc.put("_id", doc.get("_id").toString());
        }
        doc.remove("_t");
        return doc;
    }
}
