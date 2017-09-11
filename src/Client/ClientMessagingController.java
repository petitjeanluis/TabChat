package Client;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;


public class ClientMessagingController {

    @FXML
    private TextField messageField;

    @FXML
    private GridPane messageGrid;
    
    @FXML 
    private AnchorPane messagePane;
    
    @FXML
    private TilePane tilePane;
    
    private MessageLabelManager labelManager;
    private Client client;
    private long chatId;
    private Stage stage;
    
    @FXML
    void initialize(){
    	messagePane.setClip(new Rectangle(466,400));
    }
    
    @FXML
    void sendMessage(ActionEvent event) {
    	String message = messageField.getText();
    	messageField.clear();
    	if(!message.contains(":")){
    		client.sendMessage(chatId, message);
    	}
    }
    
    @FXML
    void mouseScroll(ScrollEvent e){
    	double dy = e.getDeltaY();
    	if(dy > 0){
    		labelManager.translateLabelDown(dy);
    	}else{
    		labelManager.translateLabelUp(dy*-1);
    	}
    }
    
    @FXML
    void pictureButtonClicked(ActionEvent e){
    	FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().add(
		        new ExtensionFilter("Image Files", "*.jpg"));
		File image = fileChooser.showOpenDialog(stage);
		if(image != null){
			if(image.exists()){
				client.sendImage(chatId, image);
			}
		}
    }
    
    public void init(Client c, ClientChatLog chat, String username, Stage stage){
    	this.stage = stage;
    	this.client = c;
    	this.chatId = chat.getChatId();
    	labelManager = new MessageLabelManager(messagePane, username, chat.getMessages());
    	for(String imageName : chat.getImages()){
    		String contents[] = imageName.split(":");
    		File file = new File("TabChatClientData/"+username+"/Images/"+contents[2]);
    		System.out.println(imageName);
    		System.out.println("in");
    		if(file.exists()){
    			System.out.println("inner");
    			Image image = null;
    	    	try {
    				String url = file.toURI().toURL().getPath();
    				image = new Image("file:"+url);
    			} catch (MalformedURLException e) {
    				e.printStackTrace();
    				return;
    			}
    	    	ImageView imageView = new ImageView(image);
    	    	imageView.setPreserveRatio(true);
    	    	imageView.setFitWidth(115);
    	    	tilePane.getChildren().add(imageView);
    		}
    	}
    }
    
    public void addMessage(String sender, String message){
    	labelManager.addMessage(sender, message);
    }
    
    public void addImage(String sender, File imageFile){
    	labelManager.addImage(sender, imageFile);
    	Image image = null;
    	try {
			String url = imageFile.toURI().toURL().getPath();
			image = new Image("file:"+url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
    	ImageView imageView = new ImageView(image);
    	imageView.setPreserveRatio(true);
    	imageView.setFitWidth(110);
    	tilePane.getChildren().add(imageView);
    }
}
