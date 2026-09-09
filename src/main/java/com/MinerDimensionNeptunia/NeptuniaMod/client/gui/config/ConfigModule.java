package com.MinerDimensionNeptunia.NeptuniaMod.client.gui.config;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 配置模块接口，每个配置子页面实现此接口
 */
public interface ConfigModule {
    /**
     * 模块名称（显示在列表中）
     */
    Component getDisplayName();

    /**
     * 创建模块的配置界面
     * @param parent 父界面（用于返回）
     * @return 配置界面 Screen
     */
    Screen createConfigScreen(Screen parent);
}