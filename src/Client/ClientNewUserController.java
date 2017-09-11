package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class ClientNewUserController {

    @FXML
    private Button createNewButton;

    @FXML
    private TextField lastNameLabel;

    @FXML
    private PasswordField createPassLabel;

    @FXML
    private TextField createUserLabel;

    @FXML
    private PasswordField rePassLabel;

    @FXML
    private TextField nameLabel;
    
    private Client client;

    @FXML
    void createNewUserButton(MouseEvent event) {
    	if(createPassLabel.getText().equals(rePassLabel.getText())){
    		client.createAccount(nameLabel.getText(), lastNameLabel.getText(), createUserLabel.getText(), createPassLabel.getText());
    	}else{
    		createPassLabel.clear();
    		rePassLabel.clear();
    	}
    }
    
    void setClient(Client c){
    	client = c;
    }

}
