package com.nclab.swimgamemulti;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import com.nclab.swimgamemulti.motion.SwimmingMotionDetector;
import com.nclab.swimgamemulti.network.CommunicationManager;
import com.nclab.swimgamemulti.network.NetworkManager;
import com.nclab.swimgamemulti.sound.GameSoundManager;
import com.nclab.swimgamemulti.utils.Consts;
import com.nclab.swimgamemulti.utils.FileLogger;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;

public class Game implements Runnable {
    private boolean running = false;
    private boolean isGameStart = false;
    private long startTime = 0;
    private Context context;

    private CommunicationManager mCommunicationManager;
    private SwimmingMotionDetector mSwimmingMotionDetector;
    private FileLogger mLogger;
    private GameSoundManager mGameSoundManager;

    public static final int MSG_SET_ID = 1;
    public static final int MSG_SET_FREQUENCY = 2;
    public static final int MSG_TIME_SYNC = 3;
    public static final int MSG_GAME_START = 4;
    public static final int MSG_GAME_STOP = 5;
    public static final int MSG_GAME_READY = 6;
    public static final int MSG_GAME_UNREADY = 7;
    public static final int MSG_NET_TEST_START = 8;
    public static final int MSG_NET_TEST_STOP = 9;
    public static final int MSG_STROKE_TEST_START = 10;
    public static final int MSG_STROKE_TEST_STOP = 11;
    public static final int MSG_REQUEST_TEST_STATUS = 12;
    public static final int MSG_ALL_READY = 13;
    public static final int MSG_PLAY_SOUND = 15;
    public static final int MSG_SET_SERVER_ADDR = 20;


    public static final int STROKE_FREESTROKE = 0;
    public static final int STROKE_BACKSTROKE = 1;
    public static final int STROKE_BREASTSTROKE = 2;
    public static final int STROKE_BUTTERFLY = 3;

    public static final int MSG_TYPE_DATA = 0;
    public static final int MSG_TYPE_NTP = 1;
    public static final int MSG_TYPE_COMMAND = 2;
    public static final int MSG_TYPE_ACK = 3;

    private static final int FRAME_RATE = Consts.DEFAULT_FRAME_RATE;

    private static final int GAME_STATUS_NONE = Consts.GAME_STATUS_NONE;
    private static final int GAME_STATUS_WAIT = Consts.GAME_STATUS_WAIT;
    private static final int GAME_STATUS_READY = Consts.GAME_STATUS_READY;
    private static final int GAME_STATUS_START = Consts.GAME_STATUS_START;
    private static final int GAME_STATUS_ATTACK = Consts.GAME_STATUS_ATTACK;
    private static final int GAME_STATUS_DEFENCE = Consts.GAME_STATUS_DEFENCE;
    private static final int GAME_STATUS_KNOCKDOWN = Consts.GAME_STATUS_KNOCKDOWN;
    private static final int GAME_STATUS_REST = Consts.GAME_STATUS_REST;
    private static final int GAME_STATUS_VICTORY = Consts.GAME_STATUS_VICTORY;
    private static final int GAME_STATUS_GAME_OVER = Consts.GAME_STATUS_GAME_OVER;

    private static final int SIGNAL_READY = 0;

    private static final int COMMAND_GAMESTART = 0;
    private static final int COMMAND_CHANGE_TO_ATTACK = 1;
    private static final int COMMAND_CHANGE_TO_DEFFENCE = 2;
    private static final int COMMAND_DEAD = 3;
    private static final int COMMAND_VICTORY = 4;
    private static final int COMMAND_GAMEOVER = 5;
    private static final int COMMAND_ATTACK_NORMAL= 6;
    private static final int COMMAND_ATTACK_STRONG= 7;
    private static final int COMMAND_GAMERESET = 10;


    private boolean isDefenceStarted = false;

    int myID = -1;
    int mFrequency = 1000;

    int dataCount = 0;


