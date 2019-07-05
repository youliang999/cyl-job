package com.cyl.job.admin.dao;

import com.cyl.job.admin.core.model.CylJobGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Created by xuxueli on 16/9/30.
 */
@Mapper
public interface CylJobGroupDao {

    public List<CylJobGroup> findAll();

    public List<CylJobGroup> findByAddressType(@Param("addressType") int addressType);

    public int save(CylJobGroup xxlJobGroup);

    public int update(CylJobGroup xxlJobGroup);

    public int remove(@Param("id") int id);

    public CylJobGroup load(@Param("id") int id);
}
