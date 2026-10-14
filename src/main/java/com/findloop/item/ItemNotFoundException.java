package com.findloop.item;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(Long id) {
        super("Item report not found: " + id);
    }
}
