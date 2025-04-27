#include "Order.h"

Order::Order(int id, int customerId, int distance):
 id(id), customerId(customerId), distance(distance), status(OrderStatus::PENDING), 
 collectorId(NO_VOLUNTEER), driverId(NO_VOLUNTEER) {}

int Order::getId() const 
{
    return id;
}

int Order::getCustomerId() const 
{
    return customerId;
}

void Order::setStatus(OrderStatus status) 
{
    this->status = status;
}

void Order::setCollectorId(int collectorId) 
{
    this->collectorId = collectorId;
}

void Order::setDriverId(int driverId) 
{
    this->driverId = driverId;
}

int Order::getCollectorId() const 
{
    return collectorId;
}

int Order::getDriverId() const 
{
    return driverId;
}

OrderStatus Order::getStatus() const
{
    return status;
}

int Order::getDistance() const 
{
    return distance;
}

const string Order::toString() const 
{ 
    std::string output;
    output+= ("OrderId: " + std::to_string(id) + "\n");
    output+= ("OrderStatus: " + orderStatusToString() + "\n");
    output+= ("CustomerID: " + std::to_string(customerId) + "\n");
    output+= ("Collector: " + volunteerIdToString(collectorId) + "\n");
    output+= ("Driver: " + volunteerIdToString(driverId));
    return output;
}

//Convert OrderStatus enum to string
string Order::orderStatusToString() const 
{
    switch (status) {
        case OrderStatus::PENDING:
            return "Pending";
        case OrderStatus::COLLECTING:
            return "Collecting";
        case OrderStatus::DELIVERING:
            return "Delivering";
        case OrderStatus::COMPLETED:
            return "Completed";
    }
    return "";
}

//Convert volunteerId to string ("None" if NO_VOLUNTEER)
string Order::volunteerIdToString(int volunteerId) const
{
    if (volunteerId  == NO_VOLUNTEER)
        return "None";
    else
        return std::to_string(volunteerId );
}

//Advance the order status
void Order::advanceStatus() 
{
    switch (status) {
        case OrderStatus::PENDING:
            status = OrderStatus::COLLECTING;
            break;
        case OrderStatus::COLLECTING:
            status = OrderStatus::DELIVERING;
            break;
        case OrderStatus::DELIVERING:
            status = OrderStatus::COMPLETED;
            break;
        case OrderStatus::COMPLETED:
            break;

    }
}