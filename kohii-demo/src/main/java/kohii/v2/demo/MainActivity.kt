package kohii.v2.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kohii.v2.core.Home
import kohii.v2.demo.databinding.ActivityMainBinding
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : AppCompatActivity() {

  private val home: Home by lazy(NONE) { Home[application] }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.removeTopContainer.setOnClickListener {
      DummyBottomSheet().show(supportFragmentManager, "Bottom Sheet")
    }

    /* binding.addLeftPlayback.setOnClickListener {
      manager.bucket(binding.content)
        .bind(leftBuilder, binding.containerTop)
    }

    binding.addRightPlayback.setOnClickListener {
      manager.bucket(binding.content)
        .bind(rightBuilder, binding.containerTop)
    } */
  }

  companion object {
    const val DemoUrl = "https://content.jwplatform.com/manifests/Cl6EVHgQ.m3u8"
  }
}
