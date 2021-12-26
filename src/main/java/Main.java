import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        ENHelper enHelper = new ENHelper("dom.en.cx");
        enHelper.auth("st1gven", "K59uwb6HWa");
        String page = enHelper.request( "GameScenario.aspx?gid=73362");
        System.out.println(new CordParser().paresPage(page));
    }
}
