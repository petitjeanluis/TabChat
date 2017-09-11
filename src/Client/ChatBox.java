package Client;

import java.util.ArrayList;
import java.util.Arrays;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ChatBox extends VBox{
	
	private long chatId;
	private Label users;
	private Label lastMessage;
	private boolean isOpen;
	
	public ChatBox(ArrayList<String> names, String lastMessage, long chatId, String username){
		super();
		this.isOpen = false;
		this.chatId = chatId;
		String namesList = "";
		boolean first = true;
		for(int i = 0; i < names.size(); i++){
			if(names.get(i).equals(username)) continue;
			if(first){
				namesList += names.get(i);
				first = false;
			}else{
				namesList += names.get(i) + ", ";
			}
		}
		this.users = new Label(namesList);
		this.users.getStyleClass().add("usersLabel");
		if(lastMessage != null){
			String[] contents = lastMessage.split(":");
			if(contents[1].equals("m")){
				lastMessage = contents[0]+": "+contents[2];
			}else{
				lastMessage = contents[0]+": sent and image";
			}
			this.lastMessage = new Label(lastMessage);
		}else{
			this.lastMessage = new Label(" ");
		}
		this.getChildren().addAll(this.users,this.lastMessage);
	}

	public void setLastMessage(String message) {
		String[] contents = message.split(":");
		if(contents[1].equals("m")){
			lastMessage.setText(contents[0]+": "+contents[2]);
		}else{
			lastMessage.setText(contents[0]+": sent and image");
		}
	}
	
	public void setIsOpen(boolean b){
		isOpen = b;
	}
	
	public boolean isOpen(){
		return isOpen;
	}

	public long getChatId() {
		return chatId;
	}
}
