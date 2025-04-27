#include "WareHouse.h"

WareHouse::WareHouse(const string &configFilePath) :
 isOpen(false), customerCounter(0), volunteerCounter(0), orderCounter(0),
 defaultVolunteer(new CollectorVolunteer(-1, "", -1)), defaultOrder(new Order(-1, -1, -1)), defaultCustomer(new CivilianCustomer(-1, "", -1, -1)),
 actionsLog(), volunteers(), pendingOrders(), inProcessOrders(), completedOrders(), customers()
{
    initializeWarehouse(configFilePath);
}

// Rule of 5: Destructor
WareHouse::~WareHouse() 
{
    //Clear dynamically allocated fields
    clearVectors();

    delete(defaultOrder);
    delete(defaultVolunteer);
    delete(defaultCustomer);
    
    defaultVolunteer = nullptr;
    defaultOrder = nullptr;
    defaultCustomer = nullptr;
}

//Copy constructor
    WareHouse::WareHouse(const WareHouse &otherWareHouse) :
     isOpen(otherWareHouse.isOpen) ,customerCounter(otherWareHouse.customerCounter), 
     volunteerCounter(otherWareHouse.volunteerCounter), orderCounter(otherWareHouse.orderCounter), 
     defaultVolunteer(new CollectorVolunteer(-1, "", -1)), defaultOrder(new Order(-1, -1, -1)), defaultCustomer(new CivilianCustomer(-1, "", -1, -1)),
     actionsLog(), volunteers(), pendingOrders(), inProcessOrders(), completedOrders(), customers()
    {
        //Note: the default fields are not necessary to copy since they are not unique and shared among all instances

        copyVectors(otherWareHouse); //Copy the data of otherWareHouse vectors      
    }

    //Move constructor
    WareHouse::WareHouse(WareHouse &&otherWareHouse) :
     isOpen(otherWareHouse.isOpen) ,customerCounter(otherWareHouse.customerCounter), 
     volunteerCounter(otherWareHouse.volunteerCounter), orderCounter(otherWareHouse.orderCounter), 
     defaultVolunteer(otherWareHouse.defaultVolunteer), defaultOrder(otherWareHouse.defaultOrder), defaultCustomer(otherWareHouse.defaultCustomer),
     actionsLog(), volunteers(), pendingOrders(), inProcessOrders(), completedOrders(), customers()

    {
        //Set default pointers in otherWareHouse to nullptr
        otherWareHouse.defaultVolunteer = nullptr;
        otherWareHouse.defaultOrder = nullptr;
        otherWareHouse.defaultCustomer = nullptr;
        
        //Copy and clear otherWareHouse vectors      
        for (Volunteer *volunteer: otherWareHouse.volunteers)
            volunteers.push_back(volunteer);
        otherWareHouse.volunteers.clear();

        for (Order *order: otherWareHouse.pendingOrders)
            pendingOrders.push_back(order);
        otherWareHouse.pendingOrders.clear();
        
        for (Order *order: otherWareHouse.inProcessOrders)
            inProcessOrders.push_back(order);
        otherWareHouse.inProcessOrders.clear();
        
        for (Order *order: otherWareHouse.completedOrders)
            completedOrders.push_back(order);
        otherWareHouse.completedOrders.clear();
        
        for (Customer *customer: otherWareHouse.customers)
            customers.push_back(customer);
        otherWareHouse.customers.clear();

        for (BaseAction *action: otherWareHouse.actionsLog)
            actionsLog.push_back(action);
        otherWareHouse.actionsLog.clear();
    
    }

    //Copy assignment operator
    WareHouse &WareHouse::operator=(const WareHouse &otherWareHouse) {
        if (this != &otherWareHouse) 
        {
            //copy otherWareHouse primitive fields
            isOpen = otherWareHouse.isOpen;
            customerCounter= otherWareHouse.customerCounter;
            volunteerCounter= otherWareHouse.volunteerCounter;
            orderCounter= otherWareHouse.orderCounter;
            
            //Note: the default fields are not necessary to copy since they are not unique and shared among all instances
            
            //Clear vectors data and Copy the data of otherWareHouse vectors
            clearVectors();
            copyVectors(otherWareHouse);
        }
        
        return *this;

    }

    //Move assignment operato
    WareHouse &WareHouse::operator=(WareHouse &&otherWareHouse) {
        //Clear and assign otherWareHouse vectors
        if (this != &otherWareHouse) {
            
            //copy otherWareHouse primitive fields
            isOpen = otherWareHouse.isOpen;
            customerCounter= otherWareHouse.customerCounter;
            volunteerCounter= otherWareHouse.volunteerCounter;
            orderCounter= otherWareHouse.orderCounter;            
            
            //Delete default fields and assign otherWareHouse fields
            delete(defaultOrder);
            delete(defaultVolunteer);
            delete(defaultCustomer);

            defaultVolunteer = otherWareHouse.defaultVolunteer;
            defaultOrder = otherWareHouse.defaultOrder;
            defaultCustomer = otherWareHouse.defaultCustomer;
        
            otherWareHouse.defaultVolunteer = nullptr;
            otherWareHouse.defaultOrder = nullptr;
            otherWareHouse.defaultCustomer = nullptr;
            
            //Clear current vectors
            clearVectors();

            //Copy the data of otherWareHouse vectors
            for (Volunteer *volunteer: otherWareHouse.volunteers) 
                volunteers.push_back(volunteer);
            otherWareHouse.volunteers.clear();
            
            for (Order *order: otherWareHouse.pendingOrders) 
                pendingOrders.push_back(order);
            otherWareHouse.pendingOrders.clear();
            
            for (Order *order: otherWareHouse.inProcessOrders) 
                inProcessOrders.push_back(order);
            otherWareHouse.inProcessOrders.clear();
            
            for (Order *order: otherWareHouse.completedOrders) 
                completedOrders.push_back(order);
            otherWareHouse.completedOrders.clear();
            
            for (Customer *customer: otherWareHouse.customers) 
                customers.push_back(customer);
            otherWareHouse.customers.clear();

            for (BaseAction *action: otherWareHouse.actionsLog) 
                actionsLog.push_back(action);
            otherWareHouse.actionsLog.clear();
        
        }
        return *this;

    }

