package com.drestaurant.query.handler

import com.drestaurant.customer.domain.api.CustomerCreatedEvent
import com.drestaurant.customer.query.api.FindCustomerQuery
import com.drestaurant.customer.query.api.model.CustomerModel
import com.drestaurant.query.model.CustomerEntity
import com.drestaurant.query.repository.CustomerRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.AllowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ResetHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("customer")
internal class CustomerHandler(private val repository: CustomerRepository, private val queryUpdateEmitter: QueryUpdateEmitter) {

    private fun convert(record: CustomerEntity): CustomerModel = CustomerModel(record.id, record.aggregateVersion, record.firstName, record.lastName, record.orderLimit)

    @EventHandler
    /* It is possible to allow or prevent some handlers from being replayed/reset */
    @AllowReplay(true)
    fun handle(event: CustomerCreatedEvent, @SequenceNumber aggregateVersion: Long) {
        /* saving the record in our read/query model. */
        val record = CustomerEntity(event.aggregateIdentifier.identifier, aggregateVersion, event.name.firstName, event.name.lastName, event.orderLimit.amount)
        repository.save(record)

        /* sending it to subscription queries of type FindCustomerQuery, but only if the customer id matches. */
        queryUpdateEmitter.emit(
                FindCustomerQuery::class.java,
                { query -> query.customerId == event.aggregateIdentifier },
                convert(record)
        )
    }

    /* Will be called before replay/reset starts. Do pre-reset logic, like clearing out the Projection table */
    @ResetHandler
    fun onReset() = repository.deleteAll()

    @QueryHandler
    fun handle(query: FindCustomerQuery): CustomerModel = convert(repository.findById(query.customerId.identifier).orElseThrow { UnsupportedOperationException("Customer with id '" + query.customerId + "' not found") })
}
