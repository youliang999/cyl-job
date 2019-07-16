package com.cyl.api.service;



import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.ResponseModel;
import java.util.Date;
import java.util.Map;

public interface CylJobService {

	/**
	 * page list
	 *
	 * @param start
	 * @param length
	 * @param jobGroup
	 * @param jobDesc
	 * @param executorHandler
	 * @param author
	 * @return
	 */
	public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc,
            String executorHandler, String author);

	/**
	 * add job
	 *
	 * @param jobInfo
	 * @return
	 */
	public  ResponseModel<String> add(CylJobInfo jobInfo);

	/**
	 * update job
	 *
	 * @param jobInfo
	 * @return
	 */
	public ResponseModel<String> update(CylJobInfo jobInfo);

	/**
	 * remove job
	 * 	 *
	 * @param id
	 * @return
	 */
	public ResponseModel<String> remove(int id);

	/**
	 * start job
	 *
	 * @param id
	 * @return
	 */
	public ResponseModel<String> start(int id);

	/**
	 * stop job
	 *
	 * @param id
	 * @return
	 */
	public ResponseModel<String> stop(int id);

	/**
	 * dashboard info
	 *
	 * @return
	 */
	public Map<String,Object> dashboardInfo();

	/**
	 * chart info
	 *
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public ResponseModel<Map<String,Object>> chartInfo(Date startDate, Date endDate);

}
