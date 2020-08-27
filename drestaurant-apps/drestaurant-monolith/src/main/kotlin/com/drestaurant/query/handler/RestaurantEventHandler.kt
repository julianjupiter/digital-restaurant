package com.drestaurant.query.handler

import com.drestaurant.query.model.MenuItemEmbedable
import com.drestaurant.query.model.RestaurantEntity
import com.drestaurant.query.model.RestaurantMenuEmbedable
import com.drestaurant.query.repository.RestaurantRepository
import com.drestaurant.restaurant.domain.api.RestaurantCreatedEvent
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.AllowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ResetHandler
import org.axonframework.eventhandling.SequenceNumber
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component
import java.util.*

@Component
@ProcessingGroup("restaurant")
internal class RestaurantEventHandler(private val repository: RestaurantRepository, private val messagingTemplate: SimpMessageSendingOperations) {

    @EventHandler
    @AllowReplay(true)
    fun handle(event: RestaurantCreatedEvent, @SequenceNumber aggregateVersion: Long) {

        val menuItems = ArrayList<MenuItemEmbedable>()
        for (item in event.menu.menuItems) {
            val menuItem = MenuItemEmbedable(item.id, item.name, item.price.amount)
            menuItems.add(menuItem)
        }
        val menu = RestaurantMenuEmbedable(menuItems, event.menu.menuVersion)
        repository.save(RestaurantEntity(event.aggregateIdentifier.identifier, aggregateVersion, event.name, menu))
        messagingTemplate.convertAndSend("/topic/restaurants.updates", event)
    }

    /* Will be called before replay/reset starts. Do pre-reset logic, like clearing out the Projection table */
    @ResetHandler
    fun onReset() = repository.deleteAll()

}
