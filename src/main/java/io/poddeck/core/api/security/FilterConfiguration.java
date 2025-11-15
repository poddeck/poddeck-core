package io.poddeck.core.api.security;

import io.poddeck.core.api.security.panel.PanelAuthorizationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilterConfiguration {
  @Autowired
  private EquipmentFilter equipmentFilter;
  @Autowired
  private PanelAuthorizationFilter panelAuthorizationFilter;

  @Bean
  public FilterRegistrationBean<EquipmentFilter> provideEquipmentFilter() {
    var registrationBean = new FilterRegistrationBean<EquipmentFilter>();
    registrationBean.setFilter(equipmentFilter);
    registrationBean.setOrder(1);
    return registrationBean;
  }

  @Bean
  public FilterRegistrationBean<PanelAuthorizationFilter> providePanelAuthorizationFilter() {
    var registrationBean = new FilterRegistrationBean<PanelAuthorizationFilter>();
    registrationBean.setFilter(panelAuthorizationFilter);
    registrationBean.setOrder(2);
    return registrationBean;
  }
}
