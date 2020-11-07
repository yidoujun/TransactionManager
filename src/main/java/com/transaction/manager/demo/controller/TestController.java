package com.transaction.manager.demo.controller;

import com.transaction.manager.demo.service.StudentService;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Resource
    private StudentService studentService;

    @GetMapping("/insertBatch")
    public void insertBatch(@Param("num") Integer num) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            studentService.insertBatch(num);
            stopWatch.stop();
            logger.info("insertBatch执行耗时:{}ms,num={}", stopWatch.getTotalTimeMillis(),num);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/insertOneByOne")
    public void insertOneByOne(@Param("num") Integer num) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            studentService.insertOneByOne(num);
            stopWatch.stop();
            logger.info("insertOneByOne执行耗时:{}ms,num={}", stopWatch.getTotalTimeMillis(),num);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @GetMapping("/batchHandle")
    public void batchHandle() {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            studentService.batchHandle();
            stopWatch.stop();
            logger.info("batchHandle执行耗时:{}ms", stopWatch.getTotalTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
