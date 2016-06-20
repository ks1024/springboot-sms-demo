package web;

import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import alidayu.AlidayuConfig;
import geetest.GeetestConfig;
import geetest.GeetestLib;
import web.utils.DateUtils;
import web.utils.RandomSecurityCodeUtils;

/**
 * Created by kyan on 20/06/16.
 */
@Controller
@EnableAutoConfiguration
@RequestMapping("/sms")
public class SmsController {

    private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);
    private static final String SECURITY_CODE = "SECURITY_CODE";
    private static final String PHONE_NUMBER = "PHONE_NUMBER";
    private static final String SEND_CODE_TIME = "SEND_CODE_TIME";
    private static final int EXPIRE_SECONDS = 300;//5分钟

    /**
     * 初始化Geetest的Captcha
     * 添加Captcha为了防止恶意注册
     * @param request
     * @param response
     * @return
     * @throws ServletException
     * @throws IOException
     */
    @RequestMapping("/initCaptcha")
    @ResponseBody
    public String initCaptcha(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        GeetestLib gtSdk = new GeetestLib(GeetestConfig.getCaptcha_id(), GeetestConfig.getPrivate_key());

        String resStr = "{}";

        //自定义userid
        String userid = "test";

        //进行验证预处理
        int gtServerStatus = gtSdk.preProcess(userid);

        //将服务器状态设置到session中
        request.getSession().setAttribute(gtSdk.gtServerStatusSessionKey, gtServerStatus);
        //将userid设置到session中
        request.getSession().setAttribute("userid", userid);

        resStr = gtSdk.getResponseStr();

        return resStr;
    }

    /**
     * 验证Captcha，如果验证通过发送短信验证码
     * 不需要考虑短信轰炸，阿里大鱼后台已经处理这种恶意行为，对同一个手机号码发送短信验证码，允许每分钟1条，累积每小时7条，
     * 所以我们只需要生成验证码，并且存入session中，设置5分钟的时效性。如果5分钟之内再次请求验证码，则返回相同的验证码；
     * 否则重新生成验证码，重新设置5分钟的时效性。
     * @param request
     * @param response
     * @return
     * @throws ServletException
     * @throws IOException
     * @throws ApiException
     */
    @RequestMapping("/sendCode")
    @ResponseBody
    public String verifyCaptchaAndSendCode(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, ApiException {
        //获取用户手机号
        String phoneNumber = request.getParameter(PHONE_NUMBER);
        //初始化验证码
        String securityCode = "";
        //****************验证captcha****************
        LOG.info("verify captcha begin : phone " + phoneNumber);
        GeetestLib gtSdk = new GeetestLib(GeetestConfig.getCaptcha_id(), GeetestConfig.getPrivate_key());
        String challenge = request.getParameter(GeetestLib.fn_geetest_challenge);
        String validate = request.getParameter(GeetestLib.fn_geetest_validate);
        String seccode = request.getParameter(GeetestLib.fn_geetest_seccode);
        //从session中获取gt-server状态
        int gt_server_status_code = (Integer) request.getSession().getAttribute(gtSdk.gtServerStatusSessionKey);
        //从session中获取userid
        String userid = (String)request.getSession().getAttribute("userid");
        int gtResult = 0;
        if (gt_server_status_code == 1) {
            //gt-server正常，向gt-server进行二次验证
            gtResult = gtSdk.enhencedValidateRequest(challenge, validate, seccode, userid);
            //System.out.println(gtResult);
        } else {
            // gt-server非正常情况下，进行failback模式验证
            System.out.println("failback:use your own server captcha validate");
            gtResult = gtSdk.failbackValidateRequest(challenge, validate, seccode);
            //System.out.println(gtResult);
        }
        //********************************************
        if (gtResult == 1) {
            // captcha验证成功，发送短信
            LOG.info("verify captcha success for phone number : " + phoneNumber);
            //取得用户Session
            HttpSession session = request.getSession();
            if (session != null && !phoneNumber.isEmpty()) {
                if (session.getAttribute(PHONE_NUMBER) != null
                        && phoneNumber.equals(session.getAttribute(PHONE_NUMBER))
                        && session.getAttribute(SEND_CODE_TIME) != null
                        && DateUtils.getTime() - (int)session.getAttribute(SEND_CODE_TIME) < EXPIRE_SECONDS) {
                    //如果是同一个手机号且验证码未过期，从session中获取验证码
                    securityCode = (String)session.getAttribute(SECURITY_CODE);
                    LOG.info("get old security code " + securityCode + " for phone " + phoneNumber);
                } else {
                    //否则重新生成一个验证码，然后存入session中
                    char[] charCode = RandomSecurityCodeUtils.getSecurityCode(6, RandomSecurityCodeUtils.SecurityCodeLevel.Simple, false);
                    securityCode = String.valueOf(charCode);
                    LOG.info("generate new security code " + securityCode + " for phone " + phoneNumber);
                    //将系统生成的验证码添加入session
                    session.setAttribute(PHONE_NUMBER, phoneNumber);
                    session.setAttribute(SECURITY_CODE, securityCode);
                    session.setAttribute(SEND_CODE_TIME, DateUtils.getTime());
                }
            }
            TaobaoClient client = new DefaultTaobaoClient(AlidayuConfig.getApiurl(), AlidayuConfig.getAppkey(), AlidayuConfig.getSecret());
            AlibabaAliqinFcSmsNumSendRequest req = new AlibabaAliqinFcSmsNumSendRequest();
            req.setSmsType(AlidayuConfig.getSmsType());
            req.setSmsFreeSignName(AlidayuConfig.getSmsFreeSignName());
            req.setSmsParamString(new StringBuilder("{\"code\":\"").append(securityCode).append("\"}").toString());
            req.setRecNum(request.getParameter("phone"));
            req.setSmsTemplateCode(AlidayuConfig.getSmsVericationCodeTemplate());
            AlibabaAliqinFcSmsNumSendResponse rsp = client.execute(req);
            if (rsp.getResult() != null && rsp.getResult().getSuccess()) {
                //如果验证码发送成功
                return "验证码发送成功";
            } else {
                //验证码发送失败
                if (rsp.getSubCode().equals("isv.BUSINESS_LIMIT_CONTROL")) {
                    //触发业务流控限制
                    return "验证码发送过于频繁，请稍后再重试";
                }
                return "验证码发送失败";
            }
        } else {
            LOG.error("verify captcha error for phone number : " + phoneNumber);
            return "验证captcha失败";
        }
    }

    @RequestMapping("/validate")
    @ResponseBody
    public String validate(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String phoneNumber = (String)session.getAttribute(PHONE_NUMBER);
        String securityCode = (String)session.getAttribute(SECURITY_CODE);
        String inputPhone = request.getParameter("phone");
        String inputCode = request.getParameter("code");
        if (inputPhone.equals(phoneNumber) && inputCode.equals(securityCode)) {
            if (DateUtils.getTime() - (int)session.getAttribute(SEND_CODE_TIME) > EXPIRE_SECONDS) {
                //验证码过期
                return "failure";
            } else {
                //验证成功
                session.removeAttribute(PHONE_NUMBER);
                session.removeAttribute(SECURITY_CODE);
                session.removeAttribute(SEND_CODE_TIME);
                return "success";
            }
        } else {
            return "failure";
        }
    }

}
