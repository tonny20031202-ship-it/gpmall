package com.gpmall.user;

import com.gpmall.user.constants.SysRetCodeConstants;
import com.gpmall.user.dal.entitys.Member;
import com.gpmall.user.dal.persistence.MemberMapper;
import com.gpmall.user.dto.UserLoginRequest;
import com.gpmall.user.dto.UserLoginResponse;
import com.gpmall.user.services.UserLoginServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户登录服务测试类
 * 包含密码错误、用户不存在、系统异常、Token 相关测试
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class UserLoginServiceTest {

    @Mock
    private MemberMapper memberMapper;

    @InjectMocks
    private UserLoginServiceImpl userLoginService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * 场景1：用户不存在
     */
    @Test
    public void testLogin_UserNotExist() {
        // 准备请求数据
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName("nonexistent");
        request.setPassword("password123");

        // 模拟 memberMapper 返回空列表
        when(memberMapper.selectByExample(any(Example.class))).thenReturn(new ArrayList<>());

        // 执行登录
        UserLoginResponse response = userLoginService.login(request);

        // 验证结果
        assertNotNull(response);
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getCode(), response.getCode());
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getMessage(), response.getMsg());

        // 验证 memberMapper 被调用
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        log.info("用户不存在场景测试通过");
    }

    /**
     * 场景2：密码错误
     */
    @Test
    public void testLogin_PasswordError() {
        // 准备请求数据
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName("testuser");
        request.setPassword("wrongpassword");

        // 准备模拟数据
        Member member = new Member();
        member.setId(1L);
        member.setUsername("testuser");
        member.setPassword("e10adc3949ba59abbe56e057f20f883e"); // MD5("123456")
        member.setState(1);
        member.setIsVerified("Y");

        List<Member> memberList = new ArrayList<>();
        memberList.add(member);

        // 模拟 memberMapper 返回数据
        when(memberMapper.selectByExample(any(Example.class))).thenReturn(memberList);

        // 执行登录
        UserLoginResponse response = userLoginService.login(request);

        // 验证结果
        assertNotNull(response);
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getCode(), response.getCode());
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getMessage(), response.getMsg());

        // 验证 memberMapper 被调用
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        log.info("密码错误场景测试通过");
    }

    /**
     * 场景3：Redis不可用（模拟系统异常）
     * 虽然当前 UserLoginServiceImpl 未直接使用 Redis，但我们可以模拟 memberMapper 抛出异常
     */
    @Test
    public void testLogin_SystemException() {
        // 准备请求数据
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName("testuser");
        request.setPassword("password123");

        // 模拟 memberMapper 抛出异常
        when(memberMapper.selectByExample(any(Example.class))).thenThrow(new RuntimeException("Redis connection failed"));

        // 执行登录
        UserLoginResponse response = userLoginService.login(request);

        // 验证结果
        assertNotNull(response);
        // 这里应该返回系统错误码，根据 ExceptionProcessorUtils 的处理
        assertEquals(SysRetCodeConstants.SYSTEM_ERROR.getCode(), response.getCode());
        assertEquals(SysRetCodeConstants.SYSTEM_ERROR.getMessage(), response.getMsg());

        // 验证 memberMapper 被调用
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        log.info("系统异常场景测试通过");
    }

    /**
     * 场景4：正常登录获取Token
     */
    @Test
    public void testLogin_SuccessWithToken() {
        // 准备请求数据
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName("testuser");
        request.setPassword("123456");

        // 准备模拟数据
        Member member = new Member();
        member.setId(1L);
        member.setUsername("testuser");
        member.setPassword("e10adc3949ba59abbe56e057f20f883e"); // MD5("123456")
        member.setState(1);
        member.setIsVerified("Y");
        member.setFile("avatar.jpg");

        List<Member> memberList = new ArrayList<>();
        memberList.add(member);

        // 模拟 memberMapper 返回数据
        when(memberMapper.selectByExample(any(Example.class))).thenReturn(memberList);

        // 执行登录
        UserLoginResponse response = userLoginService.login(request);

        // 验证结果
        assertNotNull(response);
        assertEquals(SysRetCodeConstants.SUCCESS.getCode(), response.getCode());
        assertEquals(SysRetCodeConstants.SUCCESS.getMessage(), response.getMsg());
        assertNotNull(response.getToken());
        assertEquals(member.getId(), response.getId());
        assertEquals(member.getUsername(), response.getUsername());
        assertEquals(member.getFile(), response.getFile());

        // 验证 memberMapper 被调用
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        log.info("正常登录获取Token场景测试通过，Token: {}", response.getToken());
    }

    /**
     * 用户未激活场景（额外补充测试）
     */
    @Test
    public void testLogin_UserNotVerified() {
        // 准备请求数据
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName("testuser");
        request.setPassword("123456");

        // 准备模拟数据
        Member member = new Member();
        member.setId(1L);
        member.setUsername("testuser");
        member.setPassword("e10adc3949ba59abbe56e057f20f883e");
        member.setState(1);
        member.setIsVerified("N"); // 未激活

        List<Member> memberList = new ArrayList<>();
        memberList.add(member);

        // 模拟 memberMapper 返回数据
        when(memberMapper.selectByExample(any(Example.class))).thenReturn(memberList);

        // 执行登录
        UserLoginResponse response = userLoginService.login(request);

        // 验证结果
        assertNotNull(response);
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getCode(), response.getCode());
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getMessage(), response.getMsg());

        log.info("用户未激活场景测试通过");
    }
}
