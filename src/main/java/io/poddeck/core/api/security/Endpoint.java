package io.poddeck.core.api.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.lang.annotation.Annotation;
import java.util.List;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class Endpoint {
  private final String path;
  private final List<Class<? extends Annotation>> annotations;
}
