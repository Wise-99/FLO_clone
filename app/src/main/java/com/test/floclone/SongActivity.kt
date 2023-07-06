package com.test.floclone

import android.media.MediaPlayer
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.test.floclone.databinding.ActivitySongBinding
import java.lang.Exception

class SongActivity: AppCompatActivity() {
    lateinit var binding: ActivitySongBinding
    lateinit var song : Song
    lateinit var timer : Timer
    // Activity가 소멸될 때 해제해주기 위해 nullable로 설정
    private var mediaPlayer : MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 데이터 받아오기
        initSong()
        setPlayer(song)

        // 아래 화살표를 눌렀을 때 액티비티 종료
        binding.songDownIb.setOnClickListener {
            finish()
        }

        // 재생 버튼을 눌렸을 때
        binding.songMiniplayerIv.setOnClickListener {
            setPlayerStatus(true)
        }

        // 정지 버튼을 눌렀을 때
        binding.songPauseIv.setOnClickListener {
            setPlayerStatus(false)
        }
    }

    // 사용자가 포커스를 잃었을 때 음악 중지
    override fun onPause() {
        super.onPause()
        setPlayerStatus(false)
        song.second = ((binding.songProgressSb.progress * song.playTime)/100)/1000
        val sharedPreferences = getSharedPreferences("song", MODE_PRIVATE)
        val editor = sharedPreferences.edit() // 에디터


    }

    override fun onDestroy() {
        super.onDestroy()
        // 스레드 강제 종료
        timer.interrupt()
        mediaPlayer?.release() // 미디어 플레이어가 갖고 있던 리소스 해제
        mediaPlayer = null // 미디어 플레이어 해제
    }

    // song 데이터 클래스 초기화
    private fun initSong(){
        if(intent.hasExtra("title") && intent.hasExtra("singer")){
            song = Song(
                intent.getStringExtra("title")!!,
                intent.getStringExtra("singer")!!,
                intent.getIntExtra("second",0),
                intent.getIntExtra("playTime", 0),
                intent.getBooleanExtra("isPlaying", false),
                intent.getStringExtra("music")!!
            )
        }
        // 객체 초기화와 동시에 타이머 작동
        startTimer()
    }

    // 정보를 받아 뷰 렌더링
    private fun setPlayer(song : Song){
        binding.songMusicTitleTv.text = intent.getStringExtra("title")!!
        binding.songSingerNameTv.text = intent.getStringExtra("singer")!!
        binding.songStartTimeTv.text = String.format("%02d:%02d", song.second / 60, song.second % 60)
        binding.songEndTimeTv.text = String.format("%02d:%02d", song.playTime / 60, song.playTime % 60)
        binding.songProgressSb.progress = (song.second * 1000 / song.playTime)
        val music = resources.getIdentifier(song.music, "raw", this.packageName)
        mediaPlayer = MediaPlayer.create(this, music)

        setPlayerStatus(song.isPlaying)
    }

    fun setPlayerStatus(isPlaying : Boolean){
        song.isPlaying = isPlaying
        timer.isPlaying = isPlaying

        // 정지 버튼 표시(노래 재생)
        if (isPlaying){
            binding.songMiniplayerIv.visibility = View.GONE
            binding.songPauseIv.visibility = View.VISIBLE
            mediaPlayer?.start()
        }
        // 재생 버튼 표시(노래 정지)
        else {
            binding.songMiniplayerIv.visibility = View.VISIBLE
            binding.songPauseIv.visibility = View.GONE
            if (mediaPlayer?.isPlaying == true){
                mediaPlayer?.pause()
            }
        }
    }

    private fun startTimer(){
        timer = Timer(song.playTime, song.isPlaying)
        timer.start()
    }

    // 시간이 지남에 따라 재생 시간과 프로그레스 바를 바꿔주는 함수
    inner class Timer(private val playTime : Int, var isPlaying : Boolean = true) : Thread(){
        private var second : Int = 0
        private var mills : Float = 0f

        override fun run() {
            super.run()
            // 스레드가 강제 종료 되도 오류가 발생하지 않도록 try-catch 사용
            try{
                // 타이머 무한 반복
                while (true){
                    // 노래 시간이 끝나면 반복문 종료
                    if (second >= playTime){
                        break
                    }
                    // 재생 중이면 50밀리초 sleep하고 더해준다
                    if (isPlaying){
                        sleep(50)
                        mills += 50

                        // 뷰 렌더링(프로그레스 바 변경)
                        runOnUiThread {
                            binding.songProgressSb.progress = ((mills / playTime) * 100).toInt()
                        }

                        // 뷰 렌더링(1초가 지날 때마다 재생 시간 변경)
                        if (mills % 1000 == 0f){
                            runOnUiThread {
                                binding.songStartTimeTv.text = String.format("%02d:%02d", second / 60, second % 60)
                            }
                            second++
                        }
                    }
                }
            } catch (e:Exception){

            }

        }
    }
}