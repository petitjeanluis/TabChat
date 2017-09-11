package Server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;

public class ServerController {

    @FXML
    private ListView<String> clientListView;;

    @FXML
    private Text serverOutput;
    
    private ObservableList<String> names = FXCollections.observableArrayList();
    
    public void output(String message){
    	String text = serverOutput.getText();
    	text = text.concat(message);
    	serverOutput.setText(text + "\n");
    }
    
    public void addClient(String user){
    	names.add(user);
    	clientListView.setItems(names);
    }
    
    public void removeClient(String user){
    	names.remove(user);
    }

}
