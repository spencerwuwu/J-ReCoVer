// https://searchcode.com/api/result/92869826/

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import bakery.Item;
import bakery.Order;
import bakery.customer.Customer;
import bakery.customer.CustomerRoll;
import bakery.inventory.Inventory;
import bakery.order.OrderList;

/**
 * Master UI Class for the Bakery. Runs all bakery related functionality
 * 
 * @author dosborne jcheng
 * @version 6.18.14
 */
public class Bakery {
    /** Master inventory */
    private Inventory inv;

    /** Master customer Roll */
    private CustomerRoll custRoll;

    /** Master order List */
    private OrderList orderList;

    /** Input scanner for handling user input */
    private Scanner inputScanner = new Scanner(System.in);

    /**
     * Constructor for Bakery
     * 
     * @param inv
     *            Inventory
     * @param custRoll
     *            CustomerRoll
     * @param orderList
     *            OrderList
     */
    Bakery(Inventory inv, CustomerRoll custRoll, OrderList orderList) {
        this.inv = inv;
        this.custRoll = custRoll;
        this.orderList = orderList;
    }

    /**
     * Getter method for the Bakery's inventory
     * 
     * @return Inventory
     */
    private Inventory getInventory() {
        return inv;
    }

    /**
     * Getter method for customer Roll
     * 
     * @return CustomerRoll
     */
    private CustomerRoll getCustomerRoll() {
        return custRoll;
    }

    /**
     * Getter method for OrderList
     * 
     * @return OrderList
     */
    private OrderList getOrderList() {
        return orderList;
    }

    /**
     * Helper method to check if customer with provided info is registered in
     * the customerRoll
     * 
     * @param lastName
     *            Last Name of customer
     * @param address
     *            Address of customer
     * @param city
     *            City of customer
     * @param state
     *            State of Customer
     * @param zipCode
     *            Integer representation of Zip Code of customer
     * @return True if they are registered, False otherwise
     */
    boolean isRegisteredCustomer(String lastName, String address,
        String city, String state, Integer zipCode) {
        return getCustomerRoll().isReturningCustomer(lastName, address, city,
            state, zipCode);
    }

    /**
     * Helper method to check if customer with provided ID is registered in the
     * customerRoll
     * 
     * @param customerID
     *            ID of the customer to search for
     * @return True if they are registered, false otherwise
     */
    boolean isRegisteredCustomer(Integer customerID) {
        return getCustomerRoll().isReturningCustomer(customerID);
    }

    /**
     * Helper method to check if Item with provided ID exists in the inventory
     * 
     * @param itemID
     *            ID of the item
     * @return True if the item exists, false otherwise
     */
    boolean isInInventory(Integer itemID) {
        return getInventory().containsItem(itemID);
    }

    /**
     * Helper method to check if Item with provided information already exists
     * in inventory
     * 
     * @param bakeryItemName
     *            Name of Item
     * @param bakeryItemCategory
     *            Category of item
     * @return True if it exists, false otherwise
     */
    boolean isInInventory(String bakeryItemName, String bakeryItemCategory) {
        return getInventory()
            .containsItem(bakeryItemName, bakeryItemCategory);
    }

    /**
     * Helper function to register new customers in the customer roll. This
     * allows a customerID to be passed in and override the
     * auto-customer-ID-generation process. This should only be used for input
     * from a file where the customerID was specified.
     * 
     * PRECONDITION: No user with provided ID should exist in the customerRoll
     * 
     * @param customerID
     *            ID of the new customer
     * @param lastName
     *            Last name of the customer
     * @param address
     *            Address of the customer
     * @param city
     *            City of the customer
     * @param state
     *            State of the customer
     * @param zipCode
     *            Integer represenation of the zip code of the customer
     * @return New Bakery with a customer registered with the provided
     *         information
     */
    private Bakery registerNewCustomer(Integer customerID, String lastName,
        String address, String city, String state, Integer zipCode) {
        return new Bakery(getInventory(), getCustomerRoll().addNewCustomer(
            customerID, lastName, address, city, state, zipCode),
            getOrderList());
    }

    /**
     * Helper function to register new customers in the customer roll. This
     * method will automatically generate a user ID for the user to use.
     * 
     * @param lastName
     *            Last name of the new customer
     * @param address
     *            Address of the new customer
     * @param city
     *            City of the new customer
     * @param state
     *            State of the new customer
     * @param zipCode
     *            Zip Code of the new customer
     * @return new Bakery with a customer roll containing the new customer
     */
    public Bakery registerNewCustomer(String lastName, String address,
        String city, String state, Integer zipCode) {
        return new Bakery(getInventory(), getCustomerRoll().addNewCustomer(
            lastName, address, city, state, zipCode), getOrderList());
    }

    /**
     * Helper function to remove customer from the bakery
     * 
     * @param customerID
     *            ID of the customer that needs to be removed
     * @return A new bakery instance with the specified customer removed
     */
    public Bakery removeCustomer(Integer customerID) {
        return new Bakery(getInventory(), getCustomerRoll().removeCustomer(
            customerID), getOrderList());
    }

    /**
     * Function to add inventory items to inventory. Accepts an itemID. Should
     * only be used for file imports where an ID is preset.
     * 
     * @param itemID
     *            ID of the new item
     * @param itemName
     *            Name of the new item
     * @param category
     *            Category of the new item
     * @param itemPrice
     *            Price of the new item
     * @return New bakery with the added item in its inventory
     */
    private Bakery addToInventory(Integer itemID, String itemName,
        String category, double itemPrice) {
        return new Bakery(getInventory().addToStock(itemID, itemName,
            category, itemPrice), getCustomerRoll(), getOrderList());
    }

