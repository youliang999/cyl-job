package com.cyl.job.admin.controller;

import com.cyl.api.dao.CylJobGroupDao;
import com.cyl.api.dao.CylJobInfoDao;
import com.cyl.api.model.CylJobGroup;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.CylJobRegistry;
import com.cyl.api.model.ResponseModel;
import com.cyl.job.core.config.CylJobAdminConfig;
import com.cyl.job.core.config.RegistryConfig;
import com.cyl.job.core.config.RegistryConfig.RegistType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("jobgroup")
public class JobGroupController {

    @Resource
    public CylJobInfoDao cylJobInfoDao;
    @Resource
    public CylJobGroupDao cylJobGroupDao;

    @RequestMapping
    public String index(Model model) {
        //job group (executor)
        List<CylJobGroup> list = cylJobGroupDao.findAll();
        model.addAttribute("list", list);
        return "jobgroup/jobgroup.index";
    }

    @RequestMapping("/save")
    @ResponseBody
    public ResponseModel<String> save(CylJobGroup cylJobGroup) {
        //valid
        if (cylJobGroup.getAppName() == null || cylJobGroup.getAppName().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入appName");
        }
        if (cylJobGroup.getAppName().length() < 4 || cylJobGroup.getAppName().length() > 64) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "appName长度限制为4~64");
        }
        if (cylJobGroup.getTitle() == null || cylJobGroup.getTitle().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入名称");
        }

        if (cylJobGroup.getAddressType() != 0) {
            if (cylJobGroup.getAddressList() == null || cylJobGroup.getAddressList().trim().length() == 0) {
                return new ResponseModel<>(ResponseModel.FAIL_CODE, "手动录入注册方式，机器地址不可为空");
            }
            String[] address = cylJobGroup.getAddressList().split(",");
            for (String item : address) {
                if (item == null || item.trim().length() == 0) {
                    return new ResponseModel<>(ResponseModel.FAIL_CODE, "机器地址格式非法");
                }
            }
        }

        int ret = cylJobGroupDao.save(cylJobGroup);
        return (ret > 0) ? ResponseModel.SUCCESS : ResponseModel.FAIL;
    }


    @RequestMapping("/update")
    @ResponseBody
    public ResponseModel<String> update(CylJobGroup cylJobGroup) {
        //valid
        if (cylJobGroup.getAppName() == null || cylJobGroup.getAppName().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入appName");
        }
        if (cylJobGroup.getAppName().length() < 4 || cylJobGroup.getAppName().length() > 64) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "appName长度限制为4~64");
        }
        if (cylJobGroup.getTitle() == null || cylJobGroup.getTitle().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入名称");
        }
        if (cylJobGroup.getAddressType() == 0) {
            // 0=自动注册
            List<String> registryList = findRegistryByAppName(cylJobGroup.getAppName());
            String addressListStr = null;
            if (registryList != null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                addressListStr = "";
                for (String item : registryList) {
                    addressListStr += item + ",";
                }
                addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
            }
            cylJobGroup.setAddressList(addressListStr);
        } else {
            //1=手动录入
            if (cylJobGroup.getAddressList() == null || cylJobGroup.getAddressList().trim().length() == 0) {
                return new ResponseModel<String>(500, "手动录入注册方式，机器地址不可为空");
            }
            String[] address = cylJobGroup.getAddressList().split(",");
            for (String item : address) {
                if (item == null || item.trim().length() == 0) {
                    return new ResponseModel<>(500, "机器地址格式非法");
                }
            }
        }
        int ret = cylJobGroupDao.update(cylJobGroup);
        return ret > 0 ? ResponseModel.SUCCESS : ResponseModel.FAIL;
    }

    private List<String> findRegistryByAppName(String appNameParam) {
        HashMap<String, List<String>> addressMap = new HashMap<>();
        List<CylJobRegistry> list = CylJobAdminConfig.getInstance().getCylJobRegistryDao()
                .findAll(RegistryConfig.DEAD_TIMEOUT);
        if (list != null) {
            for (CylJobRegistry item : list) {
                if (RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appName = item.getRegistryKey();
                    List<String> registryList = addressMap.get(appName);
                    if (registryList == null) {
                        registryList = new ArrayList<>();
                    }
                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                    addressMap.put(appName, registryList);
                }
            }
        }
        return addressMap.get(appNameParam);
    }

    @RequestMapping("/remove")
    @ResponseBody
    public ResponseModel<String> remove(int id) {
        //valid
        int count = cylJobInfoDao.pageListCount(0, 10, id, -1, null, null, null);
        if (count > 0) {
            return new ResponseModel<>(500, "拒绝删除，该执行器使用中");
        }

        List<CylJobGroup> allList = cylJobGroupDao.findAll();
        if (allList.size() == 1) {
            return new ResponseModel<>(500, "拒绝删除, 系统至少保留一个执行器");
        }

        int ret = cylJobGroupDao.remove(id);
        return ret > 0 ? ResponseModel.SUCCESS : ResponseModel.FAIL;
    }
    
}
