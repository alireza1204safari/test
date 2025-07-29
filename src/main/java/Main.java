    import controller.*;
    import com.sun.net.httpserver.HttpServer;

    import java.net.InetSocketAddress;

    public class Main {
        public static void main(String[] args) throws Exception {
    //        int port = 8090;
    //        ChatWebSocketServer server = new ChatWebSocketServer(port);
    //        server.start();
    //        System.out.println("WebSocket server started on port " + port);

            HttpServer server2 = HttpServer.create(new InetSocketAddress(9090), 0);

            server2.createContext("/auth", new AuthHandler());
            server2.createContext("/restaurants", new RestaurantHandler());
            server2.createContext("/vendors", new VendorHandler());
            server2.createContext("/items", new ItemHandler());
            server2.createContext("/coupons",new CouponHandler());
            server2.createContext("/orders" , new OrderHandler());
            server2.start();

        }
    }
