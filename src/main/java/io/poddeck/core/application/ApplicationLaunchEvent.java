package io.poddeck.core.application;

import io.poddeck.common.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class ApplicationLaunchEvent extends Event {
}