    //handler
    public Handler getGameHandler(){
        return gameHandler;
    }

    public void setUIHandler(Handler h){
        uiHandler = h;
        mCommunicationManager.setUiHandler(h);
    }

    private Handler uiHandler;
    private Handler gameHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_ID:
                    myID = (Integer)msg.obj;
                    mCommunicationManager.networkManager.setClientID(myID);
                    if(uiHandler != null){
                        uiHandler.obtainMessage(GameActivity.MSG_ID_SET, myID).sendToTarget();
                    }
                    break;
                case MSG_SET_FREQUENCY:
                    mFrequency = (Integer)msg.obj;
                    if(uiHandler != null){
                        uiHandler.obtainMessage(GameActivity.MSG_ID_FREQUENCY, mFrequency).sendToTarget();
                    }
                    break;
                case MSG_TIME_SYNC:
                    //start time sync;
                    sendNTP();
                    break;
                case MSG_GAME_START:
                    isGameStarted = true;
                    startGame();
                    break;
                case MSG_GAME_STOP:
                    isGameStarted = false;
                    stopGame();
                    break;
                case MSG_GAME_READY:
                    mCommunicationManager.obtainMessage(CommunicationManager.MSG_OUTGOING, SIGNAL_READY, 0).sendToTarget();
                    break;
                case MSG_GAME_UNREADY:
                    break;
                case MSG_NET_TEST_START:
                    //isNetTestStarted = true;
                    mCommunicationManager.networkManager.sendOne = true;
                    break;
                case MSG_NET_TEST_STOP:
                    isNetTestStarted = false;
                    break;
                case MSG_STROKE_TEST_START:
                    isStrokeTestStarted = true;
                    break;
                case MSG_STROKE_TEST_STOP:
                    isStrokeTestStarted = false;
                    break;
                case MSG_REQUEST_TEST_STATUS:
                    if(uiHandler != null){
                        uiHandler.obtainMessage(GameActivity.MSG_GAME1P_STATUS, isGameStarted).sendToTarget();
                        uiHandler.obtainMessage(GameActivity.MSG_GAME4P_STATUS, isGame4PStarted).sendToTarget();
                        uiHandler.obtainMessage(GameActivity.MSG_NET_TEST_STATUS, isNetTestStarted).sendToTarget();
                        uiHandler.obtainMessage(GameActivity.MSG_STROKE_TEST_STATUS, isStrokeTestStarted).sendToTarget();
                    }
                    break;
                case MSG_ALL_READY:
                    // Enable start button if this is host device
                    break;
                case MSG_PLAY_SOUND:
                    mGameSoundManager.playSoundEffect((String)msg.obj);
                    break;
                case MSG_SET_SERVER_ADDR:
                    mCommunicationManager.setServerAddress((String)msg.obj);
            }
        }
    };




    private boolean isGameStarted =false, isGame4PStarted=false, isNetTestStarted = false, isStrokeTestStarted = false;


    public Game(Context ctx){
        context = ctx;
        mCommunicationManager = new CommunicationManager(gameHandler);
        mLogger = new FileLogger();
        mSwimmingMotionDetector = new SwimmingMotionDetector(context, mLogger);
        mGameSoundManager = new GameSoundManager(context);
    }

    private void initialize(){
        mGameSoundManager.load();
    }

    @Override
    public void run() {
        running = true;
        startTime = System.currentTimeMillis();
        mLogger.writeLog(String.format("Start time : %d", startTime));
        initialize();
        long curTime = 0, prevTime = 0;
        long targetInterval = 1000 / FRAME_RATE;


        while(running){
            prevTime = curTime;
            curTime = System.currentTimeMillis();
            update(curTime - startTime);

            try {
                if(targetInterval - (curTime - prevTime) > 0)
                    Thread.sleep(targetInterval - (curTime - prevTime));
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                break;
            }
        }
    }

    public void startGame() {
        mGameSoundManager.playBGM("BGM_WAIT");
        currentGameStatus = GAME_STATUS_WAIT;

        for(int i=0; i<4; ++i){
            if(i != myID){
                if(soundNoteList.containsKey(i))
                    soundNoteList.remove(i);
                soundNoteList.put(i, new LinkedList<SoundNote>());
            }
        }
        isGameStart = true;
    }

    public void stopGame() {
        mGameSoundManager.stopBGM();
        currentGameStatus = GAME_STATUS_NONE;
        isGameStart = false;
    }

    public void exit() {
        running = false;
        isGameStart = false;
        isGameStarted = false;
        mSwimmingMotionDetector.close();
        mLogger.close();
    }


    long last_test_time = 0;
    int currentGameStatus = GAME_STATUS_NONE;
    int previous_game_status = GAME_STATUS_NONE;
    long last_packet_send_time;
    private void update(long time){
        //NetTest
        if(isNetTestStarted && time - last_test_time > 1000){
            SendStrokeData(STROKE_BACKSTROKE, (int)time);
            last_test_time = time;
        }

        //sensor update
        mSwimmingMotionDetector.update(time);
        if(isStrokeTestStarted || isGameStarted && (currentGameStatus != GAME_STATUS_START)){
            switch(mSwimmingMotionDetector.getStroke()){
                case SwimmingMotionDetector.FREESTYLE_LEFT_STROKE:
                case SwimmingMotionDetector.FREESTYLE_RIGHT_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_FREE1", 1.0f, 1.0f);
                    break;
                case SwimmingMotionDetector.BACKSTROKE_LEFT_STROKE:
                case SwimmingMotionDetector.BACKSTROKE_RIGHT_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_BACK", 1.0f, 1.0f);
                    break;
                case SwimmingMotionDetector.BREASTSTROKE_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_BREAST", 1.0f, 1.0f);
                    break;
                case SwimmingMotionDetector.BUTTERFLY_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_BUTTERFLY", 1.0f, 1.0f);
                    break;
            }
        }else if(isNetTestStarted || isGame4PStarted){
            switch(mSwimmingMotionDetector.getStroke()){
                case SwimmingMotionDetector.FREESTYLE_LEFT_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_FREE1", 1.0f, 1.0f);
                    SendStrokeData(STROKE_FREESTROKE, (int)time);
                    break;
                case SwimmingMotionDetector.FREESTYLE_RIGHT_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_FREE1", 1.0f, 1.0f);
                    SendStrokeData(STROKE_FREESTROKE, (int)time);
                    break;
                case SwimmingMotionDetector.BACKSTROKE_LEFT_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_BACK", 1.0f, 1.0f);
                    SendStrokeData(STROKE_BACKSTROKE, (int)time);
                    break;
                case SwimmingMotionDetector.BACKSTROKE_RIGHT_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_BACK", 1.0f, 1.0f);
                    SendStrokeData(STROKE_BACKSTROKE, (int)time);
                    break;
                case SwimmingMotionDetector.BREASTSTROKE_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_BREAST", 1.0f, 1.0f);
                    SendStrokeData(STROKE_BREASTSTROKE, (int)time);
                    break;
                case SwimmingMotionDetector.BUTTERFLY_STROKE:
                    mGameSoundManager.playSoundEffect("EFFECT_STROKE_BUTTERFLY", 1.0f, 1.0f);
                    SendStrokeData(STROKE_BUTTERFLY, (int)time);
                    break;
            }
        }

        //sound effect from others
        for(int i=0; i<4; ++i){
            if(i == myID || !soundNoteList.containsKey(i) || soundNoteList.get(i).isEmpty())
                continue;
            SoundNote note = soundNoteList.get(i).getFirst();
            if(note.time_target < System.currentTimeMillis()){
                switch(note.type){
                    case STROKE_FREESTROKE:
                        mGameSoundManager.playSoundEffect("EFFECT_STROKE_FREE1", 0.4f, 0.4f);
                        break;
                    case STROKE_BACKSTROKE:
                        mGameSoundManager.playSoundEffect("EFFECT_STROKE_BACK", 0.4f, 0.4f);
                        break;
                    case STROKE_BREASTSTROKE:
                        mGameSoundManager.playSoundEffect("EFFECT_STROKE_BREAST", 0.4f, 0.4f);
                        break;
                    case STROKE_BUTTERFLY:
                        mGameSoundManager.playSoundEffect("EFFECT_STROKE_BUTTERFLY", 0.4f, 0.4f);
                        break;
                }
                soundNoteList.get(i).removeFirst();
            }
        }

        //Game update
        if(isGameStarted){
            UpdateGame1P(time);
        }
        else if(isGame4PStarted){

        }
    }


    private int Dragon_HP = 250;
    private float My_HP = 20;

    private class PhaseInfo{
        public int type;
        public long duration;

        public PhaseInfo(int t, long d){
            type = t;
            duration = d;
        }
    }

    private LinkedList<PhaseInfo> phaseList = new LinkedList<PhaseInfo>();

    public void InitStageForGame(long time){
        if(!phaseList.isEmpty())
            phaseList.clear();

        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 30000));		//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_DEFENCE, 30000));	//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 30000));		//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_REST, 20000));		//20sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 20000));		//10sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_DEFENCE, 30000));	//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 30000));		//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_REST, 20000));		//20sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 20000));		//10sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_DEFENCE, 30000));	//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 30000));		//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_REST, 20000));		//20sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 20000));		//10sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_DEFENCE, 30000));	//30sec
        phaseList.addLast(new PhaseInfo(GAME_STATUS_ATTACK, 30000));		//30sec
    }



    private class SoundEffectNode{
        public String key;
        public long time;
        public SoundEffectNode(String k, long t){
            key = k;
            time = t;
        }
    }

    private LinkedList<SoundEffectNode> effectList = new LinkedList<SoundEffectNode>();
    private void initAttackEffectList(long time){
        if(!effectList.isEmpty())
            effectList.clear();

        effectList.add(new SoundEffectNode("EFFECT_DRAGON_STEP", time));
    }

    private long current_phase_start_time = 0;

    //start status
    private boolean isPlayedOpening = false, isPlayedOpeningNarration1 = false, isPlayedOpeningNarration2 = false;
    //attack status
    private boolean isPlayedAttackTutorial1 = false, isPlayedAttackTutorial2 = false, isPlayedAttackNarrationFromDefence1 = false, isPlayedAttackNarrationFromDefence2 = false, isPlayedAttackNarrationFromRest = false;
    private boolean isPlayedDragonHP50Narration = false, isPlayedDragonHP25Narration = false;
    //defence status
    private boolean isPlayedDefenceTutorial = false, isPlayedDefenceNarration = false;
    private boolean isPlayedMyHPLessNarration = false;
    //rest status
    private boolean isPlayedRestTutorial = false, isPlayedRestNarration = false;
    //victory
    private boolean isPlayedVictoryNarration = false;
    //gameover
    private boolean isPlayedGameOverNarration = false;

    private synchronized void UpdateGame1P(long time){

        if(currentGameStatus == GAME_STATUS_ATTACK ||
                currentGameStatus == GAME_STATUS_DEFENCE ||
                currentGameStatus == GAME_STATUS_REST){
            if(time - current_phase_start_time > phaseList.getFirst().duration){
                phaseList.removeFirst();
                previous_game_status = currentGameStatus;
                if(phaseList.isEmpty()){
                    currentGameStatus = GAME_STATUS_GAME_OVER;
                    mGameSoundManager.playBGM("NARRATION_GAMEOVER");
                }

                currentGameStatus = phaseList.getFirst().type;
                if(currentGameStatus == GAME_STATUS_ATTACK){
                    mGameSoundManager.playBGM("BGM_ATTACK");
                    isPlayedAttackNarrationFromDefence1 = false;
                    isPlayedAttackNarrationFromDefence2 = false;
                    isPlayedAttackNarrationFromRest = false;
                }else if(currentGameStatus == GAME_STATUS_DEFENCE){
                    initDefenceStage(time);
                    mGameSoundManager.playBGM("BGM_DEFENCE");
                    isPlayedDefenceNarration = false;
                }else if(currentGameStatus == GAME_STATUS_REST){
                    mGameSoundManager.playBGM("BGM_REST");
                    isPlayedRestNarration = false;
                }
                current_phase_start_time = time;
            }
        }

        switch(currentGameStatus){
            case GAME_STATUS_WAIT:{
                //nothing
                if(mSwimmingMotionDetector.getStatus() == SwimmingMotionDetector.STATUS_PRONE){
                    currentGameStatus = GAME_STATUS_START;
                    current_phase_start_time = time;
                    InitStageForGame(time);
                    mGameSoundManager.playBGM("BGM_START");
                    isPlayedOpening = false;
                    isPlayedOpeningNarration1 = false;
                    isPlayedOpeningNarration2 = false;
                    isPlayedAttackTutorial1 = false;
                    isPlayedAttackTutorial2 = false;
                    isPlayedDefenceTutorial = false;
                    isPlayedRestTutorial = false;
                    isPlayedVictoryNarration = false;
                    isPlayedGameOverNarration = false;
                    isPlayedDragonHP50Narration = false;
                    isPlayedDragonHP25Narration = false;
                    Dragon_HP = 300;
                    My_HP = 10;
                }
                break;
            }
            case GAME_STATUS_START:{
                //nothing
                if(!isPlayedOpening && time - current_phase_start_time > 2000){
                    mGameSoundManager.playAsync("Opening");
                    isPlayedOpening = true;
                }

                if(!isPlayedOpeningNarration1 && time - current_phase_start_time > 1000){
                    mGameSoundManager.playSoundEffect("NARRATION_OPENING_1");
                    isPlayedOpeningNarration1 = true;
                }


                if(!isPlayedOpeningNarration2 && time - current_phase_start_time > 13000){
                    mGameSoundManager.playSoundEffect("NARRATION_OPENING_2");
                    isPlayedOpeningNarration2 = true;
                }

                if(time - current_phase_start_time > 14000){
                    currentGameStatus = phaseList.getFirst().type;
                    if(currentGameStatus == GAME_STATUS_ATTACK){
                        mGameSoundManager.playBGM("BGM_ATTACK");
                        isPlayedAttackNarrationFromDefence1 = false;
                        isPlayedAttackNarrationFromDefence2 = false;
                        isPlayedAttackNarrationFromRest = false;
                    }else if(currentGameStatus == GAME_STATUS_DEFENCE){
                        mGameSoundManager.playBGM("BGM_DEFENCE");
                        isPlayedDefenceNarration = false;
                    }else if(currentGameStatus == GAME_STATUS_REST){
                        mGameSoundManager.playBGM("BGM_REST");
                        isPlayedRestNarration = false;
                    }
                    previous_game_status = GAME_STATUS_START;
                    current_phase_start_time = time;
                }
                break;
            }
            case GAME_STATUS_ATTACK:{
                if(!isPlayedAttackNarrationFromDefence1 && previous_game_status == GAME_STATUS_DEFENCE && time - current_phase_start_time > 1500){
                    mGameSoundManager.playSoundEffect("NARRATION_DEFENCE_TO_ATTACK_1");
                    isPlayedAttackNarrationFromDefence1 = true;
                }
                if(!isPlayedAttackNarrationFromDefence2 && previous_game_status == GAME_STATUS_DEFENCE && time - current_phase_start_time > 3500){
                    mGameSoundManager.playSoundEffect("NARRATION_DEFENCE_TO_ATTACK_2");
                    isPlayedAttackNarrationFromDefence2 = true;
                }
                if(!isPlayedAttackNarrationFromRest && previous_game_status == GAME_STATUS_REST && time - current_phase_start_time > 1500){
                    mGameSoundManager.playSoundEffect("NARRATION_REST_TO_ATTACK");
                    isPlayedAttackNarrationFromRest = true;
                }



                if(!isPlayedAttackTutorial1 && time - current_phase_start_time > 1500){
                    mGameSoundManager.playSoundEffect("NARRATION_FREESTYLE");
                    isPlayedAttackTutorial1 = true;
                }

                if(!isPlayedAttackTutorial2 && time - current_phase_start_time > 7000){
                    mGameSoundManager.playSoundEffect("NARRATION_BUTTERFLY");
                    isPlayedAttackTutorial2 = true;
                }

                if(mSwimmingMotionDetector.getStroke() == SwimmingMotionDetector.FREESTYLE_LEFT_STROKE ||
                        mSwimmingMotionDetector.getStroke() == SwimmingMotionDetector.FREESTYLE_RIGHT_STROKE){
                    Dragon_HP -= 2;
                }else if(mSwimmingMotionDetector.getStroke() == SwimmingMotionDetector.BUTTERFLY_STROKE){
                    Dragon_HP -= 10;
                }

                if(!isPlayedDragonHP50Narration && Dragon_HP < 150){
                    mGameSoundManager.playSoundEffect("NARRATION_DRAGON_HP_50");
                    isPlayedDragonHP50Narration = true;
                }

                if(!isPlayedDragonHP25Narration && Dragon_HP < 75){
                    mGameSoundManager.playSoundEffect("NARRATION_DRAGON_HP_25");
                    isPlayedDragonHP25Narration = true;
                }

                if(Dragon_HP < 0){
                    currentGameStatus = GAME_STATUS_VICTORY;
                    mGameSoundManager.playBGM("BGM_VICTORY");
                }

                break;
            }
            case GAME_STATUS_DEFENCE:{
                updateDefenceStage(time);
                break;
            }
            case GAME_STATUS_REST:{
                if(!isPlayedRestNarration && time - current_phase_start_time > 500){
                    mGameSoundManager.playSoundEffect("NARRATION_REST");
                    isPlayedRestNarration = true;
                }


                if(!isPlayedRestTutorial && time - current_phase_start_time > 4000){
                    mGameSoundManager.playSoundEffect("NARRATION_BACKSTROKE");
                    isPlayedRestTutorial = true;
                }


                if(mSwimmingMotionDetector.getStroke() == SwimmingMotionDetector.BACKSTROKE_LEFT_STROKE ||
                        mSwimmingMotionDetector.getStroke() == SwimmingMotionDetector.BACKSTROKE_RIGHT_STROKE){
                    My_HP += 0.5;
                    if(My_HP > 20)
                        My_HP = 20;
                    if(My_HP == 10)
                        mGameSoundManager.playSoundEffect("NARRATION_HEAL");
                }
                break;
            }
            case GAME_STATUS_VICTORY:{
                if(!isPlayedVictoryNarration && time - current_phase_start_time > 2000){
                    mGameSoundManager.playSoundEffect("NARRATION_VICTORY");
                    isPlayedVictoryNarration = true;
                }
                break;
            }
            case GAME_STATUS_GAME_OVER:{
                if(!isPlayedGameOverNarration && time - current_phase_start_time > 1000){
                    mGameSoundManager.playSoundEffect("NARRATION_GAMEOVER");
                    isPlayedGameOverNarration = true;
                }
                break;
            }
        }
    }


    private LinkedList<byte[]> packetList = new LinkedList<byte[]>();
    private int packetSeqNum = 0, ackSeqNum = 0, packetCount = 0;
    public void SendBTWithRetransmission(byte[] b){
        packetList.add(b);
    }

    public void sendNTP(){
        mCommunicationManager.networkManager.sendNTP = true;
    }

    public void SendStrokeData(int type, int time){
        byte[] buffer = new byte[11];
        buffer[0] = MSG_TYPE_DATA;
        buffer[1] = (byte) myID;
        ByteBuffer bb_seq = ByteBuffer.allocate(4);
        bb_seq.putInt(++packetCount);
        System.arraycopy(bb_seq.array(), 0, buffer, 2, 4);
        buffer[6] = (byte) type;
        ByteBuffer bb_time = ByteBuffer.allocate(4);
        bb_time.putInt(time);
        System.arraycopy(bb_time.array(), 0, buffer,7, 4);
        SendBTWithRetransmission(buffer);
    }

