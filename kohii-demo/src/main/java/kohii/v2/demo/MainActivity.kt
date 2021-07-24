package kohii.v2.demo

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import kohii.v2.core.Home
import kohii.v2.demo.databinding.ActivityMainBinding
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : AppCompatActivity() {

  private val home: Home by lazy(NONE) { Home[application] }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val manager = home.register(this)

    val leftBuilder = home.setUp(MediaItem.fromUri(DemoUrl)) {
      withPlayableTag("左")
    }

    val rightBuilder = home.setUp(MediaItem.fromUri(DemoUrl)) {
      withPlayableTag("右")
    }

    manager.bucket(binding.content)
      .bind(leftBuilder, binding.containerTop)
    manager.bucket(binding.content)
      .bind(rightBuilder, binding.containerBottom)

    binding.addLeftPlayback.setOnClickListener {
      manager.bucket(binding.content)
        .bind(leftBuilder, binding.containerTop)
    }

    binding.addRightPlayback.setOnClickListener {
      manager.bucket(binding.content)
        .bind(rightBuilder, binding.containerTop)
    }

    binding.removeTopContainer.setOnClickListener {
      (binding.containerTop.parent as? ViewGroup)?.removeView(binding.containerTop)
    }
  }

  companion object {
    const val DemoUrl = "https://content.jwplatform.com/manifests/Cl6EVHgQ.m3u8"
  }
}