    /**
     * Function to add item to inventory. ID will be generated for the new item
     * 
     * @param itemName
     *            Name of the item
     * @param category
     *            Category of the item
     * @param itemPrice
     *            Price of the item
     * @return New Bakery with the added item
     */
    public Bakery addToInventory(String itemName, String category,
        double itemPrice) {
        return new Bakery(getInventory().addToStock(itemName, category,
            itemPrice), getCustomerRoll(), getOrderList());
    }

    /**
     * Removes the specified item from the bakery's inventory
     * 
     * @param itemID
     *            ID Of the item to remoe
     * @return A new bakery with the item removed from its inventory
     */
    public Bakery removeFromInventory(Integer itemID) {
        return new Bakery(getInventory().removeFromStock(itemID),
            getCustomerRoll(), getOrderList());
    }

    /**
     * Adds orders to the ORderList. This function is only used for direct
     * imports where no math needs to be performed on the rewards amounts.
     * 
     * @param orderID
     *            ID of the order
     * @param customerID
     *            ID of the customer
     * @param total
     *            Total cost for the customer. This includes other orders with
     *            the same total
     * @param itemID
     *            ID of the item to be added
     * @param quantity
     *            Quantity of item purchased in this order
     * @param loyaltyAtTimeOfOrder
     *            Loyalty points the customer had when placing this order. Is
     *            used when the chart is saved
     * @param availableDiscount
     *            Discount points the customer has available after the order.
     * @param discountUsedOnOrder
     *            Discoutn points used on this order
     * @param paid
     *            True if order has been paid for, false if it needs to be
     *            billed
     * @param orderDate
     *            Date the order was placed
     * @param pickupDate
     *            Date the order will be picked up
     * @return New bakery where the customer has had his rewards points applied,
     *         and the order has been added
     */
    private Bakery performTransaction(Integer orderID, Integer customerID,
        double total, Integer itemID, Integer quantity,
        double loyaltyAtTimeOfOrder, double availableDiscount,
        double discountUsedOnOrder, boolean paid, Date orderDate,
        Date pickupDate) {

        Item item = getInventory().getItem(itemID);

        return new Bakery(getInventory(), getCustomerRoll().setPoints(
            customerID, loyaltyAtTimeOfOrder, availableDiscount),
            getOrderList().addToOrderList(customerID, orderID, total, paid,
                orderDate, pickupDate, item, quantity, loyaltyAtTimeOfOrder,
                availableDiscount, discountUsedOnOrder));
    }

    /**
     * Public function to add orders to the orderRoll. Also will adjust customer
     * rewards balances
     * 
     * PRECONDITION: discountUsedOnOrder must be positive. negativitiy will be
     * handled here
     * 
     * PRECONDITION: itemIDs(x) must correspond with itemQuantities(x)
     * 
     * PRECONDITION: discountUsedOnOrder must be between 0 and
     * previousDiscountPoints
     * 
     * @param customerID
     *            ID of the customer who made the purchase
     * @param itemIDs
     *            ArrayList containing all the items ordered for this order
     * @param itemQuantities
     *            ArrayList containing all the quantities for the items in this
     *            order. Must correspond directly (by index) with itemIDs
     * @param discountUsedOnOrder
     *            Discoutn points used by customer for this order. Must be
     * @param paid
     *            Boolean value - true if they have paid. false if not
     * @param pickupDate
     *            Date represenation of when the customer is scheduled to pick
     *            up the order
     * @return New bakery with the order added
     */
    public Bakery performTransaction(Integer customerID,
        ArrayList<Integer> itemIDs, ArrayList<Integer> itemQuantities,
        double discountUsedOnOrder, boolean paid, Date pickupDate) {
        double previousDiscountPoints = getCustomerRoll().getCustomer(
            customerID).getDiscountPoints();
        double previousLoyaltyPoints = getCustomerRoll().getCustomer(
            customerID).getLoyaltyPoints();

        if (previousDiscountPoints < discountUsedOnOrder) {
            throw new RuntimeException(
                "Not enough discount points for that user!");
        }

        double total = calculateOrderTotal(itemIDs, itemQuantities);

        // point for dollar, they're the same
        double loyaltyEarnedThisOrder = total - discountUsedOnOrder;

        double totalDue = total - discountUsedOnOrder;

        // To be stored in Order and Customer as discountPoints
        double newAvailableDiscount = previousDiscountPoints
            - discountUsedOnOrder
            + loyaltyToDiscountHelper(loyaltyEarnedThisOrder
                + previousLoyaltyPoints);

        // also referred to as availableDiscount for this order
        double newLoyaltyAmount = loyaltyToLoyalty(previousLoyaltyPoints
            + loyaltyEarnedThisOrder);

        CustomerRoll newCustomerRoll = getCustomerRoll().setPoints(
            customerID, newAvailableDiscount, newLoyaltyAmount);
        OrderList newOrderList = getOrderList();

        Integer orderID = newOrderList.getAvailableOrderID();

        for (int i = 0; i < itemIDs.size(); i++) {
            newOrderList = newOrderList.addToOrderList(customerID, orderID,
                total, paid, new Date(), pickupDate, getInventory().getItem(
                    itemIDs.get(i)), itemQuantities.get(i), newLoyaltyAmount,
                newAvailableDiscount, discountUsedOnOrder * -1);
        }

        System.out.println("--------------------------");
        System.out.println("--------RECIEPT-----------");
        System.out.println("--------------------------");
        System.out.println("Thank you, "
            + getCustomerRoll().getCustomer(customerID).getLastName());
        System.out.println("Order Date: " + new Date());
        System.out.println("Scheduled Pickup Date: " + pickupDate);
        for (int i = 0; i < itemIDs.size(); i++) {
            Item item = getInventory().getItem(itemIDs.get(i));
            System.out.println(itemQuantities.get(i) + " "
                + item.getItemName() + " - " + item.getCategory() + " :: $"
                + item.getPrice() * itemQuantities.get(i));
        }

        System.out.println("Total: $" + total);
        System.out.println("Discount Used: $" + discountUsedOnOrder);
        System.out.println("Final Cost: $" + totalDue);
        if (paid) {
            System.out.println("-----PAID-----");
        }
        else {
            System.out.println("--HAS-NOT-PAID--");
        }

        return new Bakery(getInventory(), newCustomerRoll, newOrderList);

    }

