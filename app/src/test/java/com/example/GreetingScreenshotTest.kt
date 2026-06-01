package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ErpAppMain
import com.example.viewmodel.ErpViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun runTestForTab(tabName: String) {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = ErpViewModel(application)
        viewModel.loginUser("admin@abiel.erp", "abieladmin123")
        viewModel.setActiveTab(tabName)
        
        composeTestRule.setContent {
            ErpAppMain(viewModel = viewModel)
        }
        
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/screen_${tabName.lowercase()}.png")
    }

    @Test
    fun greeting_screenshot() {
        composeTestRule.setContent { 
            ErpAppMain()
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }

    @Test
    fun test_dashboard_screen() {
        runTestForTab("Dashboard")
    }

    @Test
    fun test_sales_screen() {
        runTestForTab("Sales")
    }

    @Test
    fun test_finance_screen() {
        runTestForTab("Finance")
    }

    @Test
    fun test_reports_screen() {
        runTestForTab("Reports")
    }

    @Test
    fun test_settings_screen() {
        runTestForTab("Settings")
    }
}
