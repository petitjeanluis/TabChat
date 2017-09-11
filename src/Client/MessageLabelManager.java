package Client;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

public class MessageLabelManager {
	
	private final double PANE_HEIGHT = 400;
	private AnchorPane messagePane;
	private ArrayList<ChatLabel> labels = new ArrayList<ChatLabel>();//last label is the last message
	private ArrayList<ChatLabel> liveLabels = new ArrayList<ChatLabel>();
	private ArrayList<ChatLabel> toRemove = new ArrayList<ChatLabel>();
	private String username;
	
	public MessageLabelManager(AnchorPane anchorPane, String username, ArrayList<String> messages){
		messagePane = anchorPane;
		this.username = username;
		//populate labels
		for(String s : messages){
			String[] contents = s.split(":");
			Label nameLabel = createNameLabel(contents[0]);
			Label contentLabel = null;
			if(contents[1].equals("m")){
				contentLabel = createMessageLabel(contents[2]);
				double nameHeight = getHeight(nameLabel);
				double messageHeight = getHeight(contentLabel);
				if(contents[0].equals(username)){
					nameLabel.setText("Me");
					labels.add(new NameLabel(nameLabel, nameHeight, true));
					labels.add(new MessageLabel(contentLabel, messageHeight,true));
				}else{
					labels.add(new NameLabel(nameLabel, nameHeight, false));
					labels.add(new MessageLabel(contentLabel, messageHeight, false));
				}
			}else if(contents[1].equals("i")){
				File file = new File("TabChatClientData/"+username+"/Images/"+contents[2]);
				if(file.exists()){
					contentLabel = createImageLabel(file);
					double nameHeight = getHeight(nameLabel);
					double contentHeight = getHeight(contentLabel)+10;
					//bias of 10 is added because calculated height does not account for padding in image label
					if(contents[0].equals(username)){
						nameLabel.setText("Me");
						labels.add(new NameLabel(nameLabel, nameHeight, true));
						labels.add(new ImageLabel(contentLabel, contentHeight,true));
					}else{
						labels.add(new NameLabel(nameLabel, nameHeight, false));
						labels.add(new ImageLabel(contentLabel, contentHeight, false));
					}
				}else{
					contentLabel = createMessageLabel(contents[2]);
					double nameHeight = getHeight(nameLabel);
					double contentHeight = getHeight(contentLabel);
					if(contents[0].equals(username)){
						nameLabel.setText("Me");
						labels.add(new NameLabel(nameLabel, nameHeight, true));
						labels.add(new MessageLabel(contentLabel, contentHeight,true));
					}else{
						labels.add(new NameLabel(nameLabel, nameHeight, false));
						labels.add(new MessageLabel(contentLabel, contentHeight, false));
					}
				}
			}
		}
		updateLabels();
	}
	
	public double getHeight(Label l){
		messagePane.getChildren().add(l);
		messagePane.applyCss();
		messagePane.layout();
		double dy = l.getHeight();
		messagePane.getChildren().remove(l);
		return dy;
	}
	
	public synchronized void addMessage(String sender, String message){
		Label nameLabel = createNameLabel(sender);
		Label messageLabel = createMessageLabel(message);
		double nameHeight = getHeight(nameLabel);
		double messageHeight = getHeight(messageLabel);
		if(sender.equals(username)){
			nameLabel.setText("Me");
			labels.add(new NameLabel(nameLabel, nameHeight, true));
			labels.add(new MessageLabel(messageLabel, messageHeight, true));
		}else{
			labels.add(new NameLabel(nameLabel, nameHeight, false));
			labels.add(new MessageLabel(messageLabel, messageHeight, false));
		}
		updateLabels();
	}
	
