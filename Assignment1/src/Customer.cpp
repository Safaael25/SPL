#include "Customer.h"

Customer::Customer(int id, const string &name, int locationDistance, int maxOrders):
 id(id), name(name), locationDistance(locationDistance), maxOrders(maxOrders), ordersId() {}

//Distructor
Customer::~Customer(){}

const string& Customer::getName() const
{
    return name;
}

int Customer::getId() const 
{
    return id;
}

int Customer::getCustomerDistance() const 
{
    return locationDistance;
}

int Customer::getMaxOrders() const 
{
    return maxOrders;
}

int Customer::getNumOrders() const 
{
    return int(ordersId.size());
}

bool Customer::canMakeOrder() const 
{
    return (getNumOrders() < maxOrders);
}

const vector<int>& Customer::getOrdersIds() const
{
    return ordersId;
}

int Customer::addOrder(int orderId) 
{
    if (canMakeOrder()) 
    {
        ordersId.push_back(orderId);
        
        return orderId;
    }
    else 
        return -1; //Reached max orders limit
}

//Returns string representating the customer
std::string Customer::toString() const  
{ 
    return ("CustomerID: " + std::to_string(id) + "\n");
}


// SoldierCustomer class implementation

SoldierCustomer::SoldierCustomer(int id, const string &name, int locationDistance, int maxOrders):
 Customer(id, name, locationDistance, maxOrders) {}

SoldierCustomer* SoldierCustomer::clone() const
{
    return new SoldierCustomer(*this);
}

//Returns true, indicating that the customer is a soldier
bool SoldierCustomer::isSolider() const 
{
    return true;
}


// CivilianCustomer class implementation

CivilianCustomer::CivilianCustomer(int id, const string &name, int locationDistance, int maxOrders):
 Customer(id, name, locationDistance, maxOrders) {}

CivilianCustomer* CivilianCustomer::clone() const
{
    return new CivilianCustomer(*this);
}

//Returns false, indicating that the customer is a Civilian
bool CivilianCustomer::isSolider() const 
{
    return false;
}
