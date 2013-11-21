package com.nclab.swimgamemulti;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.nclab.swimgamemulti.utils.Consts;

public class GameActivity extends Activity {
    private GameService gameService;
    private boolean isGameStarted = false;
    private boolean isNetTestStarted = false;
    private boolean isStrokeTestStarted = false;

    int pps = Consts.DEFAULT_PPS;
    int duration = Consts.DEFAULT_DURATION;
    String prefix = Consts.DEFAULT_PREFIX;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            gameService = ((GameService.GameServiceBinder) binder)
                    .getService();
            gameService.setUIHandler(uiHandler);
            gameHandler = gameService.getGameHandler();
            int id = Integer.parseInt(idSpinner.getSelectedItem().toString());
            gameHandler.obtainMessage(Game.MSG_SET_ID, id).sendToTarget();
            gameHandler.obtainMessage(Game.MSG_REQUEST_TEST_STATUS).sendToTarget();

            String serverAddress = addressText.getText().toString();
            gameHandler.obtainMessage(Game.MSG_SET_SERVER_ADDR, serverAddress).sendToTarget();

            gameHandler.obtainMessage(Game.MSG_SET_DURATION, duration).sendToTarget();
            gameHandler.obtainMessage(Game.MSG_SET_PPS, pps).sendToTarget();
            gameHandler.obtainMessage(Game.MSG_SET_PREFIX, prefix).sendToTarget();

