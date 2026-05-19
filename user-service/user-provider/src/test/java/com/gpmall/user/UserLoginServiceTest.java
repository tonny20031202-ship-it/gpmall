package com.gpmall.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gpmall.commons.tool.exception.BizException;
import com.gpmall.commons.tool.exception.ProcessException;
import com.gpmall.user.constants.SysRetCodeConstants;
import com.gpmall.user.dal.entitys.Member;
import com.gpmall.user.dal.persistence.MemberMapper;
import com.gpmall.user.dal.persistence.UserMapper;
import com.gpmall.user.dto.UserLoginRequest;
import com.gpmall.user.dto.UserLoginResponse;
import com.gpmall.user.services.UserLoginServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserLoginServiceTest {

    @InjectMocks
    private UserLoginServiceImpl userLoginService;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private UserMapper userMapper;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(UserLoginServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    public void loginShouldReturnUserOrPasswordErrorWhenPasswordIsWrong() {
        Member member = buildMember("correct-password", "Y");
        when(memberMapper.selectByExample(any(Example.class))).thenReturn(Collections.singletonList(member));

        UserLoginResponse response = userLoginService.login(buildRequest("demo-user", "wrong-password"));

        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getCode(), response.getCode());
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getMessage(), response.getMsg());
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        assertContainsLog(Level.INFO, "Begin UserLoginServiceImpl.login: request:");
        assertNoErrorLog();
    }

    @Test
    public void loginShouldReturnUserOrPasswordErrorWhenUserDoesNotExist() {
        when(memberMapper.selectByExample(any(Example.class))).thenReturn(Collections.emptyList());

        UserLoginResponse response = userLoginService.login(buildRequest("missing-user", "password"));

        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getCode(), response.getCode());
        assertEquals(SysRetCodeConstants.USERORPASSWORD_ERRROR.getMessage(), response.getMsg());
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        assertContainsLog(Level.INFO, "Begin UserLoginServiceImpl.login: request:");
        assertNoErrorLog();
    }

    @Test
    public void loginShouldWrapProcessExceptionWhenRedisIsUnavailable() {
        ProcessException exception = new ProcessException(SysRetCodeConstants.SYSTEM_ERROR.getCode(), "Redis 不可用");
        when(memberMapper.selectByExample(any(Example.class))).thenThrow(exception);

        UserLoginResponse response = userLoginService.login(buildRequest("demo-user", "password"));

        assertEquals(SysRetCodeConstants.SYSTEM_ERROR.getCode(), response.getCode());
        assertEquals("Redis 不可用", response.getMsg());
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        assertContainsLog(Level.ERROR, "UserLoginServiceImpl.login Occur Exception :");
        assertContainsLog(Level.ERROR, "Redis 不可用");
    }

    @Test
    public void loginShouldWrapBizExceptionWhenTokenIsOverwrittenRepeatedly() {
        BizException exception = new BizException(SysRetCodeConstants.DATA_REPEATED.getCode(), "Token 重复覆盖");
        when(memberMapper.selectByExample(any(Example.class))).thenThrow(exception);

        UserLoginResponse response = userLoginService.login(buildRequest("demo-user", "password"));

        assertEquals(SysRetCodeConstants.DATA_REPEATED.getCode(), response.getCode());
        assertEquals("Token 重复覆盖", response.getMsg());
        verify(memberMapper, times(1)).selectByExample(any(Example.class));
        assertContainsLog(Level.ERROR, "UserLoginServiceImpl.login Occur Exception :");
        assertContainsLog(Level.ERROR, "Token 重复覆盖");
    }

    private UserLoginRequest buildRequest(String userName, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }

    private Member buildMember(String plainPassword, String isVerified) {
        Member member = new Member();
        member.setId(1L);
        member.setUsername("demo-user");
        member.setPassword(DigestUtils.md5DigestAsHex(plainPassword.getBytes()));
        member.setState(1);
        member.setFile("avatar.png");
        member.setIsVerified(isVerified);
        return member;
    }

    private void assertNoErrorLog() {
        assertTrue(getMessages(Level.ERROR).isEmpty());
    }

    private void assertContainsLog(Level level, String expectedMessage) {
        assertTrue(getMessages(level).stream().anyMatch(message -> message.contains(expectedMessage)));
    }

    private List<String> getMessages(Level level) {
        return listAppender.list.stream()
                .filter(event -> level.equals(event.getLevel()))
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }
}