	public synchronized void addImage(String sender, File imageFile){
		Label nameLabel = createNameLabel(sender);
		Label imageLabel = createImageLabel(imageFile);
		double nameHeight = getHeight(nameLabel);
		double imageHeight = getHeight(imageLabel) + 10;
		//bias of 10 is added because calculated height does not account for padding in image label
		if(sender.equals(username)){
			nameLabel.setText("Me");
			labels.add(new NameLabel(nameLabel, nameHeight, true));
			labels.add(new ImageLabel(imageLabel, imageHeight, true));
		}else{
			labels.add(new NameLabel(nameLabel, nameHeight, false));
			labels.add(new ImageLabel(imageLabel, imageHeight, false));
		}
		updateLabels();
	}
	
	public Label createImageLabel(File imageFile){
		Label imageLabel = new Label();
		imageLabel.getStyleClass().add("imageLabel");
		Image image = null;
		try {
			String url = imageFile.toURI().toURL().getPath();
			image = new Image("file:"+url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		ImageView imageView = new ImageView(image);
		imageView.setPreserveRatio(true);
		imageView.setFitWidth(225);
		imageLabel.setGraphic(imageView);
		return imageLabel;
	}
	
    
    public void updateLabels(){
    	messagePane.getChildren().clear();
    	//check how many labels fit
    	double y = 0;
    	int count = 0;
    	for(int i = labels.size()-1; i >= 0; i--){
    		if(labels.get(i) instanceof NameLabel){
    			y += labels.get(i).getHeight();
        		y += 5;
    		}else{
    			y += labels.get(i).getHeight();
        		y += 5;
    		}
    		count++;
    		if(y > PANE_HEIGHT){
    			break;
    		}
    	}
    	y = PANE_HEIGHT;
    	liveLabels.clear();
    	for(int i = labels.size()-1; i >= labels.size()-count; i--){
    		ChatLabel chatLabel = labels.get(i);
    		if(chatLabel instanceof NameLabel){
    			y = y - chatLabel.getHeight();
        		chatLabel.setY(y);
        		y -= 5;
        		messagePane.getChildren().add(chatLabel.getLabel());
        		liveLabels.add(0, chatLabel);
    		}else {
    			y = y - 5 - chatLabel.getHeight();
        		chatLabel.setY(y);
        		messagePane.getChildren().add(chatLabel.getLabel());
        		liveLabels.add(0, chatLabel);
    		}
			if(chatLabel.isMe()){
    			AnchorPane.setRightAnchor(chatLabel.getLabel(), 5.0);
    		}else{
    			AnchorPane.setLeftAnchor(chatLabel.getLabel(), 5.0);
    		}
    	}
    }
    
    public Label createMessageLabel(String message){
    	Label label = new Label(message);
    	//dummy style class so applyCss() wroks
    	label.getStyleClass().add("messageLabelMe");
    	return label;
    }
    
    public Label createNameLabel(String name){
    	Label label = new Label(name);
    	label.getStyleClass().add("nameLabel");
    	return label;
    }
    
	public synchronized void translateLabelUp(double dy){
    	//check if you can scroll up
    	ChatLabel lastLabel = liveLabels.get(liveLabels.size()-1);
    	int lastIndex = labels.indexOf(lastLabel);
    	if(labels.size() - 1 == lastIndex) {
    		double y;
    		if(lastLabel instanceof NameLabel){
    			y = lastLabel.getY() + lastLabel.getHeight();
    		}else{
    			y = lastLabel.getY() + lastLabel.getHeight() + 5;
    		}
    		if(y >= PANE_HEIGHT){
    			if(dy > (y - PANE_HEIGHT)){
    				for(ChatLabel ml : liveLabels){
            			ml.setY(ml.getY()-(y - PANE_HEIGHT));
            		}
    			}else{
    				for(ChatLabel ml : liveLabels){
            			ml.setY(ml.getY()-dy);
            		}
    			}
    		}
    		return;
    	}
    	//scroll all live labels up
    	for(ChatLabel ml : liveLabels){
    		double y = ml.getY() - dy;
    		if(y < 0 - 5 - ml.getHeight()){
    			ml.setY(y);
    			toRemove.add(ml);
    		}else{
    			ml.setY(y);
    		}
    	}
    	//remove labels out of screen
    	for(ChatLabel ml : toRemove){
    		liveLabels.remove(ml);
    		messagePane.getChildren().remove(ml.getLabel());
    	}
    	toRemove.clear();
    	//add labels if needed
    	double y = lastLabel.getY() + lastLabel.getHeight() + 5;
    	lastIndex++;
    	while(y < PANE_HEIGHT && lastIndex < labels.size()){
    		ChatLabel chatLabel = labels.get(lastIndex);
    		if(chatLabel instanceof NameLabel){
    			y += 5;
        		if(y < 0 - 5 - chatLabel.getHeight()){
        			y += 5 + chatLabel.getHeight();
        			lastIndex++;
        			continue;
        		}
        		chatLabel.setY(y);
        		y += chatLabel.getHeight();
    		}else{
    			y -= 5;
        		if(y < 0 - 5 - chatLabel.getHeight()){
        			y += 5 + chatLabel.getHeight();
        			lastIndex++;
        			continue;
        		}
        		chatLabel.setY(y);
        		y += 5 + chatLabel.getHeight();
    		}
    		messagePane.getChildren().add(chatLabel.getLabel());
    		liveLabels.add(chatLabel);
    		if(chatLabel.isMe()){
    			AnchorPane.setRightAnchor(chatLabel.getLabel(), 5.0);
    		}else{
    			AnchorPane.setLeftAnchor(chatLabel.getLabel(), 5.0);
    		}
    		lastIndex++;
    	}
    	
    }
    
    public synchronized void translateLabelDown(double dy){
    	//check if you can scroll up
    	ChatLabel firstLabel = liveLabels.get(0);
    	int firstIndex = labels.indexOf(firstLabel);
    	if(firstIndex == 0){
    		double y;
    		if(firstLabel instanceof NameLabel){
    			y = firstLabel.getY()-5;
    		}else{
    			y = firstLabel.getY();
    		}
    		if(y < 0){
    			if(dy > (-y)){
    				for(ChatLabel ml : liveLabels){
            			ml.setY(ml.getY()-y);
            		}
    			}else{
    				for(ChatLabel ml: liveLabels){
    					ml.setY(ml.getY()+dy);
    				}
    			}
    		}
    		return;
    	}
    	//translate live labels down
    	for(ChatLabel ml : liveLabels){
    		double y = ml.getY() + dy;;
    		if(y >= PANE_HEIGHT){
    			ml.setY(y);
    			toRemove.add(ml);
    		}else{
    			ml.setY(y);
    		}
    	}
    	//remove labels out of screen
    	for(ChatLabel ml : toRemove){
    		liveLabels.remove(ml);
    		messagePane.getChildren().remove(ml.getLabel());
    	}
    	toRemove.clear();
    	//add labels if needed
    	double y = firstLabel.getY() - 5;
    	firstIndex--;
    	while(y >= 0 && firstIndex >= 0){
    		ChatLabel chatLabel = labels.get(firstIndex);
    		if(chatLabel instanceof NameLabel){
    			y += 5;
    			y = y - chatLabel.getHeight();
        		if(y >= PANE_HEIGHT){
        			y -= 5;
        			firstIndex--;
        			continue;
        		}
        		chatLabel.setY(y);
        		y -= 5;
    		}else{
    			y = y - chatLabel.getHeight();
        		if(y >= PANE_HEIGHT){
        			y -= 5;
        			firstIndex--;
        			continue;
        		}
        		chatLabel.setY(y);
    		}
    		messagePane.getChildren().add(chatLabel.getLabel());
    		liveLabels.add(0,chatLabel);
    		if(chatLabel.isMe()){
    			AnchorPane.setRightAnchor(chatLabel.getLabel(), 5.0);
    		}else{
    			AnchorPane.setLeftAnchor(chatLabel.getLabel(), 5.0);
    		}
    		firstIndex--;
    	}
    	
    }
}
