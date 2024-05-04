import org.cd.httpnet.HttpClient;

public class httptest {
    public  static  void  main(String[]v)
    {
        String url="https://wwww.baidu.com";
       var str= HttpClient.doGet(url);
       System.out.println(str);
    }
}