    /**
     * Helper function which finds the total a order will have
     * 
     * @param itemIDs
     *            ArrayList containing all the IDs of the items the customer has
     *            ordered
     * @param quantities
     *            Matching ArrayList where each item represents the number of
     *            items ordered corresponding to itemIDs
     * @return cost of the order
     */
    public double calculateOrderTotal(ArrayList<Integer> itemIDs,
        ArrayList<Integer> quantities) {
        double total = 0;
        for (int i = 0; i < itemIDs.size(); i++) {
            total += getInventory().getItem(itemIDs.get(i)).getPrice()
                * quantities.get(i);
        }
        return total;
    }

    /**
     * Helper function to find how many discount points were earned from loyalty
     * points. Returns 10 for every 100 points
     * 
     * @param loyaltyAmount
     *            number of loyalty points
     * @return number of discount points
     */
    private double loyaltyToDiscountHelper(double loyaltyAmount) {
        return Math.floor(loyaltyAmount / 100) * 10;
    }

    /**
     * Helper function to reduce loyaltyPoints to a number below 100
     * 
     * @param loyaltyAmount
     *            Number of loyalty points
     * @return a reset number of loyalty points between 0 and 100
     */
    private double loyaltyToLoyalty(double loyaltyAmount) {
        while (loyaltyAmount >= 100) {
            loyaltyAmount -= 100;
        }
        return loyaltyAmount;
    }