// Helper function to clear vectors memory
void WareHouse::clearVectors() 
{
    for (Volunteer *volunteer: volunteers)
        delete(volunteer);
    volunteers.clear();
        
    for (Customer *customer: customers) 
        delete(customer);
    customers.clear();
        
    for (Order *order: pendingOrders) 
        delete(order);
    pendingOrders.clear();
        
    for (Order *order: inProcessOrders) 
        delete(order);
    inProcessOrders.clear();
        
    for (Order *order: completedOrders) 
         delete(order);
     completedOrders.clear();
        
    for (BaseAction *action: actionsLog) 
        delete(action);
    actionsLog.clear();
}

//Helper function to copy vectors from another WareHouse
void WareHouse::copyVectors(const WareHouse &otherWareHouse) 
{
    for (Volunteer *volunteer : otherWareHouse.volunteers)
        volunteers.push_back(volunteer->clone());

    for (Order *order : otherWareHouse.pendingOrders)
        pendingOrders.push_back(new Order(*order));

    for (Order *order : otherWareHouse.inProcessOrders)
        inProcessOrders.push_back(new Order(*order));

    for (Order *order : otherWareHouse.completedOrders)
        completedOrders.push_back(new Order(*order));

    for (Customer *customer : otherWareHouse.customers)
        customers.push_back(customer->clone());

    for (BaseAction *action : otherWareHouse.actionsLog)
        actionsLog.push_back(action->clone());
}

void WareHouse::start() 
{
    open();
    performActions();
}

