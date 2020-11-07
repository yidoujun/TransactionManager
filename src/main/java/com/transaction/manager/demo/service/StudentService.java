package com.transaction.manager.demo.service;

import com.transaction.manager.demo.SelfTransactionManager;
import com.transaction.manager.demo.dao.StudentMapper;
import com.transaction.manager.demo.entity.Student;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);


    @Resource
    private StudentMapper studentMapper;

    public void insertBatch(int num) {
        List<Student> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            Student student = new Student();
            student.setName("why" + i);
            student.setHome("大南街" + i + "号");
            list.add(student);
        }
        studentMapper.batchInsert(list);
    }

    @Transactional
    public void insertOneByOne(int num) {
        for (int i = 0; i < num; i++) {
            Student student = new Student();
            student.setName("why" + i);
            student.setHome("大南街" + i + "号");
            studentMapper.insertSelective(student);
        }
    }


    //自定义事务管理器
    @Resource
    private SelfTransactionManager selfTransactionManager;

    //子线程是否能进行提交
    public static volatile boolean IS_OK = true;

    public void batchHandle() {
        //主线程等待所有子线程执行完成
        int threadCount = 5;
        CountDownLatch childMonitor = new CountDownLatch(threadCount);
        //主线程收集到的子线程最终结果
        List<Boolean> childResponse = new ArrayList<Boolean>();
        //子线程在该对象上等待主线程通知
        CountDownLatch mainMonitor = new CountDownLatch(1);
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                //开启事务
                TransactionStatus transactionStatus = selfTransactionManager.begin();
                try {
                    studentMapper.batchInsert(buildStudentList());
                    childResponse.add(Boolean.TRUE);
                    childMonitor.countDown();
                    logger.info("线程{}正常执行完成,等待其他线程执行结束,判断是否需要回滚", Thread.currentThread().getName());
                    mainMonitor.await();
                    if (IS_OK) {
                        logger.info("所有线程都正常完成,线程{}事务提交", Thread.currentThread().getName());
                        selfTransactionManager.commit(transactionStatus);
                    } else {
                        logger.info("有线程出现异常,线程{}事务回滚", Thread.currentThread().getName());
                        selfTransactionManager.rollBack();
                    }
                } catch (Exception e) {
                    childResponse.add(Boolean.FALSE);
                    childMonitor.countDown();
                    logger.error("线程{}发生了异常,开始进行事务回滚", Thread.currentThread().getName());
                    selfTransactionManager.rollBack();
                }
            });
        }
        try {
            //主线程等待所有子线程执行完成
            childMonitor.await();
            for (Boolean resp : childResponse) {
                if (!resp) {
                    //如果有一个子线程执行失败了，则改变mainResult，让所有子线程回滚
                    logger.info("{}:IS_OK的值被修改为false", Thread.currentThread().getName());
                    IS_OK = false;
                    break;
                }
            }
            //主线程获取结果成功，让子线程开始根据主线程的结果执行（提交或回滚）
            mainMonitor.countDown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Student> buildStudentList() {
        List<Student> list = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            Student student = new Student();
            student.setName("why" + i);
            student.setHome("大南街" + i + "号");
            list.add(student);
        }
        return list;
    }
}
