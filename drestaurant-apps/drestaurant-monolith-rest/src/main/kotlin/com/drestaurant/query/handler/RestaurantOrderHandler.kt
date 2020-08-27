package com.drestaurant.query.handler

import com.drestaurant.query.FindAllRestaurantOrdersQuery
import com.drestaurant.query.FindRestaurantOrderQuery
import com.drestaurant.query.model.RestaurantOrderEntity
import com.drestaurant.query.model.RestaurantOrderItemEmbedable
import com.drestaurant.query.repository.RestaurantOrderRepository
import com.drestaurant.query.repository.RestaurantRepository
import com.drestaurant.restaurant.domain.api.RestaurantOrderCreatedEvent
import com.drestaurant.restaurant.domain.api.RestaurantOrderPreparedEvent
import com.drestaurant.restaurant.domain.api.model.RestaurantOrderState
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.AllowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ResetHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("restaurantorder")
internal class RestaurantOrderHandler(private val repository: RestaurantOrderRepository, private val restaurantRepository: RestaurantRepository, private val queryUpdateEmitter: QueryUpdateEmitter) {

    @EventHandler
    /* It is possible to allow or prevent some handlers from being replayed/reset */
    @AllowReplay(true)
    fun handle(event: RestaurantOrderCreatedEvent, @SequenceNumber aggregateVersion: Long) {
        val restaurantOrderItems = java.util.ArrayList<RestaurantOrderItemEmbedable>()
        for (item in event.lineItems) {
            val restaurantOrderItem = RestaurantOrderItemEmbedable(item.menuItemId, item.name, item.quantity)
            restaurantOrderItems.add(restaurantOrderItem)
        }
        val restaurantEntity = restaurantRepository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Restaurant with id '" + event.aggregateIdentifier + "' not found") }
        val record = RestaurantOrderEntity(event.restaurantOrderId.identifier, aggregateVersion, restaurantOrderItems, restaurantEntity, RestaurantOrderState.CREATED)
        repository.save(record)
    }

    @EventHandler
    /* It is possible to allow or prevent some handlers from being replayed/reset */
    @AllowReplay(true)
    fun handle(event: RestaurantOrderPreparedEvent, @SequenceNumber aggregateVersion: Long) {
        val record = repository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Restaurant order with id '" + event.aggregateIdentifier + "' not found") }
        record.state = RestaurantOrderState.PREPARED
        repository.save(record)

        /* sending it to subscription queries of type FindRestaurantOrderQuery, but only if the restaurant order id matches. */
        queryUpdateEmitter.emit(
                FindRestaurantOrderQuery::class.java,
                { query -> query.restaurantOrderId == event.aggregateIdentifier },
                record
        )

        /* sending it to subscription queries of type FindAllRestaurantOrders. */
        queryUpdateEmitter.emit(
                FindAllRestaurantOrdersQuery::class.java,
                { query -> true },
                record
        )
    }

    /* Will be called before replay/reset starts. Do pre-reset logic, like clearing out the Projection table */
    @ResetHandler
    fun onReset() = repository.deleteAll()

    @QueryHandler
    fun handle(query: FindRestaurantOrderQuery): RestaurantOrderEntity = repository.findById(query.restaurantOrderId.identifier).orElseThrow { UnsupportedOperationException("Restaurant order with id '" + query.restaurantOrderId + "' not found") }

    @QueryHandler
    fun handle(query: FindAllRestaurantOrdersQuery): MutableIterable<RestaurantOrderEntity> = repository.findAll()
}
