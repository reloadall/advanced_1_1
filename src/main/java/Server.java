import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

public class Server {

    public static void main(String[] args) {
        try (
                var selector = Selector.open();
                var serverSocket = ServerSocketChannel.open()
        ) {
            var buffer = ByteBuffer.allocate(2048);
            serverSocket.configureBlocking(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 9998));
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
                var selectedKeys = selector.selectedKeys();
                var iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    var key = iterator.next();
                    if (key.isAcceptable()) {
                        register(selector, serverSocket);
                    }
                    if (key.isReadable()) {
                        reedAndAnswer(buffer, key);
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void reedAndAnswer(ByteBuffer buffer, SelectionKey key) throws IOException {
        var client = (SocketChannel) key.channel();
        client.read(buffer);
        var dto = createDto(buffer);
        var result = getResult(dto);
        buffer.clear();
        buffer = ByteBuffer.wrap(result.getBytes());
        client.write(buffer);
        client.close();
    }

    private static String getResult(CommandDto dto) {
        if (dto == null) {
            return "Wrong input data";
        }
        switch (dto.getCommand()) {
            case SUM:
                return calculateSum(dto.getArguments().get(0));
            case ZIP:
                return zipAllFiles(dto.getArguments().get(0));
            case DOWNLOAD:
                return downloadFiles(dto.getArguments());
            default:
                return "Unknown command" + dto.getCommand();
        }
    }

    private static String calculateSum(String s) {
        var end = Long.parseLong(s);
        return String.valueOf(
                LongStream.range(0, end)
                        .parallel()
                        .sum()
        );
    }

    private static String zipAllFiles(String uri) {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        Boolean result = commonPool.invoke(new Zipper(uri));
        if (result) {
            return "Done!";
        }
        return "Error";
    }

    private static String downloadFiles(List<String> arguments) {
        boolean result = true;
        String dir = "D:/tmp/";

        var executor = Executors.newFixedThreadPool(arguments.size());
        var tasks = new ArrayList<Future<Boolean>>(arguments.size());

        for (var uri : arguments) {
            var task = executor.submit(() -> download(new URL(uri), dir));
            tasks.add(task);
        }

        for (var task : tasks) {
            try {
                result = result && task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                result = false;
            }
        }

        if (result) {
            return "Done!";
        }
        return "Error";
    }

    private static boolean download(URL url, String dir) {
        String path = url.getPath();
        String filename = path.substring(path.lastIndexOf('/') + 1);
        try (
                ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(dir + filename)
        ) {
            fileOutputStream.getChannel()
                    .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static CommandDto createDto(ByteBuffer buffer) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer.array());
        try (ObjectInput in = new ObjectInputStream(bis)) {
            return (CommandDto) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        var client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }
}
