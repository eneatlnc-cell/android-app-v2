package com.myagent.app.ui

import com.myagent.app.MainViewModel
import com.myagent.app.model.PersonaType
import com.myagent.app.ui.chat.ChatScreen
import com.myagent.app.ui.design.ClawDesignTheme
import com.myagent.app.ui.design.ClawNavItem
import com.myagent.app.ui.design.ClawScaffold
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * 主界面 Shell — 底部导航栏切换聊天页和设置页。
 *
 * v2.0：集成全屏仪式感人格选择界面。
 */
@Composable
fun ShellScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var showPersonaSelection by remember { mutableStateOf(false) }

  val navItems = listOf(
    ClawNavItem("聊天", Icons.Outlined.ChatBubbleOutline, 0),
    ClawNavItem("设置", Icons.Outlined.Settings, 1),
  )

  ClawDesignTheme {
    Scaffold(
      modifier = modifier,
      bottomBar = {
        // 人格选择界面隐藏底部导航栏
        if (!showPersonaSelection) {
          ClawScaffold(
            items = navItems,
            selectedIndex = selectedTab,
            onItemSelected = { selectedTab = it },
          )
        }
      },
    ) { innerPadding ->
      when (selectedTab) {
        0 -> ChatScreen(
          viewModel = viewModel,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        )
        1 -> SettingsScreen(
          viewModel = viewModel,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          onRequestPersonaSelection = { showPersonaSelection = true },
        )
      }
    }

    // 全屏人格选择叠加层
    AnimatedVisibility(
      visible = showPersonaSelection,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      PersonaSelectionScreen(
        onConfirmed = { type: PersonaType ->
          viewModel.lockPersona(type)
          showPersonaSelection = false
        },
        onDismiss = {
          showPersonaSelection = false
        },
      )
    }
  }
}