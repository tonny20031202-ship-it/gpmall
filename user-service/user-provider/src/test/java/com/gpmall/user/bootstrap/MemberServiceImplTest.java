package com.gpmall.user.bootstrap;

import com.gpmall.user.constants.SysRetCodeConstants;
import com.gpmall.user.dto.QueryMemberRequest;
import com.gpmall.user.dto.QueryMemberResponse;
import com.gpmall.user.services.IMemberService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MemberServiceImplTest {

    @Autowired
    IMemberService memberService;

    @Test
    public void testQueryMemberById() {
        QueryMemberRequest request = new QueryMemberRequest();
        request.setUserId(1L);

        QueryMemberResponse response = memberService.queryMemberById(request);

        log.info("testQueryMemberById response: code={}, msg={}", response.getCode(), response.getMsg());
        log.info("testQueryMemberById member data: id={}, username={}", response.getId(), response.getUsername());

        assertNotNull("Response should not be null", response);
        assertEquals("Response code should be success", SysRetCodeConstants.SUCCESS.getCode(), response.getCode());
        assertNotNull("Member data should not be null", response.getId());
    }

    @Test
    public void testQueryMemberById_UserNotExist() {
        QueryMemberRequest request = new QueryMemberRequest();
        request.setUserId(999999L);

        QueryMemberResponse response = memberService.queryMemberById(request);

        log.info("testQueryMemberById_UserNotExist response: code={}, msg={}", response.getCode(), response.getMsg());

        assertNotNull("Response should not be null", response);
        assertNotNull("Response code should not be null", response.getCode());
    }

    @Test(expected = Exception.class)
    public void testQueryMemberById_NullUserId() {
        QueryMemberRequest request = new QueryMemberRequest();
        memberService.queryMemberById(request);
    }
}
