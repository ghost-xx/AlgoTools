package com.ghostxx.algotools.data.mapper;

import com.ghostxx.algotools.data.model.AppProcessDto;
import com.ghostxx.algotools.domain.entity.AppProcess;

/**
 * 应用进程数据转换器
 * 负责在DTO和领域实体之间进行转换
 */
public class AppProcessMapper {
    
    /**
     * 将DTO转换为领域实体
     * @param dto 数据传输对象
     * @return 领域实体
     */
    public static AppProcess toDomain(AppProcessDto dto) {
        if (dto == null) {
            return null;
        }
        return new AppProcess(
                dto.getPackageName(),
                dto.getPid(),
                dto.getAppName()
        );
    }
    
    /**
     * 将领域实体转换为DTO
     * @param entity 领域实体
     * @return 数据传输对象
     */
    public static AppProcessDto toDto(AppProcess entity) {
        if (entity == null) {
            return null;
        }
        return new AppProcessDto(
                entity.getPackageName(),
                entity.getPid(),
                entity.getAppName()
        );
    }
    
    /**
     * 将旧的AppInfo模型转换为领域实体
     * 用于兼容旧代码
     * @param appInfo 旧的AppInfo模型
     * @return 领域实体
     */
    public static AppProcess fromLegacyAppInfo(com.ghostxx.algotools.model.AppInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        return new AppProcess(
                appInfo.getPackageName(),
                appInfo.getPid(),
                appInfo.getAppName()
        );
    }
} 