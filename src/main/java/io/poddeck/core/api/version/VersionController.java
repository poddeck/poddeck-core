package io.poddeck.core.api.version;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public final class VersionController {
  @RequestMapping(path = "/", method = RequestMethod.GET)
  public Map<String, Object> version() {
    return Map.of("success", true, "version", "V1");
  }
}