//Perform user actions based on commands
void WareHouse::performActions() 
{
    while (true) {
        string input;
        std::getline(std::cin, input);
        std::vector<std::string> command = splitString(input); //Get input words as vector

        //Perform the action based on the command
        if (command[0] == "step" && command.size()==2 && isLegalIntInput(command[1]))
        {
            SimulateStep* simulateStep = new SimulateStep(stringToInt(command[1]));
            simulateStep->act(*this); 
            
            this->actionsLog.push_back(simulateStep);   
        }
        else if (command[0] == "order" && command.size()==2 && isLegalIntInput(command[1]))  
        {
            AddOrder* addOrder = new AddOrder(stringToInt(command[1]));
            addOrder->act(*this);

            this->actionsLog.push_back(addOrder);   
        }
        else if (command[0] == "customer" && command.size()==5 && isLegalIntInput(command[3])  && isLegalIntInput(command[4])) 
        {          
            AddCustomer* addCustomer = new AddCustomer(command[1], command[2], stringToInt(command[3]), stringToInt(command[4]));
            addCustomer->act(*this);

            this->actionsLog.push_back(addCustomer);   
        }
        else if (command[0] == "orderStatus" && command.size()==2 && isLegalIntInput(command[1])) 
        {
            PrintOrderStatus* printOrderStatus = new PrintOrderStatus(stringToInt(command[1]));
            printOrderStatus->act(*this);
            
            this->actionsLog.push_back(printOrderStatus);     
        }
        else if (command[0] == "customerStatus" && command.size()==2 && isLegalIntInput(command[1]))
        {
            PrintCustomerStatus* printCustomerStatus = new PrintCustomerStatus(stringToInt(command[1]));
            printCustomerStatus->act(*this);
            
            this->actionsLog.push_back(printCustomerStatus);   
        }
        else if (command[0] == "volunteerStatus" && command.size()==2 && isLegalIntInput(command[1]))
        {
            PrintVolunteerStatus* printVolunteerStatus = new PrintVolunteerStatus(stringToInt(command[1]));
            printVolunteerStatus->act(*this);

            this->actionsLog.push_back(printVolunteerStatus);          
        }
        else if (command[0] == "log" && command.size()==1)
        {
            PrintActionsLog* printActionsLog = new PrintActionsLog();
            printActionsLog->act(*this);

            this->actionsLog.push_back(printActionsLog);    
        }
        else if (command[0] == "close" && command.size()==1)
        {
            Close close = Close();
            close.act(*this);
            break; //Finish the program
        }
        else if (command[0] == "backup" && command.size()==1) 
        {
            BackupWareHouse* backupWareHouse = new BackupWareHouse();
            backupWareHouse->act(*this);
            
            this->actionsLog.push_back(backupWareHouse);    
        }
        else if (command[0] == "restore" && command.size()==1) 
        {
            RestoreWareHouse* restoreWareHouse = new RestoreWareHouse();
            restoreWareHouse->act(*this);
            
            this->actionsLog.push_back(restoreWareHouse);    
        }
        else
        {
            std::cout << "Invalid command" << std::endl; //Invalid command
        }
    }
}

//Split input string into a vector of substrings
std::vector<std::string> WareHouse::splitString(const std::string &input) 
{
    std::vector<std::string> words;
    std::string word = "";
    
    for (char c : input) {
        if (c == ' ' || c == '\n' || c == '\t') {
            if (!word.empty()) {
                words.push_back(word);
                word.clear();
            }
        }
        else
            word += c;
    }
    
    if (!word.empty()) { //Adds last word
        words.push_back(word);
    }

    return words;
}

//Returns true if input represents a legal int value.
bool WareHouse::isLegalIntInput(const std::string &input) 
{
    
    for (char c : input) {
        if (!isdigit(c)) 
            return false;
    }

    return true;
}

//Convert str to an int assuming all characters are valid positive numbers
int WareHouse::stringToInt(const std::string& str) 
{
    int result = 0;

    for (int i = 0; i < int(str.size()); ++i) { 
        if (isdigit(str[i])) {
            result = result * 10 + (str[i] - '0');
        }
    }

    return result;
}

void WareHouse::addOrder(Order* order) 
{
    pendingOrders.push_back(order);
}

//Creates an order for a given customer, return false if created unsuccessfully
bool WareHouse::createOrderForCustomer(int customerId) 
{
    Customer& customer = getCustomer(customerId);

    if ((customerId < customerCounter) && customer.canMakeOrder()) {
        //Create a new order for the customer
        Order* newOrder = new Order(orderCounter++, customerId, customer.getCustomerDistance());
        pendingOrders.push_back(newOrder);
        customer.addOrder(newOrder->getId());

        return true;
    }

    return false;
}

void WareHouse::addAction(BaseAction* action) 
{
    actionsLog.push_back(action);
}

Customer& WareHouse::getCustomer(int customerId) const 
{
    if (customerId < customerCounter) {
        for (auto& customer : customers) {
            if (customer->getId() == customerId) {
                return *customer;
            }
        }
    }
    //If no matching customer is found, return a default customer with id -1
    return *defaultCustomer; 
}

