package com.cyl.job.admin.dao;

import com.cyl.job.admin.core.model.CylJobRegistry;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Created by xuxueli on 16/9/30.
 */
@Mapper
public interface CylJobRegistryDao {

    public int removeDead(@Param("timeout") int timeout);

    public List<CylJobRegistry> findAll(@Param("timeout") int timeout);

    public int registryUpdate(@Param("registryGroup") String registryGroup,
            @Param("registryKey") String registryKey,
            @Param("registryValue") String registryValue);

    public int registrySave(@Param("registryGroup") String registryGroup,
            @Param("registryKey") String registryKey,
            @Param("registryValue") String registryValue);

    public int registryDelete(@Param("registryGroup") String registGroup,
            @Param("registryKey") String registryKey,
            @Param("registryValue") String registryValue);

}
