package com.cyl.job.admin.dao;

import com.cyl.job.admin.core.model.CylJobLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CylJobLogDao extends JpaRepository<CylJobLog, Integer> {

}
