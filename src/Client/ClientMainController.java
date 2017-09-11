
package Client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class ClientMainController {
	
	private Client client;
	
	@FXML
	private ListView<String> onlineClientList;
	
	@FXML
	private ListView<ChatBox> chatList;

    @FXML
    private ImageView settingsButton;

    @FXML
    private ImageView addFriendButton;

    @FXML
    private ImageView createChatButton;
    
    @FXML
    void initialize(){
    	onlineClientList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }
    
    public void initChatList(Map<Long,ClientChatLog> chats){
    	if(chats.size() == 0) return;
		chats.entrySet().iterator();
		Iterator<Entry<Long, ClientChatLog>> iterator = chats.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry<Long, ClientChatLog> entry = (Map.Entry<Long, ClientChatLog>)iterator.next();
			ClientChatLog chat = entry.getValue();
			ChatBox chatBox = new ChatBox(chat.getUsers(),chat.getLastMessage(),chat.getChatId(),client.getUsername());
    		chatList.getItems().add(chatBox);
		}
    }
    
    public void addToChatList(ClientChatLog chat){
    	ChatBox chatBox = new ChatBox(chat.getUsers(),chat.getLastMessage(),chat.getChatId(),client.getUsername());
		chatList.getItems().add(chatBox);
    }
    
    @FXML
    void chatListClicked(MouseEvent e){
    	if(e.getClickCount() == 2){
    		ChatBox chatBox = chatList.getSelectionModel().getSelectedItem();
    		if(chatBox.isOpen()){
    			return;
    		}else{
    			chatBox.setIsOpen(true);
    			client.openMessagingWindow(chatBox.getChatId());
    		}
    	}
    }
    
    @FXML
    void settingsClicked(MouseEvent event) {
    	System.out.println("settings");
    }

    @FXML
    void addChatClicked(MouseEvent event) {
    	new Thread(new Runnable(){
    		public void run(){
    			String[] list = {};
    	    	list = onlineClientList.getSelectionModel().getSelectedItems().toArray(list);
    	    	List<String> newList = Arrays.asList(list);
    	    	client.newChat(new ArrayList<String>(newList));
    		}
    	}).start();
    }

    @FXML
    void addFriendClicked(MouseEvent event) {
    	System.out.println("add friend");
    }
   
    void addOnlineClient(String user){
    	onlineClientList.getItems().add(user);
    }
    
    void removeOnlineClient(String user){
    	onlineClientList.getItems().remove(user);
    }
    
    public void close(long chatId){
    	for(ChatBox chatBox : chatList.getItems()){
    		if(chatBox.getChatId() == chatId){
    			chatBox.setIsOpen(false);
    		}
    	}
    }
    
    public void updateLastMessage(long chatId, String message){
    	ChatBox toRemove = null;
    	for(ChatBox chatBox : chatList.getItems()){
    		if(chatBox.getChatId() == chatId){
    			chatBox.setLastMessage(message);
    			toRemove = chatBox;
    		}
    	}
    	if(toRemove != null){
    		chatList.getItems().remove(toRemove);
			chatList.getItems().add(0, toRemove);
    	}
    	
    }

    void setClient(Client c){
    	client = c;
    }
}

