package com.cyl.api.dao;

import com.cyl.api.model.CylJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * job log
 * @author xuxueli 2016-1-12 18:03:06
 */
@Mapper
public interface CylJobLogDao {

	// exist jobId not use jobGroup, not exist use jobGroup
	public List<CylJobLog> pageList(@Param("offset") int offset,
									@Param("pagesize") int pagesize,
									@Param("jobGroup") int jobGroup,
									@Param("jobId") int jobId,
									@Param("triggerTimeStart") Date triggerTimeStart,
									@Param("triggerTimeEnd") Date triggerTimeEnd,
									@Param("logStatus") int logStatus);
	public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("jobGroup") int jobGroup,
                             @Param("jobId") int jobId,
                             @Param("triggerTimeStart") Date triggerTimeStart,
                             @Param("triggerTimeEnd") Date triggerTimeEnd,
                             @Param("logStatus") int logStatus);

	public CylJobLog load(@Param("id") int id);

	public int save(CylJobLog xxlJobLog);

	public int updateTriggerInfo(CylJobLog xxlJobLog);

	public int updateHandleInfo(CylJobLog xxlJobLog);

	public int delete(@Param("jobId") int jobId);

	public int triggerCountByHandleCode(@Param("handleCode") int handleCode);

	public List<Map<String, Object>> triggerCountByDay(@Param("from") Date from,
                                                       @Param("to") Date to);

	public int clearLog(@Param("jobGroup") int jobGroup,
                        @Param("jobId") int jobId,
                        @Param("clearBeforeTime") Date clearBeforeTime,
                        @Param("clearBeforeNum") int clearBeforeNum);

	public List<Integer> findFailJobLogIds(@Param("pagesize") int pagesize);

	public int updateAlarmStatus(@Param("logId") int logId,
                                 @Param("oldAlarmStatus") int oldAlarmStatus,
                                 @Param("newAlarmStatus") int newAlarmStatus);

}