    /**
     * Function which dumps and entire Bakery into
     * 
     * @param filename
     *            Name of the file to save the inventory itno
     */
    public void saveInventory(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write("BakeryItemID\tBakeryItemName\tCategory\tPrice\n");
            for (Item i : getInventory()) {
                fw.write(i.getItemID().toString());
                fw.write("\t");
                fw.write(i.getItemName());
                fw.write("\t");
                fw.write(i.getCategory());
                fw.write("\t");
                fw.write(String.valueOf(i.getPrice()));
                fw.write("\n");
            }

            fw.flush();
            fw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.print("[ERROR] Could not save Inventory");
        }
    }

    /**
     * Function which dumps an entire Bakery Order Roll into a orders.txt file
     * 
     * @param filename
     *            Name of the file to save orders into
     */
    public void saveOrders(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);

            SimpleDateFormat dFormatter = new SimpleDateFormat("MM/dd/yy");

            fw.write("CustomerID\tLastName\tAddress\tCity\tState\tZipCode\t");
            fw.write("OrderID\tPaid?\tOrderDate\tPickupDate\tBakeryItemID\t");
            fw.write("BakeryItemName\tBakeryItemCategory\tQuantity\tPrice\t");
            fw.write("Total\tDiscountUsedOnOrder\tTotalDue\t");
            fw.write("AvailableDiscout\tCurrentLoyalty\n");

            for (Order o : getOrderList()) {

                Integer customerID = o.getCustomerID();
                Customer customer = getCustomerRoll().getCustomer(customerID);

                fw.write(customerID.toString());
                fw.write("\t");
                fw.write(customer.getLastName());
                fw.write("\t");
                fw.write(customer.getAddress());
                fw.write("\t");
                fw.write(customer.getCity());
                fw.write("\t");
                fw.write(customer.getState());
                fw.write("\t");
                fw.write(customer.getZipCode().toString());
                fw.write("\t");
                fw.write(o.getOrderID().toString());
                fw.write("\t");
                fw.write(o.paid() ? "Yes" : "No");
                fw.write("\t");
                fw.write(dFormatter.format(o.getOrderDate()));
                fw.write("\t");
                fw.write(dFormatter.format(o.getPickUpDate()));
                fw.write("\t");
                fw.write(o.getItem().getItemID().toString());
                fw.write("\t");
                fw.write(o.getItem().getItemName());
                fw.write("\t");
                fw.write(o.getItem().getCategory());
                fw.write("\t");
                fw.write(o.getQuantity().toString());
                fw.write("\t");
                fw.write(Double.toString(o.getItem().getPrice()));
                fw.write("\t");
                fw.write(Double.toString(o.getTotal()));
                fw.write("\t");
                fw.write(Double.toString(o.getDiscountUsedOnOrder()));
                fw.write("\t");
                fw.write(Double.toString(o.getTotalDue()));
                fw.write("\t");
                fw.write(Double.toString(o.getAvailableDiscount()));
                fw.write("\t");
                fw.write(Double.toString(o.getLoyaltyAtTimeOfOrder()));
                fw.write("\n");
            }
            fw.flush();
            fw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.print("[ERROR] Coudl not save Orders");
        }
    }

    /**
     * Main Method
     * 
     * @param args
     *            unused
     */
    public static void main(String[] args) {
        Bakery bakeryCtrl = new Bakery(Inventory.emptyInventory(),
            CustomerRoll.emptyRoll(), OrderList.emptyOrder());

        /**********************************************************************
         * Gather user input to load the Scanners for inventory/customers
         *********************************************************************/

        String userInput = "";

        Scanner inventoryScanner = new Scanner("");
        Scanner orderScanner = new Scanner("");
        boolean allSet = false;
        boolean skipLoad = false;

        while (!allSet) {
            System.out.println("Welcome to Schmiddty's Bakery!");
            System.out.println("------------------------------");
            System.out.println("1.) to use CCS provided data.");
            System.out
                .println("2.) to use resulting data from last runthrough");
            System.out.println("3.) Provide a new dataset");
            System.out.print("Enter [1/2/3/4]: ");

            userInput = bakeryCtrl.inputScanner.nextLine();
            System.out.println();
            if (userInput.equals("1")) {
                try {
                    File ordersFile = new File("orders.txt");
                    orderScanner = new Scanner(ordersFile);

                    File inventoryFile = new File("bakeryItems.txt");
                    inventoryScanner = new Scanner(inventoryFile);
                    allSet = true;
                }
                catch (Exception e) {
                    System.out.println("[ERROR] Failed to open "
                        + "orders.txt or bakeryItems.txt.");
                }
            }
            else if (userInput.equals("2")) {
                try {
                    File ordersFile = new File("ordersSave.txt");
                    orderScanner = new Scanner(ordersFile);

                    File inventoryFile = new File("bakeryItemsSave.txt");
                    inventoryScanner = new Scanner(inventoryFile);
                    allSet = true;
                }
                catch (Exception e) {
                    System.out.println("Failed to open ordersSave.txt or "
                        + "bakeryItemsSave.txt.");
                    System.out.println("Please do not select this "
                        + "option if a previous session has"
                        + " not been run.");
                }

            }
            else if (userInput.equals("3")) {

                System.out.println("------------------------------");
                try {
                    System.out.print("Orders Filename: ");
                    userInput = bakeryCtrl.inputScanner.nextLine();
                    File ordersFile = new File(userInput);
                    orderScanner = new Scanner(ordersFile);

                    System.out.print("Bakery Inventory Filename: ");
                    userInput = bakeryCtrl.inputScanner.nextLine();
                    File inventoryFile = new File(userInput);
                    inventoryScanner = new Scanner(inventoryFile);
                    allSet = true;
                }
                catch (Exception e) {
                    System.out.println("Failed to open " + userInput);
                }
            }
            else if (userInput.equals("4")) {
                skipLoad = true;
            }
            else {
                System.out.println("------------------------------");
                System.out.println("Invalid Selection.");
                System.out.print("Please choose 1, 2, 3, or 4: ");
            }
        }
        if (!skipLoad) {
            bakeryCtrl = bakeryCtrl.load(inventoryScanner, orderScanner);
        }
        bakeryCtrl = bakeryCtrl.runGUI();
        bakeryCtrl.saveOrders("ordersSave.txt");
        bakeryCtrl.saveInventory("itemsSave.txt");
        inventoryScanner.close();
        orderScanner.close();
    }

    /**
     * Loads inventory from the inventoryScanner and orders from the
     * ORderScanner into a new Bakery Object. These scanners must already be
     * initialized and open, and must conform to the required format (including
     * a header of column labels)
     * 
     * @param inventoryScanner
     *            Initialized and opened scanner for the inventory file
     * @param orderScanner
     *            Initialized and opened scanner for the order file
     * @return New Bakery with orders and inventory imported.
     */
    public Bakery load(Scanner inventoryScanner, Scanner orderScanner) {
        /**********************************************************************
         * Move items from scanner into data objects.
         *********************************************************************/

        Bakery bakeryCtrl = this;
        // skip the headers
        inventoryScanner.nextLine();

        // read actual data
        while (inventoryScanner.hasNext()) {
            String line = inventoryScanner.nextLine();
            String[] entries = line.split("\t");

            bakeryCtrl = bakeryCtrl.addToInventory(Integer
                .parseInt(entries[0]), entries[1], entries[2], Double
                .parseDouble(entries[3]));
        }

        System.out.print("Loading...");

        // skip the headers
        orderScanner.nextLine();

        // read the actual data
        while (orderScanner.hasNext()) {
            String line = orderScanner.nextLine();
            String [] entries = line.split("\t");

            Integer customerID = Integer.valueOf(entries[0]);
            String lastName = entries[1];
            String address = entries[2];
            String city = entries[3];
            String state = entries[4];
            Integer zipCode = Integer.valueOf(entries[5]);
            Integer orderID = Integer.valueOf(entries[6]);
            boolean paid = entries[7].equalsIgnoreCase("Yes") ? true : false;
            String sOrderDate = entries[8];
            String sPickupDate = entries[9];
            Integer bakeryItemID = Integer.valueOf(entries[10]);
            String bakeryItemName = entries[11];
            String bakeryItemCategory = entries[12];
            Integer quantity = Integer.valueOf(entries[13]);
            double price = Double.valueOf(entries[14]);
            double total = Double.valueOf(entries[15]);
            double discountUsedOnOrder = Double.valueOf(entries[16]);
            // double totalDue = Double.valueOf(entries[17]);
            double availableDiscount = Double.valueOf(entries[18]);
            double currentLoyalty = Double.valueOf(entries[19]);

            // Register the customer if necessary
            if (!bakeryCtrl.isRegisteredCustomer(customerID)) {
                bakeryCtrl = bakeryCtrl.registerNewCustomer(customerID,
                    lastName, address, city, state, zipCode);
            }

            // Register the item if necessary
            if (!bakeryCtrl.isInInventory(bakeryItemID)) {
                bakeryCtrl = bakeryCtrl.addToInventory(bakeryItemID,
                    bakeryItemName, bakeryItemCategory, price);
            }

            // Register the order
            SimpleDateFormat dFormatter = new SimpleDateFormat("MM/dd/yy");
            Date dPickupDate = null;
            Date dOrderDate = null;
            try {
                dPickupDate = dFormatter.parse(sPickupDate);
                dOrderDate = dFormatter.parse(sOrderDate);
            }
            catch (Exception e) {
                System.out.println("[ERROR] Invalid Date entered");
            }

            bakeryCtrl = bakeryCtrl.performTransaction(orderID, customerID,
                total, bakeryItemID, quantity, currentLoyalty,
                availableDiscount, discountUsedOnOrder, paid, dOrderDate,
                dPickupDate);
        }
        return bakeryCtrl;
    }

    /**
     * Function which runs all menu options by the user.
     * 
     * @return New Bakery with modifications made during the GUI interactions
     */
    private Bakery runGUI() {
        Bakery bakeryCtrl = this;
        /**********************************************************************
         * Run remaining GUI
         *********************************************************************/
        boolean admin = false;
        System.out.println("...loaded!");
        System.out.println("------------------------------");
        System.out.println("1.) Cashier Interface");
        System.out.println("2.) Owner Interface");
        System.out.print("Enter [1/2]: ");
        String userInput = bakeryCtrl.inputScanner.nextLine();
        System.out.println();

        if (userInput.equals("2")) {
            admin = true;
        }

        if (!admin) {
            while (true) {
                bakeryCtrl = bakeryCtrl.addNewOrder();
            }
        }

        boolean quit = false;
        while (!quit) {
            System.out.println("------------------------------");
            // orders
            System.out.println("ORDERS");
            System.out.println("1.) Add New Order");
            System.out.println("2.) View Existing Orders");
            System.out.println("3.) Update Existing Order");

            // customers
            System.out.println();
            System.out.println("CUSTOMERS");
            System.out.println("4.) Add New Customer");
            System.out.println("5.) View Existing Customer Information");
            System.out.println("6.) Update Existing Customer Info");

            // inventory
            System.out.println();
            System.out.println("INVENTORY");
            System.out.println("7.) Add Inventory Item");
            System.out.println("8.) View All Items in Inventory");
            System.out.println("9.) Update Inventory Items");

            System.out.println("10.) Save and Quit");
            System.out.print("Enter [1/2/3/4/5/6/7/8/9/10]: ");

            userInput = bakeryCtrl.inputScanner.nextLine();
            if (userInput.equals("1")) {
                bakeryCtrl = bakeryCtrl.addNewOrder();
            }
            else if (userInput.equals("2")) {
                bakeryCtrl.viewExistingOrders();
            }
            else if (userInput.equals("3")) {
                bakeryCtrl = bakeryCtrl.updateExistingOrders();
            }
            else if (userInput.equals("4")) {
                bakeryCtrl = bakeryCtrl.addNewCustomer();
            }
            else if (userInput.equals("5")) {
                bakeryCtrl.viewExistingCustomers();
            }
            else if (userInput.equals("6")) {
                bakeryCtrl = bakeryCtrl.updateExistingCustomer();
            }
            else if (userInput.equals("7")) {
                bakeryCtrl = bakeryCtrl.addInventoryItem();
            }
            else if (userInput.equals("8")) {
                bakeryCtrl.viewExistingInventory();
            }
            else if (userInput.equals("9")) {
                bakeryCtrl = bakeryCtrl.updateInventoryItems();
            }
            else if (userInput.equals("10")) {
                quit = true;
            }
            else {
                System.out.println("[ERROR] Invalid input.");
            }
        }

        return bakeryCtrl;
    }

    /**
     * GUI function used to update existing orders
     * 
     * @return new Bakery instance with modifications made by user
     */
    private Bakery updateExistingOrders() {
        // print existing orders
        System.out.println(getOrderList().toString());

        // Get item ID to be updated - ensure its valid
        boolean validInt = false;
        Integer orderID = -1;
        while (!validInt) {
            System.out.println("Please input Order ID to be updated");

            System.out.print("Order ID: ");
            String sItemID = inputScanner.nextLine();
            try {
                orderID = Integer.valueOf(sItemID);
            }
            catch (Exception e) {
                System.out.println("[ERROR] Not a valid input");
                continue;
            }
            validInt = true;
        }

        if (getOrderList().containsOrder(orderID)) {
            System.out.println(getOrderList().getOneOrderWithID(orderID));
            System.out.print("Please enter the following information: ");

            boolean validInput = false;
            boolean newPaidStatus = false;
            while (!validInput) {
                System.out.print("Paid [YES/NO]: ");
                String sPaidStatus = inputScanner.nextLine();
                if (sPaidStatus.equals("YES")) {
                    newPaidStatus = true;
                    validInput = true;
                    continue;
                }
                else if (sPaidStatus.equals("NO")) {
                    newPaidStatus = false;
                    validInput = true;
                    continue;
                }
                else {
                    System.out.println("[ERROR] Not a valid input");
                    continue;
                }
            }

            Date newPickupDate = null;
            validInput = false;
            while (!validInput) {
                System.out.println("------------");
                System.out.println("1.) Keep Same Pickup date");
                System.out.println("2.) Use Current date as Pickup Date");
                System.out.println("3.) Set New Pickup Date");

                String sUserInput = inputScanner.nextLine();
                if (sUserInput.equals("1")) {
                    newPickupDate = getOrderList().getOneOrderWithID(orderID)
                        .getPickUpDate();
                    validInput = true;
                }
                else if (sUserInput.equals("2")) {
                    newPickupDate = new Date();
                    validInput = true;
                }
                else if (sUserInput.equals("3")) {

                    boolean validDate = false;
                    while (!validDate) {
                        // get user input
                        System.out
                            .println("Enter a new Pickup Date (mm/dd/yyyy): ");
                        String userInput = inputScanner.nextLine();

                        // convert to date object
                        SimpleDateFormat dFormatter = new SimpleDateFormat(
                            "MM/dd/yy");
                        try {
                            newPickupDate = dFormatter.parse(userInput);
                            validDate = true;
                            continue;
                        }
                        catch (Exception e) {
                            System.out.println("[ERROR] Invalid input.");
                        }
                    }
                    validInput = true;
                }
                else {
                    System.out.println("[ERROR] Invalid Input");
                }
            }

            OrderList updatedOrders = getOrderList().getOrdersByOrderID(
                orderID).withNewStatus(newPaidStatus, newPickupDate);

            return new Bakery(getInventory(), getCustomerRoll(),
                getOrderList().removeOrdersWithID(orderID).addToOrderList(
                    updatedOrders));
        }
        else {
            System.out.println("That inventory item does not exist!");
            return this;
        }
    }

    /**
     * GUI function used to add new orders
     * 
     * @return new Bakery with orders added by user
     */
    Bakery addNewOrder() {
        Bakery modifiedBakery = this;

        /*************************************
         * Get Customer Info, create if necessary
         **************************************/
        System.out.println("Please enter the following customer info:");

        System.out.print("Last Name: ");
        String lastName = inputScanner.nextLine();
        System.out.println();

        System.out.print("Address: ");
        String address = inputScanner.nextLine();
        address = inputScanner.nextLine();
        System.out.println();

        System.out.print("City: ");
        String city = inputScanner.nextLine();
        System.out.println();

        System.out.print("State: ");
        String state = inputScanner.nextLine();
        System.out.println();

        Integer zipCode = 0;
        boolean validZip = false;
        while (!validZip) {
            System.out.print("Zip Code: ");
            String sZipCode = inputScanner.nextLine();
            zipCode = Integer.valueOf(sZipCode);
            validZip = true;

            try {
                zipCode = Integer.valueOf(sZipCode);
            }
            catch (Exception e) {
                System.out.println("Invalid zip code.");
            }
            if (zipCode <= 0 || zipCode > 99999) {
                System.out.println("Invalid zip code.");
                continue;
            }
        }
        System.out.println();

        // Register Customer if need be
        if (!modifiedBakery.isRegisteredCustomer(lastName, address, city,
            state, zipCode)) {
            modifiedBakery = modifiedBakery.registerNewCustomer(lastName,
                address, city, state, zipCode);
        }

        // Get their customer ID
        Integer customerID = modifiedBakery.getCustomerRoll().getCustomerID(
            lastName, address, city, state, zipCode);

        /**************************************
         * Begin Order Total Processing
         **************************************/
        // Get AvailableDiscount
        double availableDiscount = modifiedBakery.getCustomerRoll()
            .getCustomer(customerID).getDiscountPoints();

        // Gather Items, Calculate Total
        double total = 0;
        boolean notDoneOrdering = true;
        ArrayList<Integer> itemIDs = new ArrayList<Integer>();
        ArrayList<Integer> itemQuantities = new ArrayList<Integer>();

        while (notDoneOrdering) {
            // Gather the items ordered
            System.out.println("Enter an Item ID, or type 'DONE': ");
            Integer itemID;
            String userInput = inputScanner.nextLine();

            // check if they quit
            if (userInput.equals("DONE")) {
                notDoneOrdering = false;
                continue;
            }

            try {
                itemID = Integer.valueOf(userInput);
                getInventory().getItem(itemID);
            }
            catch (Exception e) {
                System.out.println("[ERROR] Invalid Item ID");
                continue;
            }

            if (itemIDs.contains(itemID)) {
                System.out.println("[ERROR] You already added that item.");
                continue;
            }

            System.out.println("How many "
                + getInventory().getItem(itemID).getItemName() + ": ");
            userInput = inputScanner.nextLine();
            Integer itemQuantity = null;
            try {
                itemQuantity = Integer.valueOf(userInput);
            }
            catch (Exception e) {
                System.out.println("[ERROR] Invalid Quantity");
                continue;
            }

            itemIDs.add(itemID);
            itemQuantities.add(itemQuantity);

            total += getInventory().getPrice(itemID) * (double) itemQuantity;
        }

        // Print Total and availableDiscount
        System.out.println("The total for your order is: " + total);
        System.out.println("Your current available rewards points is: "
            + availableDiscount);

        // Get the discountUsedOnOrder from customer input
        // double loyaltyEarnedThisOrder = total;
        double discountUsedOnOrder = 0;

        if (availableDiscount > 0) {
            boolean validInput = false;
            while (!validInput) {
                System.out.println("How many points would you like to apply "
                    + "to this order (or 0 if none): ");
                String sPointsUsed = inputScanner.nextLine();
                try {
                    discountUsedOnOrder = Double.valueOf(sPointsUsed);
                    if (discountUsedOnOrder <= availableDiscount
                        && discountUsedOnOrder >= 0) {
                        validInput = true;
                    }
                }
                catch (Exception e) {
                    System.out.println("Not a valid number!");
                }
            }
        }

        // Get when they are paying
        boolean paid = false;

        boolean validInput = false;
        while (!validInput) {
            System.out.println("-------------");
            System.out.println("1.) Pay Now");
            System.out.println("2.) Pay Later");
            System.out.print("Select [1/2]: ");
            String userInput = inputScanner.nextLine();
            try {
                Integer selection = Integer.valueOf(userInput);
                if (selection.equals(1)) {
                    paid = true;
                    validInput = true;
                }
                else if (selection.equals(2)) {
                    paid = false;
                    validInput = true;
                }
                else {
                    System.out.println("[ERROR] Invalid Input");
                }
            }
            catch (Exception e) {
                System.out.println("[ERROR] Invalid Input");
            }
        }

        // Get pickup date
        validInput = false;
        Date dPickupDate = null;
        while (!validInput) {
            // get user input
            System.out.println("Please Submit a pickup date (mm/dd/yyyy): ");
            String userInput = inputScanner.nextLine();

            // convert to date object
            SimpleDateFormat dFormatter = new SimpleDateFormat("MM/dd/yy");
            try {
                dPickupDate = dFormatter.parse(userInput);
                validInput = true;
            }
            catch (Exception e) {
                System.out.println("[ERROR] Invalid input.");
            }
        }

        modifiedBakery = modifiedBakery.performTransaction(customerID,
            itemIDs, itemQuantities, discountUsedOnOrder, paid, dPickupDate);

        return modifiedBakery;
    }

    /**
     * GUI Function used to print customers registered in the system
     */
    void viewExistingCustomers() {
        boolean quit = false;
        while (!quit) {
            System.out.println("------------");
            System.out
                .println("1.) Print All Registered Customer Information");
            System.out.println("2.) Print Customer by ID");
            System.out.println("3.) Print All Customers by Last Name");
            System.out.println("4.) Go Back");
            System.out.print("Select [1/2/3/4]: ");
            String userInput = inputScanner.nextLine();

            if (userInput.equals("1")) {
                System.out.println(getCustomerRoll().toString());
                quit = true;
            }
            else if (userInput.equals("2")) {
                System.out.println("------------");
                System.out.print("User ID: ");
                String idInput = inputScanner.nextLine();
                Integer customerID = 0;

                try {
                    customerID = Integer.valueOf(idInput);
                }
                catch (Exception e) {
                    System.out.println("[ERROR] Not a valid input");
                    continue;
                }

                if (isRegisteredCustomer(customerID)) {
                    System.out.println(getCustomerRoll().getCustomer(
                        customerID));
                    System.out.println(getOrderList().getOrdersByCustomerID(
                        customerID));
                    quit = true;
                }
                else {
                    System.out
                        .println("[ERROR] No customer exists with that ID");
                }

            }
            else if (userInput.equals("3")) {
                System.out.println("------------");
                System.out.print("Last Name: ");
                String lastName = inputScanner.nextLine();
                System.out.println(getCustomerRoll().getCustomersByLastName(
                    lastName).toString());
            }
            else if (userInput.equals("4")) {
                quit = true;
            }

            else {
                System.out.println("[ERROR] Invalid input.");
            }
        }
    }

    /**
     * GUI function used to add new customers to the bakery
     * 
     * @return new Bakery with customers added by user
     */
    public Bakery addNewCustomer() {
        System.out.println("Please enter the following customer info:");

        System.out.print("Last Name: ");
        String lastName = inputScanner.nextLine();
        System.out.println();

        System.out.print("Address: ");
        String address = inputScanner.nextLine();
        System.out.println();

        System.out.print("City: ");
        String city = inputScanner.nextLine();
        System.out.println();

        System.out.print("State: ");
        String state = inputScanner.nextLine();
        System.out.println();

        Integer zipCode = 0;
        boolean validZip = false;
        while (!validZip) {
            System.out.print("Zip Code: ");
            String sZipCode = inputScanner.nextLine();
            zipCode = Integer.valueOf(sZipCode);
            validZip = true;

            try {
                zipCode = Integer.valueOf(sZipCode);
            }
            catch (Exception e) {
                System.out.println("Invalid zip code.");
            }
            if (zipCode <= 0 || zipCode > 99999) {
                System.out.println("Invalid zip code.");
                continue;
            }
        }
        System.out.println();

        if (!isRegisteredCustomer(lastName, address, city, state, zipCode)) {
            return registerNewCustomer(lastName, address, city, state,
                zipCode);
        }
        else {
            System.out.println("That customer already exists!");
            return this;
        }
    }

    /**
     * GUI function used to add new items to the inventory
     * 
     * @return New Bakery with inventory additions made by user
     */
    public Bakery addInventoryItem() {
        System.out.println("Please enter the following Item info:");

        System.out.print("Item Name: ");
        String itemName = inputScanner.nextLine();
        System.out.println();

        System.out.print("Item Category: ");
        String itemCategory = inputScanner.nextLine();
        System.out.println();

        System.out.print("Item Price: ");
        String sItemPrice = inputScanner.nextLine();
        double itemPrice = Double.valueOf(sItemPrice);
        System.out.println();

        if (!isInInventory(itemName, itemCategory)) {
            return addToInventory(itemName, itemCategory, itemPrice);
        }
        else {
            throw new RuntimeException("That inventory item already exists!");
        }
    }

    /**
     * GUI function which prints entire inventory
     */
    void viewExistingInventory() {
        System.out.println(getInventory().toString());
    }

    /**
     * Gui function used to view existing order reciepts.
     */
    void viewExistingOrders() {
        boolean quit = false;
        while (!quit) {
            System.out.println("------------");
            System.out.println("1.) Print All Orders");
            System.out.println("2.) Print Orders by Specific Customer");
            System.out.println("3.) Print Orders with Specific Order Date");
            System.out.println("4.) Print Orders with Specific Pickup Date");
            System.out.println("5.) Go Back");
            System.out.print("Select [1/2/3/4/5]: ");
            String userInput = inputScanner.nextLine();

            if (userInput.equals("1")) {
                System.out.println(getOrderList().toString());
                quit = true;
            }
            else if (userInput.equals("2")) {
                System.out.println("------------");
                System.out.println(getCustomerRoll());
                System.out.println("------------");
                System.out.print("User ID: ");
                String idInput = inputScanner.nextLine();
                Integer customerID = 0;

                try {
                    customerID = Integer.valueOf(idInput);
                }
                catch (Exception e) {
                    System.out.println("[ERROR] Not a valid input");
                    continue;
                }

                if (isRegisteredCustomer(customerID)) {
                    System.out.println(getCustomerRoll().getCustomer(
                        customerID));
                    System.out.println(getOrderList().getOrdersByCustomerID(
                        customerID));
                    quit = true;
                }
                else {
                    System.out
                        .println("[ERROR] No customer exists with that ID");
                }

            }
            else if (userInput.equals("3")) {
                System.out.println("------------");
                Date dPickupDate = null;
                boolean validInput = false;
                while (!validInput) {
                    // get user input

                    // get user input
                    System.out.println("Please Submit a Order Date date "
                        + "(mm/dd/yyyy): ");
                    String dateInput = inputScanner.nextLine();

                    // convert to date object
                    SimpleDateFormat dFormatter = new SimpleDateFormat(
                        "MM/dd/yy");
                    try {
                        dPickupDate = dFormatter.parse(dateInput);
                        validInput = true;
                    }
                    catch (Exception e) {
                        System.out.println("[ERROR] Invalid input.");
                    }
                }

                System.out.println(getOrderList().getOrdersPlacedOn(
                    dPickupDate));
                quit = true;
            }

            else if (userInput.equals("4")) {
                System.out.println("------------");
                Date dOrderDate = null;
                boolean validInput = false;
                while (!validInput) {
                    // get user input

                    // get user input
                    System.out
                        .println("Please Submit a pickup date (mm/dd/yyyy): ");
                    String dateInput = inputScanner.nextLine();

                    // convert to date object
                    SimpleDateFormat dFormatter = new SimpleDateFormat(
                        "MM/dd/yy");
                    try {
                        dOrderDate = dFormatter.parse(dateInput);
                        validInput = true;
                    }
                    catch (Exception e) {
                        System.out.println("[ERROR] Invalid input.");
                    }
                }

                System.out.println(getOrderList().getOrdersWithPickupDate(
                    dOrderDate));
                quit = true;
            }
            else if (userInput.equals("5")) {
                quit = true;
            }
            else {
                System.out.println("[ERROR] Invalid input.");
            }
        }
    }

    /**
     * GUI Function used to update items in the inventory
     * 
     * @return New Bakery with updated items in its inventory
     */
    public Bakery updateInventoryItems() {
        // Print entire inventory
        System.out.println(getInventory().toString());

        // Get item ID to be updated - ensure its valid
        boolean validInt = false;
        Integer itemID = -1;
        while (!validInt) {
            System.out.println("Please input Item ID to be updated");

            System.out.print("Item ID: ");
            String sItemID = inputScanner.nextLine();
            try {
                itemID = Integer.valueOf(sItemID);
            }
            catch (Exception e) {
                System.out.println("[ERROR] Not a valid input");
                continue;
            }
            validInt = true;
        }

        if (isInInventory(itemID)) {
            System.out.println("Please enter the following Item info:");

            System.out.print("Item Name: ");
            String itemName = inputScanner.nextLine();
            System.out.println();

            System.out.print("Item Category: ");
            String itemCategory = inputScanner.nextLine();
            System.out.println();

            boolean validDub = false;
            double itemPrice = 0.0;
            while (!validDub) {
                System.out.print("Item Price: ");
                String sItemPrice = inputScanner.nextLine();
                try {
                    itemPrice = Double.valueOf(sItemPrice);
                }
                catch (Exception e) {
                    System.out.println("[ERROR] Not a valid input");
                    continue;
                }
                validDub = true;
            }
            System.out.println();

            return removeFromInventory(itemID).addToInventory(itemID,
                itemName, itemCategory, itemPrice);
        }
        else {
            throw new RuntimeException("That inventory item does not exist!");
        }
    }

    /**
     * GUI function used to update existing customers
     * 
     * @return new Bakery with customer information updated
     */
    public Bakery updateExistingCustomer() {
        System.out.println(getCustomerRoll().toString());

        System.out.println("Please input User ID to be updated");

        System.out.print("User ID: ");
        String sCustomerID = inputScanner.nextLine();
        Integer customerID = Integer.valueOf(sCustomerID);

        if (isRegisteredCustomer(customerID)) {
            System.out.println("Please enter the following customer info:");

            System.out.print("Last Name: ");
            String lastName = inputScanner.nextLine();
            System.out.println();

            System.out.print("Address: ");
            String address = inputScanner.nextLine();
            System.out.println();

            System.out.print("City: ");
            String city = inputScanner.nextLine();
            System.out.println();

            System.out.print("State: ");
            String state = inputScanner.nextLine();
            System.out.println();

            Integer zipCode = 0;
            boolean validZip = false;
            while (!validZip) {
                System.out.print("Zip Code: ");
                String sZipCode = inputScanner.nextLine();
                zipCode = Integer.valueOf(sZipCode);

                try {
                    zipCode = Integer.valueOf(sZipCode);
                }
                catch (Exception e) {
                    System.out.println("Invalid zip code.");
                    continue;
                }
                if (zipCode <= 0 || zipCode > 99999) {
                    System.out.println("Invalid zip code.");
                    continue;
                }
                validZip = true;
            }
            System.out.println();

            return removeCustomer(customerID).registerNewCustomer(customerID,
                lastName, address, city, state, zipCode);
        }
        else {
            throw new RuntimeException("That user does not exist!");
        }
    }
}