            Toast.makeText(GameActivity.this, "Service Connected", Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            gameService = null;
            Toast.makeText(GameActivity.this, "Service Disconnected", Toast.LENGTH_SHORT).show();
        }
    };

    private Button idButton, netTestButton, quitButton, startButton;
    Button setServerAddressButton, setPrefixButton, ppsButton, durationButton;
    private TextView idTextView, debugTextView, errorTextView;
    private TextView addressText, ppsText, durationText, prefixText;
    private Spinner idSpinner;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);

        //always bind to service
        bindService(new Intent("com.nclab.swimgamemulti.GameService"), serviceConnection, Context.BIND_AUTO_CREATE);

		/*
        if (GameService.IsRunning()) {
			bindService(new Intent("com.nclab.swimgame.proto.GameService"), serviceConnection, Context.BIND_AUTO_CREATE);
		}
		else{
			bindService(new Intent("com.nclab.swimgame.proto.GameService"), serviceConnection, Context.BIND_AUTO_CREATE);
			startService(new Intent("com.nclab.swimgame.proto.GameService"));
		}*/

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedEditor = sharedPref.edit();

        //ID UI
        idTextView = (TextView) findViewById(R.id.idLabel);
        idSpinner = (Spinner) findViewById(R.id.idSpinner);
        ArrayAdapter<CharSequence> client_id_list_adapter = ArrayAdapter.createFromResource(this, R.array.client_ids, android.R.layout.simple_spinner_item);
        client_id_list_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        idSpinner.setAdapter(client_id_list_adapter);
        idSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // TODO Auto-generated method stub
                sharedEditor.putInt("id", idSpinner.getSelectedItemPosition());
                sharedEditor.commit();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });

        idButton = (Button) findViewById(R.id.idButton);
        idButton.setText("SetID");
        idButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (gameHandler != null) {
                    int id = Integer.parseInt(idSpinner.getSelectedItem().toString());
                    gameHandler.obtainMessage(Game.MSG_SET_ID, id).sendToTarget();
                    Toast.makeText(GameActivity.this, "set id to" + id, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GameActivity.this, "no handler connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        int id_pos = sharedPref.getInt("id", 0);
        idSpinner.setSelection(id_pos);

        //Frequency UI
//        frequencyView = (TextView)findViewById(R.id.labelF);
//        spinnerF = (Spinner) findViewById(R.id.spinnerF);
//        ArrayAdapter<CharSequence>  frequency_list_adapter = ArrayAdapter.createFromResource(this, R.array.frequnt_list, android.R.layout.simple_spinner_item);
//        client_id_list_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerF.setAdapter(frequency_list_adapter);
//        spinnerF.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            public void onItemSelected(AdapterView<?> arg0, View arg1,
//                                       int arg2, long arg3) {
//                // TODO Auto-generated method stub
//                sharedEditor.putInt("frequency", spinnerF.getSelectedItemPosition());
//                sharedEditor.commit();
//            }
//
//            public void onNothingSelected(AdapterView<?> arg0) {
//                // TODO Auto-generated method stub
//            }
//        });
//
//
//        btnFrequency = (Button) findViewById(R.id.btnSetF);
//        btnFrequency.setText("SetF");
//        btnFrequency.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // TODO Auto-generated method stub
//                int frequency = Integer.parseInt(spinnerF.getSelectedItem().toString());
//                if(gameHandler != null){
//                    gameHandler.obtainMessage(Game.UIMSG_SET_FREQUENCY, frequency).sendToTarget();
//                    Toast.makeText(GameActivity.this, "set id to" + frequency, Toast.LENGTH_SHORT).show();
//                }
//                else{
//                    Toast.makeText(GameActivity.this, "no handler connected", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//        int f_pos = sharedPref.getInt("frequency", 0);
//        spinnerF.setSelection(f_pos);

        startButton = (Button) findViewById(R.id.readyButton);
        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                gameHandler.obtainMessage(Game.MSG_GAME_READY).sendToTarget();
//                if (!isGameStarted) {
//                    gameHandler.obtainMessage(Game.MSG_GAME_START).sendToTarget();
//                } else {
//                    gameHandler.obtainMessage(Game.MSG_GAME_STOP).sendToTarget();
//                }
            }
        });

        //debug view UI
        debugTextView = (TextView) findViewById(R.id.textViewPacket);
        errorTextView = (TextView) findViewById(R.id.textViewError);

        netTestButton = (Button) findViewById(R.id.netTestButton);
        //netTestButton.setText(isNetTestStarted ? "Net Stop" : "Net Start");
        netTestButton.setText("Start Burst");
        netTestButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                gameHandler.obtainMessage(Game.MSG_NET_TEST_START).sendToTarget();
                // TODO Auto-generated method stub
                //if (!isNetTestStarted) {
                //    gameHandler.obtainMessage(Game.MSG_NET_TEST_START).sendToTarget();
                //} else {
                //    gameHandler.obtainMessage(Game.MSG_NET_TEST_STOP).sendToTarget();
                //}
                //isNetTestStarted = !isNetTestStarted;
                //netTestButton.setText(isNetTestStarted ? "Net Stop" : "Net Start");
            }
        });

        Button netTimeSyncButton = (Button) findViewById(R.id.netTimeSyncButton);
        netTimeSyncButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                gameHandler.obtainMessage(Game.MSG_TIME_SYNC).sendToTarget();
            }
        });

        quitButton = (Button) findViewById(R.id.quitButton);
        quitButton.setText("Quit");
        quitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                gameService.stopService(new Intent(GameActivity.this, GameService.class));
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        {
            addressText = (TextView) findViewById(R.id.serverAddressText);
            String defaultServerAddress = sharedPref.getString("serverAddress", Consts.SERVER_HOSTNAME);
            addressText.setText(defaultServerAddress);

            setServerAddressButton = (Button) findViewById(R.id.serverAddressSetButton);
            setServerAddressButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String address = addressText.getText().toString();
                    gameHandler.obtainMessage(Game.MSG_SET_SERVER_ADDR, address).sendToTarget();
                    sharedEditor.putString("serverAddress", address);
                    sharedEditor.commit();

                }
            });
        }

        {
            prefixText = (TextView) findViewById(R.id.prefixText);
            prefix = sharedPref.getString("prefix", Consts.DEFAULT_PREFIX);
            prefixText.setText(prefix);

            setPrefixButton = (Button) findViewById(R.id.prefixButton);
            setPrefixButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String address = prefixText.getText().toString();
                    gameHandler.obtainMessage(Game.MSG_SET_PREFIX, address).sendToTarget();
                    sharedEditor.putString("prefix", address);
                    sharedEditor.commit();

                }
            });
        }

        {
            ppsText = (TextView) findViewById(R.id.ppsText);
            pps = sharedPref.getInt("pps", Consts.DEFAULT_PPS);
            ppsText.setText("" + pps);

            ppsButton = (Button) findViewById(R.id.ppsButton);
            ppsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String ppsString = ppsText.getText().toString();
                    try {
                        pps = Integer.parseInt(ppsString);
                    } catch (Exception e) {
                        e.printStackTrace();
                        pps = Consts.DEFAULT_PPS;
                    }

                    if (pps < 1)
                        pps = 1;
                    else if (pps > 1000)
                        pps = 1000;

                    ppsText.setText("" + pps);

                    gameHandler.obtainMessage(Game.MSG_SET_PPS, pps).sendToTarget();
                    sharedEditor.putInt("pps", pps);
                    sharedEditor.commit();
                }
            });
        }

        {
            durationText = (TextView) findViewById(R.id.durationText);
            duration = sharedPref.getInt("duration", Consts.DEFAULT_DURATION);
            durationText.setText("" + duration);

            durationButton = (Button) findViewById(R.id.durationSetButton);
            durationButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String ppsString = durationText.getText().toString();
                    try {
                        duration = Integer.parseInt(ppsString);
                    } catch (Exception e) {
                        e.printStackTrace();
                        duration = Consts.DEFAULT_DURATION;
                    }

                    if (duration < 2)
                        duration = 2;
                    else if (duration > 1000)
                        duration = 1000;

                    durationText.setText("" + duration);

                    gameHandler.obtainMessage(Game.MSG_SET_DURATION, duration).sendToTarget();
                    sharedEditor.putInt("duration", duration);
                    sharedEditor.commit();
                }
            });
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game_activity, menu);
        return true;
    }


