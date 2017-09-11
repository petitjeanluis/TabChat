package Client;

import javafx.scene.control.Label;

public class MessageLabel extends ChatLabel{

	
	public MessageLabel(Label messageLabel, double height, boolean isMe){
		super(messageLabel,height, isMe);
		if(isMe){
			messageLabel.getStyleClass().add("messageLabelMe");
		}else{
			messageLabel.getStyleClass().add("messageLabelOther");
		}
	}
}
