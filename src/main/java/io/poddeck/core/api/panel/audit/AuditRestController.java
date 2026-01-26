package io.poddeck.core.api.panel.audit;

import com.google.common.collect.Maps;
import io.poddeck.common.*;
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
public class AuditRestController extends ClusterRestController {
  protected AuditRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleAuditInformation(Audit audit) {
    var information = Maps.<String, Object>newHashMap();
    information.put("raw", audit.getRaw());
    information.put("controls", audit.getControlsList()
      .stream().map(this::assembleControlInformation).toList());
    information.put("totals", assembleTotalsInformation(audit.getTotals()));
    information.put("time", audit.getTime());
    return information;
  }

  protected Map<String, Object> assembleControlInformation(AuditControl control) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", control.getId());
    information.put("version", control.getVersion());
    information.put("detected_version", control.getDetectedVersion());
    information.put("text", control.getText());
    information.put("node_type", control.getNodeType());
    information.put("tests", control.getTestsList()
      .stream().map(this::assembleTestInformation).toList());
    information.put("totals", assembleTotalsInformation(control.getTotals()));
    return information;
  }

  protected Map<String, Object> assembleTestInformation(AuditTest test) {
    var information = Maps.<String, Object>newHashMap();
    information.put("section", test.getSection());
    information.put("type", test.getType());
    information.put("description", test.getDescription());
    information.put("results", test.getResultsList()
      .stream().map(this::assembleResultInformation).toList());
    information.put("totals", assembleTotalsInformation(test.getTotals()));
    return information;
  }

  protected Map<String, Object> assembleResultInformation(AuditResult result) {
    var information = Maps.<String, Object>newHashMap();
    information.put("test_number", result.getTestNumber());
    information.put("test_description", result.getTestDescription());
    information.put("audit", result.getAudit());
    information.put("audit_env", result.getAuditEnv());
    information.put("audit_config", result.getAuditConfig());
    information.put("type", result.getType());
    information.put("remediation", result.getRemediation());
    information.put("test_info", result.getTestInfo());
    information.put("status", result.getStatus());
    information.put("actual_value", result.getActualValue());
    information.put("scored", result.getScored());
    information.put("is_multiple", result.getIsMultiple());
    information.put("expected_result", result.getExpectedResult());
    information.put("reason", result.getReason());
    return information;
  }

  protected Map<String, Object> assembleTotalsInformation(AuditTotals totals) {
    var information = Maps.<String, Object>newHashMap();
    information.put("total_pass", totals.getTotalPass());
    information.put("total_fail", totals.getTotalFail());
    information.put("total_warn", totals.getTotalWarn());
    information.put("total_info", totals.getTotalInfo());
    return information;
  }
}