//    private class StageListAdapter extends ArrayAdapter<GameStage>{
//        private List<GameStage> items;
//
//        public StageListAdapter(Context context, int textViewResourceId,
//                                List<GameStage> objects) {
//            super(context, textViewResourceId, objects);
//            // TODO Auto-generated constructor stub
//            items = objects;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent){
//            View v = convertView;
//            if (v == null) {
//                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                v = vi.inflate(R.layout.stage_info, null);
//            }
//            GameStage stage = items.get(position);
//            if (stage != null) {
//                TextView top = (TextView) v.findViewById(R.id.name);
//                TextView bottom = (TextView) v.findViewById(R.id.description);
//                if (top != null) {
//                    top.setText(stage.Name);
//                }
//                if (bottom != null) {
//                    bottom.setText(stage.Description);
//                }
//            }
//            return v;
//        }
//    }


    //Bluetooth Connection
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//
//        case BluetoothManager.REQUEST_CONNECT_DEVICE:
//            // When DeviceListActivity returns with a device to connect
//            if (resultCode == Activity.RESULT_OK) {
//            	mBluetoothManager.ConnectDevice(data);
//            }
//            break;
//		case BluetoothManager.REQUEST_ENABLE_BT:
//	        // When the request to enable Bluetooth returns
//	        if (resultCode == Activity.RESULT_OK) {
//	            // Bluetooth is now enabled, so set up a chat session
//	        	mBluetoothManager.Setup();
//	        } else {
//	            // User did not enable Bluetooth or an error occurred
//	            Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
//	            finish();
//	        }
//	    }
    }

    private Handler gameHandler;
    public static final int MSG_NTP_STATUS = 0;
    public static final int MSG_ID_SET = 1;
    public static final int MSG_ID_FREQUENCY = 2;
    public static final int MSG_DEBUG_VIEW_ADD = 3;
    public static final int MSG_DEBUG_VIEW_SET = 4;
    public static final int MSG_ERROR_VIEW_SET = 5;
    public static final int MSG_GAME1P_STATUS = 6;
    public static final int MSG_GAME4P_STATUS = 7;
    public static final int MSG_NET_TEST_STATUS = 8;
    public static final int MSG_STROKE_TEST_STATUS = 9;
    public static final int MSG_TEST_STARTED = 10;
    public static final int MSG_PLAY_SOUND = 11;

    public Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ID_SET:
                    idTextView.setText("ID=" + msg.obj.toString());
                    break;
                case MSG_DEBUG_VIEW_ADD:
                    String str = msg.obj.toString() + "\r\n" + debugTextView.getText();
                    debugTextView.setText(str);
                    break;
                case MSG_DEBUG_VIEW_SET:
                    debugTextView.setText(msg.obj.toString());
                    break;
                case MSG_ERROR_VIEW_SET:
                    errorTextView.setText(msg.obj.toString());
                    break;
                case MSG_GAME1P_STATUS:
                    isGameStarted = (Boolean) msg.obj;
                    startButton.setText(isGameStarted ? "Stop" : "Start");
                    break;
                case MSG_NET_TEST_STATUS:
                    isNetTestStarted = (Boolean) msg.obj;
                    break;
                case MSG_STROKE_TEST_STATUS:
                    isStrokeTestStarted = (Boolean) msg.obj;
                    break;
                case MSG_TEST_STARTED:
                    startButton.setText("Test Stop");
                    break;
                case MSG_PLAY_SOUND:
                    gameHandler.obtainMessage(Game.MSG_PLAY_SOUND, msg.obj).sendToTarget();
            }
        }
    };
}
