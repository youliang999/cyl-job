package com.cyl.api.dao;

import com.cyl.api.model.CylJobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xuxueli 2019-05-04 16:44:59
 */
@Mapper
public interface CylJobUserDao {

	public List<CylJobUser> pageList(@Param("offset") int offset,
									 @Param("pagesize") int pagesize,
									 @Param("username") String username,
									 @Param("role") int role);
	public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("username") String username,
                             @Param("role") int role);

	public CylJobUser loadByUserName(@Param("username") String username);

	public int save(CylJobUser xxlJobUser);

	public int update(CylJobUser xxlJobUser);
	
	public int delete(@Param("id") int id);

}
