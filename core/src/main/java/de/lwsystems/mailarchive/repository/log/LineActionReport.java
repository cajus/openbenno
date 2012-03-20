package de.lwsystems.mailarchive.repository.log;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;



/**
 *
 * @author wiermer
 */
public class LineActionReport implements ActionReport{

    Date date;
    String actionName;
    String checksum;
    ActionStatus status;
    final static String delim="\t";
    final static DateFormat dateFormat=new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
    public LineActionReport(Date d,String an, String checksum,ActionStatus status) {
        date=d;
        actionName=an;
        this.checksum=checksum;
        this.status=status;
    }
    public String getActionName() {
        return actionName;
    }

    public String getChecksum() {
        return checksum;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public String marshal() {
        StringBuilder s=new StringBuilder();
        s.append(dateFormat.format(getDate()));
        s.append(delim);
        s.append(getActionName());
        s.append(delim);
        s.append(getChecksum());
        s.append(delim);
        if (getStatus().equals(ActionStatus.SUCCESS))
            s.append("SUCCESS");
        else if (getStatus().equals(ActionStatus.FAILED))
            s.append("FAILED");
        else
            s.append("DUPLICATE");
        return s.toString();


    }

    public static LineActionReport parse(String s) throws ParseException {
        String[] elems=s.split(delim);
        if (elems.length<4)
            throw new ParseException("Not enough fields in Line", 0);
        Date d=dateFormat.parse(elems[0]);
        String an=elems[1];
        String chks=elems[2];
        ActionStatus as;
        if (elems[3].equals("SUCCESS"))
            as=ActionStatus.SUCCESS;
        else if (elems[3].equals("FAILED"))
            as=ActionStatus.FAILED;
        else
            as=ActionStatus.DUPLICATE;

        return new LineActionReport(d, an, chks, as);

    }

    public Date getDate() {
      return date;
    }
    @Override
    public String toString() {
        return marshal();
    }
}
