package android.TextMessenger.view;


import java.util.Observable;
import java.util.Observer;

import android.TextMessenger.control.ButtonListner;
import android.TextMessenger.exceptions.ContactOfflineException;
import android.TextMessenger.model.Chat;
import android.TextMessenger.model.ChatManager;
import android.TextMessenger.model.ClassConstants;
import android.TextMessenger.model.ObjToObsever;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;


public class ChatScreen extends Activity implements Observer{
	private EditText messageText;
	private EditText messageHistoryText;
	private Button sendMessageButton;
	private Chat chat;
	private int chatID;
	private ButtonListner listener;
	private ChatManager chatManager;
	private Handler handler;
	
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messaging_screen);
		
		chatID = getIntent().getIntExtra("chatID", 0);
		chatManager = ClassConstants.getInstance().getChatmanager();
		
		messageHistoryText = (EditText) findViewById(R.id.messageHistory);
		messageText = (EditText) findViewById(R.id.message);
		messageText.requestFocus();	
		sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
		listener = new ButtonListner(this);
		sendMessageButton.setOnClickListener(listener);
		
		chat = ClassConstants.getInstance().getChatmanager().getChat(chatID);	
		
		if(chat == null){
			this.finish();
		}
		chat.addObserver(this);
		chat.getTextHistory();
		handler = new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				addMessageToHistory((String)msg.getData().getString("msg"));	
			}
		};
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		chat.addObserver(this);
		chat.getTextHistory();
	}
	
	 @Override
		protected void onStop() {
			// TODO Auto-generated method stub
			super.onStop();
			chat.deleteObserver(this);
			chat.setHaveBeenViewde();
		}
	
	public void addMessageToHistory(String message) {
		if (message != null) {								
			messageHistoryText.append(message);	
		}
	}
	
	public void sendMessage(){
		try {
			chatManager.sendText(messageText.getText().toString(), chatID);
			messageText.setText("");
			messageText.requestFocus();	
		} catch (ContactOfflineException e) {
			//Close chat
		}
	}
	
	 @Override
		public void update(Observable observable, Object arg) {
		 ObjToObsever msg = (ObjToObsever)arg;
			int type = msg.getMessageType();
			switch (type) {
			
			case ObserverConst.TEXT_RECIVED:
				Message m = new Message();
				Bundle b = new Bundle();
				b.putString("msg", (String)msg.getContainedData());
				m.setData(b);
				handler.sendMessage(m);
				break;
			case ObserverConst.REMOVE_CHAT:
				this.finish();
				break;
			default:
				break;
			
			
			}
			
		}
	 
	
}
