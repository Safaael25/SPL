#include "Volunteer.h"

Volunteer::Volunteer(int id, const string &name) :
 completedOrderId(NO_ORDER), activeOrderId(NO_ORDER), id(id), name(name) {}

Volunteer::~Volunteer() {}; //Destructor

int Volunteer::getId() const 
{
    return id;
}

const string& Volunteer::getName() const  
{
    return name;
}

int Volunteer::getActiveOrderId() const 
{
    return activeOrderId;
}

int Volunteer::getCompletedOrderId() const 
{
    return completedOrderId;
}

bool Volunteer::isBusy() const 
{
    return (activeOrderId != NO_ORDER);
}

//Returns a string containing the status information of the volunteer
string Volunteer::volunteerStatus(int timeLeft, int ordersLeft) const
{
    string output;

    output += "VolunteerID: " + std::to_string(getId()) + "\n";
    
    if (isBusy())
        output += "isBusy: True\n";
    else
        output += "isBusy: False\n";

    if (activeOrderId == -1)
        output += "OrderId: None\n" ;
    else
        output += "OrderId: " + std::to_string(activeOrderId) + "\n";

    if (timeLeft == 0)
        output += "TimeLeft: None\n";
    else
        output += "TimeLeft: " + std::to_string(timeLeft) + "\n";

    if (ordersLeft == -1)
        output +=  "OrdersLeft: No Limit";
    else
        output += "OrdersLeft: " + std::to_string(ordersLeft);      

    return output;
}


// CollectorVolunteer class implementation

CollectorVolunteer::CollectorVolunteer(int id, const string &name, int coolDown):
 Volunteer(id, name), coolDown(coolDown), timeLeft(0) {}

CollectorVolunteer* CollectorVolunteer::clone() const 
{
    return new CollectorVolunteer(*this);
}

void CollectorVolunteer::step()
{
    if (isBusy())
    {
        if (decreaseCoolDown()) 
        {
            //If timeLeft of busy Volunteer reaches 0, mark order as completed
            completedOrderId = activeOrderId;
            activeOrderId = NO_ORDER;
        }
    }
}

int CollectorVolunteer::getCoolDown() const 
{
    return coolDown;
}

int CollectorVolunteer::getTimeLeft() const 
{
    return timeLeft;
}

bool CollectorVolunteer::decreaseCoolDown()
{
    if (timeLeft > 0) //Ensure timeLeft is positive
    {
        timeLeft-=1;
    }
    return (timeLeft == 0);
}

bool CollectorVolunteer::hasOrdersLeft() const 
{
    return true; //Can take unlimited orders
}

bool CollectorVolunteer::canTakeOrder(const Order &order) const
{
    return (!isBusy() && (order.getStatus()==OrderStatus::PENDING));
}

void CollectorVolunteer::acceptOrder(const Order &order)
{
    activeOrderId = order.getId();
    timeLeft = coolDown;
}

string CollectorVolunteer::toString() const 
{
    //Returns a formatted status string specific to collector
    return Volunteer::volunteerStatus(getTimeLeft(),-1); 
}

//Returns the role of the volunteer (Collector)
volunteerRole CollectorVolunteer::getVolunteerRole() const
{
    return volunteerRole::COLLECTOR;
}


// LimitedCollectorVolunteer class implementation

LimitedCollectorVolunteer::LimitedCollectorVolunteer(int id, const string &name, int coolDown ,int maxOrders):
 CollectorVolunteer(id, name, coolDown), maxOrders(maxOrders), ordersLeft(maxOrders) {}

LimitedCollectorVolunteer *LimitedCollectorVolunteer::clone() const 
{
    return new LimitedCollectorVolunteer(*this);
}

bool LimitedCollectorVolunteer::hasOrdersLeft() const 
{
    return ordersLeft > 0;
}

bool LimitedCollectorVolunteer::canTakeOrder(const Order &order) const
{
    return (hasOrdersLeft() && CollectorVolunteer::canTakeOrder(order));
}

void LimitedCollectorVolunteer::acceptOrder(const Order &order) 
{
    CollectorVolunteer::acceptOrder(order);
    ordersLeft-=1;
}

