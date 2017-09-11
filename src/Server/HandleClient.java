package Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class HandleClient implements Runnable {
	
	private DataOutputStream output;
	private DataInputStream input;
	private ClientDatabase db;
	private ChatLogManager cm;
	private ServerController controller;
	private Server server;
	private boolean running;
	private int userIndex;
	public String username;
	
	public HandleClient(Socket socket, ClientDatabase db, ChatLogManager cm,  ServerController controller, Server server){
		this.db = db;
		this.controller = controller;
		this.userIndex = -1;
		this.server = server;
		this.cm = cm;
		try {
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			//login protocol
			synchronized(this){
				login();
				sendClients();
				sendOnlineClients();
				sendChats();	
			}
			//main prototcol
			while(running){
				int command  = input.readInt();
				if(command == Command.MESSAGE.ordinal()){
					receiveMessage();
				}else if(command == Command.NEW_CHAT.ordinal()){
					newChat();
				}else if(command == Command.CLIENT_EXIT.ordinal()){
					running = false;
					server.clientOffline(userIndex, username, this);
					output.close();
					input.close();
				}else if(command == Command.IMAGE.ordinal()){
					receiveImage();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessage(long chatId, String sender, String message){
		synchronized(this){
			try {
				output.writeInt(Command.MESSAGE.ordinal());
				output.writeLong(chatId);
				output.flush();
				output.writeUTF(sender);
				output.flush();
				output.writeUTF(message);
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void receiveMessage() throws IOException{
		long chatId = input.readLong();
		String message = input.readUTF();
		server.sendMessage(chatId,username,message);
	}
	
	public void sendImage(long chatId, String sender, File imageFile){
		synchronized(this){
			try {
				output.writeInt(Command.IMAGE.ordinal());
				output.writeLong(chatId);
				output.writeUTF(sender);
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void receiveImage() throws IOException{
		File file = new File("TabChatServerData");
		file.mkdir();
		file = new File("TabChatServerData/Images");
		file.mkdir();
		
		long chatId = input.readLong();
		String fileName = input.readUTF();
		long size = input.readLong();
		
		file = new File("TabChatServerData/Images/" + fileName);
		file.createNewFile();
		FileOutputStream image = new FileOutputStream(file);
		for(long i = 0; i < size; i++){
			image.write(input.readInt());
		}
		image.flush();
		image.close();
		server.sendImage(chatId,username,file);
	}
	
	public void sendClients() throws IOException{
		ArrayList<String> clients = db.getClients();
		output.writeInt(clients.size());
		output.flush();
		for(String user : clients){
			output.writeUTF(user);
			
			output.flush();
		}
	}
	
	public void sendOnlineClients() throws IOException{
		String[] clients = server.getOnlineClients();
		output.writeInt(clients.length);
		output.flush();
		for(String s : clients){
			output.writeUTF(s);
			output.flush();
		}
	}
	
	public void sendChats() throws IOException{
		ArrayList<ServerChatLog> userChats = cm.getChats(userIndex);
		output.writeInt(userChats.size());
		output.flush();
		for(ServerChatLog log : userChats){
			server.addChat(log);//ensures server always has all possible active chats
			output.writeLong(log.chatId);
			output.flush();
			ArrayList<String> users = log.getUsers();
			output.writeInt(users.size());
			output.flush();
			for(String s : users){
				output.writeUTF(s);
				output.flush();
			}
			ArrayList<String> messages = log.getMessages();
			output.writeInt(messages.size());
			output.flush();
			for(String s : messages){
				output.writeUTF(s);
				output.flush();
			}
		}
	}
	
	public void newChat() throws IOException{
		long chatId = input.readLong();
		int count = input.readInt();
		ArrayList<String> users = new ArrayList<String>();
		for(int i = 0; i < count; i++){
			users.add(input.readUTF());
		}
		//we assume the chat does not exist
		server.addNewChat(new ServerChatLog(chatId,users,new ArrayList<String>()));
	}
	
	public void notifyNewChat(ServerChatLog chat){
		synchronized(this){
			try {
				output.writeInt(Command.NEW_CHAT.ordinal());
				output.writeLong(chat.chatId);
				output.flush();
				ArrayList<String> users = chat.getUsers();
				output.writeInt(users.size());
				output.flush();
				for(String m : users){
					output.writeUTF(m);
					output.flush();
				}
				ArrayList<String> messages = chat.getMessages();
				output.writeInt(messages.size());
				output.flush();
				for(String m : messages){
					output.writeUTF(m);
					output.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void login() throws IOException{
		//wait for login or new account command;
		while(true){
			int command = input.readInt();
			if(command == Command.LOGIN.ordinal()){
				String user = input.readUTF();
				String contents[] = user.split("\\s+");
				//catch invalid input
				int result =-1;
				try {
					result = db.verifyClient(contents[0], contents[1]);
				} catch (ArrayIndexOutOfBoundsException e) {
					//do nothing
				}
				if(result != -1){
					output.writeInt(Command.VERIFIED.ordinal());
					output.flush();
					userIndex = result;
					username = contents[0];
					server.clientOnline(userIndex,username,this);
					running = true;
					return;
				}else{
					output.writeInt(Command.NOT_VERIFIED.ordinal());
					output.flush();
					controller.output("client failed to log in");
				}
			}else if(command == Command.CREATE_ACOOUNT.ordinal()){
				String user = input.readUTF();
				String[] contents = user.split("\\s+");
				db.addClient(contents[0], contents[1], contents[2], contents[3]);
				userIndex = db.findClient(contents[3]);
				username = contents[3];
				server.clientOnline(userIndex,username,this);
				running = true;
				return;
			}else if(command == Command.CLIENT_EXIT.ordinal()){
				running = false;
			}
		}
	}

	public void notifyOnlineListUpdate(int id){
		try {
			synchronized(this){
				output.writeInt(Command.ONLINE_UPDATE.ordinal());
				output.writeUTF(db.getUsername(id));
				output.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void notifyOfflineListUpdate(int id){
		try {
			synchronized(this){
				output.writeInt(Command.OFFLINE_UPDATE.ordinal());
				output.writeUTF(db.getUsername(id));
				output.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close(){
		try {
			output.close();
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