//	public void SendBTAck(int seq){
//		byte[] buffer = new byte[6];
//		buffer[0] = MSG_TYPE_ACK;
//		buffer[1] = (byte) myID;
//		ByteBuffer bb = ByteBuffer.allocate(4);
//		bb.putInt(seq);
//		System.arraycopy(bb.array(), 0, buffer, 2, 4);
//		mBluetoothManager.SendByte(buffer);
//	}

    public void SendCommand(int msg){
        byte[] buffer = new byte[7];
        buffer[0] = MSG_TYPE_COMMAND;
        buffer[1] = (byte) myID;
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(++packetCount);
        System.arraycopy(bb.array(), 0, buffer, 2, 4);
        buffer[6] = (byte) msg;
        SendBTWithRetransmission(buffer);
    }




    private class SoundNote{
        public int type;
        public long time_source;
        public long time_target;

        public SoundNote(int t, long src, long tar){
            type = t;
            time_source = src;
            time_target = tar;
        }
    }

    private HashMap<Integer, LinkedList<SoundNote>> soundNoteList = new HashMap<Integer, LinkedList<SoundNote>>();


    private LinkedList<Long> breathTimeList = new LinkedList<Long>();
    private long stomp_duration = 2800;
    private boolean defence_judged = false;
    private boolean isDefenceJudgeStarted = false;
    private float My_HP_defence_started;

    private void initDefenceStage(long time){
        if(!breathTimeList.isEmpty())
            breathTimeList.clear();

        for(int i=0; i<20; ++i){
            breathTimeList.add(time + (isPlayedDefenceNarration? 1500 : 3000) + (i+1)*2800);
        }
        isDefenceJudgeStarted = false;
        isPlayedMyHPLessNarration = false;
        My_HP_defence_started = My_HP;
    }

    private void updateDefenceStage(long time){
        if(!isPlayedDefenceNarration && time - current_phase_start_time > 500){
            mGameSoundManager.playSoundEffect("NARRATION_BREATH");
            isPlayedDefenceNarration = true;
        }

        if(!isPlayedDefenceTutorial && time - current_phase_start_time > 2500){
            mGameSoundManager.playSoundEffect("NARRATION_BREASTSTROKE");
            isPlayedDefenceTutorial = true;
        }

        long next_time = breathTimeList.getFirst();

        //sound
        if(time > next_time){
            mGameSoundManager.playSoundEffect("EFFECT_DRAGON_STOMP", 1.0f, 1.0f);
            breathTimeList.removeFirst();
            defence_judged = false;
            isDefenceJudgeStarted = true;
        }

        if(!isDefenceJudgeStarted)
            return;

        //detection
        if(!defence_judged && mSwimmingMotionDetector.getStroke() == SwimmingMotionDetector.BREASTSTROKE_STROKE){
            //mGameSoundManager.playSoundEffect("EFFECT_DEFENCE_S", 1.0f, 1.0f);
            defence_judged = true;
        }else if(!defence_judged && time < next_time && time > next_time - 1300){
            mGameSoundManager.playSoundEffect("EFFECT_DEFENCE_F", 1.0f, 1.0f);
            defence_judged = true;
            My_HP -= 1;
            if(My_HP < 0){
                currentGameStatus = GAME_STATUS_GAME_OVER;
                mGameSoundManager.playBGM("NARRATION_GAMEOVER");
                mGameSoundManager.playBGM("BGM_GAMEOVER");
            }

            if(!isPlayedMyHPLessNarration && My_HP != My_HP_defence_started && My_HP < 5){
                mGameSoundManager.playBGM("NARRATION_MY_HP_LESS");
                isPlayedMyHPLessNarration = true;
            }
        }
    }

