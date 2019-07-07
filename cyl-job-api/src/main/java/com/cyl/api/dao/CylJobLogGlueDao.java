package com.cyl.api.dao;

import com.cyl.job.admin.core.model.CylJobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * job log for glue
 * @author xuxueli 2016-5-19 18:04:56
 */
@Mapper
public interface CylJobLogGlueDao {
	
	public int save(CylJobLogGlue xxlJobLogGlue);
	
	public List<CylJobLogGlue> findByJobId(@Param("jobId") int jobId);

	public int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

	public int deleteByJobId(@Param("jobId") int jobId);
	
}