Volunteer& WareHouse::getVolunteer(int volunteerId) const 
{
    for (const auto& volunteer : volunteers) {
        if (volunteer->getId() == volunteerId) {
            return *volunteer;
        }
    }
    //If no matching volunteer is found, return a default volunteer with id -1
    return *defaultVolunteer;
}

Order& WareHouse::getOrder(int orderId) const 
{
    if (orderId < orderCounter)
    {
            for (const auto &order : pendingOrders) 
        {
            if (order->getId() == orderId) 
                return *order;
        }
        for (const auto &order : inProcessOrders) 
        {
            if (order->getId() == orderId)
                return *order;
        }
        for (const auto &order : completedOrders) 
        {
            if (order->getId() == orderId)
                return *order;
        }
    }
    //If no matching order is found, return a default order with id -1
    return *defaultOrder;
}

const vector<BaseAction*> &WareHouse::getActions() const 
{
    return actionsLog;
}

void WareHouse::close() 
{
    //Prints all orders with their status
    for (const auto &order : pendingOrders) 
        std::cout << "OrderID: " << order->getId() << ", CustomerID: " << order->getCustomerId() << 
        ", Status: " << order->orderStatusToString() << std::endl;

    for (const auto &order : inProcessOrders) 
        std::cout << "OrderID: " << order->getId() << ", CustomerID: " << order->getCustomerId() << 
        ", Status: " << order->orderStatusToString() << std::endl;

    for (const auto &order : completedOrders) 
        std::cout << "OrderID: " << order->getId() << ", CustomerID: " << order->getCustomerId() << 
        ", Status: " << order->orderStatusToString() << std::endl;

    isOpen = false;
}
 
void WareHouse::open() 
{
    isOpen = true;
    std::cout << "Warehouse is open!" << std::endl;
}

//Add a new Customer to the warehouse
void WareHouse::addCustomer(const string &customerName, bool isSolider, int distance, int maxOrders)
{
    if (isSolider)
        customers.push_back(new SoldierCustomer(customerCounter++, customerName, distance, maxOrders));
    else 
        customers.push_back(new CivilianCustomer(customerCounter++, customerName, distance, maxOrders));
}

//Returns customer details string for display
const string WareHouse::customerDetailsToString(Customer &customer) const 
{ 
    std::string output;

    output += customer.toString(); //Prints customer Id

    //Prints customer order details
    for (int orderId : customer.getOrdersIds()) 
    {
        output += "OrderId: " + std::to_string(orderId) + "\n";
        output += "OrderStatus: " + getOrder(orderId).orderStatusToString() + "\n";
    }

    output += "numOrdersLeft: " + std::to_string(customer.getMaxOrders() - customer.getNumOrders());

    return output;
}

//Returns volunteer details string for display
const string WareHouse::volunteerDetailsToString(Volunteer &volunteer) const 
{ 
    return volunteer.toString();
}

//Perform a step in the simulation.
void WareHouse::performSimulationStep()
{
    handlePendingOrders();
    handleinProcessOrders();
}

//Helper functions for assign pending orders to volunteers
void WareHouse::handlePendingOrders()
{
    auto ordersIt = pendingOrders.begin();
    while (ordersIt != pendingOrders.end()) {
        Order* currentOrder = *ordersIt;
        bool orderAssigned = false;
        for (auto volunteerIt = volunteers.begin(); volunteerIt != volunteers.end(); ++volunteerIt)
        {
            Volunteer* volunteer = *volunteerIt;

            //Assign order to a suitable volunteer
            if (volunteer->canTakeOrder(*currentOrder)) {
                volunteer->acceptOrder(*currentOrder);

                if (volunteer->getVolunteerRole() == volunteerRole::COLLECTOR || volunteer->getVolunteerRole() == volunteerRole::LIMITED_COLLECTOR)
                    currentOrder->setCollectorId(volunteer->getId());
                else
                    currentOrder->setDriverId(volunteer->getId());

                currentOrder->advanceStatus();
                inProcessOrders.push_back(currentOrder);
                ordersIt = pendingOrders.erase(ordersIt);
                orderAssigned = true;
                break;
            }

        }

        if (!orderAssigned) {
            ++ordersIt;
        }
    }
}

