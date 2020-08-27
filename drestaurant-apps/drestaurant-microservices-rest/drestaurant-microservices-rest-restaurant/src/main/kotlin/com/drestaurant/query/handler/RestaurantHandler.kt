package com.drestaurant.query.handler

import com.drestaurant.query.FindAllRestaurantsQuery
import com.drestaurant.query.FindRestaurantQuery
import com.drestaurant.query.model.MenuItemEmbedable
import com.drestaurant.query.model.RestaurantEntity
import com.drestaurant.query.model.RestaurantMenuEmbedable
import com.drestaurant.query.repository.RestaurantRepository
import com.drestaurant.restaurant.domain.api.RestaurantCreatedEvent
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.AllowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ReplayStatus
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("restaurant")
internal class RestaurantHandler(private val repository: RestaurantRepository, private val queryUpdateEmitter: QueryUpdateEmitter) {


    @EventHandler
    @AllowReplay(false)
    fun handle(event: RestaurantCreatedEvent, @SequenceNumber aggregateVersion: Long, replayStatus: ReplayStatus) {

        val menuItems = ArrayList<MenuItemEmbedable>()
        for (item in event.menu.menuItems) {
            val menuItem = MenuItemEmbedable(item.id, item.name, item.price.amount)
            menuItems.add(menuItem)
        }
        val menu = RestaurantMenuEmbedable(menuItems, event.menu.menuVersion)

        val record = RestaurantEntity(event.aggregateIdentifier.identifier, aggregateVersion, event.name, menu, emptyList())
        repository.save(record)

        /* sending it to subscription queries of type FindRestaurantQuery, but only if the restaurant id matches. */
        queryUpdateEmitter.emit(
                FindRestaurantQuery::class.java,
                { query -> query.restaurantId == event.aggregateIdentifier },
                record
        )

        /* sending it to subscription queries of type FindAllRestaurants. */
        queryUpdateEmitter.emit(
                FindAllRestaurantsQuery::class.java,
                { true },
                record
        )
    }


    @QueryHandler
    fun handle(query: FindRestaurantQuery): RestaurantEntity = repository.findById(query.restaurantId.identifier).orElseThrow { UnsupportedOperationException("Restaurant with id '${query.restaurantId}' not found") }

    @QueryHandler
    fun handle(query: FindAllRestaurantsQuery): MutableIterable<RestaurantEntity> = repository.findAll()


}
