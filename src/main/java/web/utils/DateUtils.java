package web.utils;

import java.util.Date;

/**
 * Created by kyan on 20/06/16.
 */
public class DateUtils {

    public static Integer getTime() {
        return time2int(new Date());
    }

    public static Integer time2int(Date date) { //s
        long time = date.getTime() /1000; //将毫秒转化成秒
        return (int)time;
    }

}