//Helper functions for perform a simulation step for in process orders and handle completed orders
void WareHouse::handleinProcessOrders()
{
    for (auto volunteerIt = volunteers.begin(); volunteerIt != volunteers.end(); ++volunteerIt) 
    {
        Volunteer* volunteer = *volunteerIt;
        if (volunteer->isBusy())
        {             
            volunteer->step();
  
            //Handle Completed Orders
            if (!volunteer->isBusy())
            {             
                Order* finishedOrder = &getOrder(volunteer->getCompletedOrderId());
                if (finishedOrder->getStatus() == OrderStatus::DELIVERING)
                {
                    finishedOrder->setStatus(OrderStatus::COMPLETED);
                    completedOrders.push_back(finishedOrder);
                }
                else if (finishedOrder->getStatus() == OrderStatus::COLLECTING)
                {
                    pendingOrders.push_back(finishedOrder);
                }             
                
                inProcessOrders.erase(std::remove(inProcessOrders.begin(), inProcessOrders.end(), finishedOrder), inProcessOrders.end());
                
                //Delete a volunteer if the maxOrders limit has been reached
                if (!volunteer->hasOrdersLeft())
                {
                    volunteerIt = volunteers.erase(volunteerIt);
                    delete(volunteer);
                }        
            }
        }
    }
}

//Initialize the warehouse based on the configuration file
void WareHouse::initializeWarehouse(const std::string& configFilePath) 
{
    std::ifstream file(configFilePath);
    if (!file.is_open()) {
        //Handle file opening error
        std::cerr << "Error: Unable to open configuration file." << std::endl;
        return; 
    }

    std::string line;
    while (std::getline(file, line)) {
        //Remove comments
        size_t commentPos = line.find('#');
        if (commentPos != std::string::npos) {
            line = line.substr(0, commentPos);
        }

        //Handle empty lines
        if (line.empty()) {
            continue;
        }

        std::istringstream iss(line);
        std::string type;
        iss >> type;

        //Parse customer information
        if (type == "customer") {
            std::string name, customerType;
            int distance, maxOrders;
            if (!(iss >> name >> customerType >> distance >> maxOrders)) {
                std::cerr << "Error: Failed to parse customer configuration." << std::endl;
                continue;
            }

            AddCustomer addCustomer(name, customerType, distance, maxOrders);
            addCustomer.act(*this);
        }

        //Parse volunteer information
        else if (type == "volunteer") {
            std::string name, role;
            int cooldown, distPerStep = 0, maxOrders = 0;
            if (!(iss >> name >> role >> cooldown)) {
                std::cerr << "Error: Failed to parse volunteer configuration." << std::endl;
                continue;
            }

            if (role == "driver") {
                if (!(iss >> distPerStep)) {
                    std::cerr << "Error: Failed to parse driver configuration." << std::endl;
                    continue;
                }
            }
            else if (role == "limited_collector") {
                if (!(iss >> maxOrders)) {
                    std::cerr << "Error: Failed to parse limited_collector configuration." << std::endl;
                    continue;
                }
            }
            else if (role == "limited_driver") {
                if (!(iss >> distPerStep >> maxOrders)) {
                    std::cerr << "Error: Failed to parse limited_driver configuration." << std::endl;
                    continue;
                }
            }

            addVolunteer(name, role, cooldown, distPerStep, maxOrders);
        }
        else {
            //Unknown configuration type
            std::cerr << "Error: Unknown configuration type - " << type << std::endl;
        }
    }

    file.close();

}

//Add volunteers from the configuration file to the warehouse
void WareHouse::addVolunteer(const std::string& name, const std::string& role, int cooldownOrMaxDistance, int distPerStep, int maxOrders)
{
    if (role == "collector")
        volunteers.push_back( new CollectorVolunteer(volunteerCounter++, name, cooldownOrMaxDistance));
    
    if (role == "limited_collector")
        volunteers.push_back( new LimitedCollectorVolunteer(volunteerCounter++, name, cooldownOrMaxDistance, maxOrders));
    
    if (role == "driver")
        volunteers.push_back(new DriverVolunteer(volunteerCounter++, name, cooldownOrMaxDistance, distPerStep));
    
    if (role == "limited_driver")
        volunteers.push_back(new LimitedDriverVolunteer(volunteerCounter++, name, cooldownOrMaxDistance, distPerStep, maxOrders));
}
