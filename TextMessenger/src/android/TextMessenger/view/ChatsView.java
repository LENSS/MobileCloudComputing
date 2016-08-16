package android.TextMessenger.view;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import android.TextMessenger.control.ButtonListner;
import android.TextMessenger.control.ItemClickListener;
import android.TextMessenger.model.Chat;
import android.TextMessenger.model.ChatManager;
import android.TextMessenger.model.ClassConstants;
import android.TextMessenger.model.ContactManager;
import android.TextMessenger.model.ObjToObsever;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ChatsView extends ListActivity implements Observer {
	private ArrayList<String> chats;
	private ChatManager chatManager;
	private IconicAdapter ica;
	private ItemClickListener itemlisterner; 
	private Button addChat;
	private Handler handler, textRecived;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		chats = new ArrayList<String>();
		setContentView(R.layout.chats);
		ica = new IconicAdapter();
		setListAdapter(ica);
		chatManager = ClassConstants.getInstance().getChatmanager();
		chatManager.addObserver(this);
		addChat = (Button) findViewById(R.id.addchat);
		ButtonListner l = new ButtonListner(this);
		addChat.setOnClickListener(l);
		
		itemlisterner = new ItemClickListener(this,2);
		getListView().setOnItemLongClickListener(itemlisterner);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				// TODO Auto-generated method stub
				openChat(position);
			}
		});
		
		
		
		handler = new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				if(msg.getData().getInt("add")== 1){
					ica.add(msg
							.getData().getString("msg"));
				}
				else{
					ica.remove(msg
							.getData().getString("msg"));
				}
				ica.notifyDataSetChanged();
			}
		};
		
		textRecived = new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				ica.notifyDataSetChanged();
			}
		};
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(Observable observable, Object arg) {
		Chat c;
		
		ObjToObsever msg = (ObjToObsever) arg;
		int type = msg.getMessageType();
		switch (type) {

		case ObserverConst.TEXT_RECIVED:
			textRecived.sendEmptyMessage(0);
			break;
		case ObserverConst.NEW_CHAT:
			Message m = new Message();
			Bundle b = new Bundle();
			c = (Chat) msg.getContainedData();
			b.putString("msg", c.getID()+"");
			b.putInt("add", 1);
			m.setData(b);
			handler.sendMessage(m);
			break;
		case ObserverConst.REMOVE_CHAT:
			Message m1 = new Message();
			Bundle b1 = new Bundle();
			c = (Chat) msg.getContainedData();
			b1.putString("msg", c.getID()+"");
			b1.putInt("add", 0);
			m1.setData(b1);
			handler.sendMessage(m1);
			break;

		default:
			break;
		}
	}
		
		private void openChat(int position){
			Intent i = new Intent(this, ChatScreen.class);
			int chatID = Integer.parseInt(chats.get(position));
			i.putExtra("chatID", chatID);
			startActivityForResult(i, 0);	
		}
		
		public void longPress(int position){
			int cID = Integer.parseInt(chats.get(position));
			chatManager.removeChat(cID);
		}
		
		public void newChat(){
			Intent i = new Intent(this, AddChat.class);
		
			/*//TODO Test Kode
			ContactManager c = ClassConstants.getInstance().getContactManager();
			c.addContact(44, "ole44", false);
			c.addContact(22, "ole22", false);
			c.addContact(33, "ole33", false);
			c.addContact(55, "ole55", false);
			c.addContact(443, "ole443", false);
			c.addContact(223, "ole223", false);
			c.addContact(333, "ole333", false);
			c.addContact(553, "ole553", false);
			
			c.addContact(44, "ole44", false);
			c.addContact(22, "ole22", false);
			c.addContact(33, "ole33", false);
			c.addContact(55, "ole55", false);
			c.addContact(443, "ole443", false);
			c.addContact(223, "ole223", false);
			c.addContact(333, "ole333", false);
			c.addContact(553, "ole553", false); */
			
			startActivityForResult(i, 0);
		}

		@SuppressWarnings("unchecked")
		class IconicAdapter extends ArrayAdapter {
			IconicAdapter() {
				super(ChatsView.this, R.layout.row, chats);
			}

			public View getView(int position, View convertView, ViewGroup parent) {
				LayoutInflater inflater = getLayoutInflater();
				View row = inflater.inflate(R.layout.row, parent, false);
				TextView label = (TextView) row.findViewById(R.id.label);

				label.setText(chats.get(position));
				ImageView icon = (ImageView) row.findViewById(R.id.icon);
				if((chatManager.getChat(Integer.parseInt(chats.get(position))).isTherNewMsg())){
					icon.setImageResource(R.drawable.svambe_bob);
				}
				else{
					icon.setImageResource(R.drawable.icon);
				}
				return (row);
			}
		}
	}
