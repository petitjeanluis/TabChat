package Server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Server extends Application implements Runnable{

	private ServerController controller;
	private static final int HOST_CHAT_PORT = 60123;
	private ServerSocket serverSocket;
	protected static ClientDatabase db = new ClientDatabase();
	protected static ChatLogManager cm = new ChatLogManager();
	private ArrayList<String> onlineClients = new ArrayList<String>();
	private Map<String,HandleClient> usersHandleClient = new HashMap<String,HandleClient>();
	private Map<Long,ServerChatLog> chats = new HashMap<Long,ServerChatLog>();
	
	public static void main(String[] args){
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		//set up javafx window and controller
		FXMLLoader loader = new FXMLLoader(getClass().getResource("ServerLayout.fxml"));
		Parent layout = loader.load();
		Scene scene = new Scene(layout);
		stage.setTitle("TabChat Server");
		stage.setScene(scene);
		stage.show();
		controller = (ServerController)loader.getController();
		//start server thread
		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		//check server port for incoming client connections
		try {
			serverSocket = new ServerSocket(HOST_CHAT_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(true){
			try {
				Socket socket = serverSocket.accept();
				HandleClient client = new HandleClient(socket,db,cm,controller,this);
				Thread thread = new Thread(client);
				thread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	public synchronized void sendMessage(long chatId, String sender, String message){
		 ServerChatLog log = chats.get(chatId);
		 ArrayList<String> users = log.getUsers();
		 log.addContents(sender,"m",message);
		 for(String s : users){
			 HandleClient handle = usersHandleClient.get(s);
			 if(handle != null){
				 handle.sendMessage(chatId, sender, message);
			 }
		 }
	}
	
	public synchronized void sendImage(long chatId, String sender, File imageFile){
		ServerChatLog log = chats.get(chatId);
		ArrayList<String> users = log.getUsers();
		log.addContents(sender, "i", imageFile.getName());
		for(String s : users){
			HandleClient handle = usersHandleClient.get(s);
			if(handle != null){
				new Thread(new Runnable(){
					public void run(){
						handle.sendImage(chatId,sender,imageFile);
					}
				}).start();
			}
		}
	}
	
	//returns array of online client usernames
	public synchronized String[] getOnlineClients(){
		String[] list = new String[onlineClients.size()];
		for(int i = 0; i < onlineClients.size(); i++){
			list[i] = onlineClients.get(i);
		}
		return list;
	}
	
	//adds client to list, other clients are updated while new client gets sent the full
	//client list, order crucial
	public synchronized void clientOnline(int id,String username,HandleClient hc){
		notifyOnlineListUpdate(id);
		usersHandleClient.put(username, hc);
		onlineClients.add(username);
		Platform.runLater(new Runnable(){
			public void run(){
				String user = db.getUsername(id);
				controller.addClient(user);
				controller.output(user + " logged in");
			}
		});
	}
	
	//removes client from list, order crucial
	public synchronized void clientOffline(int id, String username,HandleClient hc){
		onlineClients.remove(username);
		usersHandleClient.remove(username).close();
		notifyOfflineListUpdate(id);
		Platform.runLater(new Runnable(){
			public void run(){
				String user = db.getUsername(id);
				controller.removeClient(user);
				controller.output(user + " logged off");
			}
		});
	}
	
	//used for adding existing chats
	public synchronized void addChat(ServerChatLog chat){
		chats.put(chat.chatId,chat);
	}
	
	//used for adding newly created chats
	public synchronized void addNewChat(ServerChatLog chat){
		chats.put(chat.chatId,chat);
		for(String user : chat.getUsers()){
			cm.addChat(db.findClient(user), Long.toString(chat.chatId));
			usersHandleClient.get(user).notifyNewChat(chat);
		}
	}
	
	//notifies all online clients of new online client
	public void notifyOnlineListUpdate(int id){
		for(String user : onlineClients){
			usersHandleClient.get(user).notifyOnlineListUpdate(id);
		}
	}
	
	//notifies all online clients of new offline client
	public void notifyOfflineListUpdate(int id){
		for(String user : onlineClients){
			usersHandleClient.get(user).notifyOfflineListUpdate(id);
		}
	}
	
	@Override
	public void stop(){
		//temporary
		System.exit(0);
	}
}
