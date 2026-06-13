package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.service.BackgroundVoiceService
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Voice Assistant", appName)
  }

  @Test
  fun `verify service intent registration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceIntent = Intent(context, BackgroundVoiceService::class.java).apply {
      action = BackgroundVoiceService.ACTION_START
    }
    val componentName = context.startService(serviceIntent)
    Assert.assertNotNull(componentName)
  }

  @Test
  fun `verify permission manager onboarding states`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.permissions.PermissionManager(context)
    
    // Default first launch should be true
    Assert.assertTrue(manager.isFirstLaunch())
    
    // Complete onboarding should flag false
    manager.completeOnboarding()
    Assert.assertFalse(manager.isFirstLaunch())
    
    // Reset onboarding should revert to true
    manager.resetOnboarding()
    Assert.assertTrue(manager.isFirstLaunch())
  }

  @Test
  fun `verify permission list integrity and health status`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.permissions.PermissionManager(context)
    val list = manager.refreshPermissions()
    
    // 14 required permissions
    assertEquals(14, list.size)
    
    val healthMetric = manager.getPermissionHealthStatus()
    val percentage = manager.getPermissionHealthPercentage()
    
    Assert.assertNotNull(healthMetric)
    Assert.assertTrue(percentage in 0..100)
    
    val intent = manager.getIntentForSpecialPermission("accessibility")
    Assert.assertNotNull(intent)
    assertEquals(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS, intent?.action)
  }
}