//	@Override
//	public synchronized void onBluetoothReceived(byte[] buffer, int length) {
//		int msg_type = buffer[0];
//		int id = buffer[1];
//		if(id != myID)
//			return;
//
//		int seq = ByteBuffer.wrap(buffer, 2, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
//		//ack processing
//		if(msg_type == MSG_TYPE_ACK){
//			if(seq == packetSeqNum+1){
//				packetList.removeFirst();
//				packetSeqNum++;
//			}
//
//			return;
//		}else{
//			if(seq == ackSeqNum){
////				SendBTAck(seq);
//				return;
//			}else if(seq != ackSeqNum + 1){
//				//must not reach here;
//				return;
//			}
//
//			//new data
////			SendBTAck(seq);
//			++ackSeqNum;
//		}
//
//
//		if(msg_type == MSG_TYPE_DATA){
//			//stroke data from other user
//			int src_id = buffer[6];
//			int type = buffer[7];
//			int time = ByteBuffer.wrap(buffer, 8, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
//			if(!soundNoteList.containsKey(src_id)){
//				return;
//			}
//			if(soundNoteList.get(src_id).isEmpty()){
//				soundNoteList.get(src_id).add(new SoundNote(type, time, System.currentTimeMillis()));
//			}else{
//				long last_time = soundNoteList.get(src_id).getLast().time_source;
//				soundNoteList.get(src_id).add(new SoundNote(type, time, System.currentTimeMillis() + (time - last_time)));
//			}
//		}
//		else if(msg_type == MSG_TYPE_NTP){
//			//not use now. need to fix
//			int clientSendTime = ByteBuffer.wrap(buffer, 6, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
//			int serverRecvTime = ByteBuffer.wrap(buffer, 10, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
//			int serverSendTime = ByteBuffer.wrap(buffer, 14, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
//			int clientRecvTime = (int)(System.currentTimeMillis() - startTime);
//			int diff = serverRecvTime - (int)(((clientRecvTime - clientSendTime) - (serverSendTime - serverRecvTime)) / 2f);
//			mLogger.WriteBT(String.format("time\t%d\t%d\t%d\t%d\t%d", clientSendTime, serverRecvTime, serverSendTime, clientRecvTime, diff));
//			uiHandler.obtainMessage(GameActivity.MSG_NTP_STATUS, "Tyme Sync done").sendToTarget();
//		}
//		else if(msg_type == MSG_TYPE_COMMAND){
//			int command = buffer[6];
//			switch(command){
//			case COMMAND_GAMESTART:{
//				currentGameStatus = GAME_STATUS_START;
//				mGameSoundManager.playBGM("BGM_START");
//				break;
//			}
//			case COMMAND_CHANGE_TO_ATTACK:{
//				currentGameStatus = GAME_STATUS_ATTACK;
//				mGameSoundManager.playBGM("BGM_ATTACK");
//				break;
//			}
//			case COMMAND_CHANGE_TO_DEFFENCE:{
//				currentGameStatus = GAME_STATUS_DEFENCE;
//				mGameSoundManager.playBGM("BGM_DEFENCE");
//				isDefenceStarted = false;
//				break;
//			}
//			case COMMAND_VICTORY:{
//				currentGameStatus = GAME_STATUS_VICTORY;
//				mGameSoundManager.playBGM("BGM_VICTORY");
//				break;
//			}
//			case COMMAND_GAMEOVER:{
//				currentGameStatus = GAME_STATUS_GAME_OVER;
//				mGameSoundManager.playBGM("BGM_GAMEOVER");
//				break;
//			}
//
//			}
//
//		}
//	}
}