int LimitedCollectorVolunteer::getMaxOrders() const 
{
    return maxOrders;
}

int LimitedCollectorVolunteer::getNumOrdersLeft() const 
{
    return ordersLeft;
}

string LimitedCollectorVolunteer::toString() const
{
    //Returns a formatted status string specific to LimitedCollectorVolunteer
    return Volunteer::volunteerStatus(getTimeLeft(),getNumOrdersLeft());
}

//Returns the role of the volunteer (Limited Collector)
volunteerRole LimitedCollectorVolunteer::getVolunteerRole() const
{
    return volunteerRole::LIMITED_COLLECTOR;
}


// DriverVolunteer class implementation

DriverVolunteer::DriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep):
 Volunteer(id, name), maxDistance(maxDistance), distancePerStep(distancePerStep), distanceLeft(0) {}

DriverVolunteer *DriverVolunteer::clone() const 
{
    return new DriverVolunteer(*this);
}

int DriverVolunteer::getDistanceLeft() const 
{
    return distanceLeft;
}

int DriverVolunteer::getMaxDistance() const 
{
    return maxDistance;
}

int DriverVolunteer::getDistancePerStep() const 
{
    return distancePerStep;
}

bool DriverVolunteer::decreaseDistanceLeft()
{
    distanceLeft = distanceLeft - distancePerStep;
    //Ensure distanceLeft is positive
    if (distanceLeft < 0) 
    {
        distanceLeft = 0;
    }
    return (distanceLeft == 0);
}

bool DriverVolunteer::hasOrdersLeft() const 
{
    return true; //Can take unlimited orders
}

bool DriverVolunteer::canTakeOrder(const Order &order) const
{
    return (!isBusy() && (order.getDistance()<=getMaxDistance()) && (order.getStatus()==OrderStatus::COLLECTING));
}

void DriverVolunteer::acceptOrder(const Order &order) 
{
    activeOrderId = order.getId();
    distanceLeft = order.getDistance();
}

void DriverVolunteer::step() 
{
    if (isBusy()) 
    {
        //If timeLeft of busy Volunteer reaches 0, mark order as completed
        if (decreaseDistanceLeft())
        {
            completedOrderId = activeOrderId;
            activeOrderId = NO_ORDER;
        }
    }
}

string DriverVolunteer::toString() const
{
    //Returns a formatted status string specific to driver
    return Volunteer::volunteerStatus(getDistanceLeft(), -1);
}

//Returns the role of the volunteer (Driver)
volunteerRole DriverVolunteer::getVolunteerRole() const
{
    return volunteerRole::DRIVER;
}


// LimitedDriverVolunteer class implementation

LimitedDriverVolunteer::LimitedDriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep, int maxOrders):
 DriverVolunteer(id, name, maxDistance, distancePerStep), maxOrders(maxOrders), ordersLeft(maxOrders) {}

LimitedDriverVolunteer* LimitedDriverVolunteer::clone() const 
{
    return new LimitedDriverVolunteer(*this);
}

int LimitedDriverVolunteer::getMaxOrders() const 
{
    return maxOrders;
}

int LimitedDriverVolunteer::getNumOrdersLeft() const 
{
    return ordersLeft;
}

bool LimitedDriverVolunteer::hasOrdersLeft() const 
{
    return ordersLeft > 0;
}

bool LimitedDriverVolunteer::canTakeOrder(const Order &order) const
{
    return (hasOrdersLeft() && DriverVolunteer::canTakeOrder(order));
}

void LimitedDriverVolunteer::acceptOrder(const Order &order)
 {
    DriverVolunteer::acceptOrder(order);
    ordersLeft--;
}

string LimitedDriverVolunteer::toString() const 
{
    //Returns a formatted status string specific to limited driver
    return Volunteer::volunteerStatus(getDistanceLeft(), getNumOrdersLeft());
}

//Returns the role of the volunteer (Limited Drive)
volunteerRole LimitedDriverVolunteer::getVolunteerRole() const
{
    return volunteerRole::LIMITED_DRIVER;
}