import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
  Class for manipulation with universal event. This class presents absract
  event structure about current app's activity from mobile devices to web server
*/
public class Event implements Serializable {
    public static final String eventTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private String timestamp;

    private String userId = "";

        /** actual time of the event arrival */
    private Long inputTime;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public final String getTimestamp() {
        return timestamp;
    }

    public Long getInputTime() {
        return inputTime;
    }

    public void setInputTime() {
        inputTime = new Date().getTime();
    }

    public void setInputTime(Long inputTime) {
        this.inputTime = inputTime;
    }

    public int partition(int totalPartitions) {
        return Math.abs(this.userId.hashCode()) % totalPartitions;
    }
}
