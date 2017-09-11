package Client;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import Server.Command;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application implements Runnable{
	
	private Socket mainSocket;
	private DataOutputStream output;
	private DataInputStream input;
	
	public static final String HOST_IP = "0.0.0.0";//"71.222.43.58";
	public static final int HOST_CHAT_PORT = 60123;
	
	private ClientLoginController loginController;
	private ClientNewUserController newUserController;
	private ClientMainController mainController;
	private Map<Long,ClientMessagingController> messagingControllers = new HashMap<Long,ClientMessagingController>();
	private Stage stage;
	
	private String username;
	private ArrayList<String> clients = new ArrayList<String>();
	private ArrayList<String> onlineClients = new ArrayList<String>();
	private Map<Long,ClientChatLog> chats;
	
 	public static void main(String[] args){
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		//set up javafx window and controller
		this.stage = stage;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientLoginLayout.fxml"));
		Parent layout = loader.load();
		Scene scene = new Scene(layout);
		stage.setResizable(false);
		stage.setTitle("Login");
		stage.setScene(scene);
		stage.show();
		loginController = (ClientLoginController) loader.getController();
		loginController.setClient(this);
		//start client thread
		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		//establish connection with server and initiate I/O
		try {
			mainSocket = new Socket(HOST_IP, HOST_CHAT_PORT);
			input = new DataInputStream(new BufferedInputStream(mainSocket.getInputStream()));
			output = new DataOutputStream(new BufferedOutputStream(mainSocket.getOutputStream()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void startClientLoop(){
		//this is to avoid running in GUI thread
		new Thread(new Runnable(){
			public void run(){
				try {
					//login protocol,order is crucial
					receiveClients();
					receiveOnlineClients();
					chats = ClientChatLog.getClientLogs(username);
					receiveChats();
					Platform.runLater(new Runnable(){
						public void run(){
							mainController.initChatList(chats);
						}
					});
					//main protocol
					while(true){
						int command = input.readInt();
						if(command == Command.ONLINE_UPDATE.ordinal()){
							updateOnlineClientList();
						}else if(command == Command.OFFLINE_UPDATE.ordinal()){
							updateOfflineClientList();
						}else if(command == Command.MESSAGE.ordinal()){
							receiveMessage();
						}else if(command == Command.NEW_CHAT.ordinal()){
							receiveNewChat();
						}else if(command == Command.IMAGE.ordinal()){
							receiveImage();
						}
					}
				} catch (IOException e) {
//					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void sendMessage(long chatId, String message){
		synchronized(this){
			try {
				output.writeInt(Command.MESSAGE.ordinal());
				output.writeLong(chatId);
				output.flush();
				output.writeUTF(message);
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//recieves incoming messages
	public void receiveMessage() throws IOException{
		long chatId = input.readLong();
		String sender = input.readUTF();
		String message = input.readUTF();
		Iterator<Entry<Long, ClientChatLog>> iterator = chats.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry<Long, ClientChatLog> entry = (Map.Entry<Long, ClientChatLog>)iterator.next();
			ClientChatLog chat = entry.getValue();
			if(chat.getChatId() == chatId){
				chat.addContents(sender,"m", message);
				Platform.runLater(new Runnable(){
					public void run(){
						if(messagingControllers.get(chatId) != null){
							messagingControllers.get(chatId).addMessage(sender, message);
						}
						mainController.updateLastMessage(chatId, sender +":"+"m"+":"+message);
					}
				});
			}
		}
		//do something about the new message
	}
	
	public void sendImage(long chatId, File imageFile){
		new Thread(new Runnable(){
			public void run(){
				synchronized(this){
					try{
						output.writeInt(Command.IMAGE.ordinal());
						output.writeLong(chatId);
						output.writeUTF(imageFile.getName());
						output.flush();
						FileInputStream image = new FileInputStream(imageFile);
						output.writeLong(imageFile.length());
						output.flush();
						int i = image.read();
						while(i != -1){
							output.writeInt(i);
							i = image.read();
						}
						output.flush();
						image.close();
					}catch (IOException e){
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	public void receiveImage() throws IOException {
		File file = new File("TabChatClientData");
		file.mkdir();
		file = new File("TabChatClientData/"+username);
		file.mkdir();
		file = new File("TabChatClientData/"+username+"/Images");
		file.mkdir();
		
		long chatId = input.readLong();
		String sender = input.readUTF();
		String fileName = input.readUTF();
		long size = input.readLong();
		
		file = new File("TabChatClientData/"+username+"/Images/"+fileName);
		file.createNewFile();
		FileOutputStream image = new FileOutputStream(file);
		for(long i = 0; i < size; i++){
			image.write(input.readInt());
		}
		image.flush();
		image.close();
		//updateGUI
		Iterator<Entry<Long, ClientChatLog>> iterator = chats.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry<Long, ClientChatLog> entry = (Map.Entry<Long, ClientChatLog>)iterator.next();
			ClientChatLog chat = entry.getValue();
			if(chat.getChatId() == chatId){
				chat.addContents(sender, "i", fileName);
				Platform.runLater(new Runnable(){
					public void run(){
						if(messagingControllers.get(chatId) != null){
							File file =  new File("TabChatClientData/"+username+"/Images/"+fileName);
							messagingControllers.get(chatId).addImage(sender, file);
						}
						mainController.updateLastMessage(chatId, sender +":"+"sent an image");
					}
				});
			}
		}
	}
	
	//initiates new chat
	public void newChat(ArrayList<String> userList){
		if(userList.size() == 0) return;
		userList.add(username);
		//create chat id
		long chatId = 0;
		for(String s : userList){
			chatId += s.hashCode();
		}
		//check if log already exist
		Iterator<Entry<Long, ClientChatLog>> iterator = chats.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry<Long, ClientChatLog> entry = (Map.Entry<Long, ClientChatLog>)iterator.next();
			ClientChatLog chat = entry.getValue();
			if(chat.getChatId() == chatId){
				return;
			}
		}
		//send new chat request
		synchronized(this){
			try {
				output.writeInt(Command.NEW_CHAT.ordinal());
				output.writeLong(chatId);
				output.writeInt(userList.size());
				output.flush();
				for(String s : userList){
					output.writeUTF(s);
					output.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void receiveNewChat() throws IOException{
		ArrayList<String> clients = new ArrayList<String>();
		ArrayList<String> messages = new ArrayList<String>();
		messages.clear();
		long chatId = input.readLong();
		int clientCount = input.readInt();
		for(int i = 0; i < clientCount; i++){
			clients.add(input.readUTF());
		}
		int messageCount = input.readInt();
		for(int i = 0; i < messageCount; i++){
			messages.add(input.readUTF());
		}
		if(!ClientChatLog.findChat(chatId,username)){
			ClientChatLog chat = new ClientChatLog(chatId,clients,messages,username);
			chats.put(chat.getChatId(),chat);
			Platform.runLater(new Runnable(){
				public void run(){
					mainController.addToChatList(chat);
					launchMessagingWindow(chat);
				}
			});
		}
	}
	
	public void openMessagingWindow(long chatId){
		Iterator<Entry<Long, ClientChatLog>> iterator = chats.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry<Long, ClientChatLog> entry = (Map.Entry<Long, ClientChatLog>)iterator.next();
			ClientChatLog chat = entry.getValue();
			if(chat.getChatId() == chatId){
				launchMessagingWindow(chat);
				return;
			}
		}
	}
	
	public void launchMessagingWindow(ClientChatLog chat){
		try {
			Stage newStage = new Stage();
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientMessagingLayout.fxml"));
			Parent layout;
			layout = loader.load();
			Scene scene = new Scene(layout);
			scene.getStylesheets().add(getClass().getResource("ChatStyle.css").toExternalForm());
			newStage.setResizable(false);
			newStage.setScene(scene);
			newStage.setOnCloseRequest(event->{
				mainController.close(chat.getChatId());
				messagingControllers.remove(chat.getChatId());
			});
			ArrayList<String> userList = chat.getUsers();
			String users = "";
			boolean first = true;
			for(int i = 0; i < userList.size();i++){
				String name = userList.get(i);
				if(name.equals(username)) continue;
				if(first){
					users += userList.get(i);
					first = false;
				}else{
					users += ", " + userList.get(i);
				}
			}
			newStage.setTitle("Chat With - "+users);
			newStage.show();
			ClientMessagingController messagingController = (ClientMessagingController)loader.getController();
			messagingController.init(this,chat,username, newStage);
			messagingControllers.put(chat.getChatId(), messagingController);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//receives all clients known by server, to be implemented for frineds only
	public void receiveClients() throws IOException{
		int count = input.readInt();
		for(;count > 0; count--){
			String user = input.readUTF();
			if(user.equals(username)) continue;
			clients.add(user);
		}
	}
	
	//receives all online clients known by server, to be implemented for friends only
	public void receiveOnlineClients() throws IOException{
		int count = input.readInt();
		for(;count > 0; count--){
			String user = input.readUTF();
			if(user.equals(username)) continue;
			onlineClients.add(username);
			Platform.runLater(new Runnable(){
				public void run(){
					mainController.addOnlineClient(user);
				}
			});
		}
	}
	
	//receives all chats where client is present
	public void receiveChats() throws IOException{
		ArrayList<String> clients = new ArrayList<String>();
		ArrayList<String> messages = new ArrayList<String>();
		int chatCount = input.readInt();
		for(;chatCount > 0; chatCount--){
			clients.clear();
			messages.clear();
			long chatId = input.readLong();
			int clientCount = input.readInt();
			for(int i = 0; i < clientCount; i++){
				clients.add(input.readUTF());
			}
			int messageCount = input.readInt();
			for(int i = 0; i < messageCount; i++){
				messages.add(input.readUTF());
			}
			if(!ClientChatLog.findChat(chatId,username)){
				chats.put(chatId,new ClientChatLog(chatId,clients,messages,username));
			}else{
				ClientChatLog chat = chats.get(chatId);
				chat.updateMessages(messages);
			}
		}
	}
	
	public void login(String user, String pass) {
		//send user and pass
		try {
			output.writeInt(Command.LOGIN.ordinal());
			output.writeUTF(user + " " + pass);
			output.flush();
			if(input.readInt() == Command.NOT_VERIFIED.ordinal()){
				return;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		username = user;
		openMainLayout();
		startClientLoop();
	}

	public void openNewUserLayout(){
		FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientNewUserLayout.fxml"));
		Parent layout;
		try {
			layout = loader.load();
			Scene scene = new Scene(layout);
			stage.setScene(scene);
			stage.setTitle("Create User");
			newUserController = (ClientNewUserController) loader.getController();
			newUserController.setClient(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	public void openMainLayout(){
		FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientMainLayout.fxml"));
		Parent layout;
		try {
			layout = loader.load();
			Scene scene = new Scene(layout);
			scene.getStylesheets().add(getClass().getResource("ChatStyle.css").toExternalForm());
			stage.setScene(scene);
			stage.setTitle("TabChat - " + username);
			mainController = (ClientMainController) loader.getController();
			mainController.setClient(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createAccount(String name, String last, String user, String pass){
		String newUser = name + " " + last + " " + pass + " " + user + "\n";
		String[] contents = newUser.split("\\s+");
		//verifies valid input
		try{
			String.format(contents[0] + contents[1] + contents[2] + contents[3], "");
		}catch (ArrayIndexOutOfBoundsException e){
			return;
		}
		try {
			output.writeInt(Command.CREATE_ACOOUNT.ordinal());
			output.writeUTF(newUser);
			output.flush();	
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		username = user;
		openMainLayout();
		startClientLoop();
	}
	
	public void updateOnlineClientList(){
		try {
			String user = input.readUTF();
			onlineClients.add(user);
			Platform.runLater(new Runnable(){
				public void run(){
					mainController.addOnlineClient(user);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateOfflineClientList(){
		try {
			String user = input.readUTF();
			onlineClients.remove(user);
			Platform.runLater(new Runnable(){
				public void run(){
					mainController.removeOnlineClient(user);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getUsername(){
		return username;
	}
	
	@Override
	public void stop(){
		try {
			//inform server the client is exiting
			synchronized(this){
				if(output != null){
					output.writeInt(Command.CLIENT_EXIT.ordinal());
					output.flush();
					output.close();
					input.close();
				}
			}
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
