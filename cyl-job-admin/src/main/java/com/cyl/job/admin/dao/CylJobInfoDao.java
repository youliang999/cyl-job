package com.cyl.job.admin.dao;

import com.cyl.job.admin.core.model.CylJobInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * job info
 * @author xuxueli 2016-1-12 18:03:45
 */
@Mapper
public interface CylJobInfoDao {

	public List<CylJobInfo> pageList(@Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("jobGroup") int jobGroup,
            @Param("triggerStatus") int triggerStatus,
            @Param("jobDesc") String jobDesc,
            @Param("executorHandler") String executorHandler,
            @Param("author") String author);
	public int pageListCount(@Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("jobGroup") int jobGroup,
            @Param("triggerStatus") int triggerStatus,
            @Param("jobDesc") String jobDesc,
            @Param("executorHandler") String executorHandler,
            @Param("author") String author);
	
	public int save(CylJobInfo info);

	public CylJobInfo loadById(@Param("id") int id);
	
	public int update(CylJobInfo xxlJobInfo);
	
	public int delete(@Param("id") int id);

	public List<CylJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

	public int findAllCount();

	public List<CylJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime);

	public int scheduleUpdate(CylJobInfo xxlJobInfo);


}
