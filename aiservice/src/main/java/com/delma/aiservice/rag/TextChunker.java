package com.delma.aiservice.rag;


import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;

    public List<String> chunk(String text){
        List<String> chunks = new ArrayList<>();

        if(text == null || text.isBlank()) return chunks;

        String cleaned = text.replaceAll("\\s+"," ").trim();

        int start = 0;
        while(start < cleaned.length()){
            int end = Math.min(start + CHUNK_SIZE,cleaned.length());
            String chunk = cleaned.substring(start,end).trim();

            if(!chunk.isBlank()){
                chunks.add(chunk);
            }

            start += (CHUNK_SIZE - OVERLAP);
        }
        return chunks;
    }
}
