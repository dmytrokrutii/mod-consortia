package org.folio.consortia.config;

import org.folio.consortia.domain.converter.ConsortiumConverter;
import org.folio.consortia.domain.converter.TenantConverter;
import org.folio.consortia.domain.converter.UserTenantConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new TenantConverter());
    registry.addConverter(new UserTenantConverter());
    registry.addConverter(new ConsortiumConverter());
  }
}
