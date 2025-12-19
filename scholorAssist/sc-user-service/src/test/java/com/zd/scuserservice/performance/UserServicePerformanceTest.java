package com.zd.scuserservice.performance;

import com.zd.scuserservice.model.dto.request.UserLoginRequest;
import com.zd.scuserservice.model.dto.request.UserRegisterRequest;
import com.zd.scuserservice.test.BaseIntegrationTest;
import com.zd.scuserservice.test.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务性能测试类
 * 测试API接口的性能和并发处理能力
 * 
 * @author system
 * @since 2024-01-21
 */
@DisplayName("用户服务性能测试")
public class UserServicePerformanceTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/users";
    private static final int CONCURRENT_USERS = 50;
    private static final int REQUESTS_PER_USER = 10;

    @Test
    @DisplayName("用户注册性能测试")
    void testRegisterPerformance() throws Exception {
        int numberOfRegistrations = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfRegistrations; i++) {
            UserRegisterRequest request = TestDataFactory.createValidRegisterRequest();
            request.setUsername("user" + i);
            request.setEmail("user" + i + "@example.com");

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double avgTime = (double) duration / numberOfRegistrations;

        System.out.println("注册性能测试结果:");
        System.out.println("总注册数: " + numberOfRegistrations);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每次注册耗时: " + avgTime + "ms");

        // 断言平均响应时间应该在合理范围内（例如500ms以内）
        assertTrue(avgTime < 500, "注册平均响应时间过长: " + avgTime + "ms");
    }

    @Test
    @DisplayName("用户登录性能测试")
    void testLoginPerformance() throws Exception {
        // 先注册一个用户
        UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
                .andExpect(status().isOk());

        int numberOfLogins = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfLogins; i++) {
            UserLoginRequest loginRequest = TestDataFactory.createValidLoginRequest();

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double avgTime = (double) duration / numberOfLogins;

        System.out.println("登录性能测试结果:");
        System.out.println("总登录数: " + numberOfLogins);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每次登录耗时: " + avgTime + "ms");

        // 断言平均响应时间应该在合理范围内（例如200ms以内）
        assertTrue(avgTime < 200, "登录平均响应时间过长: " + avgTime + "ms");
    }

    @Test
    @DisplayName("并发注册测试")
    void testConcurrentRegistration() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            Future<Void> future = executor.submit(() -> {
                try {
                    UserRegisterRequest request = TestDataFactory.createValidRegisterRequest();
                    request.setUsername("concurrent_user" + userId);
                    request.setEmail("concurrent_user" + userId + "@example.com");

                    MvcResult result = mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
                return null;
            });
            futures.add(future);
        }

        // 等待所有任务完成，最多等待30秒
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("并发注册测试结果:");
        System.out.println("并发用户数: " + CONCURRENT_USERS);
        System.out.println("成功注册数: " + successCount.get());
        System.out.println("失败注册数: " + failureCount.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每次注册耗时: " + (double) duration / CONCURRENT_USERS + "ms");

        assertTrue(completed, "并发测试未在预期时间内完成");
        assertTrue(successCount.get() > 0, "没有成功的注册");
        assertTrue(failureCount.get() < CONCURRENT_USERS * 0.1, "失败率过高: " + 
                   (double) failureCount.get() / CONCURRENT_USERS * 100 + "%");
    }

    @Test
    @DisplayName("并发登录测试")
    void testConcurrentLogin() throws Exception {
        // 先注册一个用户
        UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
                .andExpect(status().isOk());

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            executor.submit(() -> {
                try {
                    UserLoginRequest loginRequest = TestDataFactory.createValidLoginRequest();

                    MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("并发登录测试结果:");
        System.out.println("并发用户数: " + CONCURRENT_USERS);
        System.out.println("成功登录数: " + successCount.get());
        System.out.println("失败登录数: " + failureCount.get());
        System.out.println("总耗时: " + duration + "ms");

        assertTrue(completed, "并发测试未在预期时间内完成");
        assertTrue(successCount.get() > 0, "没有成功的登录");
        assertTrue(failureCount.get() < CONCURRENT_USERS * 0.1, "失败率过高");
    }

    @Test
    @DisplayName("内存使用测试")
    void testMemoryUsage() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        // 记录初始内存使用
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 执行大量操作
        int numberOfOperations = 1000;
        for (int i = 0; i < numberOfOperations; i++) {
            UserRegisterRequest request = TestDataFactory.createValidRegisterRequest();
            request.setUsername("memory_test_user" + i);
            request.setEmail("memory_test_user" + i + "@example.com");

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk());
            
            // 每100次操作检查一次内存
            if (i % 100 == 0) {
                System.gc(); // 建议垃圾回收
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = currentMemory - initialMemory;
                System.out.println("操作 " + i + " 后内存增长: " + memoryIncrease / 1024 / 1024 + " MB");
            }
        }
        
        // 最终内存检查
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryIncrease = finalMemory - initialMemory;
        
        System.out.println("内存使用测试结果:");
        System.out.println("初始内存: " + initialMemory / 1024 / 1024 + " MB");
        System.out.println("最终内存: " + finalMemory / 1024 / 1024 + " MB");
        System.out.println("总内存增长: " + totalMemoryIncrease / 1024 / 1024 + " MB");
        
        // 断言内存增长在合理范围内（例如不超过100MB）
        assertTrue(totalMemoryIncrease < 100 * 1024 * 1024, 
                   "内存增长过多: " + totalMemoryIncrease / 1024 / 1024 + " MB");
    }

    @Test
    @DisplayName("数据库连接池测试")
    void testDatabaseConnectionPool() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(100);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    // 执行需要数据库连接的操作
                    mockMvc.perform(get(BASE_URL + "/check-username")
                            .param("username", "test_user" + userId))
                            .andExpect(status().isOk());
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("数据库连接池测试结果:");
        System.out.println("成功操作数: " + successCount.get());
        System.out.println("失败操作数: " + failureCount.get());

        assertTrue(completed, "数据库连接池测试未在预期时间内完成");
        assertEquals(100, successCount.get(), "应该所有操作都成功");
        assertEquals(0, failureCount.get(), "不应该有失败的操作");
    }

    @Test
    @DisplayName("响应时间分布测试")
    void testResponseTimeDistribution() throws Exception {
        // 先注册一个用户
        UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
                .andExpect(status().isOk());

        List<Long> responseTimes = new ArrayList<>();
        int numberOfRequests = 100;

        for (int i = 0; i < numberOfRequests; i++) {
            UserLoginRequest loginRequest = TestDataFactory.createValidLoginRequest();
            
            long startTime = System.currentTimeMillis();
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isOk());
            long endTime = System.currentTimeMillis();
            
            responseTimes.add(endTime - startTime);
        }

        // 计算统计信息
        responseTimes.sort(Long::compareTo);
        long min = responseTimes.get(0);
        long max = responseTimes.get(responseTimes.size() - 1);
        long median = responseTimes.get(responseTimes.size() / 2);
        long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99 = responseTimes.get((int) (responseTimes.size() * 0.99));
        double average = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("响应时间分布测试结果:");
        System.out.println("最小响应时间: " + min + "ms");
        System.out.println("最大响应时间: " + max + "ms");
        System.out.println("平均响应时间: " + String.format("%.2f", average) + "ms");
        System.out.println("中位数响应时间: " + median + "ms");
        System.out.println("95%响应时间: " + p95 + "ms");
        System.out.println("99%响应时间: " + p99 + "ms");

        // 断言响应时间在合理范围内
        assertTrue(p95 < 1000, "95%的请求响应时间应该在1秒以内");
        assertTrue(p99 < 2000, "99%的请求响应时间应该在2秒以内");
        assertTrue(average < 500, "平均响应时间应该在500ms以内");
    }
}