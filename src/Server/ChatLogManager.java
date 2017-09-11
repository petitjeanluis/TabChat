package Server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ChatLogManager {
	
	/* 
	 * This will be the format written in the client-chat databse file
	 * Each user has a username and a list of chats
	 * BEGINNING OF FILE
	 * [Username-20_Characters] [ChatId-10]...[\n]
	 * END OF FILE
	 * */
	private final int USER_SPACE = 20+(16*50)+1;
	private final int FIELD_LENGTH = 20;
	private final int CHAT_ID_LENGTH = 15;	
	
	private BufferedWriter writer;
	private RandomAccessFile reader;
	
	public ChatLogManager(){
		//creates chats folder if needed
		File dir = new File("TabChatServerData");
		dir.mkdir();
		dir = new File("TabChatServerData/Chats");
		dir.mkdir();
		//create ClientChatDatabase if needed
		try {
			dir = new File("TabChatServerData/ClientChatDatabase.txt");
			dir.createNewFile();
			writer = new BufferedWriter(new FileWriter("TabChatServerData/ClientChatDatabase.txt", true));
			reader = new RandomAccessFile(dir,"rw");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//returns a string with length of FIELD_LENGTH, right-padded with SPACE character
	public String formatString(String s, int length){
		int i = length - s.length();
		for(;i > 0; i--){
			s = s + " ";
		}
		return s;
	}
	
	public synchronized void addClient(String username){
		try {
			writer.write(formatString(username,this.FIELD_LENGTH));
			writer.flush();
			for(int i = 0; i < 50; i++){
				writer.write(":" + "               ");
				writer.flush();
			}
			writer.write("\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//adds chat to client, 50 chats max
	public synchronized void addChat(int userIndex, String chatId){
		try {
			long pos = userIndex*USER_SPACE;
			pos += 20;
			reader.seek(pos);
			char a = (char)reader.readByte();
			char b = (char)reader.readByte();
			while(a != '\n' && b != ' '){
				pos += 16;
				reader.seek(pos);
				a = (char)reader.readByte();
				b = (char)reader.readByte();
			}
			if(a == '\n'){
				return;
			}else{
				reader.seek(pos);
				reader.write((":" + formatString(chatId,this.CHAT_ID_LENGTH)).getBytes());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized ArrayList<Long> getChatIds(int userIndex){
		ArrayList<Long> ids = new ArrayList<Long>();
		
		try {
			long pos = userIndex*USER_SPACE;
			pos += 20;
			reader.seek(pos);
			char a = (char)reader.readByte();
			char b = (char)reader.readByte();
			while(a != '\n' && b != ' '){
				String number = "";
				
				while(b != ':' && b != '\n' && b != ' '){
					number += b;
					b = (char)reader.readByte();
				}
				ids.add(Long.parseLong(number));
				pos = pos + 16;
				reader.seek(pos);
				a = (char)reader.readByte();
				b = (char)reader.readByte();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ids;
	}
	
	public synchronized ArrayList<ServerChatLog> getChats(int userIndex){
		ArrayList<ServerChatLog> userChats = new ArrayList<ServerChatLog>();
		ArrayList<Long> chatIds = getChatIds(userIndex);
		for(long id : chatIds){
			userChats.add(new ServerChatLog(id));
		}
		return userChats;
	}
}
