package com.myagent.app.ui

import com.myagent.app.MainViewModel
import com.myagent.app.model.ModelDownloadState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * 根路由 — 5 步闭环流程：
 * 1. 欢迎页 → 2. 激活 → 3. 模型下载 → 4. 人格选择 → 5. 对话
 *
 * 严格拦截：模型未下载完成，即使 onboarding 标记为完成也不允许进入主界面。
 */
@Composable
fun RootScreen(viewModel: MainViewModel) {
  val welcomeDone by viewModel.welcomeCompleted.collectAsState()
  val isActivated by viewModel.isActivated.collectAsState()
  val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
  val downloadState by viewModel.downloadState.collectAsState()

  // 模型就绪判断：
  // - Completed 直接通过
  // - onboarding 已完成 + Idle（Runtime 未启动时的默认值）→ 信任上次已下载
  val modelReady = when (downloadState) {
    is ModelDownloadState.Completed -> true
    is ModelDownloadState.Idle -> onboardingCompleted
    else -> false
  }

  when {
    // 步骤1：欢迎页
    !welcomeDone -> {
      WelcomeScreen(
        onStart = { viewModel.setWelcomeCompleted() },
        modifier = Modifier.fillMaxSize(),
      )
    }
    // 步骤2：未激活 → 激活页
    !isActivated -> {
      ActivationScreen(
        onActivate = { code -> viewModel.activate(code) },
        modifier = Modifier.fillMaxSize(),
      )
    }
    // 步骤3-4：未完成引导 或 模型未就绪 → 引导流程
    !onboardingCompleted || !modelReady -> {
      OnboardingFlow(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
    // 步骤5：已完成引导 + 模型就绪 → 主界面
    else -> {
      ShellScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
  }
}