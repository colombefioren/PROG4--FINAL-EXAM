package org.cocojojo.mg.endpoint.event.consumer.model;

import org.cocojojo.mg.PojaGenerated;
import org.cocojojo.mg.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
