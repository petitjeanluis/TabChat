package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class ClientLoginController {

    @FXML
    private Button loginButton;

    @FXML
    private Button createButton;

    @FXML
    private PasswordField passLabel;

    @FXML
    private TextField usernameLabel;
    
    private Client client;

    @FXML
    void loginButtonClicked(MouseEvent event) {
    	login();
    }

    @FXML
    void createButtonClicked(MouseEvent event) {
    	client.openNewUserLayout();
    }
    
    @FXML
    void enterPressed(ActionEvent e){
    	login();
    }
    
    private void login(){
    	client.login(usernameLabel.getText(), passLabel.getText());
    }
    
    void setClient(Client c){
    	client = c;
    }

}
