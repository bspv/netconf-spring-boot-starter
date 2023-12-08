package com.bazzi.netconf.autoconfigure;

import com.bazzi.netconf.config.NetConfProperties;
import com.bazzi.netconf.service.NetConfService;
import com.bazzi.netconf.service.impl.NetConfServiceImpl;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@EnableConfigurationProperties(NetConfProperties.class)
@ConditionalOnProperty(prefix = "netconf", name = "path")
public class NetConfAutoConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    NetConfService buildNetConfService() {
        return new NetConfServiceImpl();
    }

}
