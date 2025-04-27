#pragma once

#include <string>
#include <vector>
#include <fstream>
#include <iostream>
#include <sstream>
#include <algorithm>

#include "Order.h"
#include "Customer.h"
#include "Volunteer.h"
#include "Action.h"

class BaseAction;
class Volunteer;

// Warehouse responsible for Volunteers, Customers Actions, and Orders.

class WareHouse {

    public:
        WareHouse(const string &configFilePath);
        ~WareHouse(); //Destructor
        WareHouse(const WareHouse &otherWareHouse); //Copy constructor
        WareHouse(WareHouse &&otherWareHouse); //Move constructor
        WareHouse &operator=(const WareHouse &otherWareHouse); //Copy assignment operator
        WareHouse &operator=(WareHouse &&otherWareHouse); //Move assignment operator 
        
        void start();
        void addOrder(Order* order);
        bool createOrderForCustomer(int customerId); //Creates an order for a given customer, return false if created unsuccessfully
        void addAction(BaseAction* action);
        Customer &getCustomer(int customerId) const;
        Volunteer &getVolunteer(int volunteerId) const;
        Order &getOrder(int orderId) const;
        const vector<BaseAction*> &getActions() const;
        void close();
        void open();
        void addCustomer(const string &customerName, bool isSolider, int distance, int maxOrders); //Add a new Customer to the warehouse    
        const string customerDetailsToString(Customer &customer) const; //Returns customer details string for display
        const string volunteerDetailsToString(Volunteer &volunteer) const; //Returns volunteer details string for display
        void performSimulationStep(); //Perform a step in the simulation.

    private:
        bool isOpen;
        int customerCounter; //For assigning unique customer IDs
        int volunteerCounter; //For assigning unique volunteer IDs
        int orderCounter; //For assigning unique order IDs
        CollectorVolunteer* defaultVolunteer; //Default instance of a collector       
        Order* defaultOrder; //Default instance of a order      
        CivilianCustomer* defaultCustomer; //Default instance of a volunteer      
        vector<BaseAction*> actionsLog;
        vector<Volunteer*> volunteers;
        vector<Order*> pendingOrders;
        vector<Order*> inProcessOrders;
        vector<Order*> completedOrders;
        vector<Customer*> customers;


        void clearVectors();
        void copyVectors(const WareHouse &otherWareHouse);

        //Helper functions for performing input operations in the warehouse
        void performActions();
        vector<string> splitString(const string &input); 
        bool isLegalIntInput(const std::string &input); 
        int stringToInt(const string& str);
        
        //Helper functions for performing a step in the simulation
        void handlePendingOrders();
        void handleinProcessOrders(); 
        
        //Helper functions initializing the warehouse using a configuration file
        void addVolunteer(const string& name, const string& role, int cooldownOrDistPerStep, int distPerStep, int maxOrders);
        void initializeWarehouse(const string& configFilePath);

};