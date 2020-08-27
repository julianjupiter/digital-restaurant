package com.drestaurant.query.handler

import com.drestaurant.order.domain.api.*
import com.drestaurant.order.domain.api.model.OrderState
import com.drestaurant.query.FindAllOrdersQuery
import com.drestaurant.query.FindOrderQuery
import com.drestaurant.query.model.OrderEntity
import com.drestaurant.query.model.OrderItemEmbedable
import com.drestaurant.query.repository.OrderRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.AllowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Component
import java.util.*

@Component
@ProcessingGroup("order")
internal class OrderHandler(private val orderRepository: OrderRepository, private val queryUpdateEmitter: QueryUpdateEmitter) {

    @EventHandler
    fun handle(event: OrderCreationInitiatedEvent, @SequenceNumber aggregateVersion: Long) {
        val orderItems = ArrayList<OrderItemEmbedable>()
        for (item in event.orderDetails.lineItems) {
            val orderItem = OrderItemEmbedable(item.menuItemId, item.name, item.price.amount, item.quantity)
            orderItems.add(orderItem)
        }
        val record = OrderEntity(event.aggregateIdentifier.identifier, aggregateVersion, orderItems, OrderState.CREATE_PENDING)
        orderRepository.save(record)

        /* sending it to subscription queries of type FindOrderQuery, but only if the order id matches. */
        queryUpdateEmitter.emit(
                FindOrderQuery::class.java,
                { query -> query.orderId == event.aggregateIdentifier },
                record
        )

        /* sending it to subscription queries of type FindAllOrders. */
        queryUpdateEmitter.emit(
                FindAllOrdersQuery::class.java,
                { true },
                record
        )

    }

    @EventHandler
    @AllowReplay(false)
    fun handle(event: OrderVerifiedByCustomerEvent, @SequenceNumber aggregateVersion: Long) {
        val orderEntity = orderRepository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Order with id '${event.aggregateIdentifier}' not found") }
        orderEntity.state = OrderState.VERIFIED_BY_CUSTOMER
        orderEntity.aggregateVersion = aggregateVersion
        orderRepository.save(orderEntity)

        /* sending it to subscription queries of type FindOrderQuery, but only if the order id matches. */
        queryUpdateEmitter.emit(
                FindOrderQuery::class.java,
                { query -> query.orderId == event.aggregateIdentifier },
                orderEntity
        )

        /* sending it to subscription queries of type FindAllOrders. */
        queryUpdateEmitter.emit(
                FindAllOrdersQuery::class.java,
                { true },
                orderEntity
        )
    }

    @EventHandler
    @AllowReplay(false)
    fun handle(event: OrderVerifiedByRestaurantEvent, @SequenceNumber aggregateVersion: Long) {
        val orderEntity = orderRepository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Order with id '${event.aggregateIdentifier}' not found") }
        orderEntity.aggregateVersion = aggregateVersion
        orderEntity.state = OrderState.VERIFIED_BY_RESTAURANT
        orderRepository.save(orderEntity)

        /* sending it to subscription queries of type FindOrderQuery, but only if the order id matches. */
        queryUpdateEmitter.emit(
                FindOrderQuery::class.java,
                { query -> query.orderId == event.aggregateIdentifier },
                orderEntity
        )

        /* sending it to subscription queries of type FindAllOrders. */
        queryUpdateEmitter.emit(
                FindAllOrdersQuery::class.java,
                { true },
                orderEntity
        )
    }

    @EventHandler
    @AllowReplay(false)
    fun handle(event: OrderPreparedEvent, @SequenceNumber aggregateVersion: Long) {
        val orderEntity = orderRepository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Order with id '${event.aggregateIdentifier}' not found") }
        orderEntity.aggregateVersion = aggregateVersion
        orderEntity.state = OrderState.PREPARED
        orderRepository.save(orderEntity)

        /* sending it to subscription queries of type FindOrderQuery, but only if the order id matches. */
        queryUpdateEmitter.emit(
                FindOrderQuery::class.java,
                { query -> query.orderId == event.aggregateIdentifier },
                orderEntity
        )

        /* sending it to subscription queries of type FindAllOrders. */
        queryUpdateEmitter.emit(
                FindAllOrdersQuery::class.java,
                { true },
                orderEntity
        )
    }

    @EventHandler
    @AllowReplay(false)
    fun handle(event: OrderReadyForDeliveryEvent, @SequenceNumber aggregateVersion: Long) {
        val orderEntity = orderRepository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Order with id '${event.aggregateIdentifier}' not found") }
        orderEntity.aggregateVersion = aggregateVersion
        orderEntity.state = OrderState.READY_FOR_DELIVERY
        orderRepository.save(orderEntity)

        /* sending it to subscription queries of type FindOrderQuery, but only if the order id matches. */
        queryUpdateEmitter.emit(
                FindOrderQuery::class.java,
                { query -> query.orderId == event.aggregateIdentifier },
                orderEntity
        )

        /* sending it to subscription queries of type FindAllOrders. */
        queryUpdateEmitter.emit(
                FindAllOrdersQuery::class.java,
                { true },
                orderEntity
        )
    }

    @EventHandler
    @AllowReplay(false)
    fun handle(event: OrderDeliveredEvent, @SequenceNumber aggregateVersion: Long) {
        val orderEntity = orderRepository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Order with id '${event.aggregateIdentifier}' not found") }
        orderEntity.aggregateVersion = aggregateVersion
        orderEntity.state = OrderState.DELIVERED
        orderRepository.save(orderEntity)

        /* sending it to subscription queries of type FindOrderQuery, but only if the order id matches. */
        queryUpdateEmitter.emit(
                FindOrderQuery::class.java,
                { query -> query.orderId == event.aggregateIdentifier },
                orderEntity
        )

        /* sending it to subscription queries of type FindAllOrders. */
        queryUpdateEmitter.emit(
                FindAllOrdersQuery::class.java,
                { true },
                orderEntity
        )
    }

    @EventHandler
    @AllowReplay(false)
    fun handle(event: OrderRejectedEvent, @SequenceNumber aggregateVersion: Long) {
        val orderEntity = orderRepository.findById(event.aggregateIdentifier.identifier).orElseThrow { UnsupportedOperationException("Order with id '${event.aggregateIdentifier}' not found") }
        orderEntity.aggregateVersion = aggregateVersion
        orderEntity.state = OrderState.REJECTED
        orderRepository.save(orderEntity)

        /* sending it to subscription queries of type FindOrderQuery, but only if the order id matches. */
        queryUpdateEmitter.emit(
                FindOrderQuery::class.java,
                { query -> query.orderId == event.aggregateIdentifier },
                orderEntity
        )

        /* sending it to subscription queries of type FindAllOrders. */
        queryUpdateEmitter.emit(
                FindAllOrdersQuery::class.java,
                { true },
                orderEntity
        )
    }

    @QueryHandler
    fun handle(query: FindOrderQuery): OrderEntity = orderRepository.findById(query.orderId.identifier).orElseThrow { UnsupportedOperationException("Order with id '${query.orderId}' not found") }

    @QueryHandler
    fun handle(query: FindAllOrdersQuery): MutableIterable<OrderEntity> = orderRepository.findAll()

}
