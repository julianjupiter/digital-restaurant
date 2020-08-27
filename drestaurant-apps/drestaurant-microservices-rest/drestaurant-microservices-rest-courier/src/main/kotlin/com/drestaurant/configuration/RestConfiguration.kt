package com.drestaurant.configuration

import com.drestaurant.query.model.CourierEntity
import com.drestaurant.query.model.CourierOrderEntity
import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter

@Configuration
class RestConfiguration : RepositoryRestConfigurerAdapter() {

    override fun configureRepositoryRestConfiguration(config: RepositoryRestConfiguration) {
        config.exposeIdsFor(CourierEntity::class.java, CourierOrderEntity::class.java)
    }
}