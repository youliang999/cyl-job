package com.cyl.api.dao;

import com.cyl.api.model.CylJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
