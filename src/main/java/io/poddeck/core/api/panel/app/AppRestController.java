package io.poddeck.core.api.panel.app;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.poddeck.common.App;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.Map;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
public class AppRestController extends ClusterRestController {
  protected AppRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleAppInformation(App app) {
    var information = Maps.<String, Object>newHashMap();
    information.put("repository", app.getRepository());
    information.put("name", app.getName());
    var versions = Lists.<Map<String, Object>>newArrayList();
    for (var version : app.getVersionsList()) {
      var versionInformation = Maps.<String, Object>newHashMap();
      versionInformation.put("chart_version", version.getChartVersion());
      versionInformation.put("app_version", version.getAppVersion());
      versions.add(versionInformation);
    }
    information.put("versions", versions);
    information.put("description", app.getDescription());
    information.put("keywords", app.getKeywordsList());
    information.put("installed", app.getInstalled());
    return information;
  }
}
