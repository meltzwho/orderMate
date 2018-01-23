import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class mainUI extends Application {
	
	private final double TAX=1.08;
	double total=0;
	private String password;
	File file=new File("password.txt");
	
	Statement stmt;
	boolean recordFound=false;
	Label status = new Label();
	Label orderTotal=new Label("Total (w/ tax): ");
	ComboBox<String> loadMenu=new ComboBox<String>();
	TextField tfPhoneNumber;
	TextField tfCustomerName;
	TextField tfCustomerAddress;
	TextField tfMenu;
	TextField tfCat;
	TextField tfItem;
	TextField tfPrice;
	TextField tfCustomItem;
	TextField tfCustomPrice;
	ListView<String> orderSummary;
	ListView<Integer> prevOrders=new ListView<Integer>();
	ObservableList<String> tmplineItems=FXCollections.observableArrayList();
	ObservableList<String> lineItems=FXCollections.observableArrayList();
	ObservableList<String> menus=FXCollections.observableArrayList();
	ObservableList<String> categories=FXCollections.observableArrayList();
	ObservableList<String> items=FXCollections.observableArrayList();
	ObservableList<Integer> orders=FXCollections.observableArrayList();
	
	public void start(Stage mainStage) throws FileNotFoundException {
		//connect to database
		connectDB();

		//password protection
		if(file.exists()) {
			Scanner in=new Scanner(file);
			password=in.next();
			in.close();
		}
		else {
			PrintWriter out=new PrintWriter(file);
			out.write("PASSWORD");
			out.close();
		}
		
		//create plug
		Label plug=new Label("OrderMate by Eric Meltzer");
		plug.setFont(Font.font(20));
		
		//create panes
		GridPane mainPane=new GridPane();
		GridPane customerPane=new GridPane();
		GridPane prevOrderPane=new GridPane();
		GridPane orderPane=new GridPane();
		GridPane menuPane=new GridPane();
		GridPane modLIPane=new GridPane();
		GridPane controlPane=new GridPane();
		GridPane passwordPane=new GridPane();
		GridPane modMenuPane=new GridPane();
		GridPane changePassPane=new GridPane();
		
		//create intermediate pane for switching between screens
		GridPane intermediatePane=new GridPane();
		intermediatePane.setHgap(5);
		intermediatePane.setAlignment(Pos.CENTER);
		intermediatePane.add(plug,0,1);
		
		//main OrderMate screen (mainPane)
		Button takeOut=new Button("Take-out");
		Button delivery=new Button("Delivery");
		Button dineIn=new Button("Dine-in");
		Button controlPanel=new Button("Control Panel");
		
		//register buttons
		takeOut.setOnAction(e -> {
			try {
				tfMenu.setText(loadMenu.getSelectionModel().getSelectedItem());
				viewCats();
				intermediatePane.getChildren().clear();
				intermediatePane.add(customerPane,0,0);
			}catch(NullPointerException ex) {
				status.setText("Expection: no menu selected");
			}
		});
		
		delivery.setOnAction(e -> {
			try {
				tfMenu.setText(loadMenu.getSelectionModel().getSelectedItem());
				viewCats();
				intermediatePane.getChildren().clear();
				intermediatePane.add(customerPane,0,0);
			}catch(NullPointerException ex) {
				status.setText("Expection: no menu selected");
			}
		});
		dineIn.setOnAction(e -> {
			try{
				tfMenu.setText(loadMenu.getSelectionModel().getSelectedItem());
				viewCats();
				tfPhoneNumber.setText("dineIN");
				viewCust();
				if(!recordFound) {
					insertCust();
				}
				else{
					updateCust();
				}
				intermediatePane.getChildren().clear();
				intermediatePane.add(orderPane,0,0);
			}catch(NullPointerException ex) {
				status.setText("Expection: no menu selected");
			}
		});
		
		controlPanel.setOnAction(e -> {
			intermediatePane.getChildren().clear();
			intermediatePane.add(passwordPane,0,0);
		});
		
		//add buttons to mainPane
		mainPane.add(takeOut, 0, 0);
		mainPane.add(delivery, 0, 1);
		mainPane.add(dineIn, 0, 2);
		mainPane.add(controlPanel, 1, 1);
		
		//mainPane organization
		mainPane.setAlignment(Pos.CENTER);
		mainPane.setPadding(new Insets(15));
		mainPane.setHgap(50);
		mainPane.setVgap(5);
		mainPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		
		//customer details screen (customerPane)
		Label pN=new Label("Phone number:");
		Label cN=new Label("Customer name:");
		Label cA=new Label("Customer address:");
		tfPhoneNumber=new TextField();
		tfCustomerName=new TextField();
		tfCustomerAddress=new TextField();
		Button cPdone=new Button("Done");
		cPdone.setPrefWidth(100);
		Button cPback=new Button("Back");
		Button cPprevOrder=new Button("Previous Order");
		
		//register buttons
		cPdone.setOnAction(e -> {
			if(!recordFound) {
				insertCust();
			}
			else{
				updateCust();
			}
			viewCats();
			intermediatePane.getChildren().clear();
			intermediatePane.add(orderPane,0,0);
			recordFound=false;
		});
		
		cPback.setOnAction(e -> {
			recordFound=false;
			tfPhoneNumber.clear();
			tfCustomerName.clear();
			tfCustomerAddress.clear();
			lineItems.clear();
			total=0;
			orderTotal.setText
			(String.format("%s %.02f","Total (w/ tax): ",total*TAX));
			intermediatePane.getChildren().clear();
			intermediatePane.add(plug, 0, 0);
			intermediatePane.add(mainPane,0,1);
		});
		
		cPprevOrder.setOnAction(e -> {
			tmplineItems.setAll(lineItems);
			orders.clear();
			viewCust();
			viewLineItems();
			intermediatePane.getChildren().clear();
			intermediatePane.add(prevOrderPane, 0, 0);
		});
		
		//add listener to find tfPhoneNumber in DB
		tfPhoneNumber.setOnKeyReleased(e->{
			viewCust();
		});
		
		//add nodes to customerPane
		customerPane.add(pN, 0, 0);
		customerPane.add(tfPhoneNumber, 1, 0);
		customerPane.add(cN, 0, 1);
		customerPane.add(tfCustomerName, 1, 1);
		customerPane.add(cA, 0, 2);
		customerPane.add(tfCustomerAddress, 1, 2);
		customerPane.add(cPback, 0, 3);
		customerPane.add(cPdone, 2, 3);
		customerPane.add(cPprevOrder, 2, 2);
		
		//customerPane organization
		customerPane.setAlignment(Pos.CENTER);
		customerPane.setPadding(new Insets(15));
		customerPane.setHgap(50);
		customerPane.setVgap(5);
		customerPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		
		//previous orders screen (prevOrderPane)
		Label lblPrevOrders=new Label("Order number:");
		prevOrders.setItems(orders);
		Label lblOrderItems=new Label("Items:");
		ListView<String> prevOrderItems=new ListView<String>();
		prevOrderItems.setItems(lineItems);
		Button popDone=new Button("Done");
		Button dupeOrder=new Button("Duplicate order");
		Label lblPrevOrderTotal=new Label("Total (w/ tax): ");
		
		//register nodes
		prevOrders.setOnMouseClicked(e->{
			viewLineItems();
			double prevTotal=0;
			for(String li: lineItems) {
				prevTotal+=Double.parseDouble(li.substring(li.indexOf('$')+1, li.length()));
			}
			lblPrevOrderTotal.setText
			(String.format("%s %.02f","Total (w/ tax): ",prevTotal*TAX));
		});
		popDone.setOnAction(e->{
			lineItems.setAll(tmplineItems);
			lblPrevOrderTotal.setText(String.format("%s","Total (w/ tax): "));
			intermediatePane.getChildren().clear();
			intermediatePane.add(customerPane,0,0);
		});
		dupeOrder.setOnAction(e->{
			if(!prevOrders.getSelectionModel().isEmpty()) {
				total=0;
				for(String li: lineItems) {
					total+=Double.parseDouble(li.substring(li.indexOf('$')+1, li.length()));
				}
				orderTotal.setText
				(String.format("%s %.02f","Total (w/ tax): ",total*TAX));
				lblPrevOrderTotal.setText(String.format("%s","Total (w/ tax): "));
				intermediatePane.getChildren().clear();
				intermediatePane.add(orderPane,0,0);
			}
		});
		
		//add nodes to prevrOrderPane
		prevOrderPane.add(lblPrevOrders, 0, 0);
		prevOrderPane.add(prevOrders, 0, 1);
		prevOrderPane.add(lblOrderItems, 1, 0);
		prevOrderPane.add(prevOrderItems, 1, 1);
		prevOrderPane.add(dupeOrder, 2, 1);
		prevOrderPane.add(lblPrevOrderTotal, 1, 2);		
		prevOrderPane.add(popDone, 1, 3);
		
		//prevOrderPane organization
		prevOrderPane.setAlignment(Pos.CENTER);
		prevOrderPane.setPadding(new Insets(10));
		prevOrderPane.setHgap(10);
		prevOrderPane.setVgap(5);
		prevOrderPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));			
		
		//menu and order summary screen (orderPane / menuPane)
		//menuPane
		ListView<String> mpMenuItems=new ListView<String>();
		mpMenuItems.setItems(items);
		ListView<String> mpMenuCats=new ListView<String>();
		mpMenuCats.setItems(categories);
		mpMenuItems.setPrefWidth(150);
		mpMenuCats.setPrefWidth(150);
		
		//add listeners to find menu/cat/item(s) in DB
		mpMenuCats.setOnMouseClicked(e->{
			tfCat.setText(mpMenuCats.getSelectionModel().getSelectedItem());
			viewItems();
		});
		mpMenuItems.setOnMouseClicked(e->{
			String[] split=mpMenuItems.getSelectionModel().getSelectedItem().split("\\$");
			tfItem.setText(split[0].trim());
			tfPrice.setText(split[1].trim());
		});

		//add nodes to menuPane
		menuPane.add(new Label("Choose category: "), 0, 0);
		menuPane.add(mpMenuCats, 0, 1);
		menuPane.add(new Label("Choose item: "), 1, 0);
		menuPane.add(mpMenuItems, 1, 1);
		
		//modLIPane
		Label mliItem=new Label();
		Label mliCustomItem=new Label("Special instructions: ");
		Label mliPrice=new Label("Price: ");
		TextField tfCustomItem=new TextField();
		TextField tfCustomPrice=new TextField();
		Button mliRemoveItem=new Button("Remove item");
		Button mliCancel=new Button("Cancel");
		Button mliUpdate=new Button("Update");
						
		//register buttons
		//mliRemoveItem
		//subtract from total, if menuItem remove all customItems linked to it, else remove the menu item
		//and update orderTotal
		mliRemoveItem.setOnAction(e->{
			removeLI();
			intermediatePane.getChildren().clear();
			intermediatePane.add(orderPane,0,0);
		});
		mliCancel.setOnAction(e->{
			intermediatePane.getChildren().clear();
			intermediatePane.add(orderPane,0,0);
		});
		mliUpdate.setOnAction(e->{
			total+=Double.parseDouble(tfCustomPrice.getText().trim());
			orderTotal.setText
			(String.format("%s %.02f","Total (w/ tax): ",total*TAX));
			lineItems.add(orderSummary.getSelectionModel().getSelectedIndex()+1,
					">>"+tfCustomItem.getText().trim()+"\t$"
					+tfCustomPrice.getText().trim());
			intermediatePane.getChildren().clear();
			intermediatePane.add(orderPane,0,0);
		});
						
		//add nodes to modLIPane
		modLIPane.add(mliItem, 0, 0);
		modLIPane.add(mliCustomItem, 0, 1);
		modLIPane.add(tfCustomItem, 1, 1);
		modLIPane.add(mliPrice, 2, 1);
		modLIPane.add(tfCustomPrice, 3, 1);
		modLIPane.add(mliCancel, 2, 2);
		modLIPane.add(mliRemoveItem, 0, 2);
		modLIPane.add(mliUpdate, 3, 2);
			
		//modLIPane organization
		modLIPane.setAlignment(Pos.CENTER);
		modLIPane.setPadding(new Insets(10));
		modLIPane.setHgap(10);
		modLIPane.setVgap(5);
		modLIPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		
		//orderPane
		//order summary
		orderSummary=new ListView<String>();
		orderSummary.setItems(lineItems);
		orderSummary.setPrefWidth(200);
		
		//more buttons for orderPane
		Button oPdone=new Button("Done");
		oPdone.setPrefWidth(200);
		Button oPback=new Button("Back");
		Button oPaddItem=new Button("Add item");
		oPaddItem.setPrefWidth(100);
		Button modifyLI=new Button("Modify line item");
		
		//register buttons
		//add order to DB
		oPdone.setOnAction(e ->{
			if(!lineItems.isEmpty()) {
				insertOrder();
				tfPhoneNumber.clear();
				tfCustomerName.clear();
				tfCustomerAddress.clear();
				lineItems.clear();
				mpMenuCats.getSelectionModel().clearSelection();
				intermediatePane.getChildren().clear();
				intermediatePane.add(plug, 0, 0);
				intermediatePane.add(mainPane,0,1);
			}
		});
		oPback.setOnAction(e ->{
			mpMenuCats.getSelectionModel().clearSelection();
			intermediatePane.getChildren().clear();
			intermediatePane.add(customerPane,0,0);
		});
		oPaddItem.setOnAction(e->{
			if(!(mpMenuItems.getSelectionModel().isEmpty())) {
				lineItems.add(mpMenuItems.getSelectionModel().getSelectedItem());
				total+=Double.parseDouble(tfPrice.getText().trim());
				orderTotal.setText
				(String.format("%s %.02f","Total (w/ tax): ",total*TAX));
			}
		});
		
		modifyLI.setOnAction(e ->{
			if(!orderSummary.getSelectionModel().isEmpty()) {
			tfCustomItem.clear();
			tfCustomPrice.clear();
			mliItem.setText("Item: "+orderSummary.getSelectionModel().getSelectedItem());
			intermediatePane.getChildren().clear();
			intermediatePane.add(modLIPane,0,0);
			}
		});	
		
		//add nodes to orderPane
		orderPane.add(menuPane, 0, 1);
		orderPane.add(oPback, 0, 3);
		orderPane.add(oPdone, 2, 3);
		orderPane.add(new Label("Order summary: "), 2, 0);
		orderPane.add(orderTotal, 2, 2);
		orderPane.add(orderSummary, 2, 1);
		orderPane.add(oPaddItem, 1, 1);
		orderPane.add(modifyLI, 1, 3);
		
		//orderPane organization
		orderPane.setAlignment(Pos.CENTER);
		orderPane.setPadding(new Insets(10));
		orderPane.setHgap(5);
		orderPane.setVgap(5);
		orderPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		
		//control panel screen (controlPane)
		loadMenu.setItems(menus);
		Label selectMenu=new Label("Select menu: ");
		Button modMenu=new Button("Modify menu");
		Button changePass=new Button("Change password");
		Button applyChanges=new Button("Apply changes");
		
		//register buttons
		loadMenu.setOnMouseClicked(e->{
			viewMenu();
		});
		modMenu.setOnAction(e -> {
			viewMenu();
			intermediatePane.getChildren().clear();
			intermediatePane.add(modMenuPane, 0, 0);			
		});
		changePass.setOnAction(e -> {
			intermediatePane.getChildren().clear();
			intermediatePane.add(changePassPane, 0, 0);		
		});
		applyChanges.setOnAction(e -> {
			if(!loadMenu.getSelectionModel().isEmpty())
				status.setText("Active menu: "
			+loadMenu.getSelectionModel().getSelectedItem());
			intermediatePane.getChildren().clear();
			intermediatePane.add(status, 0, 0);
			intermediatePane.add(plug, 0, 1);
			intermediatePane.add(mainPane, 0, 2);
		});
		
		//add nodes to controlPane		
		controlPane.add(selectMenu, 0, 0);
		controlPane.add(loadMenu, 1, 0);
		controlPane.add(modMenu, 0, 1);
		controlPane.add(changePass, 0, 2);
		controlPane.add(applyChanges, 1, 2);		
		
		//controlPane organization
		controlPane.setAlignment(Pos.CENTER);
		controlPane.setPadding(new Insets(10));
		controlPane.setHgap(10);
		controlPane.setVgap(5);
		controlPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		
		//password entry screen (passwordPane)
		Label passPrompt=new Label("Enter password: ");
		TextField tfPassword=new TextField();
		Button pPback=new Button("Back");
		Button pPdone=new Button("Done");
		
		//register buttons
		pPback.setOnAction(e->{
			tfPassword.clear();
			passPrompt.setText("Enter password: ");
			intermediatePane.getChildren().clear();
			intermediatePane.add(status, 0, 0);
			intermediatePane.add(plug, 0, 1);
			intermediatePane.add(mainPane, 0, 2);
		});
		pPdone.setOnAction(e->{
			if(tfPassword.getText().equals(password)) {
				tfPassword.clear();
				passPrompt.setText("Enter password: ");
				intermediatePane.getChildren().clear();
				intermediatePane.add(controlPane,0,0);
			}
			else {
				passPrompt.setText("Password incorrect: ");
			}
		});
		
		//add nodes to passwordPane
		passwordPane.add(passPrompt, 0, 0);
		passwordPane.add(tfPassword, 1, 0);
		passwordPane.add(pPback, 0, 1);
		passwordPane.add(pPdone, 1, 1);
		
		//passwordPane organization
		passwordPane.setAlignment(Pos.CENTER);
		passwordPane.setPadding(new Insets(10));
		passwordPane.setHgap(10);
		passwordPane.setVgap(5);
		passwordPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		
		//change password screen (changePassPane)
		Label lblNewPass=new Label("Enter new password: ");
		TextField tfChangePass=new TextField();
		Button confirmChangePass=new Button("Change password");
		confirmChangePass.setPrefWidth(150);
		
		//register buttons
		confirmChangePass.setOnAction(e->{
			try {
				PrintWriter out=new PrintWriter(file);
				password=tfChangePass.getText();
				out.write(password);
				out.close();
			} catch (FileNotFoundException ex) {
				//do nothing
			}
			tfChangePass.clear();
			intermediatePane.getChildren().clear();
			intermediatePane.add(controlPane, 0, 0);
		});
		
		//add nodes to changePassPane
		changePassPane.add(lblNewPass, 0, 0);
		changePassPane.add(tfChangePass, 1, 0);
		changePassPane.add(confirmChangePass, 1, 1);
		
		//changePassPane organization
		changePassPane.setAlignment(Pos.CENTER);
		changePassPane.setPadding(new Insets(10));
		changePassPane.setHgap(10);
		changePassPane.setVgap(5);
		changePassPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		
		
		//menu modification screen (modMenuPane)
		ListView<String> menuItems=new ListView<String>();
		menuItems.setItems(items);
		ListView<String> menuCats=new ListView<String>();
		menuCats.setItems(categories);
		ListView<String> menu=new ListView<String>();
		menu.setItems(menus);
		menuItems.setPrefWidth(150);
		menuCats.setPrefWidth(150);
		menu.setPrefWidth(150);
		tfMenu=new TextField();
		tfCat=new TextField();
		tfItem=new TextField();
		tfPrice=new TextField();
		Label lblMenu=new Label("Menu: ");
		Label lblCat=new Label("Category: ");
		Label lblItem=new Label("Item: ");
		Label lblPrice=new Label("Price: $");
		Button delMenu=new Button("Delete menu");
		Button newMenu=new Button("Create new menu");
		Button delCat=new Button("Delete category");
		Button newCat=new Button("Create new category");
		Button delItem=new Button("Delete item");
		Button newItem=new Button("Create new/update item");
		Button mmpDone=new Button("Done");
		mmpDone.setPrefWidth(150);
		
		//register buttons
		newMenu.setOnAction(e->{
			insertMenu();
			tfMenu.clear();
			viewMenu();
		});
		
		delMenu.setOnAction(e->{
			delMenu();
			tfMenu.clear();
			viewMenu();
		});
		
		newCat.setOnAction(e->{
			if(!categories.contains(tfCat.getText().trim())) {
				insertCat();
			}
			tfCat.clear();
			viewCats();
		});
		
		delCat.setOnAction(e->{
			delCat();
			tfCat.clear();
			viewCats();
		});
		
		newItem.setOnAction(e->{
			insertItem();
			tfItem.clear();
			tfPrice.clear();
			viewItems();
		});
		
		delItem.setOnAction(e->{
			delItem();
			tfItem.clear();
			tfPrice.clear();
			viewItems();
		});
		
		mmpDone.setOnAction(e->{
			intermediatePane.getChildren().clear();
			intermediatePane.add(controlPane,0,0);
		});

		//add listeners to find menu/cat/item(s) in DB
		menu.setOnMouseClicked(e->{
			tfMenu.setText(menu.getSelectionModel().getSelectedItem());
			viewCats();
		});
		menuCats.setOnMouseClicked(e->{
			tfCat.setText(menuCats.getSelectionModel().getSelectedItem());
			viewItems();
		});
		menuItems.setOnMouseClicked(e->{
			String[] split=menuItems.getSelectionModel().getSelectedItem().split("\\$");
			tfItem.setText(split[0].trim());
			tfPrice.setText(split[1].trim());
		});
		
		//add nodes to modMenuPane
		modMenuPane.add(lblMenu, 0, 1);
		modMenuPane.add(tfMenu, 1, 1);
		modMenuPane.add(menu, 0, 4);
		modMenuPane.add(newMenu, 0, 2);
		modMenuPane.add(delMenu, 1, 2);
		
		modMenuPane.add(lblCat, 2, 1);
		modMenuPane.add(tfCat, 3, 1);
		modMenuPane.add(menuCats, 2, 4);
		modMenuPane.add(newCat, 2, 2);
		modMenuPane.add(delCat, 3, 2);
		
		modMenuPane.add(lblItem, 4, 0);
		modMenuPane.add(tfItem, 5, 0);
		modMenuPane.add(menuItems, 4, 4);
		modMenuPane.add(lblPrice, 4, 1);
		modMenuPane.add(tfPrice, 5, 1);
		modMenuPane.add(newItem, 4, 2);
		modMenuPane.add(delItem, 5, 2);
		
		modMenuPane.add(mmpDone, 5, 5);

		//modMenuPane organization
		modMenuPane.setAlignment(Pos.CENTER);
		modMenuPane.setPadding(new Insets(10));
		modMenuPane.setHgap(10);
		modMenuPane.setVgap(5);
		modMenuPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
		

		//build stage
		intermediatePane.add(status,0,0);
		intermediatePane.add(mainPane,0,2);
		Scene scene=new Scene(intermediatePane,1000,600);
		mainStage.setTitle("OrderMate");
		mainStage.setScene(scene);
		mainStage.show();
	}
	
	public void removeLI() {
		total-=Double.parseDouble(orderSummary.getSelectionModel().getSelectedItem()
				.substring(orderSummary.getSelectionModel().getSelectedItem().lastIndexOf('$')+1,
						orderSummary.getSelectionModel().getSelectedItem().length()).trim());
		if(!orderSummary.getSelectionModel().getSelectedItem().startsWith(">")) {
			//remove the menuItem and move to next lineItem
			int indexChild=orderSummary.getSelectionModel().getSelectedIndex();
			lineItems.remove(orderSummary.getSelectionModel().getSelectedIndex());
			orderSummary.getSelectionModel().clearAndSelect(indexChild);
			try{
				while(orderSummary.getSelectionModel().getSelectedItem().startsWith(">")){
					indexChild=orderSummary.getSelectionModel().getSelectedIndex();
					total-=Double.parseDouble(orderSummary.getSelectionModel().getSelectedItem()
							.substring(orderSummary.getSelectionModel().getSelectedItem().lastIndexOf('$')+1,
									orderSummary.getSelectionModel().getSelectedItem().length()).trim());
					lineItems.remove(orderSummary.getSelectionModel().getSelectedIndex());
					orderSummary.getSelectionModel().clearAndSelect(indexChild);
				}
			}catch(NullPointerException ex){
				//do nothing
			}
		}
		else {
			lineItems.remove
			(orderSummary.getSelectionModel().getSelectedIndex());
		}
		orderTotal.setText
		(String.format("%s %.02f","Total (w/ tax): ",total*TAX));
	}
	
	public void connectDB() {
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn=DriverManager.getConnection
				("jdbc:mysql://localhost/ordermate", "root", "");
			status.setText("Database connected");
	    	stmt=conn.createStatement();
	    }
	    catch (Exception ex){
	    	status.setText("Connection failed: " + ex);
	    }
	}
		
	public void viewCust() {
		String query = "SELECT * FROM customers WHERE phoneNumber = "
			      + "'" + tfPhoneNumber.getText().trim() + "';";
		String prevOrders= "SELECT * FROM orders WHERE fkCustomer="+
				"'" + tfPhoneNumber.getText().trim() + "';";
		ResultSet rs,rs2;
		try{
	    	  rs=stmt.executeQuery(query);
	    	  if(rs.next()){
	    		  tfCustomerName.setText(rs.getString(2));
	    		  tfCustomerAddress.setText(rs.getString(3));
		    	  rs2=stmt.executeQuery(prevOrders);
	    		  while(rs2.next()) {
	    			  orders.add(rs2.getInt(1));
	    		  }
	    		  recordFound=true;
	    	  }
	    	  else {
	    		  tfCustomerName.clear();
		    	  tfCustomerAddress.clear();
		    	  recordFound=false;
	    	  }
	      }catch(SQLException ex){
	    	 ex.printStackTrace();
	      }
	}
	
	public void viewMenu() {
		menus.clear();
		categories.clear();
		String query = "SELECT * FROM menus;";
		ResultSet rs;
		try{
	    	  rs=stmt.executeQuery(query);
	    	  while(rs.next()){
	    		  menus.add(rs.getString(1));
	    	  }
	      }catch(SQLException ex){
	    	  //do nothing
	      }
	}
	
	public void viewCats() {
		categories.clear();
		items.clear();
		String query = "SELECT * FROM categories WHERE fkMenu='"+
				tfMenu.getText().trim()+"';";
		ResultSet rs;
		try{
	    	  rs=stmt.executeQuery(query);
	    	  while(rs.next()){
	    		  categories.add(rs.getString(2));
	    	  }
	      }catch(SQLException ex){
	    	  //do nothing
	      }
	}
		
	public void viewItems() {
		items.clear();
		String query = "SELECT * FROM items WHERE fkCategory = "
				+ "(SELECT idCategory FROM categories "
				+ "WHERE category='"+tfCat.getText().trim()+"' "
				+"AND fkMenu='"+tfMenu.getText().trim()+"');";
		ResultSet rs;
		try{
	    	  rs=stmt.executeQuery(query);
	    	  while(rs.next()){
	    		  items.add(rs.getString(2)+"\t$"+rs.getString(3));
	    	  }
	      }catch(SQLException ex){
	    	  //do nothing
	      }
	}
	
	public void viewLineItems() {
		lineItems.clear();
		String query = "SELECT * FROM order_line_items WHERE fkOrder = "+
				prevOrders.getSelectionModel().getSelectedItem()+";";
		ResultSet rs;
		try{
	    	  rs=stmt.executeQuery(query);
	    	  while(rs.next()){
	    		  lineItems.add(rs.getString(2)+"\t$"+rs.getString(3));
	    	  }
	      }catch(SQLException ex){
	    	  //do nothing
	      }
	}
	
	public void insertCust() {
		String insert="INSERT INTO customers(phoneNumber,name,address) "+
				"VALUES( '"+tfPhoneNumber.getText().trim()+"','"+
				tfCustomerName.getText().trim()+"','"+
				tfCustomerAddress.getText().trim()+"');";
		try{
			stmt.executeUpdate(insert);
		}catch(SQLException ex){
			//do nothing
		}
	}
	
	public void updateCust() {
		String update="UPDATE customers SET name='"+
				tfCustomerName.getText().trim()+"',address='"+
				tfCustomerAddress.getText().trim()+"'"+
				"WHERE tfPhoneNumber='"+tfPhoneNumber.getText().trim()+
				"';";
		try{
			stmt.executeUpdate(update);
		}catch(SQLException ex){
			//do nothing
		}
	}
	
	public void insertMenu() {
		String insert="INSERT INTO menus(menu) "+
				"VALUES( '"+tfMenu.getText().trim()+"');";
		try{
			stmt.executeUpdate(insert);
		}catch(SQLException ex){
			//do nothing
		}
	}
	
	public void delMenu() {
		String delete="DELETE FROM menus WHERE menu= '"+
				tfMenu.getText().trim()+"';";
		try{
			stmt.executeUpdate(delete);
		}catch(SQLException ex){
			//do nothing
		}
	}
	
	public void insertCat() {
		String insert="INSERT INTO categories(category, fkMenu) "+
				"VALUES( '"+tfCat.getText().trim()+"','"+
				tfMenu.getText().trim()+"');";
		try{
			stmt.executeUpdate(insert);
		}catch(SQLException ex){
			//do nothing
		}
	}
	
	public void delCat() {
		String delete="DELETE FROM categories WHERE category= '"+
				tfCat.getText().trim()+"' AND fkMenu= '"+
				tfMenu.getText().trim()+"';";
		try{
			stmt.executeUpdate(delete);
		}catch(SQLException ex){
			//do nothing
		}
	}
	
	public void insertItem() {
		String insert="INSERT INTO items(item,price,fkCategory) "+
				"VALUES('"+tfItem.getText().trim()+"','"+
				Double.parseDouble(tfPrice.getText().trim())+"',"
				+"(SELECT idCategory FROM categories "
				+ "WHERE category='"
				+tfCat.getText().trim()+"' AND fkMenu='"
				+tfMenu.getText().trim() +"'));";
		String update="UPDATE items SET price="+
				Double.parseDouble(tfPrice.getText().trim())
				+" WHERE idItem=(SELECT idItem "
				+ "WHERE item='"+tfItem.getText().trim()+"'"
				+" AND fkCategory=(SELECT idCategory FROM categories "
				+"WHERE category='"
				+tfCat.getText().trim()+"' AND fkMenu='"
				+tfMenu.getText().trim()+"'));";
		try{
			//executeUpdate returns row count of affected rows, if zero item does not exist
			//throw exception and insert item
			if(stmt.executeUpdate(update)==0)
				throw new SQLException();
		}catch(SQLException ex){
			try {
				stmt.executeUpdate(insert);
			} catch (SQLException exInner) {
				//do nothing
			}
		}
	}
			
	public void delItem() {
		String delete="DELETE FROM items WHERE item= '"+
				tfItem.getText().trim()+"' AND fkCategory= "
				+"(SELECT idCategory FROM categories "
				+ "WHERE category='"
				+tfCat.getText().trim()+"' AND fkMenu='"
				+tfMenu.getText().trim() +"');";
		try{
			stmt.executeUpdate(delete);
		}catch(SQLException ex){
			//do nothing
		}
	}
			
	public void insertOrder() {
		String customItem;
		double price;
		String insertOrder="INSERT INTO orders(fkCustomer) "+
				"VALUES( '"+tfPhoneNumber.getText().trim()+"');";
		try{
			stmt.executeUpdate(insertOrder);
			for(String li: lineItems) {
				customItem=li.substring(0, li.indexOf('$')-1).trim();
				price=Double.parseDouble(li.substring(li.indexOf('$')+1,li.length()).trim());
				String insertItem="INSERT INTO order_line_items(customItem,price,fkOrder) "+
					"VALUES('"+customItem+"',"+price+
					",(SELECT MAX(idOrder) from orders WHERE fkCustomer='"+tfPhoneNumber.getText().trim()+"'));";
			stmt.executeUpdate(insertItem);
			}
		}catch(SQLException ex){
			//do nothing
		}
	}
	
 	public static void main(String[] args) {
		launch(args);
	}

}
