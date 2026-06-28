package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.EmotionViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Use 34 to avoid known SDK 36 issues in some setups
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Facial Expression Recognition", appName)
  }

  @Test
  fun `test local analysis on gallery image`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = EmotionViewModel(context.applicationContext as android.app.Application)
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    viewModel.analyzeGalleryImage(bitmap, "Happy", useLocalOnly = true)
  }
}
