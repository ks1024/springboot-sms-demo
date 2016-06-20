package alidayu;

/**
 * Created by kyan on 20/06/16.
 */
public class AlidayuConfig {

    private static final String apiurl = "http://gw.api.taobao.com/router/rest";
    private static final String appkey = "";//appkey
    private static final String secret = "";//secret
    private static final String smsType = "normal";//必填，短信类型
    private static final String smsFreeSignName = "";//必填，短信签名
    private static final String smsVericationCodeTemplate = "";//必填，验证码短信模板ID

    public static final String getAppkey() {
        return appkey;
    }

    public static final String getSecret() {
        return secret;
    }

    public static String getApiurl() {
        return apiurl;
    }

    public static String getSmsType() {
        return smsType;
    }

    public static String getSmsFreeSignName() {
        return smsFreeSignName;
    }

    public static String getSmsVericationCodeTemplate() {
        return smsVericationCodeTemplate;
    }
}
