package com.codeages.javaskeletonserver.biz.queue.handler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class ConsumeHandlerRegistry {

    private final Map<String, List<ConsumeHandlerContainer>> handlers = new HashMap<>();

    public void register(String type, ConsumeHandler<?> handler, boolean log) {
        List<ConsumeHandlerContainer> list = handlers.getOrDefault(type, new LinkedList<>());
        list.add(new ConsumeHandlerContainer(handler, log));
        handlers.put(type, list);
    }

    public List<ConsumeHandlerContainer> getHandlers(String type) {
        return handlers.getOrDefault(type, null);
    }
}
