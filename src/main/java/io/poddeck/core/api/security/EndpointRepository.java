package io.poddeck.core.api.security;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.util.List;

@Component
public final class EndpointRepository {
  @Autowired
  private RequestMappingHandlerMapping requestMappingHandlerMapping;
  private final List<Endpoint> endpoints = Lists.newArrayList();

  private void load() {
    for (var entry : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
      var info = entry.getKey();
      var path = "/v1" + info.getPatternValues().iterator().next();
      var annotations = Lists.<Class<? extends Annotation>>newArrayList();
      for (var annotation : entry.getValue().getMethod().getAnnotations()) {
        annotations.add(annotation.annotationType());
      }
      endpoints.add(Endpoint.create(path, annotations));
    }
  }

  public List<Endpoint> findAnnotatedEndpoints(
    Class<? extends Annotation> annotation
  ) {
    if (endpoints.isEmpty()) {
      load();
    }
    return endpoints.stream()
      .filter(endpoint -> endpoint.annotations().contains(annotation))
      .toList();
  }

  public List<Endpoint> all() {
    if (endpoints.isEmpty()) {
      load();
    }
    return List.copyOf(endpoints);
  }
}
