package kr.hs.emirim.shookhee.quizlocker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.MultiSelectListPreference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.annotations.Nullable
import kotlinx.android.synthetic.main.activity_main.*
import kr.hs.emirim.shookhee.quizlocker.model.User

class MainActivity : AppCompatActivity() {
    val fragment = MyPreferenceFragment()
    private var mDatabase: DatabaseReference? = null
    private var mMessageReference: DatabaseReference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        // preferenceContent FrameLayout 영역을 PreferenceFragment 로 교체
        fragmentManager.beginTransaction().replace(R.id.preferenceContent, fragment).commit()

        ivSetting.setOnClickListener{
            val nextIntent = Intent(this, SettingActivity::class.java)
            startActivity(nextIntent)
        }

        userProfileCarrotImage.setOnClickListener{
            val nextIntent = Intent(this, CollectionActivity::class.java)
            startActivity(nextIntent)
        }

        showMoreRankingButton.setOnClickListener{
            val nextIntent = Intent(this, RankingActivity::class.java)
            startActivity(nextIntent)
        }

        goChatting.setOnClickListener{
            val nextIntent = Intent(this, ChatRoomActivity::class.java)
            startActivity(nextIntent)
        }

        val database = FirebaseDatabase.getInstance()
        val userReference = database.getReference("user")

        val pref =
            getSharedPreferences("pref", Context.MODE_PRIVATE)
        val editor = pref.edit()

        userReference.orderByChild("email").equalTo(pref.getString("userEmail", ""))
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(
                    dataSnapshot: DataSnapshot,
                    @Nullable s: String?
                ) {
                    val user =
                        dataSnapshot.getValue(
                            User::class.java
                        )
                    carrotCountTextView.text = "${user!!.carrotCount} 개"
                }

                override fun onChildChanged(
                    dataSnapshot: DataSnapshot,
                    @Nullable s: String?
                ) {
                    val user =
                        dataSnapshot.getValue(
                            User::class.java
                        )
                    carrotCountTextView.text = "${user!!.carrotCount} 개"
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(
                    dataSnapshot: DataSnapshot,
                    @Nullable s: String?
                ) {
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })

    }

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // 환경설정 리소스 파일 적용
            addPreferencesFromResource(R.xml.pref)
            // 퀴즈 종류 요약정보에, 현재 선택된 항목을 보여주는 코드
            val categoryPref = findPreference("category") as MultiSelectListPreference
            categoryPref.summary = categoryPref.values.joinToString(", ")
            // 환경설정 정보값이 변경될때에도 요약정보를 변경하도록 리스너 등록
            categoryPref.setOnPreferenceChangeListener { preference, newValue ->
                // newValue 파라미터가 HashSet 으로 캐스팅이 실패하면 리턴
                val newValueSet = newValue as? HashSet<*>
                        ?: return@setOnPreferenceChangeListener true
                // 선택된 퀴즈종류로 요약정보 보여줌
                categoryPref.summary = newValue.joinToString(", ")
                true
            }
            // 퀴즈 잠금화면 사용 스위치 객체 가져옴
            val useLockScreenPref = findPreference("useLockScreen") as SwitchPreference
            // 클릭되었을때의 이벤트 리스너 코드 작성
            useLockScreenPref.setOnPreferenceClickListener {
                when {
                    // 퀴즈 잠금화면 사용이 체크된 경우 LockScreenService 실행
                    useLockScreenPref.isChecked -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            activity.startForegroundService(Intent(activity, LockScreenService::class.java))
                        } else {
                            activity.startService(Intent(activity, LockScreenService::class.java))
                        }
                    }
                    // 퀴즈 잠금화면 사용이 체크 해제된 경우 LockScreenService 중단
                    else -> activity.stopService(Intent(activity, LockScreenService::class.java))
                }
                true
            }
            // 앱이 시작 되었을때 이미 퀴즈잠금화면 사용이 체크되어있으면 서비스 실행
            if (useLockScreenPref.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity.startForegroundService(Intent(activity, LockScreenService::class.java))
                } else {
                    activity.startService(Intent(activity, LockScreenService::class.java))
                }
            }
        }
    }
}