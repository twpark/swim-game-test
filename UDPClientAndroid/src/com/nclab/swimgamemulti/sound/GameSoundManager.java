package com.nclab.swimgamemulti.sound;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.SoundPool;
import android.net.Uri;
import com.nclab.swimgamemulti.R;

import java.io.IOException;
import java.util.HashMap;

public class GameSoundManager {
	
	private HashMap<String, Integer> mSoundPoolID;
	private HashMap<String, MediaPlayer> mMapMediaPlayer;	
	private SoundPool mSoundPool;
	private Context mContext;
	private static final int MAX_SOUND_STREAM_NUM = 20;
	
	public GameSoundManager(Context ctx){
		mMapMediaPlayer = new HashMap<String, MediaPlayer>();
		mSoundPoolID = new HashMap<String, Integer>();
		mContext = ctx;
		mSoundPool = new SoundPool(MAX_SOUND_STREAM_NUM, AudioManager.STREAM_MUSIC, 0);
	}
	
	
	
	private void loadMediaPlayer(String key, int id){
		Uri path = Uri.parse("android.resource://com.nclab.swimgame.proto/" + id);
		
		MediaPlayer m = new MediaPlayer();
		try {
			m.setDataSource(mContext, path);
			//m.prepare();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//MediaPlayer m = MediaPlayer.create(mContext, id);
		mMapMediaPlayer.put(key, m);
	}
	
	public void load(){
		//bgm
//
//		loadMediaPlayer("BGM_WAIT", R.raw.bgm_fresh_start);
//		loadMediaPlayer("BGM_START", R.raw.bgm_chasing_ghosts);
//		loadMediaPlayer("BGM_ATTACK", R.raw.bgm_breaking_the_enemy_lines);
//		loadMediaPlayer("BGM_DEFENCE", R.raw.bgm_the_fourth_horseman);
//		loadMediaPlayer("BGM_REST", R.raw.bgm_eclipse_of_hope);
//		loadMediaPlayer("BGM_VICTORY", R.raw.bgm_successful_accomplishment);
//		loadMediaPlayer("BGM_GAMEOVER", R.raw.bgm_sacred_forest);
//
//		loadMediaPlayer("Opening", R.raw.opening);
//		loadMediaPlayer("Comming", R.raw.comming);
		
		/*
		mMapMediaPlayer.put("BGM_WAIT",  MediaPlayer.create(mContext, R.raw.bgm_fresh_start));
		mMapMediaPlayer.put("BGM_START",  MediaPlayer.create(mContext, R.raw.bgm_chasing_ghosts));
		mMapMediaPlayer.put("BGM_ATTACK",  MediaPlayer.create(mContext, R.raw.bgm_breaking_the_enemy_lines));
		mMapMediaPlayer.put("BGM_DEFFENCE",  MediaPlayer.create(mContext, R.raw.bgm_the_fourth_horseman));
		mMapMediaPlayer.put("BGM_VICTORY",  MediaPlayer.create(mContext, R.raw.bgm_successful_accomplishment));
		mMapMediaPlayer.put("BGM_GAMEOVER",  MediaPlayer.create(mContext, R.raw.bgm_sacred_forest));
		*/
		
//		//sound effect
//		mSoundPoolID.put("EFFECT_SWORD", mSoundPool.load(mContext, R.raw.effect_sword_hit, 1));
//		mSoundPoolID.put("EFFECT_SWISH", mSoundPool.load(mContext, R.raw.effect_swishes, 1));
		mSoundPoolID.put("EFFECT_HAMMER", mSoundPool.load(mContext, R.raw.effect_hammer, 1));
//
//
		mSoundPoolID.put("EFFECT_STROKE_FREE1", mSoundPool.load(mContext, R.raw.effect_attack_sword_f_1, 1));
//		mSoundPoolID.put("EFFECT_STROKE_FREE2", mSoundPool.load(mContext, R.raw.effect_attack_sword_f_2, 1));
//		mSoundPoolID.put("EFFECT_STROKE_FREE3", mSoundPool.load(mContext, R.raw.effect_attack_sword_f_3, 1));
//		mSoundPoolID.put("EFFECT_STROKE_FREE4", mSoundPool.load(mContext, R.raw.effect_attack_sword_f_4, 1));
		mSoundPoolID.put("EFFECT_STROKE_BACK", mSoundPool.load(mContext, R.raw.effect_resurrection_count, 1));
		mSoundPoolID.put("EFFECT_STROKE_BREAST", mSoundPool.load(mContext, R.raw.effect_defence_positive_dodge, 1));
		mSoundPoolID.put("EFFECT_STROKE_BUTTERFLY", mSoundPool.load(mContext, R.raw.effect_hammer, 1));
//
//		mSoundPoolID.put("EFFECT_DRAGON_STEP", mSoundPool.load(mContext, R.raw.effect_dragon_step, 1));
//		mSoundPoolID.put("EFFECT_DRAGON_ROAR", mSoundPool.load(mContext, R.raw.effect_dragon_roar, 1));
//
//		mSoundPoolID.put("EFFECT_DRAGON_STOMP", mSoundPool.load(mContext, R.raw.effect_defence_dragon_stomp, 1));
//		mSoundPoolID.put("EFFECT_DEFENCE_S", mSoundPool.load(mContext, R.raw.effect_defence_positive_dodge, 1));
//		mSoundPoolID.put("EFFECT_DEFENCE_F", mSoundPool.load(mContext, R.raw.effect_defence_negative, 1));
//
		mSoundPoolID.put("EFFECT_RESURRECTION_COUNT", mSoundPool.load(mContext, R.raw.effect_resurrection_count, 1));
//		mSoundPoolID.put("EFFECT_RESURRECTION", mSoundPool.load(mContext, R.raw.effect_resurrection, 1));
//
//
//		//narration
//		mSoundPoolID.put("NARRATION_OPENING_1", mSoundPool.load(mContext, R.raw.narration_opening_1, 1));
//		mSoundPoolID.put("NARRATION_OPENING_2", mSoundPool.load(mContext, R.raw.narration_opening_2, 1));
//
//		mSoundPoolID.put("NARRATION_FREESTYLE", mSoundPool.load(mContext, R.raw.narration_freestyle, 1));
//		mSoundPoolID.put("NARRATION_BACKSTROKE", mSoundPool.load(mContext, R.raw.narration_backstroke, 1));
//		mSoundPoolID.put("NARRATION_BREASTSTROKE", mSoundPool.load(mContext, R.raw.narration_breaststroke, 1));
//		mSoundPoolID.put("NARRATION_BUTTERFLY", mSoundPool.load(mContext, R.raw.narration_butterfly, 1));
//
//		mSoundPoolID.put("NARRATION_BREATH", mSoundPool.load(mContext, R.raw.narration_breath_1, 1));
//		mSoundPoolID.put("NARRATION_REST_TO_ATTACK", mSoundPool.load(mContext, R.raw.narration_rest_to_attack, 1));
//		mSoundPoolID.put("NARRATION_DEFENCE_TO_ATTACK_1", mSoundPool.load(mContext, R.raw.narration_defence_to_attack_1, 1));
//		mSoundPoolID.put("NARRATION_DEFENCE_TO_ATTACK_2", mSoundPool.load(mContext, R.raw.narration_defence_to_attack_2, 1));
//		mSoundPoolID.put("NARRATION_DRAGON_HP_25", mSoundPool.load(mContext, R.raw.narration_dragon_hp_25, 1));
//		mSoundPoolID.put("NARRATION_DRAGON_HP_50", mSoundPool.load(mContext, R.raw.narration_dragon_hp_50, 1));
//		mSoundPoolID.put("NARRATION_HEAL", mSoundPool.load(mContext, R.raw.narration_heal, 1));
//		mSoundPoolID.put("NARRATION_MY_HP_LESS", mSoundPool.load(mContext, R.raw.narration_my_hp_less, 1));
//		mSoundPoolID.put("NARRATION_REST", mSoundPool.load(mContext, R.raw.narration_rest, 1));
//
//		mSoundPoolID.put("NARRATION_VICTORY", mSoundPool.load(mContext, R.raw.narration_victory, 1));
//		mSoundPoolID.put("NARRATION_GAMEOVER", mSoundPool.load(mContext, R.raw.narration_gameover, 1));
	}
	
	private MediaPlayer currentMediaPlayer = null;
	
	public void playAsync(String key){
		MediaPlayer m = mMapMediaPlayer.get(key);
		try {
			m.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					// TODO Auto-generated method stub
					mp.stop();
				}
			});
			m.prepare();
			m.seekTo(0);
			m.start();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void playBGM(String key){
		if(currentMediaPlayer != null)
			currentMediaPlayer.stop();
		
		currentMediaPlayer = mMapMediaPlayer.get(key);
		if(currentMediaPlayer != null){
			try {
				
				currentMediaPlayer.setVolume(0.2f, 0.2f);
				currentMediaPlayer.setLooping(true);
				currentMediaPlayer.prepare();
				currentMediaPlayer.seekTo(0);
				currentMediaPlayer.start();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void stopBGM(){
		if(currentMediaPlayer != null)
			currentMediaPlayer.stop();
	}
	
	public void playSoundEffect(String key){
		if(mSoundPoolID.containsKey(key)){
			int id = mSoundPoolID.get(key);
			mSoundPool.play(id, 1.0f, 1.0f, 0, 0, 1.0f);
		}
	}
	
	public void playSoundEffect(String key, float left, float right){
		if(mSoundPoolID.containsKey(key)){
			int id = mSoundPoolID.get(key);
			mSoundPool.play(id, left, right, 0, 0, 1.0f);
		}
	}
}
