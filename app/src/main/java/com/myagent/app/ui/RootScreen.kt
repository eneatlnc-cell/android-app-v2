package com.myagent.app.ui

import com.myagent.app.MainViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * 根路由 — 5 步闭环流程：
 * 1. 激活检查 → 2. 模型下载 → 3. 校验 → 4. 人格选择 → 5. 对话
 */
@Composable
fun RootScreen(viewModel: MainViewModel) {
  val isActivated by viewModel.isActivated.collectAsState()
  val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

  when {
    // 步骤1：未激活 → 激活页
    !isActivated -> {
      ActivationScreen(
        onActivate = { code -> viewModel.activate(code) },
        modifier = Modifier.fillMaxSize(),
      )
    }
    // 步骤2-4：未完成引导 → 引导流程
    !onboardingCompleted -> {
      OnboardingFlow(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
    // 步骤5：已完成引导 → 主界面
    else -> {
      ShellScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
  }
}