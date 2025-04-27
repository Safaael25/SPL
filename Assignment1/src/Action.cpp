#include "Action.h"

extern WareHouse* backup; // Global variable definition

// BaseAction class implementation

BaseAction::BaseAction() :  errorMsg(""), status(ActionStatus::COMPLETED) {} //Assuming completion at the beginning

BaseAction::~BaseAction() {}; //Destructor

ActionStatus BaseAction::getStatus() const
{
    return status;
}

void BaseAction::complete() 
{
    status = ActionStatus::COMPLETED;
}

void BaseAction::error(string errorMsg) 
{
    status = ActionStatus::ERROR;
    this->errorMsg = errorMsg;
}

string BaseAction::getErrorMsg() const 
{
    return errorMsg;
}

//Convert an ActionStatus enum value to a string
string BaseAction::actionStatusToString() const 
{
    switch (status) {
        case ActionStatus::COMPLETED:
            return "COMPLETED";
        case ActionStatus::ERROR:
            return "ERROR";
    }
    return " ";
}

//SimulateStep class implementation

SimulateStep::SimulateStep(int numOfSteps) : numOfSteps(numOfSteps) {}

void SimulateStep::act(WareHouse &wareHouse) 
{
    for (int i=1 ; i <= numOfSteps; i++)
        wareHouse.performSimulationStep(); //Performs a one step simulation
}

SimulateStep *SimulateStep::clone() const 
{
    return new SimulateStep(*this);
}

string SimulateStep::toString() const 
{
    return "simulateStep " + std::to_string(numOfSteps);
}

//AddOrder class implementation

AddOrder::AddOrder(int id) : customerId(id) {}

void AddOrder::act(WareHouse &wareHouse) 
{
    //Returns an error if the customer does not exist or cannot place an order
    if (!wareHouse.createOrderForCustomer(customerId))
    {
        error("Cannot place this order");
        std::cout << "Error: " << getErrorMsg() << std::endl;
    }
}

string AddOrder::toString() const 
{
    return "order " + std::to_string(customerId);
}

AddOrder* AddOrder::clone() const
{
    return new AddOrder(*this);
}


//AddCustomer class implementation

AddCustomer::AddCustomer(const string &customerName, const string &customerType, int distance, int maxOrders)
    : customerName(customerName), customerType((customerType == "soldier" || customerType == "Soldier") ? CustomerType::Soldier : CustomerType::Civilian),
      distance(distance), maxOrders(maxOrders) {}

void AddCustomer::act(WareHouse &wareHouse) 
{
    bool isSolider = (customerType == CustomerType::Soldier);
    wareHouse.addCustomer(customerName, isSolider, distance, maxOrders);
}

AddCustomer *AddCustomer::clone() const 
{
    return new AddCustomer(*this);
}

string AddCustomer::toString() const 
{
    return "customer " + customerName +
           (customerType == CustomerType::Soldier ? " soldier " : " civilian ") +
           std::to_string(distance) + " " + std::to_string(maxOrders);
}

//PrintOrderStatus class implementation

PrintOrderStatus::PrintOrderStatus(int id) : orderId(id) {}

void PrintOrderStatus::act(WareHouse &wareHouse) 
{
    Order& order = wareHouse.getOrder(orderId);

    //If the order is not found, return an error
    if (order.getId() == -1) 
    {
        error("Order doesn’t exist");
        std::cout << "Error: " << getErrorMsg() << std::endl;
        return;
    }
    //Print the order details
    std::cout << order.toString() << std::endl;
}

PrintOrderStatus *PrintOrderStatus::clone() const 
{
    return new PrintOrderStatus(*this);
}

string PrintOrderStatus::toString() const 
{
    return "orderStatus " + std::to_string(orderId);
}

//PrintCustomerStatus class implementation

PrintCustomerStatus::PrintCustomerStatus(int customerId) : customerId(customerId) {}

void PrintCustomerStatus::act(WareHouse &wareHouse) 
{
    Customer& customer = wareHouse.getCustomer(customerId);

   //If the customer is not found, return an error
    if (customer.getId() == -1) 
    {
        error("Customer doesn’t exist");
        std::cout << "Error: " << getErrorMsg() << std::endl;
        return;
    }

    //Print the customer details
    std::cout << wareHouse.customerDetailsToString(customer) << std::endl;
}

PrintCustomerStatus *PrintCustomerStatus::clone() const 
{
    return new PrintCustomerStatus(*this);
}

string PrintCustomerStatus::toString() const 
{
    return "customerStatus " + std::to_string(customerId);
}


// PrintVolunteerStatus class implementation

PrintVolunteerStatus::PrintVolunteerStatus(int id) : volunteerId(id) {}

void PrintVolunteerStatus::act(WareHouse &wareHouse) 
{
    Volunteer& volunteer = wareHouse.getVolunteer(volunteerId);

   //If the volunteer is not found, return an error
   if (volunteer.getId() == -1) 
   {
        error("Volunteer doesn’t exist");
        std::cout << "Error: " << getErrorMsg() << std::endl;        
        return;
    }

    //Print the volunteer details
    std::cout << wareHouse.volunteerDetailsToString(volunteer) << std::endl;
}

PrintVolunteerStatus *PrintVolunteerStatus::clone() const 
{
    return new PrintVolunteerStatus(*this);
}

string PrintVolunteerStatus::toString() const 
{
    return "volunteerStatus " + std::to_string(volunteerId);
}

//PrintActionsLog class implementation

PrintActionsLog::PrintActionsLog() {}

void PrintActionsLog::act(WareHouse &wareHouse)
{
    const std::vector<BaseAction*>& actionsLog = wareHouse.getActions();

    for (const auto &action : actionsLog)
        std::cout << action->toString() << " " <<  action->actionStatusToString() << std::endl;
}

PrintActionsLog *PrintActionsLog::clone() const 
{
    return new PrintActionsLog(*this);
}

string PrintActionsLog::toString() const 
{
    return "log";
}


//Close class implementation

Close::Close() {}

void Close::act(WareHouse &wareHouse) 
{
    wareHouse.close();
}

Close *Close::clone() const 
{
    return new Close(*this);
}

string Close::toString() const 
{
    return "Close";
}

//BackupWareHouse class implementation

BackupWareHouse::BackupWareHouse() {}

void BackupWareHouse::act(WareHouse &wareHouse) 
{
    //Delete backup if already exists
    if (backup != nullptr) 
    {
        delete backup;
    }
    //Creates new backup by copying current warehouse
    backup = new WareHouse(wareHouse);
}

BackupWareHouse *BackupWareHouse::clone() const 
{
    return new BackupWareHouse(*this);
}

string BackupWareHouse::toString() const 
{
    return "backup";
}

//RestoreWareHouse class implementation

RestoreWareHouse::RestoreWareHouse() {}

void RestoreWareHouse::act(WareHouse &wareHouse) 
{
    //If no backup available, return an error
    if (backup == nullptr) 
    {
        error("No backup available");
        std::cout << "Error: " << getErrorMsg() << std::endl;        
        return;
    }
    //Restore warehouse from backup
    wareHouse = *backup;
}

RestoreWareHouse *RestoreWareHouse::clone() const 
{
    return new RestoreWareHouse(*this);
}

string RestoreWareHouse::toString() const 
{
    return "restore";
}
